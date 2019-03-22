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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * Implements the VArrayOfInt VSE type.
 */
public class VArrayOfInt extends VArrayOf<Integer> {

	/**
	 * VSE code for the VArrayOfLong type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "arrayofint"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 4));

	/**
	 * Create array with provided elements.
	 *
	 * @param array array elements
	 */
	public VArrayOfInt(Integer[] array) {
		super(array);
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VArrayOfInt.VSE_CODE.getTags(ctx);
		ByteBuffer buf = ByteBuffer.allocate(4*array.length);
		buf.order(ByteOrder.BIG_ENDIAN);
		for (int i = 0; i < array.length; i++)
			buf.putInt(i*4, array[i]);
		VEntity value = new VBytes(buf.array());
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
				if (data.length % 4 != 0)
					throw new VModuleError("VSE encoded byte array must be multiple of 8");

				int len = data.length/4;
				Integer[] array = new Integer[len];
				ByteBuffer buf = ByteBuffer.wrap(data);
				buf.order(ByteOrder.BIG_ENDIAN);
				for (int i = 0; i < len; i++)
					array[i] = buf.getInt(i*4);
				VArrayOfInt result = new VArrayOfInt(array);

				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(result);
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}

	/**
	 * Converts input to a {@link VArrayOfInt}.
	 *
	 * <p>{@link VArrayOfInt} is returned as-is, and {@link VArrayOfLong}, Long[] or Integer[] is
	 * converted to the return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VArrayOfInt valueOf(Object value)
		throws VEntityError {
		if (value instanceof VArrayOfInt)
			return (VArrayOfInt) value;

		if (value instanceof Integer[])
			return new VArrayOfInt((Integer[]) value);

		if (value instanceof VArrayOfLong)
			value = VArrayOfLong.nativeOf(value);
		if (value instanceof Long[]) {
			Integer[] array = VArrayOfInt.nativeOf(value);
			return new VArrayOfInt(array);
		}

		throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to Integer[].
	 *
	 * <p>Integer[] is returned as-is, and {@link VArrayOfInt}, {@link VArrayOfLong} or Long[] is
	 * converted to the return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Integer[] nativeOf(Object value)
		throws VEntityError {
		if (value instanceof Integer[])
			return (Integer[]) value;

		if (value instanceof VArrayOfInt) {
			VArrayOfInt arr = (VArrayOfInt) value;
			Integer[] array = new Integer[arr.getLength()];
			int index = 0;
			for (Integer i: arr)
				array[index++] = i;
			return array;
		}

		if (value instanceof VArrayOfLong)
			value = VArrayOfLong.nativeOf(value);
		else if (value instanceof Long[]) {
			Long[] vals = (Long[]) value;
			Integer[] array = new Integer[vals.length];
			for (int i = 0; i < array.length; i++) {
				long val = vals[i];
				if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE)
					throw new VEntityError("Integer value overflow or underflow");
				array[i] = (int) val;
			}
			return array;
		}

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
		value = VArrayOfInt.valueOf(obj);
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(value);
		return new VCombiner.Pair(null, val_list);
	}

	@Override
	protected Integer[] createArray(int length) {
		return new Integer[length];
	}
}
