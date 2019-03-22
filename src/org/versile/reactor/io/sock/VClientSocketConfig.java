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

package org.versile.reactor.io.sock;


/**
 * Configuration parameters for a client socket.
 *
 * <p>'bufferLength' holds the buffer size of socket input/output data. The
 * default size is 4096 bytes.</p>
 *
 * <p>'closedCallback' is a callback which should be called when the socket
 * is closed (or null), default is null. When called, the callback should
 * be executed as a scheduled reactor call.</p>
 *
 * <p>'allowInputHalfClose' and 'allowOutputHalfClose' define whether socket
 * half-close is allowed; the defaults are true. If false then the socket is
 * closed in both direction when one direction is closed.</p>
 */
public class VClientSocketConfig {
	// When adding new fields remember to update copyTo()
	int bufferLength = 4096;
	boolean allowInputHalfClose = true;
	boolean allowOutputHalfClose = true;
	Runnable closedCallback = null;

	@Override
	public VClientSocketConfig clone() {
		VClientSocketConfig result = new VClientSocketConfig();
		this.copyTo(result);
		return result;
	}

	public int getBufferLength() {
		return bufferLength;
	}

	public void setBufferLength(int bufferLength) {
		this.bufferLength = bufferLength;
	}

	public boolean allowInputHalfClose() {
		return allowInputHalfClose;
	}

	public void setAllowInputHalfClose(boolean allowInputHalfClose) {
		this.allowInputHalfClose = allowInputHalfClose;
	}

	public boolean allowOutputHalfClose() {
		return allowOutputHalfClose;
	}

	public void setAllowOutputHalfClose(boolean allowOutputHalfClose) {
		this.allowOutputHalfClose = allowOutputHalfClose;
	}

	public Runnable getClosedCallback() {
		return closedCallback;
	}

	public void setClosedCallback(Runnable closedCallback) {
		this.closedCallback = closedCallback;
	}

	protected void copyTo(VClientSocketConfig config) {
		config.bufferLength = bufferLength;
		config.allowInputHalfClose = allowInputHalfClose;
		config.allowOutputHalfClose = allowOutputHalfClose;
		config.closedCallback = closedCallback;
	}
}
