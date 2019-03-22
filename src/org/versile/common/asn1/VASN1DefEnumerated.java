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
 * Definition for the ASN.1 Enumerated type.
 */
public class VASN1DefEnumerated extends VASN1Definition {

	public VASN1DefEnumerated() {
	}

	public VASN1DefEnumerated(String name) {
		super(name);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Enumerated.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Empty data");
		int tot_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x0a)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_read += 1;
		}
		ParseResult i_dec = new VASN1DefInteger().parseDER(data, false);
		tot_read += i_dec.getNumRead();
		VASN1Integer as_int = (VASN1Integer)i_dec.getResult();
		return new ParseResult(new VASN1Enumerated(as_int.getValue()), tot_read);
	}

}
