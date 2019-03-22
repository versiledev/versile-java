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
import java.security.interfaces.RSAPrivateKey;

import javax.security.auth.x500.X500Principal;

import org.versile.crypto.VRSAKeyPair;



/**
 * Holds a set of credentials for authentication.
 *
 * <p>Holds a keypair and may hold an associated claimed identity
 * and a certificate chain which certifies the keypair.</p>
 */
public class VPrivateCredentials extends VCredentials {
	RSAPrivateKey privateKey;
	X500Principal identity = null;
	CertPath certificates = null;

	/**
	 * Sets up empty credentials.
	 *
	 * <p>Generates a credentials object where key, identity and certificates
	 * are all null.</p>
	 */
	public VPrivateCredentials() {
		super();
		privateKey = null;
	}

	/**
	 * Sets up credentials with only a keypair.
	 *
	 * <p>Identity and certificates are set to null. Keypair must have
	 * a value (cannot be null).</p>
	 *
	 * @param keypair keypair associated with credentials
	 */
	public VPrivateCredentials(VRSAKeyPair keypair) {
		super(keypair.getPublic());
		this.privateKey = keypair.getPrivate();
	}

	/**
	 * Sets up a full set of credentials.
	 *
	 * <p>Keypair cannot be null.</p>
	 *
	 * @param keypair keypair associated with credentials
	 * @param identity claimed identity (or null)
	 * @param certificates certificate chain for keypair (or null)
	 */
	public VPrivateCredentials(VRSAKeyPair keypair, X500Principal identity, CertPath certificates) {
		super(keypair.getPublic(), identity, certificates);
		this.privateKey = keypair.getPrivate();
	}

	/**
	 * Get the keypair associated with the credentials.
	 *
	 * <p>Result is null if no keypair is associated with the identity.</p>
	 *
	 * @return keypair (or null)
	 */
	public VRSAKeyPair getKeyPair() {
		if (privateKey == null)
			return null;
		else
			return new VRSAKeyPair(publicKey, privateKey);
	}
}
