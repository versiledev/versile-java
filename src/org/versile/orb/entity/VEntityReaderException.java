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

package org.versile.orb.entity;


/**
 * Error reading {@link VEntity} from serialized data.
 */
public class VEntityReaderException extends Exception {

	private static final long serialVersionUID = 7343005948362218551L;

	public VEntityReaderException() {
	}

	public VEntityReaderException(String message) {
		super(message);
	}

	public VEntityReaderException(Throwable cause) {
		super(cause);
	}

	public VEntityReaderException(String message, Throwable cause) {
		super(message, cause);
	}

}
