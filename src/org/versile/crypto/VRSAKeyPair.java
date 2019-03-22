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
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

import org.versile.common.asn1.VASN1BitString;
import org.versile.common.asn1.VASN1Definition;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1Null;
import org.versile.common.asn1.VASN1ObjectIdentifier;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.util.VBase64;
import org.versile.common.util.VBitfield;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VBase64.BlockData;
import org.versile.common.util.VBase64.VBase64Exception;
import org.versile.crypto.asn1.cert.AlgorithmIdentifier;
import org.versile.crypto.asn1.cert.SubjectPublicKeyInfo;



/**
 * RSA key pair.
 */
public class VRSAKeyPair {

	/**
	 * Identifier of the RSA (PKCS #1 v1.5) key transport algorithm.
	 */
	public static VObjectIdentifier RSA_ENCRYPTION = new VObjectIdentifier(1, 2, 840, 113549, 1, 1, 1);

	RSAPublicKey pub_key;
	RSAPrivateKey priv_key;

	public VRSAKeyPair(RSAPublicKey pubKey, RSAPrivateKey privKey) {
		pub_key = pubKey;
		priv_key = privKey;
		if (pubKey.getModulus().compareTo(privKey.getModulus()) != 0)
			throw new RuntimeException("Mismatched key pair");
	}

	/**
	 * Get the public key component.
	 *
	 * @return public key
	 */
	public RSAPublicKey getPublic() {
		return pub_key;
	}

	/**
	 * Get the private key component.
	 *
	 * @return private key
	 */
	public RSAPrivateKey getPrivate() {
		return priv_key;
	}

	/**
	 * Exports held public key in ASCII-armored PKCS#1 key format.
	 *
	 * @return exported key
	 */
	public byte[] exportPublicArmoredPkcs() {
		return VRSAKeyPair.exportArmoredPkcs(pub_key);
	}

	/**
	 * Exports held public key in PKCS#1 DER key format.
	 *
	 * @return exported key
	 */
	public byte[] exportPublicDerPkcs() {
		return VRSAKeyPair.exportDerPkcs(pub_key);
	}

	/**
	 * Exports held public key in ASCII-armored X.509 SubjectPublicKeyInfo format.
	 *
	 * @return exported key
	 */
	public byte[] exportPublicArmoredSpki() {
		return VRSAKeyPair.exportArmoredSpki(pub_key);
	}

	/**
	 * Exports a public key in ASCII-armored PKCS#1 key format.
	 *
	 * @param key key to export
	 * @return exported key
	 */
	public static byte[] exportArmoredPkcs(RSAPublicKey key) {
		byte[] der = VRSAKeyPair.exportDerPkcs(key);
		try {
			return VBase64.encodeBlock("RSA PUBLIC KEY", der);
		} catch (VBase64Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exports a public key in PKCS#1 DER key format.
	 *
	 * @param key key to export
	 * @return DER exported key
	 */
	public static byte[] exportDerPkcs(RSAPublicKey key) {
		try {
			VASN1Sequence seq = new org.versile.crypto.asn1.pkcs.RSAPublicKey().create();
			seq.append(new VASN1Integer(key.getModulus()), "modulus");
			seq.append(new VASN1Integer(key.getPublicExponent()), "publicExponent");
			return seq.encodeDER();
		} catch (VASN1Exception e1) {
			// Should never happen
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Exports public key in ASCII-armored X.509 SubjectPublicKeyInfo format.
	 *
	 * @param key key to export
	 * @return exported key
	 */
	public static byte[] exportArmoredSpki(RSAPublicKey key) {
		try {
			byte[] der = VRSAKeyPair.exportDerSpki(key);
			return VBase64.encodeBlock("PUBLIC KEY", der);
		} catch (Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exports public key in X.509 SubjectPublicKeyInfo DER format.
	 *
	 * @param key key to export
	 * @return DER exported key
	 */
	public static byte[] exportDerSpki(RSAPublicKey key) {
		try {
			VASN1Sequence inner = new org.versile.crypto.asn1.pkcs.RSAPublicKey().create();
			inner.append(new VASN1Integer(key.getModulus()), "modulus");
			inner.append(new VASN1Integer(key.getPublicExponent()), "publicExponent");
			byte[] key_der = inner.encodeDER();

			VASN1Sequence outer = new SubjectPublicKeyInfo().create();
			VASN1Sequence _alg = new AlgorithmIdentifier().create();
			_alg.append(new VASN1ObjectIdentifier(VRSAKeyPair.RSA_ENCRYPTION), "algorithm");
			_alg.append(new VASN1Null(), "parameters");
			outer.append(_alg, "algorithm");
			outer.append(new VASN1BitString(VBitfield.fromOctets(key_der)), "subjectPublicKey");
			return outer.encodeDER();
		} catch (Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Imports public key from ASCII-armored PKCS#1 or X.509 format.
	 *
	 * @param data key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static RSAPublicKey importPublicArmored(byte[] data)
			throws VCryptoException {
		BlockData _dec;
		try {
			_dec = VBase64.decodeBlock(data);
		} catch (VBase64Exception e) {
			throw new VCryptoException(e);
		}
		if (_dec.getBlockName().equals("RSA PUBLIC KEY"))
			return VRSAKeyPair.importPublicArmoredPkcs(data);
		else if (_dec.getBlockName().equals("PUBLIC KEY"))
			return VRSAKeyPair.importPublicArmoredSpki(data);
		else
			throw new VCryptoException("Key format not supported");
	}

	/**
	 * Imports a public key in ASCII-armored PKCS#1 key format.
	 *
	 * @param data key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static RSAPublicKey importPublicArmoredPkcs(byte[] data)
			throws VCryptoException {
		try {
			byte[] der = VBase64.decodeBlock("RSA PUBLIC KEY", data);
			return VRSAKeyPair.importPublicDerPkcs(der);
		} catch (VCryptoException e) {
			throw e;
		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports a public key from PKCS#1 DER key format.
	 *
	 * @param der key DER data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static RSAPublicKey importPublicDerPkcs(byte[] der)
			throws VCryptoException {
		try {
			VASN1Definition asn1_spec = new org.versile.crypto.asn1.pkcs.RSAPublicKey();
			VASN1Sequence asn1_data = (VASN1Sequence) asn1_spec.parseDER(der).getResult();

			BigInteger n = ((VASN1Integer)asn1_data.get(0)).getValue().getBigIntegerValue();
			BigInteger e = ((VASN1Integer)asn1_data.get(1)).getValue().getBigIntegerValue();

			KeyFactory factory = null;
			try {
				factory = KeyFactory.getInstance("RSA");
			} catch (NoSuchAlgorithmException e1) {
				throw new VCryptoException("Internal key generation error");
			}
			try {
				return (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(n, e));
			} catch (Exception e1) {
				throw new VCryptoException("Internal key generation error");
			}

		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports a public key in ASCII-armored X.509 SubjectPublicKeyInfo format.
	 *
	 * @param data key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static RSAPublicKey importPublicArmoredSpki(byte[] data)
			throws VCryptoException {
		try {
			byte[] der = VBase64.decodeBlock("PUBLIC KEY", data);
			return VRSAKeyPair.importPublicDerSpki(der);
		} catch (VCryptoException e) {
			throw e;
		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports a public key from X.509 SubjectPublicKeyInfo DER key format.
	 *
	 * @param der DER key data
	 * @return imported key
	 * @throws VCryptoException key import error
	 */
	public static RSAPublicKey importPublicDerSpki(byte[] der)
			throws VCryptoException {
		try {
			VASN1Definition asn1_spec = new org.versile.crypto.asn1.cert.SubjectPublicKeyInfo();
			VASN1Sequence spki_data = (VASN1Sequence) asn1_spec.parseDER(der).getResult();

			VASN1Sequence alg_id = (VASN1Sequence)spki_data.get(0);
			VASN1BitString bit_str = (VASN1BitString)spki_data.get(1);

			VObjectIdentifier _alg_id = ((VASN1ObjectIdentifier)alg_id.get(0)).getValue();
			if (!_alg_id.equals(VRSAKeyPair.RSA_ENCRYPTION))
				throw new VCryptoException("Key algorithm invalid or not supported");

			VBitfield bits = bit_str.getValue();
			if ((bits.getLength() % 8) != 0)
				throw new VCryptoException("Key DER data not aligned to octet-convertible bitfield size");
			byte[] key_der = bits.toOctets();
			return VRSAKeyPair.importPublicDerPkcs(key_der);

		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

}
