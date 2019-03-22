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
 * Masks a generic object as an exception.
 *
 * <p>The proxy can be used for raising exceptions representing an object
 * which does not inherit Exception (such as e.g. {@link org.versile.orb.entity.VException})
 * and thus cannot be raised directly.</p>
 */
public class VExceptionProxy extends Exception {

	private static final long serialVersionUID = -2595997369052388649L;

	Object exc_data;

	/**
	 * Initialize the exception.
	 *
	 * @param obj value held by the exception
	 */
	public VExceptionProxy(Object obj) {
		super("");
		exc_data = obj;
	}

	/**
	 * Returns the value associated with this exception.
	 *
	 * @return exception value
	 */
	public Object getValue() {
		return exc_data;
	}
}
