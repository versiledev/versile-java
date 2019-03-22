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

package org.versile.reactor.service;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.versile.common.auth.VAuth;
import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.processor.VProcessor;
import org.versile.orb.link.VLinkAuth;
import org.versile.orb.link.VLinkException;
import org.versile.orb.service.VGatewayFactory;
import org.versile.orb.service.VService;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.sock.VClientSocket;
import org.versile.reactor.io.sock.VClientSocketAgent;
import org.versile.reactor.io.sock.VClientSocketConfig;
import org.versile.reactor.io.sock.VClientSocketFactory;
import org.versile.reactor.io.sock.VListeningSocket;


/**
 * Base class for reactor-based services.
 */
public abstract class VReactorService extends VService {

	VReactor reactor;
	boolean owns_reactor = false;
	VListeningSocket listener = null;
	VClientSocketFactory socket_factory = null;

	/**
	 * Set up reactor service.
	 *
	 * <p>For other constructor parameters see {@link org.versile.orb.service.VService}.</p>
	 *
	 * @param reactor owning reactor (or null)
	 * @throws VLinkException global org.versile.Versile copyleft info not configured
	 */
	public VReactorService(VGatewayFactory gwFactory, VPrivateCredentials credentials, VLinkAuth linkAuthorizer,
	        ServerSocketChannel channel, VProcessor processor, VReactor reactor, VReactorServiceConfig config)
			throws IOException, VLinkException {
		super(gwFactory, credentials, linkAuthorizer, channel, processor, config);

		// Override config set on parent if config was null
		if (config == null) {
			config = new VReactorServiceConfig();
			this.config = config;
		}

		if (reactor == null) {
			owns_reactor = true;
			reactor = new VReactor(config.getReactorLogger());
		}
		this.reactor = reactor;
	}

	@Override
	public synchronized void start()
			throws IOException {
		if (owns_reactor)
			reactor.start();

		socket_factory = this.createSocketFactory();

		if (channel == null) {
			channel = this.createListeningSocket();
		}

		listener = new VListeningSocket(reactor, channel, socket_factory, null);
		this.activate();
	}

	/**
	 * Get the reactor registered on the service.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	/**
	 * Create a reactor I/O chain factory for accepted connections.
	 *
	 * @return client socket factory
	 */
	protected VClientSocketFactory createSocketFactory() {
		class Factory implements VClientSocketFactory {
			@Override
			public VClientSocket build(VReactor reactor, SocketChannel channel,
					Runnable closedCallback) throws IOException {

				// Get a reference to service configuration of appropriate type
				VReactorServiceConfig service_conf = (VReactorServiceConfig) config;

				// Generate a socket configuration object and override "close callback"
				VClientSocketConfig sock_config = null;
				if (service_conf.getSocketConfig() != null)
					sock_config = service_conf.getSocketConfig().clone();
				else
					sock_config = new VClientSocketConfig();
				sock_config.setClosedCallback(closedCallback);

				// Set up client socket agent
				VClientSocketAgent client_sock = new VClientSocketAgent(reactor, channel, true, sock_config);

				// Perform socket peer validation
				VAuth peer_auth = service_conf.getPeerAuthorizer();
				if (peer_auth != null)
					if (client_sock.getPeer() == null || !peer_auth.acceptPeer(client_sock.getPeer())) {
						client_sock.closeIO(false);
						throw new IOException("Socket peer was rejected by service peer authorizer");
					}

				ByteAgent agent = createByteAgent();
				client_sock.getIOPair().attach(agent.getIOPair());
				return client_sock;
			}
		}
		return new Factory();
	}

	/**
	 * Generate listening socket bound to protocol's default port.
	 *
	 * @return bound listening socket
	 * @throws IOException could not set up socket
	 */
	protected abstract ServerSocketChannel createListeningSocket()
			throws IOException;

	@Override
	protected void schedule(Runnable job) {
		class ReactorJob implements VReactorFunction {
			Runnable job;
			public ReactorJob(Runnable job) {
				this.job = job;
			}
			@Override
			public Object execute() throws Exception {
				job.run();
				return null;
			}
		}
		reactor.schedule(new ReactorJob(job));
	}

	@Override
	protected void stopListener() {
		synchronized(statusLock) {
			if (active) {
				listener.closeIO(true);
				active = false;
				statusLock.notifyAll();
			}
		}
	}

	@Override
	protected void stopThreads() {
		synchronized(this) {
			if (owns_reactor)
				reactor.stopReactor();
			if (ownsProcessor)
				processor.shutdown();
		}
	}

	/**
	 * Generate external byte producer/consumer pair for new link.
	 *
	 * <p>Called internally to generate a byte producer/consumer pair for a new
	 * link, which are typically connected to a client socket agent.</p>
	 *
	 * @return producer and consumer
	 * @throws IOException error creating byte agent
	 */
	protected abstract ByteAgent createByteAgent()
			throws IOException;

	/**
	 * Holds a byte consumer/producer pair.
	 */
	static public class ByteAgent {
		VByteConsumer consumer;
		VByteProducer producer;
		public ByteAgent(VByteConsumer consumer, VByteProducer producer) {
			this.consumer = consumer;
			this.producer = producer;
		}
		/**
		 * Get byte consumer.
		 *
		 * @return consumer
		 */
		public VByteConsumer getConsumer() {
			return consumer;
		}
		/**
		 * Get byte producer.
		 *
		 * @return producer
		 */
		public VByteProducer getProducer() {
			return producer;
		}

		/**
		 * Get byte I/O pair.
		 *
		 * @return I/O pair
		 */
		public VByteIOPair getIOPair() {
			return new VByteIOPair(getConsumer(), getProducer());
		}
	}

}
