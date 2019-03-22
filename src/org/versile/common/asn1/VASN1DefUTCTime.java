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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Definition for the ASN.1 UTCTime type.
 */
public class VASN1DefUTCTime extends VASN1Definition {

	public VASN1DefUTCTime() {
	}

	public VASN1DefUTCTime(String name) {
		super(name);
	}

	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Empty data");
		int tot_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x17)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_read += 1;
		}
		ParseResult o_dec = new VASN1DefOctetString().parseDER(data, false);
		byte[] str_bytes = ((VASN1OctetString)o_dec.getResult()).value;
		tot_read += o_dec.getNumRead();
		String str;
		try {
			str = new String(str_bytes, "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new VASN1Exception(e);
		}
		if (!str.endsWith("Z"))
			throw new VASN1Exception("Invalid encoding");
		str = str.substring(0, str.length()-1);
		SimpleDateFormat fmt = new SimpleDateFormat("yyMMddHHmmss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date;
		try {
			date = fmt.parse(str);
		} catch (ParseException e) {
			throw new VASN1Exception(e);
		}
		return new ParseResult(new VASN1UTCTime(date), tot_read);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1UTCTime.getUniversalTag();
	}

}
