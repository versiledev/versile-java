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

import org.versile.common.util.VByteBuffer;


/**
 * An ASN.1 Set.
 */
public class VASN1Set extends VASN1Sequence {

	/**
	 * Set up set.
	 *
	 * <p>Sets up with default explicit encoding.</p>
	 */
	public VASN1Set() {
		if (this.name == null)
			this.name = "SET";
	}

	/**
	 * Set up set.
	 *
	 * <p>If 'defaultExplicit' is true, tagged values are default encoded with
	 * explicit encoding; if false they are default encoded with implicit encoding.</p>
	 *
	 * @param defaultExplicit if true tagged values are default explicit encoded
	 */
	public VASN1Set(boolean defaultExplicit) {
		super(defaultExplicit);
		if (this.name == null)
			this.name = "SET";
	}

	/**
	 * Set up set.
	 *
	 * <p>If 'defaultExplicit' is true, tagged values are default encoded with
	 * explicit encoding; if false they are default encoded with implicit encoding.</p>
	 *
	 * @param defaultExplicit if true tagged values are default explicit encoded
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1Set(boolean defaultExplicit, String name,
			VASN1Definition definition) {
		super(defaultExplicit, name, definition);
		if (this.name == null)
			this.name = "SET";
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x31}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1Set.getUniversalTag();
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690: Sequence always has constructed encoding
			buf.append(this.getTag().encodeDER(true));
		}
		buf.append(super.encodeDER(false));
		return buf.popAll();
	}
}
