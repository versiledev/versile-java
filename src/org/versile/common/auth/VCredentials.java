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

package org.versile.common.auth;

import java.security.cert.CertPath;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.x500.X500Principal;


/**
 * Holds a set of credentials for authentication.
 *
 * <p>May hold either or all of an RSA public key, a claimed identity, or
 * a certificate chain certifying the public key.</p>
 */
public class VCredentials {
	RSAPublicKey publicKey = null;
	X500Principal identity = null;
	CertPath certificates = null;

	/**
	 * Sets up an empty set of credentials.
	 */
	public VCredentials() {
	}

	/**
	 * Sets up credentials with only an RSA key.
	 *
	 * <p>Identity and certificates are set to null.</p>
	 *
	 * @param key public key
	 */
	public VCredentials(RSAPublicKey key) {
		this.publicKey = key;
	}

	/**
	 * Sets up a full set of credentials.
	 *
	 * @param key public key
	 * @param identity claimed identity (or null)
	 * @param certificates certificate chain for keypair (or null)
	 */
	public VCredentials(RSAPublicKey key, X500Principal identity, CertPath certificates) {
		this.publicKey = key;
		this.identity = identity;
		this.certificates = certificates;
	}

	/**
	 * Get credentials' RSA public key.
	 *
	 * @return public key (or null)
	 */
	public RSAPublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * Get claimed identity included with credentials.
	 *
	 * @return claimed identity (or null)
	 */
	public X500Principal getIdentity() {
		return identity;
	}

	/**
	 * Get certificates included with credentials.
	 *
	 * @return certificate path (or null)
	 */
	public CertPath getCertificates() {
		return certificates;
	}
}
