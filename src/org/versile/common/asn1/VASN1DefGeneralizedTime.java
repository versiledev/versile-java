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
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Definition for the ASN.1 GeneralizedTime type.
 */
public class VASN1DefGeneralizedTime extends VASN1Definition {

	public VASN1DefGeneralizedTime() {
	}

	public VASN1DefGeneralizedTime(String name) {
		super(name);
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Empty data");
		int tot_read = 0;
		if (withTag) {
			if (data[0] != (byte)0x18)
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
			throw new VASN1Exception();
		str = str.substring(0, str.length()-1);
		String micros = "000000";
		if (str.indexOf('.') >= 0) {
			String[] subs = str.split("\\.");
			if (subs.length != 2)
				throw new VASN1Exception("Invalid encoding");
			micros = subs[1];
			str = subs[0];
		}
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date;
		try {
			date = fmt.parse(str);
		} catch (ParseException e) {
			throw new VASN1Exception(e);
		}
		int msec;
		try {
			while (micros.length() < 6)
				micros += "0";
			BigInteger _micros = new BigInteger(micros);
			if (_micros.compareTo(BigInteger.ZERO) < 0
				|| _micros.compareTo(BigInteger.valueOf(999999)) > 0)
				throw new VASN1Exception("Invalid microseconds components");
			msec = _micros.intValue();
		} catch (NumberFormatException e) {
			throw new VASN1Exception(e);
		}
		return new ParseResult(new VASN1GeneralizedTime(date, msec), tot_read);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1GeneralizedTime.getUniversalTag();
	}

}
