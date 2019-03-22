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
import org.versile.orb.entity.VInteger;


/**
 * An ASN.1 Enumerated value.
 */
public class VASN1Enumerated extends VASN1Base {

	VInteger value;

	/**
	 * Set up enumerated value.
	 *
	 * @param value integer value
	 */
	public VASN1Enumerated(VInteger value) {
		this.value = value;
		if (this.name == null)
			this.name = "ENUMERATED";
	}

	/**
	 * Set up enumerated value.
	 *
	 * @param value integer value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1Enumerated(VInteger value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value;
		if (this.name == null)
			this.name = "ENUMERATED";
	}

	/**
	 * Get the associated value.
	 *
	 * @return value
	 */
	public VInteger getValue() {
		return value;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x0a}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1Enumerated.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return this;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690: Enumerated always has primitive encoding
			buf.append(this.getTag().encodeDER(false));
		}
		buf.append(new VASN1Integer(value).encodeDER(false));
		return buf.popAll();
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VASN1Enumerated)
			return (((VASN1Enumerated)other).value).equals(this.value);
		else
			return false;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
