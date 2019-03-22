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
 * Definition for the ASN.1 Null type.
 */
public class VASN1DefNull extends VASN1Definition {

	public VASN1DefNull() {
	}

	public VASN1DefNull(String name) {
		super(name);
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int num_read = 0;
		if (withTag) {
			if ((data[0] & 0xff) != 0x05)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] tmp = new byte[data.length-1];
			for (int i = 0; i < tmp.length; i++)
				tmp[i] = data[i+1];
			data = tmp;
			num_read += 1;
		}
		return new ParseResult(new VASN1Null(), num_read);
	}
	@Override
	public VASN1Tag getTag() {
		return VASN1Null.getUniversalTag();
	}

}
