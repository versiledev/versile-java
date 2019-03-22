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


/**
 * Module framework for resolving VER encoded data types.
 *
 * <p>The module framework is used for handling conversion of VER encoded
 * data types to/from native representations. A {@link org.versile.orb.module.VModule}
 * handles a set of encoded representations for a set of VER tags, and registering
 * with a {@link org.versile.orb.module.VModuleResolver} for resolving the associated
 * types. A resolver can be set on a Versile ORB Link for handling link
 * VER conversion.</p>
 *
 * <p>The module framework is used for handling Versile Standard Entities data types,
 * see {@link org.versile.vse} for additional information.</p>
 */
package org.versile.orb.module;
