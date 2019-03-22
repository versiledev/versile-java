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

package org.versile.reactor.url;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCall;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.peer.VInetSocketPeer;
import org.versile.crypto.VCryptoException;
import org.versile.orb.entity.VObject;
import org.versile.orb.link.VLinkException;
import org.versile.orb.url.VUrlData;
import org.versile.orb.url.VUrlException;
import org.versile.orb.url.VUrlResolver;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.link.VLinkAgent;
import org.versile.reactor.io.sock.VClientSocketAgent;
import org.versile.reactor.io.vec.VEntityChannel;
import org.versile.reactor.io.vop.VOPBridge;
import org.versile.reactor.io.vop.VOPClientBridge;
import org.versile.reactor.io.vop.VOPBridge.Transport;
import org.versile.reactor.io.vts.VSecureClient;


/**
 * Implements VUrl for VOP-based connections.
 */
class VopUrl extends VUrl {

	VUrlConfig config;

	public VopUrl(VUrlData urldata)
		throws VUrlException {
		super(urldata);
		if (!(urldata.getScheme().equals("vop")))
			throw new VUrlException("Invalid scheme");
	}

	@Override
	public VCall<VUrlResolver> nowaitConnect(VPrivateCredentials credentials, VObject gw, VUrlConfig config) {
		if (gw == null)
			gw = new VObject();
		if (config == null)
			config = new VUrlConfig();
		this.config = config;

		VCall<VUrlResolver> result = new VCall<VUrlResolver>();

		class ResultHandler implements VCallResultHandler<InetAddress> {
			VCall<VUrlResolver> result;
			VPrivateCredentials credentials;
			VUrlConfig config;
			public ResultHandler(VCall<VUrlResolver> result, VPrivateCredentials credentials,
						         VUrlConfig config) {
				this.result = result;
				this.credentials = credentials;
				this.config = config;
			}
			@Override
			public void callback(InetAddress address) {
				try {
					// Set up link and connected VEC I/O processing chain
					VLinkAgent link = new VLinkAgent(config.getLocalGateway(), config.getProcessor(),
													 config.getReactor(), config.getLinkConfig());
					VReactor reactor = link.getReactor();
					VEntityChannel vec = new VEntityChannel(reactor, link, config.getVecConfig());
					vec.getEntityIOPair().attach(link.getIOPair());

					// Set up a VOP client bridge for the link
					VOPClientBridge vop;
					VOPBridge.TransportFactory vts_factory = null;
					if (config.isVtsEnabled()) {
						class VTSTransport extends VOPBridge.TransportFactory {
							VReactor reactor;
							VPrivateCredentials credentials;
							VUrlConfig config;
							public VTSTransport(VReactor reactor, VPrivateCredentials credentials, VUrlConfig config) {
								this.reactor = reactor;
								this.credentials = credentials;
								this.config = config;
							}
							@Override
							public Transport createTransport() {
								VSecureClient vts = null;
								try {
									vts = new VSecureClient(reactor, credentials, config.getVtsConfig());
								} catch (VCryptoException e) {
									return null;
								}
								return new VOPBridge.Transport(vts.getCiphertextIOPair(), vts.getPlaintextIOPair());
							}
						}
						vts_factory = new VTSTransport(reactor, credentials, config);
					}
					VOPBridge.TransportFactory tls_factory = null; // TLS not yet supported
					boolean allow_insecure = config.isInsecureEnabled();
					vop = new VOPClientBridge(reactor, vec.getByteIOPair(), vts_factory, tls_factory, allow_insecure);

					// Set up a socket channel and connect to the VOP bridge
					SocketChannel channel = SocketChannel.open();
					channel.configureBlocking(false);
					VClientSocketAgent sock_agent = new VClientSocketAgent(reactor, channel, false,
																		   config.getSocketConfig());
					sock_agent.getIOPair().attach(vop.getExternalIOPair());

					// Initiate client socket connection
					int port = urldata.getPort();
					if (port < 0)
						port = 4433;
					sock_agent.connect(new InetSocketAddress(address.getHostAddress(), port));

					result.silentPushResult(createResolver(link, urldata));
				} catch (VLinkException e) {
					result.silentPushException(e);
				} catch (IOException e) {
					result.silentPushException(e);
				} finally {
					result = null;
				}
			}
		}

		VCall<InetAddress> lookup = VInetSocketPeer.nowaitLookup(urldata.getDomain());
		lookup.addHandlerPair(new ResultHandler(result, credentials, config),
				              result.pushExceptionHandler());
		return result;
	}
}
