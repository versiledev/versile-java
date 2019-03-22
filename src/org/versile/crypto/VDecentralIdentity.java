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

package org.versile.crypto;

import java.io.UnsupportedEncodingException;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.util.VByteGenerator;
import org.versile.crypto.rand.VPseudoRandomHMAC;



/**
 * Implements Versile Decentral Identity (VDI).
 *
 * <p>This class is not intended to be instantiated, but provides static methods for
 * generating VDI identities for the schemes defined by Versile Platform.</p>
 */
public final class VDecentralIdentity {

	/**
	 * Generates an identity for Versile Decentral Identity Scheme A.
	 *
	 * @param bits number of key bits (must be >= 512 and multiple of 8)
	 * @param purpose 'purpose' identity parameter
	 * @param personal 'personal' identity parameter
	 * @param passphrase 'passphrase' identity parameter (minimum 10 characters)
	 * @return generated identity
	 * @throws VCryptoException illegal parameters or unable to generate
	 */
	static public VRSACrtKeyPair dia(int bits, String purpose, String personal, String passphrase)
			throws VCryptoException {
		if (bits < 512 || (bits % 8) != 0)
			throw new VCryptoException("Must be >= 512 bits and bits must be multiple of 8");
		if (passphrase.length() < 10)
			throw new VCryptoException("Passphrase must be minimum 10 characters");
		String in_str = "Purpose:" + purpose + ":Personal:" + personal;
		in_str += ":Passphrase:" + passphrase + ":Scheme:dia" + bits;
		byte[] prf_input = null;
		try {
			prf_input = in_str.getBytes("utf8");
		} catch (UnsupportedEncodingException e) {
			throw new VCryptoException("Internal conversion error");
		}
		VHash sha256 = VHash.getHashGenerator("sha256");
		VByteGenerator pseudo_rand = new VPseudoRandomHMAC(sha256, new byte[0], prf_input);
		return VRSACipher.vcaGenerateKeyPair(bits, pseudo_rand);
	}

	/**
	 * Generates an identity for Versile Decentral Identity Scheme A.
	 *
	 * @param bits number of key bits (must be >= 512 and multiple of 8)
	 * @param purpose 'purpose' identity parameter
	 * @param personal 'personal' identity parameter
	 * @param passphrase 'passphrase' identity parameter (minimum 10 characters)
	 * @return generated identity as a credentials object
	 * @throws VCryptoException illegal parameters or unable to generate
	 */
	static public VPrivateCredentials diaCredentials(int bits, String purpose, String personal, String passphrase)
			throws VCryptoException {
		VRSAKeyPair keypair = VDecentralIdentity.dia(bits, purpose, personal, passphrase);
		return new VPrivateCredentials(keypair);
	}
}
