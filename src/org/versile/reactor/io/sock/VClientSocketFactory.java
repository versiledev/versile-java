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

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.versile.reactor.VReactor;


/**
 * Factory for a reactor-driven client socket for an accepted connection.
 */
public interface VClientSocketFactory {

	/**
	 * Create a reactor-enabled socket handler for an accepted socket channel.
	 *
	 * <p>The created handler must be created with a status as "already connected",
	 * i.e. passing the "connected" argument as true to the {@link VClientSocket}
	 * constructor.</p>
	 *
	 * @param reactor owning reactor
	 * @param channel accepted socket channel
	 * @param closedCallback callback when client socket is closed (or null)
	 * @return created socket handler
	 * @throws IOException
	 */
	public VClientSocket build(VReactor reactor, SocketChannel channel, Runnable closedCallback)
			throws IOException;
}
