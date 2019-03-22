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

import org.versile.common.util.VCombiner;
import org.versile.orb.entity.decoder.VIntegerDecoder;


/**
 * Represents the VFE VFloat type.
 *
 * <p>Holds an arbitrary-precision floating point value of any integer base >=2.</p>
 */
public final class VFloat extends VEntity {

	BigInteger base, digits, exp;

	/**
	 * Initializes as the value digits*base^exp.
	 *
	 * @param digits floating point digits
	 * @param base floating point base (must be >= 2)
	 * @param exp floating point exponent
	 */
	public VFloat(Number digits, Number base, Number exp) {
		try {
			this.digits = VInteger.asBigInt(digits);
			this.base = VInteger.asBigInt(base);
			this.exp = VInteger.asBigInt(exp);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Arguments must be int, long or BigInteger");
		}
		if (this.base.compareTo(BigInteger.valueOf(2)) < 0)
			throw new IllegalArgumentException("Base must be >= 2");
	}

	/**
	 * Creates a {@link VFloat} from a double.
	 *
	 * @param d value to convert
	 * @return converted value
	 */
	public static VFloat fromDouble(Double d) {
		long bits = Double.doubleToLongBits(d);

		boolean positive = ((bits >> 63) & 0x1L) == 0;
		long exponent = ((bits >>> 52) & 0x7ffL) - 1023 - 52;
		long digits = 0x0010000000000000L | (bits & 0x000fffffffffffffL);
		if (!positive)
			digits = -digits;
		return new VFloat(digits, 2, exponent);
	}

	/**
	 * Creates a {@link VFloat} from a BigDecimal.
	 *
	 * @param d value to convert
	 * @return converted value
	 */
	public static VFloat fromBigDecimal(BigDecimal d) {
		BigInteger unscaled = d.unscaledValue();
		int scale = d.scale();
		return new VFloat(unscaled, 10, -scale);
	}

	/**
	 * Returns represented value.
	 *
	 * <p>Returns a converted, native type if possible, otherwise VFloat. If
	 * conversion of base-2 is not possible, it may still be possible to
	 * generate a native type with {@link #toBigDecimal()}, however this
	 * will change the base of the number.</p>
	 *
	 * @return value
	 */
	public Object getValue() {
		return this._v_native();
	}

	/**
	 * Converts value to BigDecimal.
	 *
	 * <p>Conversion is only supported for base-2, base-5 and base-10.
	 * Other bases will throw an exception. Also, an exception is thrown
	 * if unable to fit the VFloat in a BigDecimal structure (e.g. if
	 * the exponent is too largs).</p>
	 *
	 * @return converted value
	 * @throws VEntityError base cannot be converted
	 */
	public BigDecimal toBigDecimal() throws VEntityError {
		if ((base.compareTo(BigInteger.TEN) != 0) && (base.compareTo(BigInteger.valueOf(2)) != 0)
			 && (base.compareTo(BigInteger.valueOf(5)) != 0))
			throw new VEntityError("Only base-10, base-2 and base-5 allowed for this method");

		if ((exp.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0)
			|| (exp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0))
			throw new VEntityError("Unable to perform conversion due to large exponent");

		try {
			if (base.compareTo(BigInteger.TEN) == 0) {
				// Base-10
				BigDecimal result = new BigDecimal(digits);
				return result.scaleByPowerOfTen(exp.intValue()).stripTrailingZeros();
			}
			else {
				// Base-2 or Base-5
				int e = exp.intValue();
				int _base;
				if (base.compareTo(BigInteger.valueOf(2)) == 0)
					_base = 2;
				else
					_base = 5;
				if (exp.compareTo(BigInteger.ZERO) >= 0) {
					// Positive exponent
					BigDecimal result = new BigDecimal(base);
					result = result.pow(e);
					return result.multiply(new BigDecimal(digits)).stripTrailingZeros();
				}
				else {
					// Negative exponent
					BigDecimal result;
					if (_base == 2)
						// Using the opposite base, due to 2^(-n) = 5^n * 10^(-n)
						result = BigDecimal.valueOf(5);
					else
						// Using the opposite base, due to 5^(-n) = 2^n * 10^(-n)
						result = BigDecimal.valueOf(2);
					result = result.pow(-e);
					result = result.multiply(new BigDecimal(digits));
					return result.scaleByPowerOfTen(e).stripTrailingZeros();
				}
			}
		}
		catch (Exception e) {
			// Catch-all for any error which may arise when performing above operations
			throw new VEntityError("Unable to perform conversion");
		}
	}

	@Override
	public Object _v_native() {
		// If base-10 try to convert to BigDecimal (if possible)
		if (base.compareTo(BigInteger.TEN) == 0) {
			int scale = exp.intValue();
			if (exp.compareTo(BigInteger.valueOf(scale)) == 0)
				return new BigDecimal(digits, -scale);
		}

		// If base-2 try to convert to Double
		if (base.compareTo(BigInteger.valueOf(2)) == 0) {
			if (digits.compareTo(BigInteger.ZERO) == 0)
				return (Double)0.0d;

			boolean positive = true;
			BigInteger mantissa = digits;
			BigInteger scale = exp;

			if (mantissa.compareTo(BigInteger.ZERO) < 0) {
				positive = false;
				mantissa = mantissa.multiply(BigInteger.valueOf(-1));
			}

			int z_bits = mantissa.getLowestSetBit();
			if (z_bits > 0) {
				mantissa = mantissa.shiftRight(z_bits);
				scale = scale.add(BigInteger.valueOf(z_bits));
			}
			int m_bits = mantissa.bitLength();
			if (m_bits <= 53) {
				if (m_bits < 53) {
					mantissa = mantissa.shiftLeft(53-m_bits);
					scale = scale.subtract(BigInteger.valueOf(53-m_bits));
				}
				long d_bits = mantissa.longValue() & 0xfffffffffffffL;

				scale = scale.add(BigInteger.valueOf(1023+52));
				int i_scale = scale.intValue();
				if (scale.compareTo(BigInteger.valueOf(i_scale)) == 0 && i_scale >= 0 && i_scale <= 0x7ff) {
					d_bits |= ((long)i_scale) << 52;
					if (!positive)
						d_bits |= 0x8000000000000000L;
					return Double.longBitsToDouble(d_bits);
				}
			}
		}

		// Fallback if no conversion could be made
		return this;
	}

	/**
	 * The base of the floating point.
	 *
	 * @return base
	 */
	public BigInteger getBase() {
		return base;
	}

	/**
	 * The digits of the floating point.
	 *
	 * @return digits
	 */
	public BigInteger getDigits() {
		return digits;
	}

	/**
	 * The exponent of the floating point.
	 *
	 * @return exponent
	 */
	public BigInteger getExp() {
		return exp;
	}

	@Override
	public String toString() {
		Object val = this._v_native();
		if (!(val instanceof VFloat))
			return val.toString();
		return "" + digits + "*" + base + "^(" + exp + ")";
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
			if (obj instanceof Float || obj instanceof Double)
				value = VFloat.fromDouble(((Number)obj).doubleValue());
			else if (obj instanceof BigDecimal)
				value = VFloat.fromBigDecimal((BigDecimal)obj);
			else
				throw new VEntityError("Cannot convert object");
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
		int code;
		if (this.base.compareTo(BigInteger.valueOf(2)) == 0)
			code = 0xfa;
		else if (this.base.compareTo(BigInteger.valueOf(10)) == 0)
			code = 0xf9;
		else
			code = 0xfb;

		VEncoderData result = new VEncoderData(new byte[] {(byte)code}, new byte[0]);
		result.addEmbedded(new VInteger(this.digits), false);
		if (code == 0xfb)
			result.addEmbedded(new VInteger(this.base), false);
		result.addEmbedded(new VInteger(this.exp), false);

		return result;
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
	 * Converts input to a {@link VFloat}.
	 *
	 * <p>{@link VFloat} is returned as-is, and Double, Float, BigDecimal,
	 * VInteger, Long, Integer and BigInteger is converted to the return type.
	 * Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static VFloat valueOf(Object value)
		throws VEntityError {
		if (value instanceof VFloat)
			return (VFloat) value;
		else if (value instanceof Double)
			return VFloat.fromDouble((Double)value);
		else if (value instanceof Float)
			return VFloat.fromDouble(((Float)value).doubleValue());
		else if (value instanceof BigDecimal)
			return VFloat.fromBigDecimal((BigDecimal)value);

		BigInteger intVal = null;
		if (value instanceof VInteger)
			intVal = ((VInteger)value).getBigIntegerValue();
		else if (value instanceof Integer)
			intVal = BigInteger.valueOf((Integer)value);
		else if (value instanceof Long)
			intVal = BigInteger.valueOf((Long)value);
		else if (value instanceof BigInteger)
			intVal = (BigInteger)value;
		else
			throw new VEntityError("Cannot convert to VFloat");
		return VFloat.fromBigDecimal(new BigDecimal(intVal));
	}
}
