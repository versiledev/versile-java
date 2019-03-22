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
 * An ASN.1 data object of unknown encoding.
 */
public class VASN1Unknown extends VASN1Base {

	byte[] data;
	boolean isExplicit;

	public VASN1Unknown(byte[] data, boolean isExplicit) {
		this.data = data;
		this.isExplicit = isExplicit;
	}

	@Override
	public VASN1Tag getTag()
			throws VASN1Exception {
		return VASN1Tag.fromDER(data).getTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return this;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		if (isExplicit)
			return data;
		else
			throw new VASN1Exception("Implicit encoding not known");
	}

}
