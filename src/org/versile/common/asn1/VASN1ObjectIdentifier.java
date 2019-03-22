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

import java.util.LinkedList;

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 Object Identifier value.
 */
public class VASN1ObjectIdentifier extends VASN1Base {

	VObjectIdentifier value;

	/**
	 * Set up with a value.
	 *
	 * @param value value
	 */
	public VASN1ObjectIdentifier(VObjectIdentifier value) {
		this.value = value;
		if (this.name == null)
			this.name = "OBJECT IDENTIFIER";
	}

	/**
	 * Set up with a value.
	 *
	 * @param value value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1ObjectIdentifier(VObjectIdentifier value, String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value;
		if (this.name == null)
			this.name = "OBJECT IDENTIFIER";
	}

	/**
	 * Get value held by object.
	 *
	 * @return value
	 */
	public VObjectIdentifier getValue() {
		return value;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x06}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1ObjectIdentifier.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VInteger[] _oids = value.getIdentifiers();
		int o_len = _oids.length-1;
		if (o_len < 1)
			o_len = 1;
		VInteger[] oids = new VInteger[o_len];
		int first_val = 0;
		if (_oids.length > 0)
			first_val = 40*_oids[0].getValue().intValue();
		if (_oids.length > 1)
			first_val += _oids[1].getValue().intValue();
		oids[0] = new VInteger(first_val);
		for (int i = 1; i < o_len; i++)
			oids[i] = _oids[i+1];

		VByteBuffer buf = new VByteBuffer();
		LinkedList<Byte> _buf = new LinkedList<Byte>();
		for (VInteger num: oids) {
			byte[] data = VInteger.posint_to_bytes(num.getValue());
			String bits = "";
			for (int i = 0; i < data.length; i++) {
				String add_bits = Integer.toBinaryString(data[i] & 0xff);
				while (add_bits.length() < 8)
					add_bits = "0" + add_bits;
				bits += add_bits;
			}
			int first_bit = bits.indexOf("1");
			if (first_bit >= 0)
				bits = bits.substring(first_bit);
			else
				bits = "";
			while (!bits.isEmpty()) {
				int delim = bits.length() - 7;
				if (delim < 0)
					delim = 0;
				String nxt = "0" + bits.substring(delim);
				_buf.addFirst((byte)(Integer.valueOf(nxt, 2) | 0x80));
				bits = bits.substring(0, delim);
			}
			if (!_buf.isEmpty()) {
				byte last_byte = _buf.removeLast();
				_buf.addLast((byte)(last_byte & 0x7f));
			}
			else
				_buf.addLast((byte)0x00);
			while (!_buf.isEmpty())
				buf.append(new byte[] {_buf.removeFirst()});
		}
		byte[] content = buf.popAll();

		if (withTag) {
			// X.690 specifies octet identifier always has primitive encoding
			buf.append(this.getTag().encodeDER(false));
		}
		buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length)));
		buf.append(content);
		return buf.popAll();
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VASN1ObjectIdentifier)
			return ((VASN1ObjectIdentifier)other).value.equals(value);
		else if (other instanceof VObjectIdentifier)
			return ((VObjectIdentifier)other).equals(value);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
