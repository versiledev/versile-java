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

import java.math.BigInteger;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 integer value.
 */
public class VASN1Integer extends VASN1Base {

	VInteger value;

	/**
	 * Set up integer.
	 *
	 * @param value integer value
	 */
	public VASN1Integer(VInteger value) {
		this.value = value;
		if (name == null)
			name = "INTEGER";
	}

	/**
	 * Set up integer.
	 *
	 * @param value integer value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1Integer(VInteger value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value;
		if (name == null)
			name = "INTEGER";
	}

	/**
	 * Set up integer.
	 *
	 * @param value integer value
	 */
	public VASN1Integer(Number value) {
		this.value = new VInteger(value);
		if (name == null)
			name = "INTEGER";
	}

	/**
	 * Set up integer.
	 *
	 * @param value integer value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1Integer(Number value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = new VInteger(value);
		if (name == null)
			name = "INTEGER";
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
			return VASN1Tag.fromDER(new byte[] {(byte)0x02}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	static byte[] berEncIntFamily(VInteger value)
			throws VASN1Exception {
		if (value.equals(new VInteger(0)))
			return new byte[] {(byte)0x01, (byte)0x00};
		else if (value.getBigIntegerValue().compareTo(BigInteger.ZERO) > 0) {
			VByteBuffer buf = new VByteBuffer();
			byte[] content = VInteger.posint_to_bytes(value.getValue());
			if ((content[0] & 0x80) != 0) {
				buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length+1)));
				buf.append(new byte[] {(byte)0x00});
			}
			else {
				buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length)));
			}
			buf.append(content);
			return buf.popAll();
		}
		else {
			Number neg_value = value.getBigIntegerValue().multiply(BigInteger.valueOf(-1));
			byte[] block = VInteger.posint_to_bytes(neg_value);
			BigInteger _val = BigInteger.ONE.shiftLeft(8*block.length);
			_val = _val.add(value.getBigIntegerValue());
			byte[] content = VInteger.posint_to_bytes(_val);
			VByteBuffer buf = new VByteBuffer();
			buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length)));
			buf.append(content);
			return buf.popAll();
		}
	}

	static byte[] berEncInteger(VInteger value)
			throws VASN1Exception {
		return VASN1Integer.berEncIntFamily(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VASN1Integer)
			return (((VASN1Integer)other).value).equals(this.value);
		else if (other instanceof Number || other instanceof VInteger)
			return value.equals(other);
		else
			return false;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1Integer.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value.getValue();
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		byte[] payload = VASN1Integer.berEncInteger(value);
		if (withTag) {
			// X.690: Integer always has primitive encoding
			VByteBuffer buf = new VByteBuffer();
			buf.append(this.getTag().encodeDER(false));
			buf.append(payload);
			return buf.popAll();
		}
		else
			return payload;
	}
}
