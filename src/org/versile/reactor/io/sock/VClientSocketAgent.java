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
import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import org.versile.common.peer.VPeer;
import org.versile.common.peer.VSocketPeer;
import org.versile.common.util.VByteBuffer;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.VConsumer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;


/**
 * Byte producer/consumer interface to a client socket.
 *
 * <p>Implements {@link org.versile.reactor.io.VByteProducer} and
 * {@link org.versile.reactor.io.VByteConsumer} interfaces to client socket I/O.
 * The consumer interface is available from {@link #getConsumer} and the
 * producer interface is available from {@link #getProducer}.</p>
 */
public class VClientSocketAgent extends VClientSocket {

	WeakReference<SocketConsumer> consumer;
	WeakReference<SocketProducer> producer;

	VByteProducer _c_producer = null;
	boolean _c_eod = false;
	boolean _c_eod_clean = false;
	long _c_consumed = 0L;
	long _c_lim_sent = 0L;
	VByteBuffer _c_buffer;
	boolean _c_aborted = false;

	VByteConsumer _p_consumer = null;
	boolean _p_closed = false;
	long _p_produced = 0L;
	long _p_prod_lim = 0L;
	VByteBuffer _p_buffer;
	boolean _p_aborted = false;

	/**
	 * Set up client socket consumer/producer.
	 *
	 * @see VClientSocket#VClientSocket(VReactor, SocketChannel, boolean, VClientSocketConfig)
	 */
	public VClientSocketAgent(VReactor reactor, SocketChannel channel,
			boolean connected, VClientSocketConfig config) {
		super(reactor, channel, connected, config);
		consumer = null;
		producer = null;

		_c_buffer = new VByteBuffer();
		_p_buffer = new VByteBuffer();
	}

	/**
	 * Get consumer interface to the socket.
	 *
	 * @return byte consumer interface
	 */
	public VByteConsumer getConsumer() {
		SocketConsumer result = null;
		if (consumer != null)
			result = consumer.get();
		if (result == null) {
			result = new SocketConsumer(this);
			consumer = new WeakReference<SocketConsumer>(result);
		}
		return result;
	}

	/**
	 * Get producer interface to the socket.
	 *
	 * @return byte producer interface
	 */
	public VByteProducer getProducer() {
		SocketProducer result = null;
		if (producer != null)
			result = producer.get();
		if (result == null) {
			result = new SocketProducer(this);
			producer = new WeakReference<SocketProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to the socket.
	 *
	 * @return byte I/O pair
	 */
	public VByteIOPair getIOPair() {
		return new VByteIOPair(getConsumer(), getProducer());
	}

	@Override
	public boolean canConnect(SocketAddress address) {
		if (_p_consumer != null) {
			try {
				return _p_consumer.getControl().canConnect(VSocketPeer.fromAddress(address));
			} catch (VIOMissingControl e) {
				return true;
			}
		}
		else
			return true;
	}

	/**
	 * Get the socket's connected peer.
	 *
	 * @return socket peer (null if not connected)
	 */
	public VSocketPeer getPeer() {
		return _peer;
	}

	@Override
	protected void connected() {
		super.connected();
		if (_p_consumer != null) {
			class Func implements VReactorFunction {
				VPeer peer;
				public Func(VPeer peer) {
					this.peer = peer;
				}
				@Override
				public Object execute() throws Exception {
					try {
						_p_consumer.getControl().notifyConnected(peer);
					} catch (VIOMissingControl e) {
						// SILENT
					}
					return null;
				}
			}
			reactor.schedule(new Func(_peer));
		}
	}

	@Override
	public void doRead() {
		if (_p_consumer == null)
			this.stopReading();

		int max_read = bufLen - _p_buffer.length();
		if (_p_prod_lim >= 0)
			max_read = Math.max(max_read,  (int)(_p_prod_lim - _p_produced));
		if (max_read <= 0) {
			this.stopReading();
			return;
		}

		byte[] data = null;
		try {
			data = this.readSome(max_read);
		} catch (ClosedChannelException e) {
			this._p_abort();
			return;
		} catch (IOException e) {
			reactor.log("Socket: read error, aborting");
			this._p_abort();
			return;
		}

		if (data != null && data.length > 0) {
			_p_buffer.append(data);
			int old_len = _p_buffer.length();
			if (_p_prod_lim < 0 || _p_produced < _p_prod_lim) {
				try {
					long new_lim = _p_consumer.consume(_p_buffer);
					_p_produced += old_len - _p_buffer.length();
					_p_prod_lim = new_lim;
				} catch (IOException e) {
					reactor.log("Socket: producer error, aborting");
					this._p_abort();
					return;
				}
			}
		}
	}

	@Override
	public void doWrite() {
		if (_c_buffer.length() == 0) {
			this.stopWriting();
			return;
		}

		byte[] data = _c_buffer.peek(bufLen);
		try {
			int num_written = this.writeSome(data);
			if (num_written > 0) {
				_c_buffer.remove(num_written);
				if (_c_producer != null) {
					_c_lim_sent = (_c_consumed + bufLen - _c_buffer.length());
					_c_producer.canProduce(_c_lim_sent);
				}
			}
			if (_c_buffer.length() == 0) {
				this.stopWriting();
				if (_c_eod)
					this._c_abort();
			}
		} catch (ClosedChannelException e) {
			_c_abort();
			return;
		} catch (IOException e) {
			reactor.log("Socket: write error, aborting");
			_c_abort();
			return;
		}
	}

	@Override
	protected void inputClosed(boolean clean) {
		if (_p_consumer != null)
			this._p_consumer.endConsume(clean);
		else
			this._p_abort();
	}

	@Override
	protected void outputClosed(boolean clean) {
		this._c_abort();
	}

	long _c_consume(VByteBuffer data) throws IOException {
		if (_c_eod || (_c_producer == null) || (_c_lim_sent >= 0 && _c_consumed >= _c_lim_sent))
			throw new IOException("Consume error");
		else if (data.length() == 0)
			throw new IOException("No data");

		int max_cons = bufLen - _c_buffer.length();
		if (_c_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_c_lim_sent-_c_consumed));
		boolean was_empty = (_c_buffer.length() == 0);
		byte[] indata = data.pop(max_cons);
		_c_buffer.append(indata);
		_c_consumed += indata.length;
		if (was_empty)
			this.startWriting();
		return _c_lim_sent;
	}

	void _c_end_consume(boolean clean) {
		if (_c_eod)
			return;
		_c_eod = true;
		_c_eod_clean = clean;
		if (_c_buffer.length() == 0) {
			this.closeOutput(true);
			if (_c_producer != null) {
				_c_producer.abort();
				this._c_detach(true);
			}
		}
	}

	void _c_abort() {
		if (!_c_aborted) {
			_c_aborted = true;
			_c_eod = true;
			_c_consumed = 0L;
			_c_lim_sent = 0L;
			_c_buffer.clear();
			this.closeOutput(true);
			if (_c_producer != null) {
				_c_producer.abort();
				this._c_detach(true);
			}
		}
	}

	void _c_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_c_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_c_producer == producer)
			return;
		else if (_c_producer != null)
			throw new IOException("Producer already attached");
		_c_producer = producer;
		_c_consumed = 0L;
		_c_lim_sent = 0L;
		producer.attach(this.getConsumer(), true);
		_c_lim_sent = bufLen;
		producer.canProduce(_c_lim_sent);

		try {
			producer.getControl().notifyConsumerAttached(this.getConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _c_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_c_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_c_producer != null) {
			VByteProducer prod = _c_producer;
			_c_producer = null;
			prod.detach(true);
			_c_consumed = 0L;
			_c_lim_sent = 0L;
		}
	}

	VIOControl _c_get_control() {
		class Control extends VIOControl {
		}
		return new Control();
	}

	VByteProducer _c_get_producer() {
		return _c_producer;
	}

	void _p_can_produce(long limit) throws IOException {
		if (_p_consumer == null)
			throw new IOException("No connected consumer");

		if (limit < 0) {
			if (_p_prod_lim >= 0 && _p_produced >= _p_prod_lim)
				this.startReading();
			_p_prod_lim = limit;
		}
		else {
			if (_p_prod_lim >= 0 && _p_prod_lim < limit) {
				if (_p_produced >= _p_prod_lim)
					this.startReading();
				_p_prod_lim = limit;
			}
		}
	}

	void _p_abort() {
		if (!_p_aborted) {
			_p_aborted = true;
			_p_produced = 0L;
			_p_prod_lim = 0L;
			this.closeInput(true);
			if (_p_consumer != null) {
				_p_consumer.abort();
				this._p_detach(true);
			}
		}
	}

	void _p_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_p_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_p_consumer == consumer)
			return;
		else if (_p_consumer != null)
			throw new IOException("Consumer already attached");
		_p_consumer = consumer;
		_p_produced = 0L;
		_p_prod_lim = 0L;
		consumer.attach(this.getProducer(), true);

		try {
			consumer.getControl().notifyProducerAttached(this.getProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _p_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_p_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_p_consumer != null) {
			VByteConsumer cons = _p_consumer;
			_p_consumer = null;
			cons.detach();
			_p_produced = 0L;
			_p_prod_lim = 0L;
		}
	}

	VIOControl _p_get_control() {
		class Control extends VIOControl {
			VClientSocketAgent agent;
			public Control(VClientSocketAgent agent) {
				this.agent = agent;
			}
			@Override
			public void requestProducerState(VConsumer consumer) {
				if (agent._p_consumer != null && _peer != null) {
					class Func implements VReactorFunction {
						VPeer peer;
						public Func(VPeer peer) {
							this.peer = peer;
						}
						@Override
						public Object execute() throws Exception {
							try {
								_p_consumer.getControl().notifyConnected(peer);
							} catch (VIOMissingControl e) {
								// SILENT
							}
							return null;
						}
					}
					reactor.schedule(new Func(_peer));
				}
			}
		}
		return new Control(this);
	}

	VByteConsumer _p_get_consumer() {
		return _p_consumer;
	}


	class SocketConsumer implements VByteConsumer {

		VClientSocketAgent agent;

		public SocketConsumer(VClientSocketAgent agent) {
			this.agent = agent;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return agent._c_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			agent._c_end_consume(clean);
		}

		@Override
		public void abort() {
			agent._c_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			agent._c_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._c_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._c_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return agent._c_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}

	class SocketProducer implements VByteProducer {

		VClientSocketAgent agent;

		public SocketProducer(VClientSocketAgent agent) {
			this.agent = agent;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			agent._p_can_produce(limit);
		}

		@Override
		public void abort() {
			agent._p_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			agent._p_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._p_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._p_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return agent._p_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}

}
