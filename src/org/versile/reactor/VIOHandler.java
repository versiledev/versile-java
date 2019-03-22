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
import java.nio.channels.SelectableChannel;


/**
 * General handler for reactor I/O read/write events.
 */
public abstract class VIOHandler implements VHandler {

	protected VReactor reactor;

	/**
	 * Set up handler.
	 *
	 * @param reactor reactor managing handler events
	 */
	public VIOHandler(VReactor reactor) {
		this.reactor = reactor;
	}

	/**
	 * Handler method for reactor "read" event.
	 *
	 * <p>Called by the reactor in response to a "read" event when the
	 * handler is configured for receiving read events. Should only be
	 * called by the owning reactor.</p>
	 */
	public abstract void doRead();

	/**
	 * Handler method for reactor "write" event.
	 *
	 * <p>Called by the reactor in response to a "write" event when the
	 * handler is configured for receiving write events. Should only be
	 * called by the owning reactor.</p>
	 */
	public abstract void doWrite();

	/**
	 * Register with owning reactor for receiving "read" events.
	 *
	 * @throws IOException error condition prevents receiving events
	 */
	public void startReading()
		throws IOException {
		reactor.startReading(this);
	}

	/**
	 * Register with owning reactor for receiving "write" events.
	 *
	 * @throws IOException error condition prevents receiving events
	 */
	public void startWriting()
			throws IOException {
			reactor.startWriting(this);
		}

	/**
	 * Inform owning reactor to stop processing "read" events.
	 */
	public void stopReading() {
		reactor.stopReading(this);
	}

	/**
	 * Inform owning reactor to stop processing "write" events.
	 */
	public void stopWriting() {
		reactor.stopWriting(this);
	}

	/**
	 * Get owning reactor.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	@Override
	public abstract SelectableChannel getChannel();
}
