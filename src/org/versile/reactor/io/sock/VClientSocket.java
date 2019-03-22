/**
 * Copyright (C) 2012-2013 Versile AS
 *
 * This file is part of Versile Java.
 *
 * Versile Java is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.versile.reactor.io.sock;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import org.versile.common.peer.VSocketPeer;
import org.versile.reactor.VConnectingHandler;
import org.versile.reactor.VIOHandler;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;


/**
 * Handler for reactor-based client socket communication.
 */
public abstract class VClientSocket extends VIOHandler implements VConnectingHandler {

	/**
	 * Associated socket channel.
	 */
	protected SocketChannel channel;
	/**
	 * Client socket configuration parameters.
	 */
	protected VClientSocketConfig config;
	/**
	 * Buffer length of socket read/write operations.
	 */
	protected int bufLen;
	ByteBuffer buffer;
	boolean was_connected;
	boolean delayed_read_start = false;
	boolean delayed_write_start = false;
	/**
	 * True if socket input has been closed.
	 */
	protected boolean closedInput = false;
	/**
	 * True if socket output has been closed.
	 */
	protected boolean closedOutput = false;
	VSocketPeer _peer = null;

	/**
	 * Set up client socket handler.
	 *
	 * <p>The provided 'channel' must be set up with non-blocking mode.</p>
	 *
	 * <p>The 'connected' argument must be set to reflect the channel's current
	 * connectivity. If the channel is connected it must be set to true. If
	 * the channel has not been connected then it must later be connected with
	 * {@link #connect}.</p>
	 *
	 * @param reactor owning reactor
	 * @param channel client socket channel
	 * @param connected if true the socket is already connected
	 * @param config socket configuration parameters (or null)
	 */
	public VClientSocket(VReactor reactor, SocketChannel channel,
						 boolean connected, VClientSocketConfig config) {
		super(reactor);
		this.channel = channel;
		if (config == null)
			config = new VClientSocketConfig();
		this.config = config;
		this.bufLen = config.getBufferLength();
		buffer = ByteBuffer.allocate(bufLen);
		this.was_connected = connected;
		if (connected)
			this.connected();
	}

	/**
	 * Connect client socket.
	 *
	 * <p>Should only be called if the client socket has not already been connected.</p>
	 *
	 * <p>Method is thread safe and can be called from outside reactor thread.</p>
	 *
	 * @param address target address
	 * @throws IOException error initializing connect
	 */
	public void connect(SocketAddress address)
		throws IOException {
		this.connect(address, false);
	}

	/**
	 * Connect client socket.
	 *
	 * <p>Should only be called if the client socket has not already been connected.</p>
	 *
	 * @param address target address
	 * @param safe false unless known to be called from reactor thread
	 * @throws IOException error initializing connect
	 */
	public void connect(SocketAddress address, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VClientSocket sock;
				SocketAddress address;
				public Func(VClientSocket sock, SocketAddress address) {
					this.sock = sock;
					this.address = address;
				}
				@Override
				public Object execute() throws Exception {
					sock.connect(address, true);
					return null;
				}
			}
			reactor.schedule(new Func(this, address));
			return;
		}
		if (!this.canConnect(address))
			throw new IOException("Not allowed to connect to provided address");
		channel.connect(address);
		this.startHandlingConnect();
	}

	/**
	 * Check whether connecting to address is allowed.
	 *
	 * <p>Called internally before making a client connection. Default returns true,
	 * derived classes can override.</p>
	 *
	 * @param address address to validate
	 * @return true if connecting is allowed
	 */
	public boolean canConnect(SocketAddress address) {
		return true;
	}

	/**
	 * Read data from socket (non-blocking).
	 *
	 * @param max_read max bytes to read
	 * @return data read
	 * @throws ClosedChannelException channel was closed
	 * @throws IOException channel read operation error
	 */
	public byte[] readSome(int max_read)
		throws IOException {
		if (max_read == 0)
			return new byte[0];
		if (max_read >= buffer.capacity())
			max_read = buffer.capacity();
		int bpos = buffer.capacity() - max_read;
		buffer.position(bpos);
		int num_read = channel.read(buffer);
		if (num_read < 0)
			throw new ClosedChannelException();
		byte[] data = new byte[num_read];
		System.arraycopy(buffer.array(), bpos, data, 0, num_read);
		return data;
	}

	/**
	 * Write data to socket (non-blocking).
	 *
	 * @param data data to write
	 * @return number of bytes written
	 * @throws ClosedChannelException channel was closed
	 * @throws IOException channel read operation error
	 */
	public int writeSome(byte[] data)
			throws IOException {
		int num_write = data.length;
		if (num_write == 0)
			return 0;
		if (num_write > buffer.capacity())
			num_write = buffer.capacity();
		int bpos = buffer.capacity() - num_write;
		System.arraycopy(data, 0, buffer.array(), bpos, num_write);
		buffer.position(bpos);
		return channel.write(buffer);
	}

	/**
	 * Close socket (input and output).
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeIO(boolean clean) {
		this.closeInput(clean);
		this.closeOutput(clean);
	}

	/**
	 * Close socket input.
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeInput(boolean clean) {
		if (closedInput)
			return;
		this.stopReading();
		try {
			channel.socket().shutdownInput();
			reactor.log("Socket: closed input");
			if (!closedOutput && !config.allowInputHalfClose())
				this.closeOutput(clean);
			if (closedOutput) {
				channel.socket().close();
				reactor.log("Socket: closed");
				this.closedCallback();
			}
		} catch (IOException e) {
			reactor.log("Socket: closed input (with error)");
			// SILENT
		} finally {
			closedInput = true;
			this.inputClosed(clean);
		}
	}

	/**
	 * Close socket output.
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeOutput(boolean clean) {
		if (closedOutput)
			return;
		this.stopWriting();
		try {
			channel.socket().shutdownOutput();
			reactor.log("Socket: closed output");
			if (!closedInput && !config.allowOutputHalfClose())
				this.closeInput(clean);
			if (closedInput) {
				channel.socket().close();
				reactor.log("Socket: closed");
				this.closedCallback();
			}
		} catch (IOException e) {
			reactor.log("Socket: closed output (with error)");
			// SILENT
		} finally {
			closedOutput = true;
			this.outputClosed(clean);
		}
	}

	/**
	 * Called internally when socket input is closed.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 *
	 * @param clean if true input was closed cleanly
	 */
	protected void inputClosed(boolean clean) {

	}

	/**
	 * Called internally when socket output is closed.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 *
	 * @param clean if true input was closed cleanly
	 */
	protected void outputClosed(boolean clean) {
	}

	/**
	 * Called internally when socket is connected.
	 *
	 * <p>Sets the socket's connected peer. Derived classes can override, but
	 * should call the method on 'super' first.</p>
	 */
	protected void connected() {
		_peer = VSocketPeer.fromAddress(channel.socket().getRemoteSocketAddress());
		reactor.log("Socket: connected to " + _peer.getAddress());
	}

	@Override
	public void startReading()
			throws IOException {
		if (!was_connected)
			delayed_read_start = true;
		else if (!closedInput)
			super.startReading();
	}

	@Override
	public void startWriting()
			throws IOException {
		if (!was_connected)
			delayed_write_start = true;
		else if (!closedOutput)
			super.startWriting();
	}

	@Override
	public void stopReading() {
		if (!was_connected)
			delayed_read_start = false;
		else
			super.stopReading();
	}

	@Override
	public void stopWriting() {
		if (!was_connected)
			delayed_write_start = false;
		else
			super.stopWriting();
	}

	@Override
	public void handleConnect() {
		if (!was_connected) {
			was_connected = true;
			try {
				channel.finishConnect();
				this.connected();
				if (delayed_read_start)
					this.startReading();
				if (delayed_write_start)
					this.startWriting();
				delayed_read_start = false;
				delayed_write_start = false;
			} catch (IOException e) {
				// Close if connect fails
				this.closeIO(false);
			}
		}
		this.stopHandlingConnect();
	}

	@Override
	public void startHandlingConnect() throws IOException {
		if (!was_connected)
			this.getReactor().startHandlingConnect(this);
	}

	@Override
	public void stopHandlingConnect() {
		this.getReactor().stopHandlingConnect(this);
	}

	@Override
	public SelectableChannel getChannel() {
		return channel;
	}

	/**
	 * Get the socket's connected peer.
	 *
	 * @return socket peer (null if not connected)
	 */
	public VSocketPeer getPeer() {
		return _peer;
	}

	void closedCallback() {
		if (config.getClosedCallback() != null) {
			class Function implements VReactorFunction {
				Runnable callback;
				public Function(Runnable callback) {
					this.callback = callback;
				}
				@Override
				public Object execute() throws Exception {
					try {
						callback.run();
					} catch (Exception e) {
						reactor.log("Socket: socket close callback failed");
						// SILENT
					}
					return null;
				}
			}
			reactor.schedule(new Function(config.getClosedCallback()));
		}
	}
}
