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

package org.versile.common.call;


/**
 * Handler for a {@link VCall} exception.
 */
public interface VCallExceptionHandler {

	/**
	 * Called with an exception raised by an asynchronous call.
	 *
	 * <p>Implementations must ensure no synchronization is performed by this
	 * call which may trigger a deadlock condition with the code which
	 * triggered the call result.</p>
	 *
	 * @param e exception thrown by asynchronous call
	 */
	public void callback(Exception e);
}
