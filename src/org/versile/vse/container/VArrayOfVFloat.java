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

import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VFloat;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VInteger.NetbytesResult;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * Implements the VArrayOfVFloat VSE type.
 */
public class VArrayOfVFloat extends VArrayOf<VFloat> {

	/**
	 * VSE code for the VArrayOfLong type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "arrayofvfloat"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 9));

	/**
	 * Create array with provided elements.
	 *
	 * @param array array elements
	 */
	public VArrayOfVFloat(VFloat[] array) {
		super(array);
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VArrayOfVFloat.VSE_CODE.getTags(ctx);

		byte[][] b_arr = new byte[array.length*3][];
		int len = 0;
		for (int i = 0; i < array.length; i++) {
			b_arr[3*i] = VInteger.signedint_to_netbytes(array[i].getDigits());
			b_arr[3*i+1] = VInteger.signedint_to_netbytes(array[i].getBase());
			b_arr[3*i+2] = VInteger.signedint_to_netbytes(array[i].getExp());
			len += b_arr[3*i].length + b_arr[3*i+1].length + b_arr[3*i+2].length;
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

				LinkedList<VFloat> nums = new LinkedList<VFloat>();
				Number[] component = new Number[3];
				int c_i = 0;
				int offset = 0;
				while (offset < data.length) {
					NetbytesResult _result = VInteger.netbytes_to_signedint(data, offset);
					if (_result.hasValue()) {
						component[c_i++] = _result.getValue();
						offset += _result.getBytesRead();
						if (c_i == 3) {
							nums.addLast(new VFloat(component[0], component[1], component[2]));
							c_i = 0;
						}
					}
					else
						throw new VModuleError("Invalid encoding of number");
				}
				if (c_i != 0)
					throw new VModuleError("Invalid encoding of number");
				VArrayOfVFloat result = new VArrayOfVFloat(nums.toArray(new VFloat[] {}));

				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(result);
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}

	/**
	 * Converts input to a {@link VArrayOfVFloat}.
	 *
	 * <p>{@link VArrayOfVFloat} is returned as-is, and VFloat[] is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VArrayOfVFloat valueOf(Object value)
		throws VEntityError {
		if (value instanceof VArrayOfVFloat)
			return (VArrayOfVFloat) value;

		if (value instanceof VFloat[])
			return new VArrayOfVFloat((VFloat[]) value);

		throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a VFloat[].
	 *
	 * <p>VFloat[] is returned as-is, and {@link VArrayOfVFloat} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VFloat[] nativeOf(Object value)
		throws VEntityError {
		if (value instanceof VFloat[])
			return (VFloat[]) value;

		if (value instanceof VArrayOfVFloat) {
			VArrayOfVFloat arr = (VArrayOfVFloat) value;
			VFloat[] array = new VFloat[arr.getLength()];
			int index = 0;
			for (VFloat f: arr)
				array[index++] = f;
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
		value = VArrayOfVFloat.valueOf(obj);
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(value);
		return new VCombiner.Pair(null, val_list);
	}

	@Override
	protected VFloat[] createArray(int length) {
		return new VFloat[length];
	}
}
