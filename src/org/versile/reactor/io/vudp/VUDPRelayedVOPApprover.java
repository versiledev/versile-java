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

package org.versile.reactor.io.vudp;

import java.io.IOException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.util.VExceptionProxy;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VRSAKeyPair;
import org.versile.crypto.rand.VSecureRandom;
import org.versile.crypto.x509.VX509Certificate;
import org.versile.orb.entity.VCallContext;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VException;
import org.versile.orb.entity.VObject;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;


/**
 * Base class for service provider for VUDP Relay service.
 *
 * <p>Implements the base mechanism for a service dispatched by a VUDP Relay.</p>
 *
 * <p>See {@link VUDPRelayedVOPConnecter} for information about UDP filters.</p>
 */
public abstract class VUDPRelayedVOPApprover extends VExternal {

	VUDPHostFilter _udp_filter;
	VSecureRandom _rand;

	/**
	 * Set up an approver without UDP Host filter.
	 */
	public VUDPRelayedVOPApprover() {
		this._construct(null);
	}

	/**
	 * Set up approver.
	 *
	 * @param udpFilter filter for allowed UDP addresses (or null)
	 */
	public VUDPRelayedVOPApprover(VUDPHostFilter udpFilter) {
		this._construct(udpFilter);
	}

	void _construct(VUDPHostFilter udpFilter) {
		_udp_filter = udpFilter;
		_rand = new VSecureRandom();
	}

	/**
	 * Remote method called by link to initiate UDP based service.
	 *
     * <p>Returns null if connection is rejected, otherwise a connecter
     * and local token for initiating a connection. If approved then
     * the connecter is allowed to start UDP handshake with the
     * relay, even before this method has returned.</p>
	 *
	 * @param ctx call context
	 * @param path path to requested resource
	 * @param host relay host address for UDP negotiation
	 * @param port relay port number for UDP negotiation
	 * @param r_token relay token for UDP authentication
	 * @param client_ip client's IP address (or null)
	 * @param client_key client key (or null)
	 * @param client_certs client certificate chain (or null)
	 * @return (connecter, l_token), or null
	 */
	@Publish(show=true, ctx=true)
	public Object[] approve(VCallContext ctx, String[] path, String host, int port, byte[] r_token,
				            String client_ip, String client_key, String[] client_certs)
		throws VExceptionProxy, VCallError {
		if (path == null || host == null || r_token == null)
			throw new VException("Required parameter(s) missing").getProxy();
		if (port < 1024 || port > 65535)
			throw new VException("Illegal port number").getProxy();
		if (r_token.length > 32)
			throw new VException("Illegal relay token value").getProxy();

		// Parse client key and client certificates
		RSAPublicKey key = null;
		if (client_key != null) {
			try {
				key = VRSAKeyPair.importPublicArmoredPkcs(client_key.getBytes());
			} catch (VCryptoException e) {
				throw new VException("Invalid client key").getProxy();
			}
		}
		CertPath c_path = null;
		if (client_certs != null) {
			LinkedList<X509Certificate> _certs = new LinkedList<X509Certificate>();
			for (String s: client_certs) {
				if (s == null)
					throw new VException("Invalid certificate chain").getProxy();
				try {
					X509Certificate _cert = VX509Certificate.importArmored(s.getBytes()).getNative();
					_certs.addLast(_cert);
				} catch (VCryptoException e) {
					throw new VException("Invalid certificate chain").getProxy();
				}
			}
			try {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				c_path = cf.generateCertPath(_certs);
			} catch (CertificateException e) {
				throw new VCallError();
			}
		}

		// Perform filtering on relay's UDP host/port
		if (_udp_filter != null && !_udp_filter.allowRelay(host, port))
			return null;

		VUDPRelayedVOPConnecter connecter;
		connecter = this.handleApprove(path, host, port, client_ip, key, c_path, ctx);
		if (connecter != null) {
			byte[] l_token = this._rand.getBytes(32);
			try {
				connecter.connectUdp(host, port, l_token, r_token);
			} catch (IOException e) {
				return null;
			}
			return new Object[] {connecter, l_token};
		}
		else
			return null;
	}

	/**
	 * Called internally by a relay to approve a link connection.
	 *
	 * <p>Returns a connecter if approved, or null if not approved.</p>
	 *
	 * <p>If a UDP filter is registered on the object, it is performed on host/port
	 * before this method is called, so the method can assume if it is called that
	 * host/port was allowed.</p>
	 *
	 * @param path path to requested resource
	 * @param host relay host address for UDP negotiation
	 * @param port relay port number for UDP negotiation
	 * @param clientIP client's IP address (or null)
	 * @param clientKey client key (or null)
	 * @param clientCerts client certificate chain (or null)
	 * @param ctx call context
	 * @return connecter object, or null
	 */
	protected abstract VUDPRelayedVOPConnecter handleApprove(String[] path, String host, int port, String clientIP,
			                                                 RSAPublicKey clientKey, CertPath clientCerts,
			                                                 VCallContext ctx);

	/**
	 * Convenience method for creating a connecter object.
	 *
	 * <p>Creates a connecter object in server mode; otherwise similar to generating an object
	 * with the {@link VUDPRelayedVOPConnecter} constructor.</p>
	 *
	 * @param credentials link credentials (or null)
	 * @param gateway link gateway object to send to peer (or null)
	 * @return connecter object
	 */
	protected VUDPRelayedVOPConnecter createConnecter(VPrivateCredentials credentials, VObject gateway) {
		return this.createConnecter(credentials, gateway, null);
	}

	/**
	 * Convenience method for creating a connecter object.
	 *
	 * <p>Creates a connecter object in server mode; otherwise similar to generating an object
	 * with the {@link VUDPRelayedVOPConnecter} constructor.</p>
	 *
	 * @param credentials link credentials (or null)
	 * @param gateway link gateway object to send to peer (or null)
	 * @param config additional config (or null)
	 * @return connecter object
	 */
	protected VUDPRelayedVOPConnecter createConnecter(VPrivateCredentials credentials, VObject gateway,
													  VUDPRelayedVOPConfig config) {
		return new VUDPRelayedVOPConnecter(false, credentials, gateway, config);
	}
}
