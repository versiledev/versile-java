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

import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.orb.entity.decoder.VBooleanDecoder;


/**
 * Represents the VFE VBoolean type.
 *
 * <p>Holds a boolean true/false value.</p>
 */
public final class VBoolean extends VEntity {

	boolean value;

	/**
	 * Initializes with the provided value.
	 *
	 * @param value object value
	 */
	public VBoolean(boolean value) {
		this.value = value;
	}

	/**
	 * Value represented by this object.
	 *
	 * @return value
	 */
	public boolean getValue() {
		return value;
	}

	@Override
	public Boolean _v_native() {
		return (Boolean) value;
	}

	@Override
	public String toString() {
		return ((Boolean)value).toString();
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
			value = new VBoolean((Boolean)(obj));
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
		if (value)
			return new VEncoderData(new byte[] {(byte)0xf2}, new byte[0]);
		else
			return new VEncoderData(new byte[] {(byte)0xf1}, new byte[0]);
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
			reader.setDecoder(new VBooleanDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Converts input to a {@link VBoolean}.
	 *
	 * <p>{@link VBoolean} is returned as-is, and Boolean is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static VBoolean valueOf(Object value)
		throws VEntityError {
		if (value instanceof VBoolean)
			return (VBoolean) value;
		else if (value instanceof Boolean)
			return new VBoolean((Boolean)value);
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a Boolean.
	 *
	 * <p>Boolean is returned as-is, and {@link VBoolean} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static Boolean nativeOf(Object value)
		throws VEntityError {
		if (value instanceof Boolean)
			return (Boolean)value;
		else if (value instanceof VBoolean)
			return ((VBoolean)value).getValue();
		else
			throw new VEntityError("Cannot convert input value");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VBoolean)
			return ((VBoolean)other).value == value;
		else if (other instanceof Boolean) {
			return (Boolean)other == value;
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return ((Boolean)value).hashCode();
	}
}
