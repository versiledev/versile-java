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

package org.versile.orb.url;


/**
 * VRI related error.
 */
public class VUrlException extends Exception {

	private static final long serialVersionUID = 1L;

	public VUrlException() {
		super();
	}

	public VUrlException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public VUrlException(String arg0) {
		super(arg0);
	}

	public VUrlException(Throwable arg0) {
		super(arg0);
	}
}
