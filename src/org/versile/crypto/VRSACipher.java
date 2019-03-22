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
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VByteGenerator;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.rand.VSecureRandom;
import org.versile.orb.entity.VInteger;



/**
 * VCA algorithms for RSA asymmetric cipher.
 */
public class VRSACipher {

	/**
	 * Identifier of the RSSA PKCS #1 v1.5 Signature algorithm.
	 */
	public static VObjectIdentifier SHA1_WITH_RSA_SIGNATURE = new VObjectIdentifier(1, 2, 840, 113549, 1, 1, 5);

	/**
	 * Block chain modes for VCA RSA block ciphers.
	 */
	public enum CipherMode {
		/**
		 * Cipher-block chaining.
		 */
		CBC
		};

	RSAPublicKey public_key;
	RSAPrivateKey private_key;

	/**
	 * Sets a public and/or private key for the cipher.
	 *
	 * @param publicKey RSA public key
	 * @param privateKey RSA private key
	 * @throws VCryptoException missing key or mismatched keys
	 */
	public VRSACipher(RSAPublicKey publicKey, RSAPrivateKey privateKey)
		throws VCryptoException {
		if (publicKey == null && privateKey == null)
			throw new VCryptoException("No keys set");
		if (publicKey != null && privateKey != null)
			if (publicKey.getModulus().compareTo(privateKey.getModulus()) != 0)
				throw new VCryptoException("Mismatched keys");
		public_key = publicKey;
		private_key = privateKey;
	}

	/**
	 * Sets a keypair.
	 *
	 * @param keyPair RSA keypair
	 * @throws VCryptoException missing key or mismatched keys
	 */
	public VRSACipher(VRSAKeyPair keyPair)
		throws VCryptoException {
		public_key = keyPair.getPublic();
		private_key = keyPair.getPrivate();
	}

	/**
	 * Generate an RSA encipher transform for the cipher's public key.
	 *
	 * <p>Returns a transform for num -> num^e mod m where 'e' is the public key exponent
	 * and 'm' is the public key modulus.</p>
	 *
	 * @return encipher transform
	 * @throws VCryptoException no RSA public key set
	 */
	public VNumberTransform getEncoder()
		throws VCryptoException {
		class VRSAEncipherTransform extends VNumberTransform {
			@Override
			public BigInteger transform(BigInteger number)
					throws VCryptoException {
				if (number.compareTo(BigInteger.ZERO) < 0)
					throw new VCryptoException("Number cannot be negative");
				BigInteger modulus = public_key.getModulus();
				if (number.compareTo(modulus) >= 0)
					throw new VCryptoException("Number exceeds modulus");
				return number.modPow(public_key.getPublicExponent(), modulus);
			}
			@Override
			public BigInteger getTransformModulus() {
				return public_key.getModulus();
			}
		}
		if (public_key == null)
			throw new VCryptoException("No public key set");
		return new VRSAEncipherTransform();
	}

	/**
	 * Generate an RSA decipher transform for the cipher's private key.
	 *
	 * <p>Returns transform for num^d mod m where 'd' is the private key exponent
	 * and 'm' is the private key modulus.</p>
	 *
	 * @return decipher transform
	 * @throws VCryptoException no RSA private key set
	 */
	public VNumberTransform getDecoder()
		throws VCryptoException {
		class VRSADecipherTransform extends VNumberTransform {
			@Override
			public BigInteger transform(BigInteger number)
					throws VCryptoException {
				if (number.compareTo(BigInteger.ZERO) < 0)
					throw new VCryptoException("Number cannot be negative");
				BigInteger modulus = private_key.getModulus();
				if (number.compareTo(modulus) >= 0)
					throw new VCryptoException("Number exceeds modulus");
				return number.modPow(private_key.getPrivateExponent(), modulus);
			}
			@Override
			public BigInteger getTransformModulus() {
				return private_key.getModulus();
			}
		}
		if (private_key == null)
			throw new VCryptoException("No private key set");
		return new VRSADecipherTransform();
	}

	/**
	 * Get the modulus of the RSA public and/or private key.
	 *
	 * @return key modulus
	 */
	public BigInteger getModulus() {
		if (public_key != null)
			return public_key.getModulus();
		else
			return private_key.getModulus();
	}

	/**
	 * Get the largest number that can be transformed.
	 *
	 * <p>The largest value that can be provided to the transforms generated
	 * by {@link #getEncoder()} or {@link #getDecoder()}.</p>
	 *
	 * @return largest transform input
	 */
	public BigInteger maxTransformInput() {
		return this.getModulus().subtract(BigInteger.ONE);
	}

	/**
	 * Get the public key.
	 *
	 * @return public key (or null)
	 */
	public RSAPublicKey getPublicKey() {
		return public_key;
	}

	/**
	 * Get the private key.
	 *
	 * @return private key (or null)
	 */
	public RSAPrivateKey getPrivateKey() {
		return private_key;
	}

	/**
	 * Check if cipher has a public key.
	 *
	 * @return true if has public key
	 */
	public boolean hasPublicKey() {
		return (public_key != null);
	}

	/**
	 * Check if cipher has a private key.
	 *
	 * @return true if has private key
	 */
	public boolean hasPrivateKey() {
		return (private_key != null);
	}

	/**
	 * Generates a VCA RSA block encoder transform.
	 *
	 * <p>Creates a block encoder without initialization vector.</p>
	 *
	 * @param rand random data generator for cipher padding
	 * @param mode block chaining mode
	 * @return encoder transform
	 * @throws VCryptoException cannot generate encoder (no public key)
	 */
	public VBlockTransform vcaBlockEncoder(VByteGenerator rand, CipherMode mode)
			throws VCryptoException {
		return this.vcaBlockEncoder(rand,  mode, null);
	}

	/**
	 * Generates a VCA RSA block encoder transform.
	 *
	 * @param rand random data generator for cipher padding
	 * @param mode block chaining mode
	 * @param iv initialization vector (or null)
	 * @return encoder transform
	 * @throws VCryptoException cannot generate encoder (no public key)
	 */
	public VBlockTransform vcaBlockEncoder(VByteGenerator rand, CipherMode mode, byte[] iv)
			throws VCryptoException {
		class VRSABlockEncoder extends VBlockTransform {
			VNumberTransform trans;
			VByteGenerator rand;
			CipherMode mode;
			byte[] iv;
			VByteBuffer buffer;
			public VRSABlockEncoder(int c_size, VNumberTransform trans, VByteGenerator rand,
									CipherMode mode, byte[] iv)
					throws VCryptoException {
				super(c_size-11, c_size);
				this.trans = trans;
				this.rand = rand;
				this.mode = mode;

				if (mode == CipherMode.CBC) {
					if (iv != null) {
						if (iv.length != c_size-11)
							throw new VCryptoException("Invalid initialization vector length");
						this.iv = new byte[iv.length];
						for (int i = 0; i < iv.length; i++)
							this.iv[i] = iv[i];
					}
					else {
						this.iv = new byte[c_size-11];
						for (int i = 0; i < this.iv.length; i++)
							this.iv[i] = (byte)0x0;
					}
				}
				else
					throw new VCryptoException("Unsupported cipher mode");

				this.buffer = new VByteBuffer();
			}
			@Override
			protected byte[] _transform(byte[] data)
					throws VCryptoException {
				int blocksize = this.getInputBlockSize();
				if (data.length % blocksize != 0)
					throw new VCryptoException("Input data not aligned to block size");

				int num_blocks = data.length / blocksize;
				byte[][] result_blocks = new byte[num_blocks][];
				byte[] _data = new byte[blocksize];
				for (int bnum = 0; bnum < num_blocks; bnum++) {
					int _offset = bnum*blocksize;
					for (int i = 0; i < blocksize; i++)
						_data[i] = data[_offset+i];

					if (mode == CipherMode.CBC) {
						byte[] _tmp = new byte[_data.length];
						for (int i = 0; i < _tmp.length; i++)
							_tmp[i] = (byte)(_data[i] ^ iv[i]);
						_data = _tmp;
					}
					buffer.append(new byte[] {(byte)0x02});
					buffer.append(_data);
					buffer.append(new byte[] {(byte)0x00});
					buffer.append(rand.getBytes(8));
					Number _in_num = VInteger.bytes_to_posint(buffer.popAll());
					BigInteger in_num = new VInteger(_in_num).getBigIntegerValue();
					BigInteger out_num = trans.transform(in_num);
					byte[] _out_data = VInteger.posint_to_bytes(out_num);
					byte[] result = _out_data;
					if (_out_data.length != outputBlocksize) {
						result = new byte[outputBlocksize];
						int pos = 0;
						int pad_len = outputBlocksize - _out_data.length;
						for (int i = 0; i < pad_len; i++) {
							result[pos] = 0;
							pos += 1;
						}
						for (int i = 0; i < _out_data.length; i++) {
							result[pos] = _out_data[i];
							pos += 1;
						}
					}

					// Update initialization vector
					if (mode == CipherMode.CBC) {
						for (int i = 0; i < iv.length; i++)
							iv[i] = result[i];
					}

					result_blocks[bnum] = result;
				}
				int ret_len = 0;
				for (byte[] block: result_blocks)
					ret_len += block.length;
				byte[] ret_val = new byte[ret_len];
				int pos = 0;
				for (byte[] block: result_blocks) {
					System.arraycopy(block, 0, ret_val, pos, block.length);
					pos += block.length;
				}
				return ret_val;
			}
		}
		VNumberTransform trans = this.getEncoder();
		byte[] _maxnum = VInteger.posint_to_bytes(trans.getMaxTransformInput());
		if (_maxnum.length < 12)
			throw new VCryptoException("RSA key too small to generate an encoder");
		return new VRSABlockEncoder(_maxnum.length, trans, rand, mode, iv);
	}

	/**
	 * Generates a VCA RSA block decoder transform.
	 *
	 * <p>Creates a block decoder without initialization vector.</p>
	 *
	 * @param mode block chaining mode
	 * @return decoder transform
	 * @throws VCryptoException cannot generate decoder (no private key)
	 */
	public VBlockTransform vcaBlockDecoder(CipherMode mode)
			throws VCryptoException {
		return this.vcaBlockDecoder(mode, null);
	}

	/**
	 * Generates a VCA RSA block decoder transform.
	 *
	 * @param mode block chaining mode
	 * @param iv initialization vector (or null)
	 * @return decoder transform
	 * @throws VCryptoException cannot generate decoder (no private key)
	 */
	public VBlockTransform vcaBlockDecoder(CipherMode mode, byte[] iv)
			throws VCryptoException {
		class VRSABlockDecoder extends VBlockTransform {
			VNumberTransform trans;
			CipherMode mode;
			byte[] iv;
			public VRSABlockDecoder(int c_size, VNumberTransform trans, CipherMode mode, byte[] iv)
					throws VCryptoException {
				super(c_size, c_size-11);
				this.trans = trans;
				this.mode = mode;

				if (mode == CipherMode.CBC) {
					if (iv != null) {
						if (iv.length != c_size-11)
							throw new VCryptoException("Invalid initialization vector length");
						this.iv = new byte[iv.length];
						for (int i = 0; i < iv.length; i++)
							this.iv[i] = iv[i];
					}
					else {
						this.iv = new byte[c_size-11];
						for (int i = 0; i < this.iv.length; i++)
							this.iv[i] = (byte)0x0;
					}
				}
				else
					throw new VCryptoException("Unsupported cipher mode");
			}
			@Override
			protected byte[] _transform(byte[] data)
					throws VCryptoException {
				int blocksize = this.getInputBlockSize();
				if (data.length % blocksize != 0)
					throw new VCryptoException("Input data not aligned to block size");

				int num_blocks = data.length / blocksize;
				byte[][] result_blocks = new byte[num_blocks][];
				byte[] _data = new byte[blocksize];
				for (int bnum = 0; bnum < num_blocks; bnum++) {
					int _offset = bnum*blocksize;
					for (int i = 0; i < blocksize; i++)
						_data[i] = data[_offset+i];

					Number _in_num = VInteger.bytes_to_posint(_data);
					BigInteger in_num = new VInteger(_in_num).getBigIntegerValue();

					BigInteger out_num = trans.transform(in_num);
					byte[] odata = VInteger.posint_to_bytes(out_num);
					if (odata.length != inputBlocksize-1)
						throw new VCryptoException("Invalid ciphertext");
					else if (odata[0] != (byte)0x02)
						throw new VCryptoException("Invalid ciphertext");
					else if (odata[odata.length-9] != (byte)0x00)
						throw new VCryptoException("Invalid ciphertext");
					byte[] result = new byte[outputBlocksize];
					for (int i = 0; i < result.length; i++)
						result[i] = odata[i+1];
					if (mode == CipherMode.CBC) {
						for (int i = 0; i < result.length; i++) {
							result[i] = (byte)(result[i] ^ iv[i]);
							iv[i] = _data[i];
						}
					}
					result_blocks[bnum] = result;
				}
				int ret_len = 0;
				for (byte[] block: result_blocks)
					ret_len += block.length;
				byte[] ret_val = new byte[ret_len];
				int pos = 0;
				for (byte[] block: result_blocks) {
					System.arraycopy(block, 0, ret_val, pos, block.length);
					pos += block.length;
				}
				return ret_val;
			}
		}
		VNumberTransform trans = this.getDecoder();
		byte[] _maxnum = VInteger.posint_to_bytes(trans.getMaxTransformInput());
		if (_maxnum.length < 12)
			throw new VCryptoException("RSA key too small to generate an encoder");
		return new VRSABlockDecoder(_maxnum.length, trans, mode, iv);
	}

	/**
	 * Get ciphertext block size of associated VCA RSA block cipher.
	 *
	 * @return ciphertext block size
	 * @throws VCryptoException key too small for a VCA block cipher
	 */
	public int getVCACiphertextBlockSize()
			throws VCryptoException {
		if (this.hasPublicKey()) {
			VNumberTransform trans = this.getEncoder();
			byte[] _maxnum = VInteger.posint_to_bytes(trans.getMaxTransformInput());
			return _maxnum.length;
		}
		else {
			VNumberTransform trans = this.getDecoder();
			byte[] _maxnum = VInteger.posint_to_bytes(trans.getMaxTransformInput());
			return _maxnum.length;
		}
	}

	/**
	 * Get plaintext block size of associated VCA RSA block cipher.
	 *
	 * @return plaintext block size
	 * @throws VCryptoException key too small for a VCA block cipher
	 */
	public int getVCAPlaintextBlockSize()
			throws VCryptoException {
		return this.getVCACiphertextBlockSize()-11;
	}

	/**
	 * Generate an RSA key as per VDI scheme A.
	 *
	 * <p>The number of key bits must be minimum 512 and must be a multiple of 8.</p>
	 *
	 * <p>Generates a key with < 2^(-64) probability the key is not valid. Uses
	 * the default secure random provider to generate random data for the key.</p>
	 *
	 * @param bits number of key bits
	 * @return generated key
	 * @throws VCryptoException key generation error
	 */
	static public VRSACrtKeyPair vcaGenerateKeyPair(int bits)
			throws VCryptoException {
		VSecureRandom rand = new VSecureRandom();
		return VRSACipher.vcaGenerateKeyPair(bits, rand, 64);
	}

	/**
	 * Generate an RSA key as per VDI scheme A.
	 *
	 * <p>The number of key bits must be minimum 512 and must be a multiple of 8.</p>
	 *
	 * <p>Generates a key with < 2^(-64) probability the key is not valid.</p>
	 *
	 * @param bits number of key bits
	 * @param rand secure random generator for key creation
	 * @return generated key
	 * @throws VCryptoException key generation error
	 */
	static public VRSACrtKeyPair vcaGenerateKeyPair(int bits, VByteGenerator rand)
			throws VCryptoException {
		return VRSACipher.vcaGenerateKeyPair(bits, rand, 64);
	}

	/**
	 * Generate an RSA key as per VDI scheme A.
	 *
	 * <p>The number of key bits must be minimum 512 and must be a multiple of 8.</p>
	 *
	 * <p>The probability the generated key is not a valid key is less than 2^certainty.</p>
	 *
	 * @param bits number of key bits
	 * @param rand secure random generator for key creation
	 * @param certainty certainty factor for primality test
	 * @return generated key
	 * @throws VCryptoException key generation error
	 */
	static public VRSACrtKeyPair vcaGenerateKeyPair(int bits, VByteGenerator rand, int certainty)
			throws VCryptoException {
		if (bits < 512 || bits % 8 != 0)
			throw new VCryptoException("Key bits must be minimum 512 bits and multiple of 8");

		int key_bytes = bits/8;
		int p_len = key_bytes/2;
		int q_len = key_bytes/2 + key_bytes%2;

		BigInteger p = VRSACipher.generatePrime(rand.getBytes(p_len), certainty+1);
		BigInteger q = VRSACipher.generatePrime(rand.getBytes(q_len), certainty+1);
		BigInteger n = p.multiply(q);
		BigInteger t = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
		BigInteger e = BigInteger.valueOf(65537);
		while (t.remainder(e).compareTo(BigInteger.ZERO) == 0) {
			e = e.add(BigInteger.valueOf(2));
			while (!e.isProbablePrime(certainty+1))
				e = e.add(BigInteger.valueOf(2));
		}
		BigInteger d = e.modInverse(t);
		if ((d.multiply(e)).remainder(t).compareTo(BigInteger.ONE) != 0)
			throw new VCryptoException("Internal key generation error");

		KeyFactory factory = null;
		try {
			factory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1) {
			throw new VCryptoException("Internal key generation error");
		}
		RSAPublicKey pubkey = null;
		try {
			pubkey = (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(n, e));
		} catch (Exception e1) {
			throw new VCryptoException("Internal key generation error");
		}
		RSAPrivateCrtKey privkey = null;
		try {
			BigInteger exp_p = d.mod(p.subtract(BigInteger.ONE));
			BigInteger exp_q = d.mod(q.subtract(BigInteger.ONE));
			BigInteger coeff = q.modInverse(p);
			RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(n, e, d, p, q, exp_p, exp_q, coeff);
			privkey = (RSAPrivateCrtKey) factory.generatePrivate(spec);
		} catch (Exception e1) {
			throw new VCryptoException("Internal key generation error: " + e1);
		}
		return new VRSACrtKeyPair(pubkey, privkey);
	}

	static BigInteger generatePrime(byte[] data, int certainty) {
		data[0] = (byte)(data[0] | (byte)0xc0);
		data[data.length-1] = (byte)(data[data.length-1] | (byte)0x01);
		BigInteger result = new VInteger(VInteger.bytes_to_posint(data)).getBigIntegerValue();
		while (true) {
			if (result.isProbablePrime(certainty))
				break;
			result = result.add(BigInteger.valueOf(2));
		}
		return result;
	}

	/**
	 * Sign message with a RSSA PKCS #1 v1.5 Signature.
	 *
	 * <p>Produces a signature as defined by :term:`PKCS#1`\ . Raises an exception
	 * if a signature cannot be produced for the combination of the provided
	 * key and hash class.</p>
	 *
	 * <p>Signatures require using a hash method which has an associated registered
	 * {@link org.versile.crypto.VHash#getIdentifier()}. Also, key length must be sufficient
	 * to hold signature data.</p>
	 *
	 * @param key private key for signing
	 * @param hash hash type for signature
	 * @param msg message to sign
	 * @return signature
	 * @throws VCryptoException could not make signature
	 */
	public static byte[] rsassa_pkcs1_v1_5_sign(RSAPrivateKey key, VHash hash, byte[] msg)
			throws VCryptoException {
		// Create digest
		int key_len = VInteger.posint_to_bytes(key.getModulus()).length;
		byte[] _digest = hash.emsaPKCS1v1_5Encode(msg, key_len);
		BigInteger digest = new VInteger(VInteger.bytes_to_posint(_digest)).getBigIntegerValue();

		// Create signature
		BigInteger sig_num = digest.modPow(key.getPrivateExponent(), key.getModulus());
		return VInteger.posint_to_bytes(sig_num);
	}

	/**
	 * Verifies an RSSA PKCS #1 v1.5 Signature.
	 *
	 * @param key public key for validating
	 * @param hash hash type for signature
	 * @param msg message that was signed
	 * @param signature signature to compare with
	 * @return true if signature verifies, otherwise false
	 * @throws VCryptoException could not perform validation
	 */
	public static boolean rsassa_pkcs1_v1_5_verify(RSAPublicKey key, VHash hash, byte[] msg, byte[] signature)
			throws VCryptoException {
		// Create digest message
		int key_len = VInteger.posint_to_bytes(key.getModulus()).length;
		byte[] _digest = hash.emsaPKCS1v1_5Encode(msg, key_len);
		BigInteger digest = new VInteger(VInteger.bytes_to_posint(_digest)).getBigIntegerValue();

		// Decipher signature with public key
		BigInteger sig_num = new VInteger(VInteger.bytes_to_posint(signature)).getBigIntegerValue();
		BigInteger sig_digest = sig_num.modPow(key.getPublicExponent(), key.getModulus());

		// Compare computed digest with signature's implied digest
		return ((digest.compareTo(sig_digest)) == 0);
	}
}
