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
 * Configuration parameters for a listening socket.
 *
 * <p>The 'listening' property is true if the provided socket has listening
 * enabled, default is false.</p>
 *
 * <p>'controlled' is true if the socket's "can_accept" state is being
 * externally controlled. The default is false.</p>
 *
 * <p>'acceptCallback' is a callback which should be performed when
 * a new client socket is accepted by the listener. The default is null.</p>
 *
 * <p>'closedCallback' is a callback which should be performed when an
 * accepted client socket is closed.</p>
 */
public class VListeningSocketConfig {
	boolean controlled = false;
	Runnable acceptCallback = null;
	Runnable closedCallback = null;

	public boolean isControlled() {
		return controlled;
	}

	public void setControlled(boolean controlled) {
		this.controlled = controlled;
	}

	public Runnable getAcceptCallback() {
		return acceptCallback;
	}

	public void setAcceptCallback(Runnable acceptCallback) {
		this.acceptCallback = acceptCallback;
	}

	public Runnable getClosedCallback() {
		return closedCallback;
	}

	public void setClosedCallback(Runnable closedCallback) {
		this.closedCallback = closedCallback;
	}
}
