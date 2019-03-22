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

package org.versile.vse.vnative.python;

import org.versile.orb.entity.VEntity;
import org.versile.vse.vnative.VNativeException;


/**
 * Base class for exceptions raised by remote native python objects.
 */
public abstract class VPythonException extends VNativeException {

	/**
	 * Set up native python exception.
	 *
	 * @param tag python native-type tag
	 * @param args exception arguments
	 */
	public VPythonException(VEntity tag, VEntity[] args) {
		super(tag, args);
	}
}
