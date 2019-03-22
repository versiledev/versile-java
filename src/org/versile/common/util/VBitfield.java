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
import java.util.BitSet;

import org.versile.orb.entity.VInteger;



/**
 * Represents a bit field.
 *
 * <p>Represents a bit field (i.e. bit string, sequence of binary true/false values). The bit at position
 * zero is the leftmost (most significant) bit.</p>
 */
public class VBitfield {
	BitSet bits;
	int numBits;

	/**
	 * Set up bit field.
	 *
	 * @param bits bit values
	 * @param length bit field length
	 */
	public VBitfield(BitSet bits, int length) {
		this.bits = (BitSet)bits.clone();
		numBits = length;
		// Clear any out-of-range bits
		while (this.bits.length() > length) {
			this.bits.clear(this.bits.length()-1);
		}
	}

	/**
	 * Set up from a bit string.
	 *
	 * <p>Bit string must consist of only 0 or 1 characters.</p>
	 *
	 * @param bitString bit string
	 * @throws IllegalArgumentException
	 */
	public VBitfield(String bitString)
			throws IllegalArgumentException {
		bits = new BitSet();
		numBits = bitString.length();
		for (int i = 0; i < numBits; i++)
			if (bitString.charAt(i) == '1')
				bits.set(i);
			else if (bitString.charAt(i) == '0')
				bits.clear(i);
			else
				throw new IllegalArgumentException();
	}

	/**
	 * Get bit field's number of bits.
	 *
	 * @return length
	 */
	public int getLength() {
		return numBits;
	}

	/**
	 * Get bit value at position.
	 *
	 * @param pos bit position
	 * @return bit value
	 */
	public boolean getBit(int pos) {
		if (pos < 0 || pos >= numBits)
			throw new IndexOutOfBoundsException();
		return bits.get(pos);
	}

	/**
	 * Get an array of bit field's bits.
	 *
	 * @return bit array
	 */
	public boolean[] getBits() {
		boolean[] result = new boolean[numBits];
		for (int i = 0; i < result.length; i++)
			result[i] = bits.get(i);
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VBitfield) {
			VBitfield b_other = (VBitfield) other;
			if (numBits != b_other.numBits)
				return false;
			for (int i = 0; i < numBits; i++)
				if (b_other.getBit(i) != this.getBit(i))
					return false;
			return true;
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return new Integer(numBits).hashCode() ^ bits.hashCode();
	}

	@Override
	public String toString() {
		String result = "";
		for (int i = 0; i < numBits; i++)
			if (bits.get(i))
				result += "1";
			else
				result += "0";
		return result;
	}

	@Override
	public VBitfield clone() {
		return new VBitfield(bits, numBits);
	}

	/**
	 * Converts bits to octets.
	 *
	 * <p>Performs left zero-padding before bitfield MSB to align with octet boundary.</p>
	 *
	 * @return converted bit data
	 */
	public byte[] toOctets() {
		int padding = numBits % 8;
		if (padding != 0)
			padding = 8 - padding;
		int num_bytes = (numBits + padding) / 8;
		byte[] result = new byte[num_bytes];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte)0x00;
			for (int j = 0; j < 8; j++) {
				int mask = 0x80 >>> j;
				if (i*8+j >= padding) {
					if (bits.get(i*8+j-padding))
						result[i] |= (byte)mask;
				}
			}
		}
		return result;
	}

	/**
	 * Adds leading zero-value bits.
	 *
	 * @param numPadding number of left zero-bits to add
	 */
	public void zeroPad(int numPadding) {
		BitSet new_bits = new BitSet(numBits+numPadding);
		for (int i = 0; i < numBits; i++)
			new_bits.set(numPadding+i, bits.get(i));
		bits = new_bits;
		numBits += numPadding;
	}

	/**
	 * Generate a bitwise OR with another bit field.
	 *
	 * <p>Performs bitwise operation after aligning the bit fields with {@link #align}, by
	 * performing left-padding of zero bits as needed in order to align the length of the
	 * input bit fields. The length of the resulting field equals the maximum length of the
	 * two OR'ed fields. </p>
	 *
	 * @param other bit field to OR with
	 * @return result of logical operation
	 */
	public VBitfield or(VBitfield other) {
		VBitfield[] aligned = VBitfield.align(this, other);
		VBitfield result = aligned[0];
		for (int i = 0; i < result.numBits; i++)
			result.bits.set(i, result.bits.get(i) | aligned[1].bits.get(i));
		return result;
	}

	/**
	 * Generate a bitwise AND with another bit field.
	 *
	 * <p>Performs bitwise operation after aligning the bit fields with {@link #align}, by
	 * performing left-padding of zero bits as needed in order to align the length of the
	 * input bit fields. The length of the resulting field equals the maximum length of the
	 * two AND'ed fields. </p>
	 *
	 * @param other bit field to AND with
	 * @return result of logical operation
	 */
	public VBitfield and(VBitfield other) {
		VBitfield[] aligned = VBitfield.align(this, other);
		VBitfield result = aligned[0];
		for (int i = 0; i < result.numBits; i++)
			result.bits.set(i, result.bits.get(i) & aligned[1].bits.get(i));
		return result;
	}

	/**
	 * Generate a bitwise XOR with another bit field.
	 *
	 * <p>Performs bitwise operation after aligning the bit fields with {@link #align}, by
	 * performing left-padding of zero bits as needed in order to align the length of the
	 * input bit fields. The length of the resulting field equals the maximum length of the
	 * two XOR'ed fields. </p>
	 *
	 * @param other bit field to XOR with
	 * @return result of logical operation
	 */
	public VBitfield xor(VBitfield other) {
		VBitfield[] aligned = VBitfield.align(this, other);
		VBitfield result = aligned[0];
		for (int i = 0; i < result.numBits; i++)
			result.bits.set(i, result.bits.get(i) ^ aligned[1].bits.get(i));
		return result;
	}

	/**
	 * Generate an inverse of the bit field by performing NOT on all bits.
	 *
	 * @return bit field inverse
	 */
	public VBitfield not() {
		VBitfield result = this.clone();
		for (int i = 0; i < numBits; i++)
			result.bits.set(i, !result.bits.get(i));
		return result;
	}

	/**
	 * Generates a cloned bit field with the set bit length.
	 *
	 * <p>Bits are zero-padded or truncated on the left side of the resulting bit field
	 * as needed in order to force the requested bit field length.</p>
	 *
	 * @param newLength length of resulting bit field
	 * @return bit field of set length
	 */
	public VBitfield newLength(int newLength) {
		if (newLength == numBits)
			return this.clone();
		else if (newLength > numBits) {
			VBitfield result = this.clone();
			result.zeroPad(newLength-numBits);
			return result;
		}
		else {
			int offset = numBits - newLength;
			BitSet _bits = new BitSet();
			for (int i = 0; i < newLength; i++)
				_bits.set(i, bits.get(offset+i));
			return new VBitfield(_bits, newLength);
		}
	}

	/**
	 * Removes all leading (left-size) zero bits.
	 */
	public void normalize() {
		int firstSet = -1;
		for (int i = 0; i < numBits; i++)
			if (bits.get(i)) {
				firstSet = i;
				break;
			}
		if (firstSet < 0) {
			bits.clear();
			numBits = 0;
		}
		else if (firstSet > 0) {
			numBits -= firstSet;
			BitSet _bits = new BitSet(numBits);
			for (int i = 0; i < numBits; i++)
				_bits.set(i, bits.get(firstSet+i));
			bits = _bits;
		}
	}

	/**
	 * Returns an unsigned integer representation of the bit field.
	 *
	 * @return unsigned integer
	 */
	public VInteger toInteger() {
		return new VInteger(VInteger.bytes_to_posint(this.toOctets()));
	}

	/**
	 * Aligns two bitfields.
	 *
	 * <p>Returns two cloned bitfields which are normalized with zero padding as needed in order
	 * to ensure they have the same length. Zero padding is performed with {@link #zeroPad} on the
	 * value with the fewest bits (if any).</p>
	 *
	 * @param val1 value to normalize
	 * @param val2 value to normalize
	 * @return array [aligned1, aligned2] of normalized cloned bitfields
	 */
	public static VBitfield[] align(VBitfield val1, VBitfield val2) {
		VBitfield aligned1 = val1.clone();
		VBitfield aligned2 = val2.clone();
		if (aligned1.numBits < aligned2.numBits)
			aligned1.zeroPad(aligned2.numBits-aligned1.numBits);
		else if (aligned2.numBits < aligned1.numBits)
			aligned2.zeroPad(aligned1.numBits-aligned2.numBits);
		return new VBitfield[] {aligned1, aligned2};
	}

	/**
	 * Generate a bitfield from a set of octets.
	 *
	 * @param data input octets
	 * @return generated bitfield
	 */
	public static VBitfield fromOctets(byte[] data) {
		int length = data.length*8;
		BitSet bits = new BitSet();
		for (int i = 0; i < data.length; i++)
			for (int j = 0; j < 8; j++)
				if ((data[i] & (0x80 >>> j)) != 0)
					bits.set(8*i+j);
		return new VBitfield(bits, length);
	}

	/**
	 * Set up from an unsigned (non-negative) integer.
	 *
	 * <p>Generates a bit field with the integer's shortest-length binary unsigned
	 * representation (i.e. leftmost bit is not zero).</p>
	 *
	 * @param num integer to convert
	 * @throws IllegalArgumentException non-negative value
	 */
	public static VBitfield fromNumber(VInteger num)
			throws IllegalArgumentException{
		if (num.getBigIntegerValue().compareTo(BigInteger.ZERO)< 0)
			throw new IllegalArgumentException("Integer must be non-negative");
		byte[] data = VInteger.posint_to_bytes(num.getValue());
		VBitfield result = VBitfield.fromOctets(data);
		result.normalize();
		return result;
	}

	/**
	 * Set up from an unsigned (non-negative) integer.
	 *
	 * <p>Generates a bit field with the integer's shortest-length binary unsigned
	 * representation (i.e. leftmost bit is not zero).</p>
	 *
	 * @param num integer to convert
	 * @throws IllegalArgumentException non-negative value
	 */
	public static VBitfield fromNumber(int num)
			throws IllegalArgumentException{
		return VBitfield.fromNumber(new VInteger(num));
	}
}
