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

package org.versile.crypto.rand;

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VByteGenerator;
import org.versile.crypto.VHash;



/**
 * Generates pseudo-random data based on HMAC.
 *
 * <p>Implements the pseudo-random function defined by RFC 5246.</p>
 */
public class VPseudoRandomHMAC extends VByteGenerator {

	VHash hash;
	byte[] secret;
	byte[] seed;
	VByteBuffer buffer;
	byte[] alg_a;

	/**
	 * Set up generator.
	 *
	 * @param hash hash method for generator
	 * @param secret HMAC secret
	 * @param seed HMAC seed
	 */
	public VPseudoRandomHMAC(VHash hash, byte[] secret, byte[] seed) {
		this.hash = hash;
		this.secret = secret;
		this.seed = seed;
		this.buffer = new VByteBuffer();
		alg_a = seed;
	}

	@Override
	public byte[] getBytes(int numBytes) {
		byte[] result = new byte[numBytes];
		int bytes_left = numBytes;
		int pos = 0;
		while (bytes_left > 0) {
			if (buffer.isEmpty()) {
				// Generate new A_i
				byte[] a_seed = new byte[alg_a.length + seed.length];
				int apos = 0;
				for (int i = 0; i < alg_a.length; i++) {
					a_seed[apos] = alg_a[i];
					apos += 1;
				}
				for (int i = 0; i < seed.length; i++) {
					a_seed[apos] = seed[i];
					apos += 1;
				}
				alg_a = hash.hmacDigestOf(secret, a_seed);

				// Generate next PRF bytes
				a_seed = new byte[alg_a.length + seed.length];
				apos = 0;
				for (int i = 0; i < alg_a.length; i++) {
					a_seed[apos] = alg_a[i];
					apos += 1;
				}
				for (int i = 0; i < seed.length; i++) {
					a_seed[apos] = seed[i];
					apos += 1;
				}
				buffer.append(hash.hmacDigestOf(secret, a_seed));
			}
			byte[] data = buffer.pop(bytes_left);
			for (int i = 0; i < data.length; i++) {
				result[pos] = data[i];
				pos += 1;
				bytes_left -= 1;
			}
		}
		return result;
	}

}
