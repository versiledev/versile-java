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

package org.versile.vse;

import org.versile.orb.module.VModule;


/**
 * Base class for VSE modules.
 */
public abstract class VSEModule extends VModule {

	/**
	 * Gets the internal module code associated with the VSE module.
	 *
	 * <p>Intended for internal use by the VSE framework.</p>
	 *
	 * @return internal module code
	 */
	public abstract int getVSEModuleCode();

}
