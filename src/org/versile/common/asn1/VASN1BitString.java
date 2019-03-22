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

import org.versile.common.util.VBitfield;
import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 BitString value.
 */
public class VASN1BitString extends VASN1Base {

	VBitfield value;

	/**
	 * Set up bit string.
	 *
	 * @param value value
	 */
	public VASN1BitString(VBitfield value) {
		this.value = value.clone();
		name = "BIT STRING";
	}

	/**
	 * Set up bit string.
	 *
	 * @param value value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1BitString(VBitfield value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value.clone();
		if (name == null)
			this.name = "BIT STRING";
	}

	/**
	 * Get held value.
	 *
	 * @return value
	 */
	public VBitfield getValue() {
		return value.clone();
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x03}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1BitString.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value.clone();
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		return this.encodeBER(withTag, true);
	}

	public byte[] encodeBER(boolean withTag, boolean useDefinite) throws VASN1Exception {
		boolean[] bits = value.getBits();
		int padding = bits.length % 8;
		if (padding > 0) {
			padding = 8 - padding;
			boolean[] _bits = new boolean[bits.length+padding];
			for (int i = 0; i < bits.length; i++)
				_bits[i] = bits[i];
			for (int i = 0; i < padding; i++)
				_bits[i+bits.length] = false;
			bits = _bits;
		}
		VByteBuffer buf = new VByteBuffer();
		byte first_byte = (byte)padding;
		buf.append(new byte[] {first_byte});
		for (int i = 0; i < bits.length; i += 8) {
			String bstr = "0";
			for (int j = 0; j < 8; j++)
				if (bits[i+j])
					bstr += "1";
				else
					bstr += "0";
			buf.append(new byte[] {(byte)Integer.valueOf(bstr, 2).intValue()});
		}
		byte[] content = buf.popAll();

		if (useDefinite) {
			buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length)));
			buf.append(content);
		}
		else {
			buf.append(VASN1Base.berEncLengthIndefinite());
			buf.append(VASN1Base.berEncContentIndefinite(content));
		}
		byte[] payload = buf.popAll();

		if (withTag) {
			// X.690 section 10.2 specifies DER uses primitive form
			buf.append(this.getTag().encodeDER(false));
			buf.append(payload);
			return buf.popAll();
		}
		else
			return payload;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VASN1BitString) {
			return (((VASN1BitString)other).getValue().equals(value));
		}
		else if (other instanceof VBitfield) {
			return ((VBitfield)other).equals(value);
		}
		return false;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
