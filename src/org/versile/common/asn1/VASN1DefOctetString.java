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


/**
 * Definition for the ASN.1 OctetString type.
 */
public class VASN1DefOctetString extends VASN1Definition {

	public VASN1DefOctetString() {
	}

	public VASN1DefOctetString(String name) {
		super(name);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1OctetString.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int tot_length = 0;
		if (withTag) {
			if (data[0] != (byte)0x04)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_length += 1;
		}
		DecodedLength ldec = VASN1Definition.berDecLength(data);
		if (ldec == null)
			throw new VASN1Exception("Indefinite length not supported");
		int l_len = ldec.getBytesRead();
		int c_len = ldec.getDecodedLength().getValue().intValue();
		tot_length += l_len + c_len;
		if (data.length < l_len + c_len)
			throw new VASN1Exception("Incomplete data");
		byte[] content = new byte[c_len];
		for (int i = 0; i < c_len; i++)
			content[i] = data.clone()[l_len+i];
		return new ParseResult(new VASN1OctetString(content), tot_length);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param value target value
	 */
	public VASN1OctetString create(byte[] value) {
		return new VASN1OctetString (value, this.name, this);
	}

}
