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

package org.versile.vse.stream;


/**
 * Stream operation error.
 */
public class VStreamError extends VStreamException {

	private static final long serialVersionUID = -7287250160682303158L;

	public VStreamError() {
	}

	public VStreamError(String arg0) {
		super(arg0);
	}

	public VStreamError(Throwable arg0) {
		super(arg0);
	}

	public VStreamError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Creates an exception appropriate for the given VSE stream error code.
	 *
	 * @param code VSE stream error code
	 * @return associated exception
	 */
	public static VStreamError createFromCode(int code) {
		if (code == 1)                        // GENERAL stream error
			return new VStreamError();
		else if (code == 2)
			return new VStreamInvalidPos();   // INVALID_POS stream error
		else
			return new VStreamError();
	}

	/**
	 * Creates an exception appropriate for the given VSE stream error code.
	 *
	 * @param code VSE stream error code
	 * @param msg exception message
	 * @return associated exception
	 */
	public static VStreamError createFromCode(int code, String msg) {
		if (code == 1)                           // GENERAL stream error
			return new VStreamError(msg);
		else if (code == 2)
			return new VStreamInvalidPos(msg);   // INVALID_POS stream error
		else
			return new VStreamError(msg);
	}

	/**
	 * Creates an exception appropriate for the given VSE stream error code.
	 *
	 * @param code VSE stream error code
	 * @param e throwable
	 * @return associated exception
	 */
	public static VStreamError createFromCode(int code, Throwable e) {
		if (code == 1)                         // GENERAL stream error
			return new VStreamError(e);
		else if (code == 2)
			return new VStreamInvalidPos(e);   // INVALID_POS stream error
		else
			return new VStreamError(e);
	}

	/**
	 * Creates an exception appropriate for the given VSE stream error code.
	 *
	 * @param code VSE stream error code
	 * @param msg exception message
	 * @param e throwable
	 * @return associated exception
	 */
	public static VStreamError createFromCode(int code, String msg, Throwable e) {
		if (code == 1)                              // GENERAL stream error
			return new VStreamError(msg, e);
		else if (code == 2)
			return new VStreamInvalidPos(msg, e);   // INVALID_POS stream error
		else
			return new VStreamError(msg, e);
	}

}
