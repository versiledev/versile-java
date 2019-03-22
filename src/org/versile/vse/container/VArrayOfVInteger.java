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

package org.versile.vse.container;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VInteger.NetbytesResult;
import org.versile.orb.entity.VTagged;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * Implements the VArrayOfVInteger VSE type.
 */
public class VArrayOfVInteger extends VArrayOf<Number> {

	/**
	 * VSE code for the VArrayOfLong type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "arrayofvinteger"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 6));

	/**
	 * Create array.
	 *
	 * <p>Elements must be Integer, Long, or BigInteger.</p>
	 *
	 * @param array array elements
	 * @throws IllegalArgumentException if an array element is not Integer, Long, or BigInteger
	 */
	public VArrayOfVInteger(Number[] array) throws IllegalArgumentException {
		super(array);
		for (int i = 0; i < array.length; i++) {
			Number _num = array[i];
			if (!(_num instanceof Integer || _num instanceof Long || _num instanceof BigInteger))
				throw new IllegalArgumentException("Elements must be Integer, Long, or BigInteger");
			this.array[i] = _num;
		}
	}

	/**
	 * Create array.
	 *
	 * @param array array elements
	 */
	public VArrayOfVInteger(VInteger[] array) {
		super(new Number[array.length]);
		for (int i = 0; i < array.length; i++) {
			try {
				this.array[i] = VInteger.nativeOf(array[i]);
			} catch (VEntityError e) {
				// Should never happen
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VArrayOfVInteger.VSE_CODE.getTags(ctx);

		byte[][] b_arr = new byte[array.length][];
		int len = 0;
		for (int i = 0; i < array.length; i++) {
			b_arr[i] = VInteger.signedint_to_netbytes(array[i]);
			len += b_arr[i].length;
		}
		byte[] concat = new byte[len];
		int index = 0;
		for (int i = 0; i < b_arr.length; i++) {
			System.arraycopy(b_arr[i], 0, concat, index, b_arr[i].length);
			index += b_arr[i].length;
		}
		VEntity value = new VBytes(concat);
		return new VTagged(value, tags);
	}

	/**
	 * Get VSE decoder for tag data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				if (tags.length > 0)
					throw new VModuleError("Illegal use of residual tags");

				byte[] data = null;
				try {
					data = VBytes.nativeOf(value);
				} catch (VEntityError e) {
					throw new VModuleError("Illegal VSE encoding");
				}

				LinkedList<Number> nums = new LinkedList<Number>();
				int offset = 0;
				while (offset < data.length) {
					NetbytesResult _result = VInteger.netbytes_to_signedint(data, offset);
					if (_result.hasValue()) {
						nums.addLast(_result.getValue());
						offset += _result.getBytesRead();
					}
					else
						throw new VModuleError("Invalid encoding of number");
				}
				VArrayOfVInteger result;
				try {
					result = new VArrayOfVInteger(nums.toArray(new Number[] {}));
				} catch (IllegalArgumentException e) {
					// Should never happen
					throw new RuntimeException(e);
				}

				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(result);
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}

	/**
	 * Converts input to a {@link VArrayOfVInteger}.
	 *
	 * <p>{@link VArrayOfVInteger} is returned as-is, and {@link VArrayOfInt}, {@link VArrayOfLong},
	 * Long[], Integer[], BitInteger[] or Number[] is converted to the return type.
	 * Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VArrayOfVInteger valueOf(Object value)
		throws VEntityError {
		if (value instanceof VArrayOfVInteger)
			return (VArrayOfVInteger) value;

		if (value instanceof VArrayOfInt)
			value = VArrayOfInt.nativeOf(value);
		else if (value instanceof VArrayOfLong)
			value = VArrayOfLong.nativeOf(value);

		if (value instanceof Long[])
			return new VArrayOfVInteger((Long[]) value);
		else if (value instanceof Integer[])
			return new VArrayOfVInteger((Integer[]) value);
		else if (value instanceof BigInteger[])
			return new VArrayOfVInteger((Integer[]) value);
		else if (value instanceof Number[])
			return new VArrayOfVInteger((Number[]) value);
		else if (value instanceof VInteger[])
			return new VArrayOfVInteger((VInteger[]) value);

		throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a Number[].
	 *
	 * <p>Number[] is returned as-is, and {@link VArrayOfVInteger}, {@link VArrayOfInt},
	 * {@link VArrayOfLong}, Integer[] or Long[] is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Number[] nativeOf(Object value)
		throws VEntityError {
		if (value instanceof Number[])
			return (Number[]) value;

		if (value instanceof VArrayOfVInteger) {
			VArrayOfVInteger arr = (VArrayOfVInteger) value;
			Number[] array = new Number[arr.getLength()];
			int index = 0;
			for (Number n: arr)
				array[index++] = n;
			return array;
		}

		if (value instanceof VArrayOfInt)
			value = VArrayOfInt.nativeOf(value);
		else if (value instanceof VArrayOfLong)
			value = VArrayOfLong.nativeOf(value);

		if (value instanceof Long[])
			return (Long[]) value;
		else if (value instanceof Integer[])
			return (Integer[]) value;
		else if (value instanceof BigInteger[])
			return (BigInteger[]) value;
		else if (value instanceof Number[])
			return (Number[]) value;

		throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Generates a lazy-conversion structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
		throws VEntityError {
		Object value;
		value = VArrayOfVInteger.valueOf(obj);
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(value);
		return new VCombiner.Pair(null, val_list);
	}

	@Override
	protected Number[] createArray(int length) {
		return new Number[length];
	}
}
