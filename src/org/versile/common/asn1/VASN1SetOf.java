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
 * An ASN.1 SetOf.
 */
public class VASN1SetOf extends VASN1Set {

	/**
	 * Set up set.
	 *
	 * <p>Sets up with default explicit encoding.</p>
	 */
	public VASN1SetOf() {
		if (this.name == null)
			this.name = "SET OF";
	}

	/**
	 * Set up set.
	 *
	 * <p>If 'defaultExplicit' is true, tagged values are default encoded with
	 * explicit encoding; if false they are default encoded with implicit encoding.</p>
	 *
	 * @param defaultExplicit if true tagged values are default explicit encoded
	 */
	public VASN1SetOf(boolean defaultExplicit) {
		super(defaultExplicit);
		if (this.name == null)
			this.name = "SET OF";
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
	public VASN1SetOf(boolean defaultExplicit, String name,
			VASN1Definition definition) {
		super(defaultExplicit, name, definition);
		if (this.name == null)
			this.name = "SET OF";
	}

}
