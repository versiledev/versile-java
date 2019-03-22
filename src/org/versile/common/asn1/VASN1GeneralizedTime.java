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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.versile.common.util.VByteBuffer;


/**
 * An ASN.1 GeneralizedTime value.
 */
public class VASN1GeneralizedTime extends VASN1Base implements VASN1Time {

	Date date;
	int micro_sec;

	/**
	 * Set up object with 0 microseconds.
	 *
	 * <p>Strips the milliseconds component off the provided date and moves it into the
	 * microseconds component.</p>
	 *
	 * @param date date component
	 */
	public VASN1GeneralizedTime(Date date)
			throws VASN1Exception {
		long time = date.getTime();
		int millisecs = (int)(time % 1000);
		this.date = new Date(time - millisecs);
		this.micro_sec = 1000 * millisecs;
		if (this.name == null)
			this.name = "GeneralizedTime";
	}

	/**
	 * Set up object.
	 *
	 * <p>Strips the milliseconds component off the provided date and moves it into the
	 * microseconds component.</p>
	 *
	 * <p>The microseconds component (including Date object carry-over) must be in the range 0..999999</p>
	 *
	 * @param date date component
	 * @param microSeconds additional microseconds
	 * @throws VASN1Exception invalid microseconds value
	 */
	public VASN1GeneralizedTime(Date date, int microSeconds)
			throws VASN1Exception {
		long time = date.getTime();
		int millisecs = (int)(time % 1000);
		this.date = new Date(time - millisecs);
		this.micro_sec = 1000 * millisecs;
		this.micro_sec += microSeconds;
		if (this.micro_sec < 0 || this.micro_sec >= 1000000)
			throw new VASN1Exception("Invalid microSeconds value, must be 0 <= val <= 999999");
		if (this.name == null)
			this.name = "GeneralizedTime";
	}

	/**
	 * Set up object.
	 *
	 * <p>The microseconds component must be in the range 0..999999</p>
	 *
	 * @param date date component
	 * @param microSeconds additional microseconds
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 * @throws VASN1Exception invalid microseconds value
	 */
	public VASN1GeneralizedTime(Date date, int microSeconds,
			                    String name, VASN1Definition definition)
	        throws VASN1Exception {
		super(name, definition);
		if (this.name == null)
			this.name = "GeneralizedTime";
		if (microSeconds < 0 || microSeconds >= 1000000)
			throw new VASN1Exception("Invalid microSeconds value, must be 0 <= val <= 999999");
		this.date = date;
		this.micro_sec = microSeconds;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x18}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the value's date component.
	 *
	 * @return date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Get the value's microseconds component.
	 *
	 * @return microseconds
	 */
	public int getMicroSeconds() {
		return micro_sec;
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1GeneralizedTime.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return this;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690 section 10.2 specifies DER uses primitive form
			buf.append(this.getTag().encodeDER(false));
		}
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date_str = fmt.format(date);
		if (micro_sec != 0) {
			date_str += "." + String.format("%06d", micro_sec);;
			while (date_str.endsWith("0"))
				date_str = date_str.substring(0, date_str.length()-1);
		}
		date_str += "Z";
		try {
			buf.append(new VASN1OctetString(date_str.getBytes("ASCII")).encodeDER(false));
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
		return buf.popAll();
	}

	@Override
	public VASN1Base getTimeObject() {
		return this;
	}

	@Override
	public int hashCode() {
		int code = date.hashCode();
		if (micro_sec != 0)
			code ^= ((Integer)micro_sec).hashCode();
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VASN1GeneralizedTime) {
			VASN1GeneralizedTime g_other = (VASN1GeneralizedTime) obj;
			return (g_other.date.equals(date) && g_other.micro_sec == micro_sec);
		}
		else if (obj instanceof VASN1UTCTime) {
			VASN1UTCTime g_other = (VASN1UTCTime) obj;
			return (g_other.getValue().equals(date) && micro_sec == 0);
		}
		else if (obj instanceof Date) {
			long time_other = ((Date)obj).getTime();
			int millisec = (int)(time_other % 1000);
			return (new Date(time_other-millisec).equals(date) && micro_sec == 1000*millisec);
		}
		else
			return false;
	}

	@Override
	public String toString() {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String result = fmt.format(date);
		if (micro_sec != 0) {
			result += "." + String.format("%06d", micro_sec);
			while (result.endsWith("0"))
				result = result.substring(0, result.length()-1);
			result += " UTC";
		}
		return result;
	}
}
