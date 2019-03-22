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

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VCombiner;
import org.versile.orb.entity.decoder.VBytesDecoder;


/**
 * Represents the VFE VBytes type.
 *
 * <p>Holds an array of byte data.</p>
 */
public final class VBytes extends VEntity {

	byte[] value;

	/**
	 * Initialize with provided value.
	 *
	 * @param value byte array data
	 */
	public VBytes(byte[] value) {
		this.value = value;
	}

	/**
	 * Initialize with provided value.
	 *
	 * @param value byte array data
	 */
	public VBytes(Byte[] value) {
		this.value = new byte[value.length];
		for (int i = 0; i < value.length; i++)
			this.value[i] = value[i];
	}

	/**
	 * Get represented value.
	 *
	 * <p>Returns a new shallow copy of held array.</p>
	 *
	 * @return value
	 */
	public byte[] getValue() {
		byte[] result = new byte[value.length];
		for (int i = 0; i < result.length; i++)
			result[i] = value[i];
		return value;
	}

	/**
	 * Get byte array element.
	 *
	 * @param index array index
	 * @return byte value at index
	 */
	public byte get(int index) {
		return value[index];
	}

	/**
	 * Get number of bytes in array.
	 *
	 * @return number of bytes
	 */
	public int length() {
		return value.length;
	}

	@Override
	public byte[] _v_native() {
		return value;
	}

	@Override
	public String toString() {
		final String _vals = "0123456789abcdef";
		String result = "b'";
		for (byte b: value) {
			int val = (int)(b & 0xff);
			result += "\\x";
			result += String.valueOf(_vals.charAt((val >>> 4) & 0x0f));
			result += String.valueOf(_vals.charAt(val & 0x0f));
		}
		result += "'";
		return result;
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
				value = VBytes.valueOf(obj);
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
		VByteBuffer header = new VByteBuffer();
		if (explicit)
			header.append(new byte[] {(byte)0xf3});
		header.append(VInteger.posint_to_netbytes(value.length));
		return new VEncoderData(header.popAll(), value);
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
			reader.setDecoder(new VBytesDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Converts input to a {@link VBytes}.
	 *
	 * <p>{@link VBytes} is returned as-is, and byte[] or Byte[] is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static VBytes valueOf(Object value)
		throws VEntityError {
		if (value instanceof VBytes)
			return (VBytes) value;
		else if (value instanceof byte[])
			return new VBytes((byte[])value);
		else if (value instanceof Byte[])
			return new VBytes((Byte[])value);
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a byte[].
	 *
	 * <p>byte[] is returned as-is, {@link VBytes} or Byte[] is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static byte[] nativeOf(Object value)
		throws VEntityError {
		if (value instanceof byte[])
			return (byte[]) value;
		if (value instanceof Byte[]) {
			Byte[] b_val = (Byte[]) value;
			byte[] result = new byte[b_val.length];
			for (int i = 0; i < result.length; i++)
				result[i] = b_val[i];
			return result;
		}
		else if (value instanceof VBytes)
			return ((VBytes)value).getValue();
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Concatenates multiple arrays into one single array.
	 *
	 * <p>Convenience method for concatinating native arrays.</p>
	 *
	 * @param data arrays to concatenate
	 * @return concatenated array
	 */
	public static byte[] concat(byte[]... data) {
		int length = 0;
		for (byte[] item: data)
			if (item != null)
				length += item.length;
		byte[] result = new byte[length];
		int pos = 0;
		for (byte[] item: data)
			if (item != null && item.length > 0) {
				System.arraycopy(item, 0, result, pos, item.length);
				pos += item.length;
			}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof byte[]) {
			byte[] data = (byte[]) obj;
			if (value.length != data.length)
				return false;
			for (int i = 0; i < value.length; i++)
				if (value[i] != data[i])
					return false;
			return true;
		}
		else if (obj instanceof VBytes)
			return this.equals(((VBytes)obj).value);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
