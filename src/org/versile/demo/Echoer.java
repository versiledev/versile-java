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

package org.versile.demo;

import org.versile.orb.external.Doc;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;


/**
 * Example remote-enabled echo service class.
 *
 * <p>Provides a remote-enabled method {@link #echo(Object)} for echoing a
 * received argument as a return value.</p>
 *
 * <p>This class is intended for testing and demonstration only and is not
 * formally part of the Versile Java framework.</p>
 */
@Doc(doc="Provides an echo service")
public class Echoer extends VExternal {

	/**
	 * Returns ("echoes") the received parameter
	 *
	 * @param obj value to return
	 * @return received argument
	 */
	@Publish(show=true, ctx=false)
	@Doc(doc="Echo service, returns the provided argument.")
	public Object echo(Object obj) {
		return obj;
	}
}
