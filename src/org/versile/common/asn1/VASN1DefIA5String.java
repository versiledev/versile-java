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


/**
 * Definition for the ASN.1 IA5String type.
 */
public class VASN1DefIA5String extends VASN1Definition {

	public VASN1DefIA5String() {
	}

	public VASN1DefIA5String(String name) {
		super(name);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1IA5String.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Empty data");
		int tot_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x16)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_read += 1;
		}
		ParseResult o_dec = new VASN1DefOctetString().parseDER(data, false);
		byte[] str_bytes = ((VASN1OctetString)o_dec.getResult()).value;
		String str;
		try {
			str = new String(str_bytes, "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new VASN1Exception(e);
		}
		tot_read += o_dec.getNumRead();
		if (!str.matches("\\A\\p{ASCII}*\\z"))
			throw new VASN1Exception("Illegal IA5String character");
		return new ParseResult(new VASN1IA5String(str), tot_read);
	}
}
