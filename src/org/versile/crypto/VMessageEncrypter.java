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
import org.versile.common.util.VByteGenerator;
import org.versile.orb.entity.VInteger;



/**
 * Encrypter for the VCA VMessage plaintext-encapsulation format.
 *
 * <p>Wraps plaintext input inside an integrity-protected message format
 * before feeding to a ciphertext encrypter.</p>
 *
 * <p>This class is not thread-safe and for multi-threaded use the caller must take
 * responsibility to perform required synchronization.</p>
 */
public class VMessageEncrypter {

	static int MAX_PLAIN_LEN = 0x10000;

	VBlockTransform encrypter;
	VHash hash;
	VByteGenerator padding;
	byte[] mac_secret;
	int plain_blocksize;
	int hash_len;
	long msg_num = 0L;
	VByteBuffer plain_buf;
	VByteBuffer msg_buf;
	VByteBuffer mac_buf;


	/**
	 * Set up encrypter.
	 *
	 * @param encrypter ciphertext encrypter
	 * @param hash hash method for MAC plaintext validation
	 * @param padding generator for padding data
	 * @param mac_secret MAC secret for message validation
	 */
	public VMessageEncrypter(VBlockTransform encrypter, VHash hash,
							 VByteGenerator padding, byte[] mac_secret) {
		this.encrypter = encrypter;
		this.hash = hash;
		this.padding = padding;
		if (mac_secret == null)
			mac_secret = new byte[0];
		else {
			byte[] _tmp = new byte[mac_secret.length];
			for (int i = 0; i < _tmp.length; i++)
				_tmp[i] = mac_secret[i];
			mac_secret = _tmp;
		}
		this.mac_secret = mac_secret;
		plain_blocksize = encrypter.getInputBlockSize();
		hash_len = hash.getDigestLength();
		plain_buf = new VByteBuffer();
		msg_buf = new VByteBuffer();
		mac_buf = new VByteBuffer();
	}

	/**
	 * Generates ciphertext for the provided plaintext.
	 *
	 * @param plaintext input plaintext (cannot be empty)
	 * @return enciphered message-protected output
	 * @throws VCryptoException
	 */
	public byte[] encrypt(byte[] plaintext) throws VCryptoException {
		if (plaintext == null || plaintext.length == 0)
			throw new VCryptoException("Empty plaintext not allowed");
		try {
			plain_buf.append(plaintext);
			while(!plain_buf.isEmpty()) {
				byte[] plain = plain_buf.pop(MAX_PLAIN_LEN);
				byte len_high = (byte)(((plain.length-1) & 0xff00) >>> 8);
				byte len_low = (byte)((plain.length-1) & 0xff);
				msg_buf.append(new byte[] {len_high, len_low});
				msg_buf.append(plain);
				int msg_len = 2 + plain.length + hash_len;
				int pad_len = (plain_blocksize - (msg_len % plain_blocksize)) % plain_blocksize;
				if (pad_len > 0) {
					byte[] _padding = padding.getBytes(pad_len);
					msg_buf.append(_padding);
				}
				mac_buf.append(VInteger.posint_to_bytes(msg_num));
				mac_buf.append(msg_buf.peekAll());
				byte[] mac = hash.hmacDigestOf(mac_secret, mac_buf.popAll());
				msg_buf.append(mac);
				msg_num += 1L;
			}
			return encrypter.transform(msg_buf.popAll());
		} finally {
			plain_buf.clear();
			msg_buf.clear();
			mac_buf.clear();
		}
	}
}
