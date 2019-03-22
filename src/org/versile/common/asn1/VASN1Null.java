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
import org.versile.orb.entity.VNone;



/**
 * An ASN.1 null value.
 */
public class VASN1Null extends VASN1Base {

	public VASN1Null() {
		if (name == null)
			this.name = "NULL";
	}

	public VASN1Null(String name, VASN1Definition definition) {
		super(name, definition);
		if (name == null)
			this.name = "NULL";
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Null.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return null;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		byte[] payload = VASN1Null.berEncNull();
		if (withTag) {
			// X.690: Null always has primitive encoding
			VByteBuffer buf = new VByteBuffer();
			buf.append(this.getTag().encodeDER(false));
			buf.append(payload);
			return buf.popAll();
		}
		else
			return payload;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x05}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	static byte[] berEncNull() {
		return new byte[] {(byte)0x00};
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof VASN1Null || other instanceof VNone || other == null);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "null";
	}
}
