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
 * Error invoking a {@link VObject} or {@link VReference} remote call.
 *
 * <p>Indicates an error condition with call invocation, e.g. the objects
 * remote interface cannot be accessed, invalid arguments were provided
 * to the object, or call arguments, return value or thrown exception
 * could not be passed.</p>
 *
 * <p>If the remote call could be invoked with proper use of the method's
 * external interface, however there is an error-condition within the call
 * itself, the call should normally raise another type of exception (e.g.
 * throw {@link VException}).</p>
 */
public class VCallError extends Exception {

	private static final long serialVersionUID = -3368371043086819478L;

	public VCallError() {
		super();
	}

	public VCallError(String message) {
		super(message);
	}

	public VCallError(Throwable cause) {
		super(cause);
	}

	public VCallError(String message, Throwable cause) {
		super(message, cause);
	}
}
