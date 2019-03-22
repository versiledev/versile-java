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
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 Octet String value.
 */
public class VASN1OctetString extends VASN1Base {

	byte[] value;

	/**
	 * Set up value.
	 *
	 * @param value octet string value
	 */
	public VASN1OctetString(byte[] value) {
		if (name == null)
			name = "OCTET STRING";
		this.value = new byte[value.length];
		for (int i = 0; i < value.length; i++)
			this.value[i] = value[i];
	}

	/**
	 * Set up value.
	 *
	 * @param value octet string value
	 */
	public VASN1OctetString(VBytes value) {
		if (name == null)
			name = "OCTET STRING";
		this.value = value.getValue();
	}

	/**
	 * Set up value.
	 *
	 * @param value octet string value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1OctetString(byte[] value, String name, VASN1Definition definition) {
		super(name, definition);
		if (this.name == null)
			this.name = "OCTET STRING";
		this.value = new byte[value.length];
		for (int i = 0; i < value.length; i++)
			this.value[i] = value[i];
	}

	/**
	 * Get held value.
	 *
	 * @return value
	 */
	public byte[] getValue() {
		byte[] result = new byte[value.length];
		for (int i = 0 ; i < result.length; i++)
			result[i] = value[i];
		return result;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x04}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1OctetString.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		byte[] result = new byte[value.length];
		for (int i = 0; i < result.length; i++)
			result[i] = value[i];
		return result;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690 section 10.2 specifies DER uses primitive form
			buf.append(this.getTag().encodeDER(false));
		}
		buf.append(VASN1Base.berEncLengthDefinite(new VInteger(value.length)));
		buf.append(value);
		return buf.popAll();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof byte[])
			return new VBytes(value).equals(new VBytes((byte[])other));
		else if (other instanceof VBytes)
			return ((VBytes)other).equals(new VBytes(value));
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return new VBytes(value).toString();
	}
}
