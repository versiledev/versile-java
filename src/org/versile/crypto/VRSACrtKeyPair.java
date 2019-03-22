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

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.versile.common.asn1.VASN1Definition;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.util.VBase64;
import org.versile.common.util.VBase64.VBase64Exception;



/**
 * RSA key pair with Chinese Remainder Theorem components.
 */
public class VRSACrtKeyPair extends VRSAKeyPair {

	/**
	 * Set up key pair.
	 *
	 * @param pubKey public key component
	 * @param privKey private key with CRT parameters
	 */
	public VRSACrtKeyPair(RSAPublicKey pubKey, RSAPrivateCrtKey privKey) {
		super(pubKey, privKey);
	}

	@Override
	public RSAPrivateCrtKey getPrivate() {
		return (RSAPrivateCrtKey)priv_key;
	}

	/**
	 * Exports held private key in ASCII-armored PKCS#1 key format.
	 *
	 * @return exported key
	 */
	public byte[] exportPrivateArmored() {
		return VRSACrtKeyPair.exportArmored(this.getPrivate());
	}

	/**
	 * Exports held private key in PKCS#1 DER key format.
	 *
	 * @return DER exported key
	 */
	public byte[] exportPrivateDer() {
		return VRSACrtKeyPair.exportDer(this.getPrivate());
	}

	/**
	 * Exports a private key in ASCII-armored PKCS#1 key format.
	 *
	 * @param key key to export
	 * @return exported key
	 */
	public static byte[] exportArmored(RSAPrivateCrtKey key) {
		byte[] der = VRSACrtKeyPair.exportDer(key);
		try {
			return VBase64.encodeBlock("RSA PRIVATE KEY", der);
		} catch (VBase64Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exports a private key to PKCS#1 DER key format.
	 *
	 * @param key key to export
	 * @return DER exported key
	 */
	public static byte[] exportDer(RSAPrivateCrtKey key) {
		try {
			VASN1Sequence seq = new org.versile.crypto.asn1.pkcs.RSAPrivateKey().create();
			seq.append(new VASN1Integer(0), "version");
			seq.append(new VASN1Integer(key.getModulus()), "modulus");
			seq.append(new VASN1Integer(key.getPublicExponent()), "publicExponent");
			seq.append(new VASN1Integer(key.getPrivateExponent()), "privateExponent");
			seq.append(new VASN1Integer(key.getPrimeP()), "prime1");
			seq.append(new VASN1Integer(key.getPrimeQ()), "prime2");
			seq.append(new VASN1Integer(key.getPrimeExponentP()), "exponent1");
			seq.append(new VASN1Integer(key.getPrimeExponentQ()), "exponent2");
			seq.append(new VASN1Integer(key.getCrtCoefficient()), "coefficient");
			return seq.encodeDER();
		} catch (VASN1Exception e1) {
			// Should never happen
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Imports a keypair from ASCII-armored PKCS#1 private key format.
	 *
	 * @param data key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static VRSACrtKeyPair importPrivateArmored(byte[] data)
			throws VCryptoException {
		try {
			byte[] der = VBase64.decodeBlock("RSA PRIVATE KEY", data);
			return VRSACrtKeyPair.importPrivateDer(der);
		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports a keypair from DER PKCS#1 private key format.
	 *
	 * @param der DER key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static VRSACrtKeyPair importPrivateDer(byte[] der)
			throws VCryptoException {
		try {
			VASN1Definition asn1_spec = new org.versile.crypto.asn1.pkcs.RSAPrivateKey();
			VASN1Sequence asn1_data = (VASN1Sequence) asn1_spec.parseDER(der).getResult();

			BigInteger n = ((VASN1Integer)asn1_data.get(1)).getValue().getBigIntegerValue();
			BigInteger e = ((VASN1Integer)asn1_data.get(2)).getValue().getBigIntegerValue();
			BigInteger d = ((VASN1Integer)asn1_data.get(3)).getValue().getBigIntegerValue();
			BigInteger p = ((VASN1Integer)asn1_data.get(4)).getValue().getBigIntegerValue();
			BigInteger q = ((VASN1Integer)asn1_data.get(5)).getValue().getBigIntegerValue();
			BigInteger exp_p = ((VASN1Integer)asn1_data.get(6)).getValue().getBigIntegerValue();
			BigInteger exp_q = ((VASN1Integer)asn1_data.get(7)).getValue().getBigIntegerValue();
			BigInteger coeff = ((VASN1Integer)asn1_data.get(8)).getValue().getBigIntegerValue();

			KeyFactory factory = null;
			factory = KeyFactory.getInstance("RSA");

			RSAPublicKey pub_key = (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(n, e));
			RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(n, e, d, p, q, exp_p, exp_q, coeff);
			RSAPrivateCrtKey priv_key = (RSAPrivateCrtKey) factory.generatePrivate(spec);
			return new VRSACrtKeyPair(pub_key, priv_key);

		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

}
