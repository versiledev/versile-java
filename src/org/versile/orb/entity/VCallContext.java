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

package org.versile.orb.entity;

import java.security.cert.CertPath;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.x500.X500Principal;

import org.versile.common.peer.VPeer;


/**
 * Remote-object context for a remote method call.
 *
 * <p>Typically used as a local-context argument passed by a {@link org.versile.orb.link.VLink}
 * when calling an object's remote-method interface on behalf of the remote link peer, in
 * order to provide the method with context information regarding which link context the
 * method is being executed within.</p>
 */
public class VCallContext {

	/**
	 * Secure transport mechanism.
	 */
	public enum SecureTransport {
		/**
		 * Insecure (plaintext) transport.
		 */
		NONE,
		/**
		 * Versile Transport Security (VTS) transport.
		 */
		VTS,
		/**
		 * Transport Layer Security (TLS) transport.
		 */
		TLS
		};

	VPeer peer = null;
	RSAPublicKey key = null;
	X500Principal claimedIdentity = null;
	CertPath certificates = null;
	SecureTransport secureTransport = SecureTransport.NONE;

	/**
	 * Set up empty call context.
	 */
	public VCallContext() {
	}

	/**
	 * Remote communication peer for the method call.
	 *
	 * @return communication peer (null if not set)
	 */
	public VPeer getPeer() {
		return peer;
	}

	/**
	 * Set the remote communication peer.
	 *
	 * <p>Should normally only be set by the link (or context) which owns and handshakes
	 * the remote communication context, e.g. a {@link org.versile.orb.link.VLink}.</p>
	 *
	 * @param peer remote peer
	 */
	public void setPeer(VPeer peer) {
		this.peer = peer;
	}

	/**
	 * Get the key used by peer to identify itself.
	 *
	 * @return identifying key (or null)
	 */
	public RSAPublicKey getPublicKey() {
		return key;
	}

	/**
	 * Set the key used by peer to identify itself.
	 *
	 * @param key identifying key
	 */
	public void setPublicKey(RSAPublicKey key) {
		this.key = key;
	}

	/**
	 * Get the peer's stated identity.
	 *
	 * @return claimed identity (or null)
	 */
	public X500Principal getClaimedIdentity() {
		return claimedIdentity;
	}

	/**
	 * Set the peer's stated identity.
	 *
	 * @param claimedIdentity claimed identity
	 */
	public void setClaimedIdentity(X500Principal claimedIdentity) {
		this.claimedIdentity = claimedIdentity;
	}

	/**
	 * Get certificate chain for peer's identifying key.
	 *
	 * @return certificate chain (or null)
	 */
	public CertPath getCertificates() {
		return certificates;
	}

	/**
	 * Set certificate chain for peer's identifying key.
	 *
	 * @param certificates certificate chain (or null)
	 */
	public void setCertificates(CertPath certificates) {
		this.certificates = certificates;
	}

	/**
	 * Get secure transport protocol.
	 *
	 * @return secure transport protocol
	 */
	public SecureTransport getSecureTransport() {
		return secureTransport;
	}

	/**
	 * Set secure transport protocol.
	 *
	 * @param secureTransport secure transport protocol
	 */
	public void setSecureTransport(SecureTransport secureTransport) {
		this.secureTransport = secureTransport;
	}
}
