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

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VInteger;



/**
 * Decrypter for the VCA VMessage plaintext-encapsulation format.
 *
 * <p>Performs integrity validation on plaintext by validating enciphered
 * plaintext messages, and ensures only validated plaintext is returned
 * by the decoder.</p>
 *
 * <p>This class is not thread-safe and for multi-threaded use the caller must take
 * responsibility to perform required synchronization.</p>
 */
public class VMessageDecrypter {

	static int MAX_PLAIN_LEN = 0x10000;

	VBlockTransform decrypter;
	VHash hash;
	byte[] mac_secret;
	int plain_blocksize;
	int cipher_blocksize;
	int hash_len;
	VByteBuffer read_buf;
	VByteBuffer in_buf;
	VByteBuffer msg_buf;
	VByteBuffer mac_buf;
	VByteBuffer out_buf;
	boolean have_len = false;
	long msg_num = 0L;

	int num_len_bytes = 0;
	byte first_len_byte = (byte)0x0;
	byte second_len_byte = (byte)0x0;
	int plaintext_len = 0;
	int msg_parsed = 0;
	int msg_unread = 0;
	int msg_padlen = 0;
	int ciphertext_unresolved = 0;

	boolean failed = false;

	/**
	 * Set up decrypter.
	 *
	 * @param decrypter ciphertext decrypter
	 * @param hash hash method for MAC plaintext validation
	 * @param mac_secret MAC secret for message validation
	 */
	public VMessageDecrypter(VBlockTransform decrypter, VHash hash, byte[] mac_secret)
			throws VCryptoException {
		this.decrypter = decrypter;
		this.hash = hash;
		if (mac_secret == null)
			mac_secret = new byte[0];
		else {
			byte[] _tmp = new byte[mac_secret.length];
			for (int i = 0; i < _tmp.length; i++)
				_tmp[i] = mac_secret[i];
			mac_secret = _tmp;
		}
		this.mac_secret = mac_secret;
		plain_blocksize = decrypter.getOutputBlockSize();
		cipher_blocksize = decrypter.getInputBlockSize();
		hash_len = hash.getDigestLength();
		read_buf = new VByteBuffer();
		in_buf = new VByteBuffer();
		msg_buf = new VByteBuffer();
		mac_buf = new VByteBuffer();
		out_buf = new VByteBuffer();
	}

	/**
	 *
	 * <p>Return value is the total number of unresolved ciphertext bytes including earlier
	 * calls to the method. If there is a failure condition on the decrypter (i.e. a message
	 * was invalidated), then the value is returned as a negative number.</p>
	 *
	 * @param ciphertext ciphertext data to decipher and decode
	 * @return unresolved ciphertext bytes (possibly negative)
	 * @throws VCryptoException
	 */
	public int decrypt(byte[] ciphertext)
			throws VCryptoException {
		if (failed) {
			ciphertext_unresolved += ciphertext.length;
			return -ciphertext_unresolved;
		}
		read_buf.append(ciphertext);

		// Decrypt ciphertext
		int c_read_len = read_buf.length();
		c_read_len -= c_read_len % decrypter.getInputBlockSize();
		if (c_read_len == 0) {
			return read_buf.length();
		}
		in_buf.append(decrypter.transform(read_buf.pop(c_read_len)));

		// Parse decrypted data
		while(!in_buf.isEmpty()) {
			if (num_len_bytes == 0) {
				first_len_byte = in_buf.pop(1)[0];
				num_len_bytes += 1;
				msg_parsed += 1;
			}
			else if (num_len_bytes == 1) {
				second_len_byte = in_buf.pop(1)[0];
				num_len_bytes += 1;
				msg_parsed += 1;
				plaintext_len = ((first_len_byte << 8) & 0xff00) + (second_len_byte & 0xff) + 1;
				msg_unread = plaintext_len + hash_len;
				msg_padlen = (plain_blocksize - ((msg_unread+2) % plain_blocksize)) % plain_blocksize;
				msg_unread += msg_padlen;
			}
			else {
				byte[] data = in_buf.pop(msg_unread);
				msg_buf.append(data);
				msg_parsed += data.length;
				msg_unread -= data.length;
				if (msg_unread > 0)
					continue;

				// Extract parameters
				byte[] plaintext = msg_buf.pop(plaintext_len);
				byte[] padding = msg_buf.pop(msg_padlen);
				byte[] msg_mac = msg_buf.popAll();

				// Validate integrity
				mac_buf.append(VInteger.posint_to_bytes(msg_num));
				mac_buf.append(new byte[] {first_len_byte, second_len_byte});
				mac_buf.append(plaintext);
				mac_buf.append(padding);
				byte[] mac = hash.hmacDigestOf(mac_secret, mac_buf.popAll());
				boolean valid_mac = true;
				if (mac.length != msg_mac.length)
					valid_mac = false;
				else {
					for (int i = 0; i < mac.length; i++)
						if (mac[i] != msg_mac[i]) {
							valid_mac = false;
							break;
						}
				}
				if (!valid_mac) {
					ciphertext_unresolved += read_buf.length();
					int inbuf_unresolved = (in_buf.length() + msg_buf.length()+2);
					inbuf_unresolved /= decrypter.getOutputBlockSize();
					inbuf_unresolved *= decrypter.getInputBlockSize();
					ciphertext_unresolved += inbuf_unresolved;
					read_buf.clear();
					in_buf.clear();
					msg_buf.clear();
					failed = true;
					return -ciphertext_unresolved;
				}

				// Message was validated
				out_buf.append(plaintext);
				num_len_bytes = 0;
				msg_num += 1L;
				msg_parsed = 0;
			}
		}
		return read_buf.length() + msg_parsed;
	}

	/**
	 * Get plaintext data that has been decrypted and validated
	 *
	 * <p>Retrieved data is popped off the decrypter's buffer.</p>
	 *
	 * @return decrypted and validated plaintext
	 */
	public byte[] getDecrypted() {
		return out_buf.popAll();
	}

	/**
	 * Check if decrypter holds decrypted and validated plaintext data.
	 *
	 * @return true if has validated plaintext data
	 */
	public boolean hasDecrypted() {
		return !out_buf.isEmpty();
	}

	/**
	 * Check whether decrypter holds partially decoded ciphertext.
	 *
	 * @return true if decrypter holds partially decoded data
	 * @throws VCryptoException decrypter is in a failed state
	 */
	public boolean hasParsedPartial()
			throws VCryptoException {
		if (failed)
			throw  new VCryptoException("An earlier message did not validate.");
		return (num_len_bytes > 0);
	}

	/**
	 * Check if failure condition is set on decrypter.
	 *
	 * <p>A failure condition is set if invalid plaintext data was decrypted.</p>
	 *
	 * @return true if decrypter has failure condition
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Resets the failure flag on the decrypter.
	 *
	 * <p>Puts the decrypter in a state where it is ready to start decrypting
	 * ciphertext from the point in the ciphertext stream immediately after
	 * the latest successfully fully decrypted and validated message.</p>
	 */
	public void resetFailure() {
		if (failed) {
			read_buf.clear();
			in_buf.clear();
			msg_buf.clear();
			num_len_bytes = 0;
			failed = true;
			ciphertext_unresolved = 0;
			msg_parsed = 0;
		}
	}
}
