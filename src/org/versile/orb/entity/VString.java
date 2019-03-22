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
import org.versile.orb.entity.decoder.VStringDecoder;


/**
 * Represents the VFE VString type.
 *
 * <p>Holds a String value.</p>
 */
public final class VString extends VEntity {

	String value;

	/**
	 * Set up string object.
	 *
	 * @param value string value
	 */
	public VString(String value) {
		this.value = value;
	}

	/**
	 * Value represented by this object.
	 *
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Enables comparison with String or {@link VString}.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VString)
			return value.equals(((VString)obj).getValue());
		else if (obj instanceof String)
			return value.equals((String)obj);
		else
			return false;
	}

	@Override
	public String _v_native() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_encode(ctx,  explicit, null);
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
				value = new VString((String)obj);
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

	/**
	 * Generates encoder parameters for serializing the object.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param ctx context for object serialization
	 * @param explicit if True use explicit VEntity encoding
	 * @param encoding string encoding format
	 * @return encoding parameters
	 * @throws VEntityWriterException writer error
	 */
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit, String encoding)
			throws VEntityWriterException {
		String ctx_enc = ctx.getStrEncoding();
		boolean with_enc = true;
		if (ctx_enc != null && (encoding == null || encoding == ctx_enc)) {
			encoding = ctx_enc;
			with_enc = false;
		}
		else if (encoding == null)
			encoding = "utf8";

		VEncoderData result;
		if (with_enc) {
			result = new VEncoderData(new byte[] {(byte)0xf5}, null);
			try {
				result.addEmbedded(new VBytes(encoding.getBytes(encoding)), false);
			} catch (Exception e) {
				throw new VEntityWriterException("String encoding error");
			}
		}
		else
			result = new VEncoderData(new byte[] {(byte)0xf4}, null);

		try {
			result.addEmbedded(new VBytes(value.getBytes(encoding)), false);
		} catch (Exception e) {
			throw new VEntityWriterException("String encoding error");
		}
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
			reader.setDecoder(new VStringDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Accepted (valid) string encodings.
	 *
	 * @return accepted encoding names
	 */
	public static String[] _v_codecs() {
		return new String[] {"utf8", "utf16"};
	}

	/**
	 * Converts input to a {@link VString}.
	 *
	 * <p>{@link VString} is returned as-is, and String is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static VString valueOf(Object value)
		throws VEntityError {
		if (value instanceof VString)
			return (VString) value;
		else if (value instanceof String)
			return new VString((String)value);
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a String.
	 *
	 * <p>String is returned as-is, and {@link VString} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static String nativeOf(Object value)
		throws VEntityError {
		if (value instanceof String)
			return (String)value;
		else if (value instanceof VString)
			return ((VString)value).getValue();
		else
			throw new VEntityError("Cannot convert input value");
	}
}
