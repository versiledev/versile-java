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

package org.versile.common.asn1;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VBoolean;



/**
 * An ASN.1 boolean value.
 */
public class VASN1Boolean extends VASN1Base {

	boolean value;

	/**
	 * Set up value.
	 *
	 * @param value value
	 */
	public VASN1Boolean(boolean value) {
		this.value = value;
		if (name == null)
			name = "BOOLEAN";
	}

	/**
	 * Set up value.
	 *
	 * @param value value
	 * @param name type name (or null)
	 * @param definition ASN.1 definition (or null)
	 */
	public VASN1Boolean(boolean value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value;
		if (name == null)
			name = "BOOLEAN";
	}

	/**
	 * Get the associated value.
	 *
	 * @return value
	 */
	public boolean getValue() {
		return value;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x01}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	static byte[] berEncBoolean(boolean value) {
		return VASN1Boolean.berEncBoolean(value, (byte)0xff);
	}

	static byte[] berEncBoolean(boolean value, byte trueValue) {
		if (trueValue == (byte)0x00)
			throw new RuntimeException("Illegal ASN1Boolean byte value for 'true'");
		if (value)
			return new byte[] {(byte)0x01, trueValue};
		else
			return new byte[] {(byte)0x01, (byte)0x00};
	}

	@Override
	public int hashCode() {
		return ((Boolean)value).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VASN1Boolean)
			return ((VASN1Boolean)other).value == value;
		else if (other instanceof VBoolean)
			return ((VBoolean)other).getValue() == value;
		else if (other instanceof Boolean)
			return (Boolean)other == value;
		else
			return false;
	}

	@Override
	public String toString() {
		return ((Boolean)value).toString();
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1Boolean.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		byte[] payload = VASN1Boolean.berEncBoolean(value);
		if (withTag) {
			// X.690: Boolean always has primitive encoding
			VByteBuffer buf = new VByteBuffer();
			buf.append(this.getTag().encodeDER(false));
			buf.append(payload);
			return buf.popAll();
		}
		else
			return payload;
	}
}
