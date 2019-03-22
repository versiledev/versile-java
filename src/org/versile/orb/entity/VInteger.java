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

package org.versile.orb.entity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Vector;

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VCombiner;
import org.versile.orb.entity.decoder.VIntegerDecoder;


/**
 * Represents the VFE VInteger type.
 *
 * <p>Holds an arbitrary-size integer value.</p>
 */
public final class VInteger extends VEntity {

	Number value;

	/**
	 * Initialize with provided value.
	 *
	 * @param value the entity's value (Integer, Long or BigInteger)
	 * @throws IllegalArgumentException illegal value type
	 */
	public VInteger(Number value) {
		if (value instanceof Integer || value instanceof Long || value instanceof BigInteger)
			this.value = VInteger.normalize(value);
		else
			throw new IllegalArgumentException("Value must be an Integer, Long or BigInteger");
	}

	/**
	 * Gets VInteger value as a normalized integer.
	 *
	 * <p>Returns the normalized value, ref. {@link #normalize(Number)}.</p>
	 *
	 * @return value
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * Get VInteger value as a BigInteger.
	 *
	 * @return value
	 */
	public BigInteger getBigIntegerValue() {
		if (value instanceof Integer)
			return BigInteger.valueOf((Integer)value);
		else if (value instanceof Long)
			return BigInteger.valueOf((Long)value);
		else
			return (BigInteger)value;
	}

	@Override
	public Number _v_native() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		Number other = null;
		try {
			other = VInteger.valueOf(obj).getValue();
		} catch (VEntityError e) {
			return false;
		}

		if (value instanceof Integer && other instanceof Integer)
			return ((Integer)value).equals(((Integer)other));
		else if (value instanceof Long && other instanceof Long)
			return ((Long)value).equals(((Long)other));
		else if (value instanceof BigInteger && other instanceof BigInteger)
			return ((BigInteger)value).equals((BigInteger)other);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Normalizes an (integer) number to the smallest integer type which fits.
	 *
	 * <p>Can be used e.g. to normalize integer keys of dictionaries in order to be able
	 * to compare based on value. Input is returned as the smallest type (Integer,
	 * Long or BigInteger) which fits the value.</p>
	 *
	 * @param number number to normalize (Integer, Long or BigInteger)
	 * @return normalized number
	 */
	public static Number normalize(Number number) {
		if (number instanceof BigInteger) {
			BigInteger _bnum = (BigInteger)number;
			long _lval = _bnum.longValue();
			if (_bnum.equals(BigInteger.valueOf(_lval)))
				number = _lval;
		}
		if (number instanceof Long) {
			Long _lnum = (Long)number;
			int _ival = _lnum.intValue();
			if (_lnum == _ival)
				number = _ival;
		}
		return number;
	}

	/**
	 * Generates a native converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
		throws VEntityError {
		Object value;
		try {
			value = new VInteger((Number)(obj));
		} catch (Exception e) {
			throw new VEntityError("Cannot convert object");
		}
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(value);
		return new VCombiner.Pair(null, val_list);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		Vector<Object> obj_list = new Vector<Object>();
		obj_list.add(this.getValue());
		return new VCombiner.Pair(null, obj_list);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		if (!explicit) {
			byte[] header = VInteger.signedint_to_netbytes(value);
			return new VEncoderData(header, new byte[0]);
		}

		// Generate header encoding elements
		VByteBuffer data = new VByteBuffer();
		if (value instanceof Integer) {
			Integer inum = (Integer) value;
			if (inum >= 0xee) {
				data.append(new byte[]{(byte)0xef});
				data.append(VInteger.posint_to_netbytes(inum - 0xee));
			}
			else if (inum < -1) {
				data.append(new byte[]{(byte)0xf0});
				data.append(VInteger.posint_to_netbytes(-(Long.valueOf(inum) + 2)));
			}
			else
				data.append(new byte[] {(byte)(inum.intValue()+1)});
		}
		else if (value instanceof Long) {
			Long lnum = (Long) value;
			if (lnum >= 0xee) {
				data.append(new byte[]{(byte)0xef});
				data.append(VInteger.posint_to_netbytes(lnum - 0xee));
			}
			else if (lnum < -1) {
				data.append(new byte[]{(byte)0xf0});
				BigInteger _num = BigInteger.valueOf(lnum).add(BigInteger.valueOf(2));
				_num = _num.multiply(BigInteger.valueOf(-1));
				data.append(VInteger.posint_to_netbytes(_num));
			}
			else
				data.append(new byte[] {(byte)(lnum.intValue()+1)});
		}
		else if (value instanceof BigInteger) {
			BigInteger bnum = (BigInteger) value;
			if (bnum.compareTo(BigInteger.valueOf(0xee)) >= 0) {
				data.append(new byte[]{(byte)0xef});
				data.append(VInteger.posint_to_netbytes(bnum.subtract(BigInteger.valueOf(0xee))));
			}
			else if (bnum.compareTo(BigInteger.valueOf(-1)) < 0) {
				data.append(new byte[]{(byte)0xf0});
				BigInteger _num = bnum.add(BigInteger.valueOf(2));
				_num = _num.multiply(BigInteger.valueOf(-1));
				data.append(VInteger.posint_to_netbytes(_num));
			}
			else
				data.append(new byte[] {(byte)(bnum.intValue()+1)});
		}

		// Join data elements into a single returned byte[]
		byte[] header = data.popAll();
		return new VEncoderData(header, new byte[0]);
	}

	/**
	 * Returns the sum of this integer and 'number'.
	 *
	 * @param number number to add
	 * @return sum
	 */
	public VInteger add(VInteger number) {
		if (value instanceof Integer && number.getValue() instanceof Integer) {
			return new VInteger(((Integer)value).longValue() + ((Integer)number.getValue()).longValue());
		}
		else {
			return new VInteger(this.getBigIntegerValue().add(number.getBigIntegerValue()));
		}
	}

	/**
	 * Returns the subtraction of 'number' from this integer.
	 *
	 * @param number number to add
	 * @return subtracted value
	 */
	public VInteger subtract(VInteger number) {
		if (value instanceof Integer && number.getValue() instanceof Integer) {
			return new VInteger(((Integer)value).longValue() - ((Integer)number.getValue()).longValue());
		}
		else {
			return new VInteger(this.getBigIntegerValue().subtract(number.getBigIntegerValue()));
		}
	}

	/**
	 * Returns the product of this integer and 'number'.
	 *
	 * @param number number to multiply
	 * @return product
	 */
	public VInteger multiply(VInteger number) {
		return new VInteger(this.getBigIntegerValue().multiply(number.getBigIntegerValue()));
	}

	/**
	 * Returns the integer part of this integer divided by 'number'.
	 *
	 * @param number divisor
	 * @return divided value
	 * @throws ArithmeticException division by zero
	 */
	public VInteger divide(VInteger number)
		throws VEntityError, ArithmeticException {
		return new VInteger(this.getBigIntegerValue().divide(number.getBigIntegerValue()));
	}

	/**
	 * Returns the remainder of this integer divided by 'number'.
	 *
	 * @param number divisor
	 * @return division remainder
	 * @throws ArithmeticException division by zero
	 */
	public VInteger remainder(VInteger number)
		throws VEntityError, ArithmeticException {
		return new VInteger(this.getBigIntegerValue().remainder(number.getBigIntegerValue()));
	}

	/**
	 * Returns the sum of this integer and 'number'.
	 *
	 * <p>The input argument must be lazy-convertible to {@link VInteger} with
	 * {@link VInteger#valueOf(Object)}.</p>
	 *
	 * @param number number to add
	 * @return sum
	 * @throws VEntityError could not parse number as an integer value
	 */
	public VInteger lazyAdd(Object number)
		throws VEntityError {
		VInteger other = VInteger.valueOf(number);
		return this.add(other);
	}

	/**
	 * Returns the subtraction of 'number' from this integer.
	 *
	 * <p>The input argument must be lazy-convertible to {@link VInteger} with
	 * {@link VInteger#valueOf(Object)}.</p>
	 *
	 * @param number number to add
	 * @return subtracted value
	 * @throws VEntityError could not parse number as an integer value
	 */
	public VInteger lazySubtract(Object number)
		throws VEntityError {
		VInteger other = VInteger.valueOf(number);
		return this.subtract(other);
	}

	/**
	 * Returns the product of this integer and 'number'.
	 *
	 * <p>The input argument must be lazy-convertible to {@link VInteger} with
	 * {@link VInteger#valueOf(Object)}.</p>
	 *
	 * @param number number to multiply
	 * @return product
	 * @throws VEntityError could not parse number as an integer value
	 */
	public VInteger lazyMultiply(Object number)
		throws VEntityError {
		VInteger other = VInteger.valueOf(number);
		return this.multiply(other);
	}

	/**
	 * Returns the integer part of this integer divided by 'number'.
	 *
	 * <p>The input argument must be lazy-convertible to {@link VInteger} with
	 * {@link VInteger#valueOf(Object)}.</p>
	 *
	 * @param number divisor
	 * @return divided value
	 * @throws VEntityError could not parse number as an integer value
	 * @throws ArithmeticException division by zero
	 */
	public VInteger lazyDivide(Object number)
		throws VEntityError, ArithmeticException {
		VInteger other = VInteger.valueOf(number);
		return this.divide(other);
	}

	/**
	 * Returns the remainder of this integer divided by 'number'.
	 *
	 * <p>The input argument must be lazy-convertible to {@link VInteger} with
	 * {@link VInteger#valueOf(Object)}.</p>
	 *
	 * @param number divisor
	 * @return division remainder
	 * @throws VEntityError could not parse number as an integer value
	 * @throws ArithmeticException division by zero
	 */
	public VInteger lazyRemainder(Object number)
		throws VEntityError, ArithmeticException {
		VInteger other = VInteger.valueOf(number);
		return this.remainder(other);
	}

	/**
	 * Converts non-negative integer to Versile Platform byte representation.
	 *
	 * @param value value to convert
	 * @return byte array representation
	 * @throws IllegalArgumentException value not an integer or value is negative
	 */
	public static byte[] posint_to_bytes(Number value) {
		if (value instanceof BigInteger) {
			BigInteger bnum = (BigInteger) value;
			if (bnum.compareTo(BigInteger.ZERO) < 0)
				throw new IllegalArgumentException("Value must be positive");
			byte[] data = bnum.toByteArray();
			if (data.length == 1 || data[0] != (byte)0x00)
				return data;
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			return _data;
		}
		else if (value instanceof Long) {
			long num = (Long)value;
			if (num < 0)
				throw new IllegalArgumentException("Value must be positive");
			if ((num & 0xff00000000000000L) > 0)
				return new byte[] {
						(byte)((num >>> 56) & 0xff),
						(byte)((num >>> 48) & 0xff),
						(byte)((num >>> 40) & 0xff),
						(byte)((num >>> 32) & 0xff),
						(byte)((num >>> 24) & 0xff),
						(byte)((num >>> 16) & 0xff),
						(byte)((num >>> 8) & 0xff),
						(byte)(num & 0xff)
	                	};
			else if ((num & 0xff000000000000L) > 0)
				return new byte[] {
					(byte)((num >>> 48) & 0xff),
					(byte)((num >>> 40) & 0xff),
					(byte)((num >>> 32) & 0xff),
					(byte)((num >>> 24) & 0xff),
					(byte)((num >>> 16) & 0xff),
					(byte)((num >>> 8) & 0xff),
					(byte)(num & 0xff)
                	};
			else if ((num & 0xff0000000000L) > 0)
				return new byte[] {
					(byte)((num >>> 40) & 0xff),
					(byte)((num >>> 32) & 0xff),
					(byte)((num >>> 24) & 0xff),
					(byte)((num >>> 16) & 0xff),
					(byte)((num >>> 8) & 0xff),
					(byte)(num & 0xff)
                	};
			else if ((num & 0xff00000000L) > 0)
				return new byte[] {
					(byte)((num >>> 32) & 0xff),
					(byte)((num >>> 24) & 0xff),
					(byte)((num >>> 16) & 0xff),
					(byte)((num >>> 8) & 0xff),
					(byte)(num & 0xff)
                	};
			else if ((num & 0xff000000) > 0)
				return new byte[] {
						(byte)((num >>> 24) & 0xff),
						(byte)((num >>> 16) & 0xff),
						(byte)((num >>> 8) & 0xff),
						(byte)(num & 0xff)
	                	};
			else if ((num & 0xff0000) > 0)
				return new byte[] {
					(byte)((num >>> 16) & 0xff),
					(byte)((num >>> 8) & 0xff),
					(byte)(num & 0xff)
                	};
			else if ((num & 0xff00) > 0)
				return new byte[] {(byte)((num >>> 8) & 0xff), (byte)(num & 0xff)};
			else
				return new byte[] {(byte)(num & 0xff)};
		}
		else if (value instanceof Integer) {
			int num = (Integer)value;
			if (num < 0)
				throw new IllegalArgumentException("Value must be positive");
			if ((num & 0xff000000) > 0)
				return new byte[] {
						(byte)((num >>> 24) & 0xff),
						(byte)((num >>> 16) & 0xff),
						(byte)((num >>> 8) & 0xff),
						(byte)(num & 0xff)
	                	};
			else if ((num & 0xff0000) > 0)
				return new byte[] {
					(byte)((num >>> 16) & 0xff),
					(byte)((num >>> 8) & 0xff),
					(byte)(num & 0xff)
                	};
			else if ((num & 0xff00) > 0)
				return new byte[] {(byte)((num >>> 8) & 0xff), (byte)(num & 0xff)};
			else
				return new byte[] {(byte)(num & 0xff)};
		}
		else
			throw new NumberFormatException("Value must be an Integer or a BigInteger");
	}

	/**
	 * Converts non-negative integer to Versile Platform netbytes representation.
	 *
	 * @param value value to convert
	 * @return netbytes representation
	 * @throws IllegalArgumentException value not an integer or is negative
	 */
	public static byte[] posint_to_netbytes(Number value) {
		VByteBuffer data = new VByteBuffer();

		// Convert integer value to a sequence of byte[] data
		if (value instanceof BigInteger) {
			BigInteger num = (BigInteger)value;

			if (num.compareTo(BigInteger.valueOf(246)) <= 0)
				return new byte[] {(byte)(num.intValue())};

			num = num.subtract(BigInteger.valueOf(247));
			byte[] bdata = VInteger.posint_to_bytes(num);
			if (bdata.length <= 8) {
				data.append(new byte[] {(byte)(246+bdata.length)});
			}
			else {
				data.append(new byte[] {(byte)0xff});
				data.append(VInteger.posint_to_netbytes(bdata.length-9));
			}
			data.append(bdata);
		}
		else if (value instanceof Integer) {
			int inum = (Integer) value;
			if (inum <= 246)
				return new byte[] {(byte)inum};

			inum -= 247;
			byte[] bdata = VInteger.posint_to_bytes(inum);
			if (bdata.length <= 8) {
				data.append(new byte[] {(byte)(246+bdata.length)});
			}
			else {
				data.append(new byte[] {(byte)0xff});
				data.append(VInteger.posint_to_netbytes(bdata.length-9));
			}
			data.append(bdata);
		}
		else if (value instanceof Long) {
			long lnum = (Long) value;
			if (lnum <= 246)
				return new byte[] {(byte)lnum};

			lnum -= 247;
			byte[] bdata = VInteger.posint_to_bytes(lnum);
			if (bdata.length <= 8) {
				data.append(new byte[] {(byte)(246+bdata.length)});
			}
			else {
				data.append(new byte[] {(byte)0xff});
				data.append(VInteger.posint_to_netbytes(bdata.length-9));
			}
			data.append(bdata);
		}
		else
			throw new NumberFormatException("Value must be an Integer or a BigInteger");

		// Join data elements into a single returned byte[]
		return data.popAll();
	}

	/**
	 * Converts a signed integer to a Versile Platform byte-array representation.
	 *
	 * @param value value to convert
	 * @return byte array representation
	 * @throws IllegalArgumentException value not an integer
	 */
	public static byte[] signedint_to_bytes(Number value) {
		BigInteger num;
		if (value instanceof BigInteger)
			num = (BigInteger) value;
		else if (value instanceof Long)
			num = BigInteger.valueOf((Long)value);
		else if (value instanceof Integer)
			num = BigInteger.valueOf((Integer)value);
		else
			throw new NumberFormatException("Value must be an Integer or a BigInteger");

		if (num.compareTo(BigInteger.ZERO) >= 0)
			num = num.multiply(BigInteger.valueOf(2));
		else
			num = num.multiply(BigInteger.valueOf(-2)).add(BigInteger.ONE);
		return VInteger.posint_to_bytes(num);
	}

	/**
	 * Converts a signed integer to a Versile Platform netbytes representation.
	 *
	 * @param value value to convert
	 * @return byte array representation
	 * @throws IllegalArgumentException value not an integer
	 */
	public static byte[] signedint_to_netbytes(Number value) {
		BigInteger num;
		if (value instanceof BigInteger)
			num = (BigInteger) value;
		else if (value instanceof Long)
			num = BigInteger.valueOf((Long)value);
		else if (value instanceof Integer)
			num = BigInteger.valueOf((Integer)value);
		else
			throw new NumberFormatException("Value must be an Integer or a BigInteger");

		if (num.compareTo(BigInteger.ZERO) >= 0)
			num = num.multiply(BigInteger.valueOf(2));
		else
			num = num.multiply(BigInteger.valueOf(-2)).add(BigInteger.ONE);
		return VInteger.posint_to_netbytes(num);
	}

	/**
	 * Decodes a Versile Platform byte representation of a non-negative integer.
	 *
	 * @param data byte data to convert
	 * @return decoded value
	 */
	public static Number bytes_to_posint(byte[] data) {
		if (data.length < 4 || (data.length == 4 && (data[0] & (byte)0x80) == 0)) {
			int num = 0;
			for (int i = 0; i < data.length; i++)
				num = (num << 8) + (data[i] & 0xff);
			return num;
		}

		if (data.length < 8 || (data.length == 8 && (data[0] & (byte)0x80) == 0)) {
			long num = 0;
			for (int i = 0; i < data.length; i++)
				num = (num << 8) + (data[i] & 0xff);
			return num;
		}

		if ((data[0] & 0x80) == 0) {
			return new BigInteger(data);
		}
		else {
			byte[] data2 = new byte[data.length + 1];
			data2[0] = (byte)0;
			System.arraycopy(data, 0, data2, 1, data.length);
			return new BigInteger(data2);
		}
	}

	/**
	 * Result of a Versile Platform netbytes conversion.
	 *
	 * <p>Conversion may not be complete, should {@link #hasValue()} to check for result.</p>
	 */
	public static class NetbytesResult {

		Number value;
		int bytes_read;
		Number min_bytes, max_bytes;

		public NetbytesResult() {
			value = null;
			bytes_read = 0;
			min_bytes = null;
			max_bytes = null;
		}

		/**
		 * Check whether conversion resulted in a value.
		 *
		 * @return true if a value is available
		 */
		public boolean hasValue() {
			return (value != null);
		}

		/**
		 * Resulting value from conversion.
		 *
		 * @return resulting value
		 */
		public Number getValue() {
			return value;
		}

		/**
		 * Number of bytes read during conversion.
		 *
		 * @return number bytes read
		 */
		public int getBytesRead() {
			return bytes_read;
		}

		/**
		 * Minimum bytes for encoding (or null if not known).
		 *
		 * @return minimum bytes for netbytes encoding
		 */
		public Number getMinBytes() {
			return min_bytes;
		}

		/**
		 * Maximum bytes for encoding (or null if not known).
		 *
		 * @return maximum bytes for netbytes encoding
		 */
		public Number getMaxBytes() {
			return max_bytes;
		}

		void _set_value(Number value, int length) {
			this.value = value;
			bytes_read = length;
			min_bytes = length;
			max_bytes = length;
		}

		void _set_partial(Number min_len, Number max_len) {
			this.value = null;
			bytes_read = 0;
			min_bytes = min_len;
			max_bytes = max_len;
		}
	}

	/**
	 * Decodes a Versile Platform netbytes representation of a non-negative integer.
	 *
	 * @param data byte data to convert
	 * @return parsed data as a netbytes result, which may not be complete
	 */
	public static NetbytesResult netbytes_to_posint(byte[] data) {
		NetbytesResult result = new NetbytesResult();
		if (data.length == 0)
			return result;

		int first_byte = (int)(data[0] & 0xff);
		if (first_byte <= 246) {
			result._set_value(first_byte, 1);
			return result;
		}
		else if (first_byte < 255) {
			int num_bytes = first_byte - 246;
			if (data.length >= (num_bytes+1)) {
				byte[] num_data = new byte[num_bytes];
				System.arraycopy(data, 1, num_data, 0, num_bytes);
				Number _value = VInteger.bytes_to_posint(num_data);
				if (_value instanceof Integer) {
					int _val = (Integer)_value;
					if (_val <= Integer.MAX_VALUE - 247)
						_value = _val + 247;
					else
						_value = _val + 247L;
				}
				else if (_value instanceof Long) {
					long _val = (Long)_value;
				    if (_val <= Long.MAX_VALUE - 247L)
				    	_value = _val + 247L;
				    else
				    	_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
				}
				else
					_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
				result._set_value(_value, num_bytes+1);
				return result;
			}
			else {
				result._set_partial(num_bytes+1, num_bytes+1);
				return result;
			}
		}
		else {
			if (data.length < 2) {
				result._set_partial(10, null);
				return result;
			}

			byte[] len_data = new byte[data.length-1];
			System.arraycopy(data, 1, len_data, 0, data.length-1);
			NetbytesResult int_len = VInteger.netbytes_to_posint(len_data);
			if (int_len.hasValue()) {
				int num_bytes = (Integer)(int_len.getValue()) + 9;
				int bytes_read = int_len.getBytesRead() + 1;
				if (data.length >= (bytes_read + num_bytes)) {
					byte[] num_data = new byte[num_bytes];
					System.arraycopy(data, bytes_read, num_data, 0, num_bytes);
					Number _value = VInteger.bytes_to_posint(num_data);
					if (_value instanceof Integer) {
						int _val = (Integer)_value;
						if (_val <= Integer.MAX_VALUE - 247)
							_value = _val + 247;
						else
							_value = _val + 247L;
					}
					else if (_value instanceof Long) {
						long _val = (Long)_value;
					    if (_val <= Long.MAX_VALUE - 247L)
					    	_value = _val + 247L;
					    else
					    	_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
					}
					else
						_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
					result._set_value(_value, bytes_read+num_bytes);
					return result;
				}
				else {
					int _total_bytes = bytes_read + num_bytes;
					result._set_partial(_total_bytes, _total_bytes);
					return result;
				}
			}
			else {
				Number min_bytes = int_len.getMinBytes();
				if (min_bytes == null)
					min_bytes = 9;
				Number max_bytes = int_len.getMaxBytes();
				result._set_partial(min_bytes,  max_bytes);
				return result;
			}
		}
	}

	/**
	 * Decodes a Versile Platform netbytes representation of a non-negative integer.
	 *
	 * @param data byte data to convert
	 * @param offset offset to data to convert
	 * @return parsed data as a netbytes result, which may not be complete
	 */
	public static NetbytesResult netbytes_to_posint(byte[] data, int offset) {
		NetbytesResult result = new NetbytesResult();
		if (data.length <= offset)
			return result;

		int first_byte = (int)(data[offset] & 0xff);
		if (first_byte <= 246) {
			result._set_value(first_byte, 1);
			return result;
		}
		else if (first_byte < 255) {
			int num_bytes = first_byte - 246;
			if (data.length >= (num_bytes+1)) {
				byte[] num_data = new byte[num_bytes];
				System.arraycopy(data, offset+1, num_data, 0, num_bytes);
				Number _value = VInteger.bytes_to_posint(num_data);
				if (_value instanceof Integer) {
					int _val = (Integer)_value;
					if (_val <= Integer.MAX_VALUE - 247)
						_value = _val + 247;
					else
						_value = _val + 247L;
				}
				else if (_value instanceof Long) {
					long _val = (Long)_value;
				    if (_val <= Long.MAX_VALUE - 247L)
				    	_value = _val + 247L;
				    else
				    	_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
				}
				else
					_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
				result._set_value(_value, num_bytes+1);
				return result;
			}
			else {
				result._set_partial(num_bytes+1, num_bytes+1);
				return result;
			}
		}
		else {
			if (data.length - offset < 2) {
				result._set_partial(10, null);
				return result;
			}

			NetbytesResult int_len = VInteger.netbytes_to_posint(data, offset+1);
			if (int_len.hasValue()) {
				int num_bytes = (Integer)(int_len.getValue()) + 9;
				int bytes_read = int_len.getBytesRead() + 1;
				if (data.length - offset >= (bytes_read + num_bytes)) {
					byte[] num_data = new byte[num_bytes];
					System.arraycopy(data, offset+bytes_read, num_data, 0, num_bytes);
					Number _value = VInteger.bytes_to_posint(num_data);
					if (_value instanceof Integer) {
						int _val = (Integer)_value;
						if (_val <= Integer.MAX_VALUE - 247)
							_value = _val + 247;
						else
							_value = _val + 247L;
					}
					else if (_value instanceof Long) {
						long _val = (Long)_value;
					    if (_val <= Long.MAX_VALUE - 247L)
					    	_value = _val + 247L;
					    else
					    	_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
					}
					else
						_value = ((BigInteger)_value).add(BigInteger.valueOf(247));
					result._set_value(_value, bytes_read+num_bytes);
					return result;
				}
				else {
					int _total_bytes = bytes_read + num_bytes;
					result._set_partial(_total_bytes, _total_bytes);
					return result;
				}
			}
			else {
				Number min_bytes = int_len.getMinBytes();
				if (min_bytes == null)
					min_bytes = 9;
				Number max_bytes = int_len.getMaxBytes();
				result._set_partial(min_bytes,  max_bytes);
				return result;
			}
		}
	}

	/**
	 * Decodes a Versile Platform byte representation of a signed integer.
	 *
	 * @param data byte data to convert
	 * @return value as a (big) integer or long
	 */
	public static Number bytes_to_signedint(byte[] data) {
		Number pos_num = VInteger.bytes_to_posint(data);
		if ((data[data.length - 1] & 0x01) > 0) {
			if (pos_num instanceof Integer) {
				int rnum = (Integer) pos_num;
				return -(rnum >>> 1);
			}
			else if (pos_num instanceof Long) {
				long rnum = (Long) pos_num;
				return -(rnum >>> 1);
			}
			else {
				BigInteger rnum = (BigInteger) pos_num;
				rnum = rnum.shiftRight(1);
				return rnum.multiply(BigInteger.valueOf(-1));
			}
		}
		else {
			if (pos_num instanceof Integer) {
				int rnum = (Integer) pos_num;
				return rnum >>> 1;
			}
			else if (pos_num instanceof Long) {
				long rnum = (Long) pos_num;
				return rnum >>> 1;
			}
			else {
				BigInteger rnum = (BigInteger) pos_num;
				return rnum.shiftRight(1);
			}
		}
	}

	/**
	 * Decodes a Versile Platform netbytes representation of a signed integer.
	 *
	 * @param data byte data to convert
	 * @return parsed data as a netbytes result, which may not be complete
	 */
	public static NetbytesResult netbytes_to_signedint(byte[] data) {
		NetbytesResult pos_res = VInteger.netbytes_to_posint(data);
		if (!(pos_res.hasValue()))
			return pos_res;

		NetbytesResult result = new NetbytesResult();
		int bytes_read = pos_res.getBytesRead();
		Number pos_val = pos_res.getValue();
		if (pos_val instanceof Integer) {
			int val = (Integer) pos_val;
			if ((val & 0x01) > 0)
				result._set_value(-(val >>> 1), bytes_read);
			else
				result._set_value((val >>> 1), bytes_read);
		}
		else if (pos_val instanceof Long) {
			long val = (Long) pos_val;
			if ((val & 0x01) > 0)
				result._set_value(-(val >>> 1), bytes_read);
			else
				result._set_value((val >>> 1), bytes_read);
		}
		else {
			BigInteger val = (BigInteger) pos_val;
			if (val.getLowestSetBit() == 0)
				val = val.shiftRight(1).multiply(BigInteger.valueOf(-1));
			else
				val = val.shiftRight(1);
			result._set_value(val, bytes_read);
		}
		return result;
	}

	/**
	 * Decodes a Versile Platform netbytes representation of a signed integer.
	 *
	 * @param data byte data to convert
	 * @param offset offset to data to convert
	 * @return parsed data as a netbytes result, which may not be complete
	 */
	public static NetbytesResult netbytes_to_signedint(byte[] data, int offset) {
		NetbytesResult pos_res = VInteger.netbytes_to_posint(data, offset);
		if (!(pos_res.hasValue()))
			return pos_res;

		NetbytesResult result = new NetbytesResult();
		int bytes_read = pos_res.getBytesRead();
		Number pos_val = pos_res.getValue();
		if (pos_val instanceof Integer) {
			int val = (Integer) pos_val;
			if ((val & 0x01) > 0)
				result._set_value(-(val >>> 1), bytes_read);
			else
				result._set_value((val >>> 1), bytes_read);
		}
		else if (pos_val instanceof Long) {
			long val = (Long) pos_val;
			if ((val & 0x01) > 0)
				result._set_value(-(val >>> 1), bytes_read);
			else
				result._set_value((val >>> 1), bytes_read);
		}
		else {
			BigInteger val = (BigInteger) pos_val;
			if (val.getLowestSetBit() == 0)
				val = val.shiftRight(1).multiply(BigInteger.valueOf(-1));
			else
				val = val.shiftRight(1);
			result._set_value(val, bytes_read);
		}
		return result;
	}

	/**
	 * Converts an integer to a BigInteger.
	 *
	 * @param num an Integer, Long or BigInteger value
	 * @return converted value
	 * @throws IllegalArgumentException illegal argument type
	 */
	public static BigInteger asBigInt(Number num)
			throws IllegalArgumentException {
			if (num instanceof Integer)
				return BigInteger.valueOf((Integer)num);
			else if (num instanceof Long)
				return BigInteger.valueOf((Long)num);
			else if (num instanceof BigInteger)
				return (BigInteger)num;
			else
				throw new IllegalArgumentException("Cannot convert to BigInteger");
	}

	/**
	 * Generate a reader for reading this entity class from (explicit) serialized data.
	 *
	 * @param ctx serialization I/O context
	 * @return reader
	 */
	public static VEntityReader _v_reader(VIOContext ctx) {
		VEntityReader reader = new VEntityReader();
		try {
			reader.setDecoder(new VIntegerDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Converts input number to an integer.
	 *
	 * <p>A {@link VInteger} is returned as-is. Instances of Integer, Long or
	 * BigInteger are constructed with the appropriate return type. Instances of
	 * Float, Double or BigDecimal are constructed if they represent an integer value.
	 * Any other input results in an exception.</p>
	 *
	 * @param number input number object
	 * @return number as VInteger
	 * @throws VEntityError not an integer or not a supported type
	 */
	public static VInteger valueOf(Object number)
		throws VEntityError {
		if (number instanceof VInteger)
			return (VInteger)number;
		else if (number instanceof Integer || number instanceof Long || number instanceof BigInteger)
			return new VInteger((Number)number);

		BigDecimal decimal = null;
		if (number instanceof Float)
			decimal = BigDecimal.valueOf((Float)number);
		else if (number instanceof Double)
			decimal = BigDecimal.valueOf((Double)number);
		else if (number instanceof BigDecimal)
			decimal = (BigDecimal)number;
		else
			throw new VEntityError("Not a supported integer value");
		try {
			return new VInteger(decimal.toBigIntegerExact());
		} catch (ArithmeticException e) {
			throw new VEntityError("Not an integer value");
		}
	}

	/**
	 * Converts input number to a (normalized) native integer type.
	 *
	 * <p>Tries to convert to Integer, Long, or BigInteger, using the
	 * normalized representation from an initial {@link #valueOf(Object)}
	 * conversion.</p>
	 *
	 * @param number input number object
	 * @return number as VInteger
	 * @throws VEntityError not an integer or not a supported type
	 */
	public static Number nativeOf(Object number)
		throws VEntityError {
		VInteger vint = VInteger.valueOf(number);
		return vint.getValue();
	}

}
