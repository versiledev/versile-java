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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Null;
import org.versile.common.asn1.VASN1ObjectIdentifier;
import org.versile.common.asn1.VASN1OctetString;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.asn1.cert.AlgorithmIdentifier;
import org.versile.crypto.asn1.pkcs.DigestInfo;



/**
 * Hash digest generator for VCA hash-operations.
 */
public class VHash {

	/**
	 * SHA-1 hash algorithm identifier.
	 */
	public static VObjectIdentifier SHA1_ID = new VObjectIdentifier(1, 3, 14, 3, 2, 26);

	/**
	 * SHA-256 hash algorithm identifier.
	 */
	public static VObjectIdentifier SHA256_ID = new VObjectIdentifier(2, 16, 840, 1, 101, 3, 4, 2, 1);

	/**
	 * SHA-384 hash algorithm identifier.
	 */
	public static VObjectIdentifier SHA384_ID = new VObjectIdentifier(2, 16, 840, 1, 101, 3, 4, 2, 2);

	/**
	 * SHA-512 hash algorithm identifier.
	 */
	public static VObjectIdentifier SHA512_ID = new VObjectIdentifier(2, 16, 840, 1, 101, 3, 4, 2, 3);

	static String[] supported_methods = null;
	static Lock supported_methods_lock = new ReentrantLock();

	String vca_name;
	String native_name;

	VHash(String vca_name, String native_name) {
		this.vca_name = vca_name;
		this.native_name = native_name;
	}

	/**
	 * Generate a message digest instance.
	 *
	 * @return message digester
	 */
	public MessageDigest getInstance() {
		try {
			return MessageDigest.getInstance(native_name);
		} catch (NoSuchAlgorithmException e) {
			// This should never happen
			throw new RuntimeException();
		}
	}

	/**
	 * Generate a message digest instance initialized with some data.
	 *
	 * @param updateData data fed to the digester's update() method
	 * @return message digester
	 */
	public MessageDigest getInstance(byte[] updateData) {
		try {
			MessageDigest result = MessageDigest.getInstance(native_name);
			result.update(updateData);
			return result;
		} catch (NoSuchAlgorithmException e) {
			// This should never happen
			throw new RuntimeException();
		}
	}

	/**
	 * Performs a message on the provided input data.
	 *
	 * <p>Convenience method for quickly creating digest in one single operation.</p>
	 *
	 * @param data input data to digest
	 * @return digest of provided data (only)
	 */
	public byte[] digestOf(byte[] data) {
		MessageDigest digester = this.getInstance();
		digester.update(data);
		return digester.digest();
	}

	/**
	 * Get the Versilt Crypto Algorithms name associated with this hash method type.
	 *
	 * @return VCA hash name
	 */
	String getVCAHashName() {
		return vca_name;
	}

	/**
	 * Get number of bytes in hash digest.
	 *
	 * @return hash digest length
	 */
	public int getDigestLength() {
		int result = this.getInstance().getDigestLength();
		if (result == 0)
			// This should not happen
			throw new RuntimeException();
		return result;
	}

	/**
	 * Create a hash method generator for provided VCA standard name.
	 *
	 * @param name VCA hash method name
	 * @return hash method generator
	 * @throws VCryptoException hash method not supported
	 */
	static public VHash getHashGenerator(String name)
		throws VCryptoException {
		String[] hash_names = VHash.listHashMethods();
		for (String hash_name: hash_names) {
			if (!name.equals(hash_name))
				continue;

			String native_name = null;
			if (name.equals("sha1"))
				native_name = "SHA-1";
			else if (name.equals("sha256"))
				native_name = "SHA-256";
			else if (name.equals("sha384"))
				native_name = "SHA-384";
			else if (name.equals("sha512"))
				native_name = "SHA-512";
			else if (name.equals("md5"))
				native_name = "MD5";
			else
				throw new VCryptoException("Hash method not supported");
			return new VHash(name, native_name);
		}
		throw new VCryptoException("Hash method not supported");
	}

	/**
	 * List supported VCA hash methods.
	 *
	 * @return hash method names
	 */
	static public String[] listHashMethods() {
		synchronized(VHash.supported_methods_lock) {
			if (VHash.supported_methods == null) {
				LinkedList<String> methods = new LinkedList<String>();
				try {
					MessageDigest.getInstance("SHA-1");
					methods.add("sha1");
				} catch (NoSuchAlgorithmException e) {}
				try {
					MessageDigest.getInstance("SHA-256");
					methods.add("sha256");
				} catch (NoSuchAlgorithmException e) {}
				try {
					MessageDigest.getInstance("SHA-384");
					methods.add("sha384");
				} catch (NoSuchAlgorithmException e) {}
				try {
					MessageDigest.getInstance("SHA-512");
					methods.add("sha512");
				} catch (NoSuchAlgorithmException e) {}
				try {
					MessageDigest.getInstance("MD5");
					methods.add("md5");
				} catch (NoSuchAlgorithmException e) {}
				VHash.supported_methods = methods.toArray(new String[0]);
			}
			String[] result = new String[VHash.supported_methods.length];
			for (int i = 0; i < result.length; i++)
				result[i] = VHash.supported_methods[i];
			return result;
		}
	}

	/**
	 * Generates a HMAC digest for this hash method as per RFC 2104.
	 *
	 * @param secret HMAC secret data (or null)
	 * @param message HMAC message data (or null)
	 * @return HMAC digest
	 */
	public byte[] hmacDigestOf(byte[] secret, byte[] message) {
		if (secret == null)
			secret = new byte[0];

		int hash_len = this.getDigestLength();

		byte[] sec = null;
		if (secret.length >= hash_len)
			sec = this.digestOf(secret);
		else {
			sec = new byte[hash_len];
			for (int i = 0; i < sec.length; i++)
				if (i < secret.length)
					sec[i] = secret[i];
				else
					sec[i] = (byte)0x0;
		}

		byte[] inner = new byte[hash_len];
		for (int i = 0; i < inner.length; i++)
			inner[i] = (byte)((byte)0x36 ^ sec[i]);
		MessageDigest i_hash = this.getInstance();
		i_hash.update(inner);
		i_hash.update(message);
		byte[] i_digest = i_hash.digest();

		byte[] outer = new byte[hash_len];
		for (int i = 0; i < outer.length; i++)
			outer[i] = (byte)((byte)0x5c ^ sec[i]);
		MessageDigest o_hash = this.getInstance();
		o_hash.update(outer);
		o_hash.update(i_digest);
		return o_hash.digest();
	}

	/**
	 * Get hash method object identifier.
	 *
	 * @return object identifier (or null if not supported for hash method)
	 */
	public VObjectIdentifier getIdentifier() {
		if (vca_name.equals("sha1"))
			return SHA1_ID;
		else if (vca_name.equals("sha256"))
			return SHA256_ID;
		else if (vca_name.equals("sha384"))
			return SHA384_ID;
		else if (vca_name.equals("sha512"))
			return SHA512_ID;
		else
			return null;
	}

	/**
	 * Get hash method associated with object identifier.
	 *
	 * @param id hash method identifier
	 * @return hash method (or null if not known or not supported)
	 */
	public static VHash fromIdentifier(VObjectIdentifier id) {
		try {
			if (id.equals(SHA1_ID))
				return VHash.getHashGenerator("sha1");
			else if (id.equals(SHA256_ID))
				return VHash.getHashGenerator("sha256");
			else if (id.equals(SHA384_ID))
				return VHash.getHashGenerator("sha384");
			else if (id.equals(SHA512_ID))
				return VHash.getHashGenerator("sha512");
			else
				return null;
		} catch (VCryptoException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Encodes an EMSA-PKCS1-v_5-ENCODE message digest.
	 *
	 * See :term:`PKCS#1` for details. When a strong hash function is used, this
	 * method produces an encoded representation of *msg* which is suitable for
	 * digital signatures.
	 *
	 * An exception is raised if the encoding cannot be made (typically because
	 * the message does not fit inside a bytes object of length *enc_len*\ due
	 * to the length of *hash_cls* hash method digests).
	 *
	 * @param msg binary message to encode
	 * @param enc_len length of encoded message
	 * @return encoded message
	 * @throws VCryptoException hash method not supported, or enc_len too small
	 */
	public byte[] emsaPKCS1v1_5Encode(byte[] msg, int enc_len)
		throws VCryptoException {

		MessageDigest hasher = this.getInstance(msg);

		VASN1Sequence asn1_data = new DigestInfo().create();
		VObjectIdentifier hash_id = this.getIdentifier();
		if (hash_id == null)
			throw new VCryptoException("Hash algorithm not supported");

		byte[] param_T;
		try {
			VASN1Sequence alg = new AlgorithmIdentifier().create();
			alg.append(new VASN1ObjectIdentifier(hash_id), "algorithm");
			alg.append(new VASN1Null(), "parameters");
			asn1_data.append(alg, "digestAlgorithm");
			asn1_data.append(new VASN1OctetString(hasher.digest()), "digest");
			if (!asn1_data.validate())
				throw new VCryptoException("Internal ASN.1 validation error");
			param_T = asn1_data.encodeDER();
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		int pad_len = enc_len - param_T.length - 3;
		if (pad_len < 0)
			throw new VCryptoException("Encoding length too small");
		VByteBuffer buf = new VByteBuffer();
		buf.append((byte)0x00);
		buf.append((byte)0x01);
		for (int i = 0; i < pad_len; i++)
			buf.append((byte)0xff);
		buf.append((byte)0x00);
		buf.append(param_T);
		return buf.popAll();
	}

}
