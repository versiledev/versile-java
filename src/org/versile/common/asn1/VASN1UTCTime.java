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
 * An ASN.1 UTCTime value.
 */
public class VASN1UTCTime extends VASN1Base implements VASN1Time {

	Date value;

	/**
	 * Set up object.
	 *
	 * <p>Strips the milliseconds component off the provided date as ASN.1 UTCTime does
	 * not track time at less than second precision.</p>
	 *
	 * @param value value
	 */
	public VASN1UTCTime(Date value) {
		long time = value.getTime();
		time = time - time % 1000;
		this.value = new Date(time);
		if (this.name == null)
			this.name = "UTCTime";
	}

	/**
	 * Set up object.
	 *
	 * <p>Strips the milliseconds component off the provided date as ASN.1 UTCTime does
	 * not track time at less than second precision.</p>
	 *
	 * @param value value
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1UTCTime(Date value, String name, VASN1Definition definition) {
		super(name, definition);
		long time = value.getTime();
		time = time - time % 1000;
		this.value = new Date(time);
		if (this.name == null)
			this.name = "UTCTime";
	}

	/**
	 * Get held value.
	 *
	 * @return value
	 */
	public Date getValue() {
		return value;
	}

	/**
	 * Get held date value.
	 *
	 * <p>Same as {@link #getValue()}.</p>
	 *
	 * @return value
	 */
	public Date getDate() {
		return value;
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x17}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1UTCTime.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return value;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690 section 10.2 specifies DER uses primitive form
			buf.append(this.getTag().encodeDER(false));
		}
		SimpleDateFormat fmt = new SimpleDateFormat("yyMMddHHmmss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		byte[] date_b;
		try {
			date_b = (fmt.format(value) + "Z").getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
		buf.append(new VASN1OctetString(date_b).encodeDER(false));
		return buf.popAll();
	}

	@Override
	public VASN1Base getTimeObject() {
		return this;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VASN1UTCTime) {
			return value.equals(((VASN1UTCTime)obj).getValue());
		}
		else if (obj instanceof VASN1GeneralizedTime) {
			VASN1GeneralizedTime g_time = (VASN1GeneralizedTime) obj;
			if (g_time.date.equals(value) && g_time.micro_sec == 0)
				return true;
			else
				return false;
		}
		else if (obj instanceof Date)
			return value.equals((Date)obj);
		else
			return false;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
