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
import org.versile.orb.entity.decoder.VNoneDecoder;


/**
 * Represents the VFE VNone type.
 *
 * <p>Holds no actual value, it acts as a cross-platform representation
 * of a "null" reference. A single shared object is used to represent
 * VNone, use {@link #get} to obtain a reference.</p>
 */
public final class VNone extends VEntity {

	static VNone _shared = new VNone();

	// Internal constructor, use static method instead
	VNone() {}

	/**
	 * Create a VNone instance.
	 *
	 * <p>Returns a reference to a single shared VNone instance.</p>
	 *
	 * @return reference to shared static VNone
	 */
	public static VNone get() {
		return _shared;
	}

	/**
	 * Value represented by this object (i.e. null).
	 *
	 * @return represented value
	 */
	public Object getValue() {
		return null;
	}

	@Override
	public Object _v_native() {
		return null;
	}

	@Override
	public String toString() {
		return "VNone";
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
		if (obj != null)
			throw new VEntityError("Cannot convert object");
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(new VNone());
		return new VCombiner.Pair(null, val_list);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		Vector<Object> obj_list = new Vector<Object>();
		obj_list.add(null);
		return new VCombiner.Pair(null, obj_list);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return new VEncoderData(new byte[] {(byte)0xf8}, new byte[0]);
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
			reader.setDecoder(new VNoneDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Converts input to a {@link VNone}.
	 *
	 * <p>{@link VNone} is returned as-is, and null is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static VNone valueOf(Object value)
		throws VEntityError {
		if (value instanceof VNone)
			return (VNone) value;
		else if (value == null)
			return new VNone();
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts (valid) input to null.
	 *
	 * <p>null is returned as-is, and {@link VNone} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static Object nativeOf(Object value)
		throws VEntityError {
		if (value == null || value instanceof VNone)
			return null;
		else
			throw new VEntityError("Cannot convert input value");
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return true;
		else if (other instanceof VNone)
			return true;
		else
			return false;
	}
}
