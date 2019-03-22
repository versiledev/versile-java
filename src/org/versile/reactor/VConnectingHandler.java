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

package org.versile.reactor;

import java.io.IOException;


/**
 * Handler for processing an I/O selector "connected" event.
 */
public interface VConnectingHandler extends VHandler {

	/**
	 * Handler method for reactor "connect" event.
	 *
	 * <p>Called by the reactor in response to a "connect" event when the
	 * handler is configured for receiving such events. Should only be
	 * called by the owning reactor.</p>
	 */
	public abstract void handleConnect();

	/**
	 * Register with owning reactor for receiving "connect" events.
	 *
	 * @throws IOException error condition prevents receiving events
	 */
	public void startHandlingConnect()
		throws IOException;

	/**
	 * Inform owning reactor to stop processing "connect" events.
	 */
	public void stopHandlingConnect();

}
