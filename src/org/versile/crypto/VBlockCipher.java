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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.versile.common.util.VByteGenerator;


/**
 * Block cipher generator for VCA block ciphers.
 */
public class VBlockCipher {
	static Map<String, Set<String>> supported_ciphers = null;
	static Lock supported_ciphers_lock = new ReentrantLock();

	String vca_name;
	String vca_mode;
	String native_name;
	int min_key_bits;
	int max_key_bits;

	// maxKeyBits should be <0 if there is no upper limit
	VBlockCipher(String name, String mode, String nativeName, int minKeyBits, int maxKeyBits) {
		vca_name = name;
		vca_mode = mode;
		native_name = nativeName;
		min_key_bits = minKeyBits;
		max_key_bits = maxKeyBits;
	}

	/**
	 * Get the VCA cipher name associated with this cipher.
	 *
	 * @return VCA cipher name
	 */
	public String getVCACipherName() {
		return vca_name;
	}

	/**
	 * Get the VCA mode name associated with this cipher.
	 *
	 * @return VCA mode name
	 */
	public String getVCAModeName() {
		return vca_mode;
	}

	/**
	 * Get minimum key bits for cipher.
	 *
	 * @return min key bits
	 */
	public int getMinKeyBits() {
		return min_key_bits;
	}

	/**
	 * Get maximum key bits for cipher.
	 *
	 * @return max key bits (or -1 if no limit)
	 */
	public int getMaxKeyBits() {
		if (max_key_bits >= 0)
			return max_key_bits;
		return -1;
	}

	/**
	 * Generate a key for the cipher.
	 *
	 * <p>Uses the maximum key size. If there is no key size upper limit
	 * a 512-bit key is generated. Uses the default system provider of secure data.</p>
	 *
	 * @return generated key
	 */
	public SecretKey generateKey() {
		try {
			if (max_key_bits >= 0)
				return this.generateKey(max_key_bits/8);
			else
				return this.generateKey(512/8);
		} catch (VCryptoException e) {
			// Should never happen
			throw new RuntimeException();
		}
	}

	/**
	 * Generate a key for the cipher.
	 *
	 * <p>Uses the default system provider of secure data.</p>
	 *
	 * @param numBytes number of key bytes (note: bytes, not bits)
	 * @return generated key
	 * @throws VCryptoException could not generate key
	 */
	public SecretKey generateKey(int numBytes)
		throws VCryptoException {
		return this.generateKey(numBytes, new SecureRandom());
	}

	/**
	 * Generate a key for the cipher.
	 *
	 * @param numBytes number of key bytes (note: bytes, not bits)
	 * @param rand random data source
	 * @return generated key
	 * @throws VCryptoException could not generate key
	 */
	public SecretKey generateKey(int numBytes, SecureRandom rand)
		throws VCryptoException {
		int num_bits = numBytes*8;
		if (num_bits < min_key_bits)
			throw new VCryptoException("Key length too small");
		if (max_key_bits >= 0 && num_bits > max_key_bits)
			throw new VCryptoException("Key length too large");

		KeyGenerator keygen = null;
		try {
			keygen = KeyGenerator.getInstance(native_name);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException();
		}
		keygen.init(num_bits, rand);
		return keygen.generateKey();
	}

	/**
	 * Import a key from raw key data.
	 *
	 * @param keydata raw key data
	 * @return imported key
	 * @throws VCryptoException invalid key
	 */
	public SecretKey importKey(byte[] keydata)
		throws VCryptoException {
		int num_bits = keydata.length*8;
		if (num_bits < min_key_bits)
			throw new VCryptoException("Key length too small");
		if (max_key_bits >= 0 && num_bits > max_key_bits)
			throw new VCryptoException("Key length too large");

		SecretKey result = new SecretKeySpec(keydata, native_name);

		// Test creating a cipher to check whether key size allowed
		try {
			Cipher _cipher = Cipher.getInstance(native_name + "/" + vca_mode.toUpperCase() + "/NoPadding");
			_cipher.init(Cipher.ENCRYPT_MODE, result);
		} catch (Exception e) {
			throw new VCryptoException("Invalid key for this cipher");
		}

		return result;
	}

	/**
	 * Import a key of default size from a byte generator.
	 *
	 * <p>Imports a key of default length for VCA for the cipher.</p>
	 *
	 * <p>If a random data source is used, this method can also be used for
	 * generating a random key.</p>
	 *
	 * @param source key data source
	 * @return imported key
	 * @throws VCryptoException invalid key
	 */
	public SecretKey importKey(VByteGenerator source)
		throws VCryptoException {
		int key_bytes = this._default_vca_keysize();
		return this.importKey(source.getBytes(key_bytes));
	}

	/**
	 * Get block size for a cipher associated with key.
	 *
	 * @param key key initializing the cipher
	 * @return block size
	 */
	public int getBlockSize(SecretKey key) {
		try {
			return Cipher.getInstance(native_name + "/CBC/NoPadding").getBlockSize();
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			throw new RuntimeException();
		} catch (NoSuchPaddingException e) {
			// Should never happen
			throw new RuntimeException();
		}
	}

	/**
	 * Generate a block transform for cipher encryption.
	 *
	 * <p>Generated transform does not use an initialization vector.</p>
	 *
	 * @param key cipher key
	 * @return encryption transform
	 * @throws VCryptoException illegal key or IV for cipher
	 */
	public VBlockTransform getEncrypter(SecretKey key)
		throws VCryptoException {
		return this.getEncrypter(key, null);
	}

	/**
	 * Generate a block transform for cipher encryption.
	 *
	 * @param key cipher key
	 * @param iv initialization vector (or null)
	 * @return encryption transform
	 * @throws VCryptoException illegal key or IV for cipher
	 */
	public VBlockTransform getEncrypter(SecretKey key, byte[] iv)
		throws VCryptoException {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(native_name + "/" + vca_mode.toUpperCase() + "/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			throw new RuntimeException();
		} catch (NoSuchPaddingException e) {
			// Should never happen
			throw new RuntimeException();
		}
		try {
			if (iv == null) {
				iv = new byte[this.getBlockSize(key)];
				for (int i = 0; i < iv.length; i++)
					iv[i] = (byte)0x0;
			}
			try {
				cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			} catch (InvalidAlgorithmParameterException e) {
				throw new VCryptoException("Invalid initialization vector for cipher");
			}
		} catch (InvalidKeyException e) {
			throw new VCryptoException("Invalid key for cipher");
		}
		return new CipherTransform(cipher);
	}

	/**
	 * Generate a block transform for cipher encryption.
	 *
	 * <p>Generated transform does not use an initialization vector.</p>
	 *
	 * @param key cipher key
	 * @return decryption transform
	 * @throws VCryptoException illegal key or IV for cipher
	 */
	public VBlockTransform getDecrypter(SecretKey key)
		throws VCryptoException {
		return this.getDecrypter(key, null);
	}

	/**
	 * Generate a block transform for cipher encryption.
	 *
	 * @param key cipher key
	 * @param iv initialization vector (or null)
	 * @return decryption transform
	 * @throws VCryptoException illegal key or IV for cipher
	 */
	public VBlockTransform getDecrypter(SecretKey key, byte[] iv)
		throws VCryptoException {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(native_name + "/" + vca_mode.toUpperCase() + "/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			throw new RuntimeException();
		} catch (NoSuchPaddingException e) {
			// Should never happen
			throw new RuntimeException();
		}
		try {
			if (iv == null) {
				iv = new byte[this.getBlockSize(key)];
				for (int i = 0; i < iv.length; i++)
					iv[i] = (byte)0x0;
			}
			try {
				cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			} catch (InvalidAlgorithmParameterException e) {
				throw new VCryptoException("Invalid initialization vector for cipher");
			}
		} catch (InvalidKeyException e) {
			throw new VCryptoException("Invalid key for cipher");
		}

		return new CipherTransform(cipher);
	}

	// Generates default VCA keysize for given cipher
	int _default_vca_keysize() {
		if (vca_name.equals("blowfish"))
			return 56;
		else if (vca_name.equals("blowfish128"))
			return 16;
		else if (vca_name.equals("aes128"))
			return 128/8;
		else if (vca_name.equals("aes192"))
			return 192/8;
		else if (vca_name.equals("aes256"))
			return 256/8;
		else
			throw new RuntimeException("Internal crypto error");
	}

	/**
	 * Create a cipher generator for VCA standard cipher name and mode.
	 *
	 * @param name VCA cipher name
	 * @param mode VCA cipher mode name
	 * @return cipher generator
	 * @throws VCryptoException hash method not supported
	 */
	static public VBlockCipher getCipher(String name, String mode)
		throws VCryptoException {

		Map<String, Set<String>> _ciphers = VBlockCipher.listCiphers();
		if (_ciphers.get(name) == null)
			throw new VCryptoException("Cipher not supported");

		Set<String> _modes = _ciphers.get(name);
		if (_modes == null)
			throw new VCryptoException("Cipher not supported");
		if (!_modes.contains(mode))
			throw new VCryptoException("Cipher mode not supported");

		String native_name = null;
		int min_bits = -1;
		int max_bits = -1;
		if (name.equals("blowfish")) {
			native_name = "Blowfish";
			min_bits = 0;
			max_bits = -1;
		}
		else if (name.equals("blowfish128")) {
			native_name = "Blowfish";
			min_bits = 0;
			max_bits = 128;
		}
		else if (name.equals("aes128")) {
			native_name = "AES";
			min_bits = 128;
			max_bits = 128;
		}
		else if (name.equals("aes192")) {
			native_name = "AES";
			min_bits = 192;
			max_bits = 192;
		}
		else if (name.equals("aes256")) {
			native_name = "AES";
			min_bits = 256;
			max_bits = 256;
		}
		else
			throw new VCryptoException("Cipher/mode not supported");

		return new VBlockCipher(name, mode, native_name, min_bits, max_bits);
	}

	/**
	 * List supported VCA ciphers.
	 *
	 * <p>Returns list of ciphers as a map whose keys are the VCA names of supported
	 * ciphers, and the associated value of a key is a set of the VCA cipher mode names of
	 * supported modes for that cipher.</p>
	 *
	 * @return map from cipher names to cipher modes
	 */
	static public Map<String, Set<String>> listCiphers() {
		synchronized(VBlockCipher.supported_ciphers_lock) {
			if (VBlockCipher.supported_ciphers == null) {
				VBlockCipher.supported_ciphers = new Hashtable<String, Set<String>>();
				LinkedList<String> _ciphers = new LinkedList<String>();
				for (String mode: new String[] {"CBC", "OFB"}) {
					try {
						String _trans = "Blowfish/" + mode + "/NoPadding";
						Cipher.getInstance(_trans);
						int _max_bits = Cipher.getMaxAllowedKeyLength(_trans);
						if(_max_bits >= 128)
							_ciphers.addLast("blowfish128");
						if(_max_bits >= 448)
							_ciphers.addLast("blowfish");
					} catch (NoSuchAlgorithmException e) {
					} catch (NoSuchPaddingException e) {}
					try {
						String _trans = "AES/" + mode + "/NoPadding";
						Cipher.getInstance(_trans);
						int _max_bits = Cipher.getMaxAllowedKeyLength(_trans);
						if (_max_bits >= 128)
							_ciphers.addLast("aes128");
						if (_max_bits >= 192)
							_ciphers.addLast("aes192");
						if (_max_bits >= 256)
							_ciphers.addLast("aes256");
					} catch (NoSuchAlgorithmException e) {
					} catch (NoSuchPaddingException e) {}
					for (String _cipher: _ciphers) {
						Set<String> _modes = VBlockCipher.supported_ciphers.get(_cipher);
						if (_modes == null) {
							_modes = new HashSet<String>();
							VBlockCipher.supported_ciphers.put(_cipher, _modes);
						}
						_modes.add(mode.toLowerCase());
					}
				}
			}
			Map<String, Set<String>> result = new Hashtable<String, Set<String>>();
			for (String key: VBlockCipher.supported_ciphers.keySet()) {
				Set<String> modes = new HashSet<String>();
				for (String mode: VBlockCipher.supported_ciphers.get(key))
					modes.add(mode);
				result.put(key,  modes);
			}
			return result;
		}
	}

	static class CipherTransform extends VBlockTransform {
		Cipher cipher;
		public CipherTransform(Cipher cipher) {
			super(cipher.getBlockSize(), cipher.getBlockSize());
			this.cipher = cipher;
		}
		@Override
		protected byte[] _transform(byte[] data) {
			return cipher.update(data);
		}
	}
}
