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

package org.versile.common.util;

import java.math.BigInteger;

import org.versile.orb.entity.VInteger;



/**
 * Generator for data based on a byte generation source
 */
public abstract class VByteGenerator {

	/**
	 * Generate byte data from byte generation source.
	 *
	 * @param numBytes number of bytes to generate
	 * @return generated data
	 */
	public abstract byte[] getBytes(int numBytes);

	/**
	 * Generate an integer from the byte generation source.
	 *
	 * @param minNumber minimum value of number
	 * @param maxNumber maximum value of number
	 * @return generated number
	 */
	public int getNumber(int minNumber, int maxNumber) {
		return this.getNumber(new VInteger(minNumber), new VInteger(maxNumber)).getValue().intValue();
	}

	/**
	 * Generate an integer from the byte generation source.
	 *
	 * @param minNumber minimum value of number
	 * @param maxNumber maximum value of number
	 * @return generated number
	 */
	public long getNumber(long minNumber, long maxNumber) {
		return this.getNumber(new VInteger(minNumber), new VInteger(maxNumber)).getValue().longValue();
	}

	/**
	 * Generate an integer from the byte generation source.
	 *
	 * @param minNumber minimum value of number
	 * @param maxNumber maximum value of number
	 * @return generated number
	 */
	public VInteger getNumber(VInteger minNumber, VInteger maxNumber) {
		BigInteger min_num = minNumber.getBigIntegerValue();
		BigInteger max_num = maxNumber.getBigIntegerValue();
		if (min_num.compareTo(max_num) > 0)
			throw new RuntimeException("min number cannot be larger than max number");
		if (min_num.compareTo(max_num) == 0)
			return minNumber;
		BigInteger diff = max_num.subtract(min_num);
		byte[] diff_b = VInteger.posint_to_bytes(diff);
		byte first = diff_b[0];

		// Generate a mask for bits which can be stripped off the first byte of generated random, without
		// skewing the distribution of generated random numbers
		byte mask = 0x0;
		for (int i = 7; i >= 0; i--) {
			byte _mask = (byte)(0x01 << i);
			if ((first & _mask) == 0)
				mask |= _mask;
			else
				break;
		}

		// Generate byte data for the diff to resolve between min/max numbers. If the generated number falls
		// outside the allowed range, the whole operation is repeated with a new set of data. This increases
		// the amount of data which is pulled from the random source, however it is required in order to
		// avoid introducing a bias in the distribution of generated numbers.
		BigInteger num = null;
		while(true) {
			byte[] data = this.getBytes(diff_b.length);
			data[0] |= mask;
			data[0] ^= mask;
			num = new VInteger(VInteger.bytes_to_posint(data)).getBigIntegerValue();
			if (num.compareTo(diff) <= 0)
				break;
		}
		return new VInteger(min_num.add(num));
	}
}
