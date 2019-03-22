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

package org.versile.common.util;

import java.util.Vector;

import org.versile.orb.entity.VInteger;



/**
 * Represents an Object Identifier.
 */
public class VObjectIdentifier {

	Vector<VInteger> oid;

	/**
	 * Set up identifier.
	 *
	 * @param oid identifier components
	 */
	public VObjectIdentifier(VInteger... oid) {
		this.oid = new Vector<VInteger>();
		for (VInteger i: oid)
			this.oid.add(i);
	}

	/**
	 * Set up identifier.
	 *
	 * @param oid identifier components
	 */
	public VObjectIdentifier(Integer... oid) {
		this.oid = new Vector<VInteger>();
		for (Integer i: oid)
			this.oid.add(new VInteger(i));
	}

	/**
	 * Get a list of individual OID identifiers
	 *
	 * @return identifiers
	 */
	public VInteger[] getIdentifiers() {
		return oid.toArray(new VInteger[0]);
	}

	/**
	 * Get the number of OID elements
	 *
	 * @return OID length
	 */
	public int getLength() {
		return oid.size();
	}

	@Override
	public String toString() {
		String result = "";
		boolean first = true;
		for (VInteger item: oid) {
			if (first)
				first = false;
			else
				result = result + ".";
			result = result + item;
		}
		return result;
	}

	@Override
	public int hashCode() {
		return oid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VObjectIdentifier))
			return false;
		VObjectIdentifier other = (VObjectIdentifier)obj;
		if (oid.size() != other.getLength())
			return false;
		VInteger[] other_ids = other.getIdentifiers();
		for (int i = 0; i < oid.size(); i++)
			if (!oid.get(i).equals(other_ids[i]))
				return false;
		return true;
	}
}
