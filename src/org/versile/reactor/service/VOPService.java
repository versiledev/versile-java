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
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.processor.VProcessor;
import org.versile.crypto.VCryptoException;
import org.versile.orb.entity.VObject;
import org.versile.orb.link.VLinkAuth;
import org.versile.orb.link.VLinkException;
import org.versile.orb.service.VGatewayFactory;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.link.VLinkAgent;
import org.versile.reactor.io.link.VLinkAgentConfig;
import org.versile.reactor.io.vec.VEntityChannel;
import org.versile.reactor.io.vec.VEntityChannelConfig;
import org.versile.reactor.io.vop.VOPBridge;
import org.versile.reactor.io.vop.VOPServerBridge;
import org.versile.reactor.io.vop.VOPBridge.Transport;
import org.versile.reactor.io.vts.VSecureServer;


/**
 * Listening service for the VOP protocol.
 */
public class VOPService extends VReactorService {

	VPrivateCredentials credentials;

	/**
	 * Set up VOP reactor service.
	 *
	 * <p>For constructor parameters see {@link VReactorService}. Sets up a service with
	 * socket channel, processor and reactor lazy-created by the service, and
	 * with a default service configuration object.</p>
	 */
	public VOPService(VGatewayFactory gwFactory, VPrivateCredentials credentials,
			VLinkAuth linkAuthorizer) throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, null, null, null, new VOPServiceConfig());
		this.credentials = credentials;
	}

	/**
	 * Set up VOP reactor service.
	 *
	 * <p>For constructor parameters see {@link VReactorService}. Sets up a service with
	 * socket channel, processor and reactor lazy-created by the service.</p>
	 */
	public VOPService(VGatewayFactory gwFactory, VPrivateCredentials credentials,
			VLinkAuth linkAuthorizer, VOPServiceConfig config) throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, null, null, null, config);
		this.credentials = credentials;

		// If config is null, override value set on parent
		if (config == null) {
			config = new VOPServiceConfig();
			this.config = config;
		}
	}

	/**
	 * Set up VOP reactor service.
	 *
	 * <p>For constructor parameters see {@link VReactorService}.</p>
	 */
	public VOPService(VGatewayFactory gwFactory, VPrivateCredentials credentials,
			VLinkAuth linkAuthorizer, ServerSocketChannel channel, VProcessor processor,
			VReactor reactor, VOPServiceConfig config) throws IOException, VLinkException {
		super(gwFactory, null, linkAuthorizer, channel, processor, reactor, config);
		this.credentials = credentials;

		// If config is null, override value set on parent
		if (config == null) {
			config = new VOPServiceConfig();
			this.config = config;
		}
	}

	@Override
	protected ServerSocketChannel createListeningSocket() throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.socket().bind(new InetSocketAddress(4433));
		channel.configureBlocking(false);

		// Set up address reuse
		VOPServiceConfig service_conf = (VOPServiceConfig) config;
		if (service_conf != null & service_conf.reuseAddress())
			channel.socket().setReuseAddress(true);

		return channel;
	}

	@Override
	protected ByteAgent createByteAgent()
			throws IOException {
		VObject gateway = gwFactory.build();

		// Get a reference to service configuration of appropriate type
		VOPServiceConfig service_conf = (VOPServiceConfig) config;

		// Set up link and connected VEC I/O processing chain
		VLinkAgentConfig link_conf = null;
		if (service_conf.getLinkConfig() != null)
			link_conf = service_conf.getLinkConfig().clone();
		else
			link_conf = new VLinkAgentConfig();
		if (linkAuth != null)
			link_conf.setAuthorizer(linkAuth);
		VLinkAgent link;
		try {
			link = new VLinkAgent(gateway, processor, reactor, link_conf);
		} catch (VLinkException e) {
			throw new IOException(e);
		}
		VEntityChannelConfig vec_conf = null;
		if (service_conf.getVecConfig() != null)
			vec_conf = service_conf.getVecConfig().clone();
		else
			vec_conf = new VEntityChannelConfig();
		VEntityChannel vec = new VEntityChannel(reactor, link, vec_conf);
		vec.getEntityIOPair().attach(link.getIOPair());

		// Set up a VOP client bridge for the link
		VOPBridge.TransportFactory vts_factory = null;
		if (service_conf.isVtsEnabled()) {
			class VTSTransport extends VOPBridge.TransportFactory {
				VReactor reactor;
				VPrivateCredentials credentials;
				VOPServiceConfig config;
				public VTSTransport(VReactor reactor, VPrivateCredentials credentials, VOPServiceConfig config) {
					this.reactor = reactor;
					this.credentials = credentials;
					this.config = config;
				}
				@Override
				public Transport createTransport() {
					VSecureServer vts = null;
					try {
						vts = new VSecureServer(reactor, credentials, config.getVtsConfig());
					} catch (VCryptoException e) {
						return null;
					}
					return new VOPBridge.Transport(vts.getCiphertextIOPair(), vts.getPlaintextIOPair());
				}
			}
			vts_factory = new VTSTransport(reactor, credentials, service_conf);
		}
		VOPBridge.TransportFactory tls_factory = null; // TLS not yet supported
		boolean allow_insecure = service_conf.isInsecureEnabled();
		VOPServerBridge vop = new VOPServerBridge(reactor, vec.getByteIOPair(), vts_factory, tls_factory, allow_insecure);
		return new ByteAgent(vop.getExternalConsumer(), vop.getExternalProducer());
	}

}
