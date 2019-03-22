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

import java.io.UnsupportedEncodingException;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VString;



/**
 * An ASN.1 NumericString value.
 */
public class VASN1NumericString extends VASN1Base implements VASN1String{

	String value;

	/**
	 * Set up object.
	 *
	 * <p>Allows only the characters '0'..'9' and ' '.</p>
	 *
	 * @param value value
	 * @throws VASN1Exception invalid string
	 */
	public VASN1NumericString(String value)
			throws VASN1Exception {
		for (int i = 0; i < value.length(); i++)
			if ("0123456789 ".indexOf(value.charAt(i)) < 0)
				throw new VASN1Exception("Illegal NumericString character");
		this.value = value;
		if (this.name == null)
			this.name = "NumericString";
	}

	/**
	 * Set up object.
	 *
	 * <p>Allows only the characters '0'..'9' and ' '.</p>
	 *
	 * @param value value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 * @throws VASN1Exception invalid string
	 */
	public VASN1NumericString(String value, String name, VASN1Definition definition)
			throws VASN1Exception {
		super(name, definition);
		for (int i = 0; i < value.length(); i++)
			if ("0123456789 ".indexOf(value.charAt(i)) < 0)
				throw new VASN1Exception("Illegal NumericString character");
		this.value = value;
		if (this.name == null)
			this.name = "NumericString";
	}

	/**
	 * Get held string.
	 *
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x12}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1NumericString.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690 section 10.2 specifies DER uses primitive form
			buf.append(this.getTag().encodeDER(false));
		}
		try {
			buf.append(new VASN1OctetString(value.getBytes("ASCII")).encodeDER(false));
		} catch (UnsupportedEncodingException e) {
			throw new VASN1Exception(e);
		}
		return buf.popAll();
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VASN1String)
			return value.equals(((VASN1String)obj).getValue());
		else if (obj instanceof String)
			return value.equals((String)obj);
		else if (obj instanceof VString)
			return value.equals(((VString)obj).getValue());
		else
			return false;
	}

	@Override
	public String toString() {
		return value;
	}
}
