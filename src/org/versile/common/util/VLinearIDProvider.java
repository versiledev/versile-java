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


/**
 * Provides a provider of a linearly increasing set of integer IDs.
 *
 * <p>The provider generates IDs in sequence, each new ID is one larger
 * than the previous ID.</p>
 */
public class VLinearIDProvider {
	long next_id;

	/**
	 * Constructs a provider which provides 1L as the initial ID.
	 */
	public VLinearIDProvider() {
		next_id = 1L;
	}

	/**
	 * Constructs a provider.
	 *
	 * @param startValue initial ID generated by the provider
	 */
	public VLinearIDProvider(long startValue) {
		next_id = startValue;
	}

	/**
	 * Generate the next provider ID.
	 *
	 * @return the next provider ID
	 */
	public synchronized long getID() {
		long result = next_id;
		next_id += 1;
		return result;
	}
}
