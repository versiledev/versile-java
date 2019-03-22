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
 * Definition for an undetermined universal type.
 */
public class VASN1DefUniversal extends VASN1Definition {

	boolean allowUnknown;

	/**
	 * Set up definition which allows parsing unknown types.
	 */
	public VASN1DefUniversal() {
		allowUnknown = false;
	}

	/**
	 * Set up unnamed definition.
	 *
	 * @param allowUnknown if true parse unknown types
	 */
	public VASN1DefUniversal(boolean allowUnknown) {
		this.allowUnknown = allowUnknown;
	}

	/**
	 * Set up definition.
	 *
	 * <p>If allowing unknowns the unknown types are returned as
	 * {@link VASN1Unknown}.</p>
	 *
	 * @param allowUnknown if true parse unknown types
	 * @param name definition name (or null)
	 */
	public VASN1DefUniversal(boolean allowUnknown, String name) {
		super(name);
		this.allowUnknown = allowUnknown;
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int tag = data[0] & 0xff;
		VASN1Definition def;
		if (tag == 0x01)
			def = new VASN1DefBoolean(name);
		else if (tag == 0x02)
			def = new VASN1DefInteger(name);
		else if (tag == 0x03)
			def = new VASN1DefBitString(name);
		else if (tag == 0x04)
			def = new VASN1DefOctetString(name);
		else if (tag == 0x05)
			def = new VASN1DefNull(name);
		else if (tag == 0x06)
			def = new VASN1DefObjectIdentifier(name);
		else if (tag == 0x0a)
			def = new VASN1DefEnumerated(name);
		else if (tag == 0x0c)
			def = new VASN1DefUTF8String(name);
		else if (tag == 0x12)
			def = new VASN1DefNumericString(name);
		else if (tag == 0x13)
			def = new VASN1DefPrintableString(name);
		else if (tag == 0x16)
			def = new VASN1DefIA5String(name);
		else if (tag == 0x17)
			def = new VASN1DefUTCTime(name);
		else if (tag == 0x18)
			def = new VASN1DefGeneralizedTime(name);
		else if (tag == 0x1a)
			def = new VASN1DefVisibleString(name);
		else if (tag == 0x1c)
			def = new VASN1DefUniversalString(name);
		else if (tag == 0x30) {
			VASN1Definition _uasn1def = new VASN1DefUniversal(allowUnknown, null);
			def = new VASN1DefSequenceOf(_uasn1def, name);
		}
		else if (tag == 0x31) {
			VASN1Definition _uasn1def = new VASN1DefUniversal(allowUnknown, null);
			def = new VASN1DefSetOf(_uasn1def, name);
		}
		else if (allowUnknown) {
			def = new VASN1DefUnknown(name);
		}
		else
			throw new VASN1Exception("Not a supported universal type");
		return def.parseDER(data);
	}
}
