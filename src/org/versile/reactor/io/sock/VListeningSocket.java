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
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.versile.reactor.VAcceptingHandler;
import org.versile.reactor.VHandler;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;


/**
 * Handler for a listening socket.
 *
 * <p>Holds a bound listening socket. The handler receives and handles
 * "accept" events from the owning reactor.</p>
 */
public class VListeningSocket implements VHandler, VAcceptingHandler {
	VReactor reactor;
	ServerSocketChannel schannel;
	VClientSocketFactory factory;
	VListeningSocketConfig config;

	boolean controlled;
	boolean can_accept = true;

	/**
	 * Set up listening socket.
	 *
	 * <p>Constructing the listener will automatically register the listener with the
	 * owning reactor, unless the listener is configured as being "controlled".</p>
	 *
	 * <p>In order enable using with the server socket channel with the reactor framework
	 * the constructor sets the channel to a non-blocking mode.</p>
	 *
	 * @param reactor socket's owning reactor
	 * @param schannel channel for listening on new connections
	 * @param factory factory for reactor client sockets for new connections
	 * @param config listening socket configuration (or null)
	 * @throws IOException error setting up listening socket
	 */
	public VListeningSocket(VReactor reactor, ServerSocketChannel schannel,
							VClientSocketFactory factory, VListeningSocketConfig config)
			throws IOException {
		this.reactor = reactor;
		schannel.configureBlocking(false);
		this.schannel = schannel;
		this.factory = factory;
		if (config == null)
			config = new VListeningSocketConfig();
		this.config = config;

		controlled = config.isControlled();
		if (controlled)
			can_accept = false;

		if (can_accept) {
			class Function implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					startHandlingAccept();
					return null;
				}
			}
			reactor.schedule(new Function());
		}
	}

	@Override
	public SelectableChannel getChannel() {
		return schannel;
	}

	@Override
	public void handleAccept() {
		if (!can_accept)
			this.stopHandlingAccept();

		SocketChannel client_sock = null;
		try {
			client_sock = schannel.accept();
			SocketAddress _address = client_sock.socket().getRemoteSocketAddress();
			reactor.log("Listener: accepted connection from " + _address);
			client_sock.configureBlocking(false);
		} catch (IOException e) {
			reactor.log("Listener: Connection dropped before accept() could complete");
		}

		if (controlled) {
			can_accept = false;
			this.stopHandlingAccept();
		}
		this.accepted(client_sock);
		if (config.getAcceptCallback() != null)
			try {
				config.getAcceptCallback().run();
			} catch (Exception e) {
				reactor.log("Listener: Error performing accept() callback");
			}
	}

	@Override
	public void startHandlingAccept() throws IOException {
		reactor.startHandlingAccept(this);
	}

	@Override
	public void stopHandlingAccept() {
		reactor.stopHandlingAccept(this);
	}

	/**
	 * Called internally when a connection is accepted.
	 *
	 * @param channel
	 */
	protected void accepted(SocketChannel channel) {
		try {
			// No references are retained here to the constructed client socket, the chain is
			// responsible for ensuring it is referenced during its life span
			factory.build(reactor, channel, config.getClosedCallback());
		} catch (IOException e) {
			reactor.log("Listener: Error executing post-accept() handler");
		}
	}

	/**
	 * Enable a single socket accept for controlled listener.
	 *
	 * <p>Thread-safe.</p>
	 */
	public void enableAccept() {
		this.enableAccept(false);
	}

	/**
	 * Enable a single socket accept for controlled listener.
	 *
	 * @param safe false unless known to be executed from reactor thread
	 */
	public void enableAccept(boolean safe) {
		if (!controlled)
			return;
		if (!safe) {
			class Function implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					enableAccept(true);
					return null;
				}
			}
			reactor.schedule(new Function());
			return;
		}
		if (!can_accept) {
			can_accept = true;
			try {
				startHandlingAccept();
			} catch (IOException e) {
				// SILENT
			}
		}
	}

	/**
	 * Closes the listening socket.
	 *
	 * @param clean if true socket is closed cleanly
	 */
	public void closeIO(boolean clean) {
		this.stopHandlingAccept();
		try {
			schannel.socket().close();
		} catch (IOException e) {
			// SILENT
		}
	}
}
