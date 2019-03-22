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

import org.versile.orb.entity.VInteger;



/**
 * Definition for the ASN.1 Integer type.
 */
public class VASN1DefInteger extends VASN1Definition {

	public VASN1DefInteger() {
	}

	public VASN1DefInteger(String name) {
		super(name);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param value integer value
	 */
	public VASN1Integer create(VInteger value) {
		return new VASN1Integer(value, this.name, this);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Integer.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int num_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x02)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			num_read += 1;
		}
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");
		DecodedLength ldec = VASN1Definition.berDecLength(data);
		if (ldec == null)
			throw new VASN1Exception("Indefinite representation not supported");
		if (ldec.getDecodedLength().getBigIntegerValue().compareTo(BigInteger.ONE) < 0)
			throw new VASN1Exception("Decoded length must be positive");
		int _nread = ldec.getBytesRead();
		int tot_length = _nread + ldec.getDecodedLength().getValue().intValue();
		if (data.length < tot_length)
			throw new VASN1Exception("Incomplete data");
		byte[] content = new byte[tot_length-_nread];
		for (int i = 0; i < content.length; i++)
			content[i] = data[_nread+i];
		num_read += tot_length;
		BigInteger _val = new VInteger(VInteger.bytes_to_posint(content)).getBigIntegerValue();
		if ((content[0] & 0x80) != 0) {
			_val = _val.subtract(BigInteger.ONE.shiftLeft(8*content.length));
		}
		return new ParseResult(new VASN1Integer(new VInteger(_val)), num_read);
	}

}
