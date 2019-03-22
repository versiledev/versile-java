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
 * Definition for the ASN.1 Boolean type.
 */
public class VASN1DefBoolean extends VASN1Definition {

	public VASN1DefBoolean() {
	}

	public VASN1DefBoolean(String name) {
		super(name);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Boolean.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int num_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x01)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			num_read += 1;
		}
		if (data.length < 2)
			throw new VASN1Exception("Incomplete data");
		if (data[0] != (byte)0x01)
			throw new VASN1Exception("Invalid encoding");
		num_read += 2;
		if (data[1] == (byte)0x00)
			return new ParseResult(new VASN1Boolean(false), num_read);
		else
			return new ParseResult(new VASN1Boolean(true), num_read);
	}
}
