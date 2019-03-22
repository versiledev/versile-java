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

package org.versile.reactor.io.vudp;


/**
 * Filter for UDP addresses of relayed connection.
 *
 * <p>The filter provides an allow/deny response for the host and port used during
 * UDP handshake with a relay, and for the host and port used when initiating a
 * peer-to-peer connection.</p>
 *
 * <p>The default implementation allows all connections, derived classes can
 * override.</p>
 */
public class VUDPHostFilter {

	/**
	 * Requests allowing a UDP handshake to relay provided address.
	 *
	 * <p>Default returns true, derived classes can override.</p>
	 *
	 * @param host IP address
	 * @param port UDP port number
	 * @return true if allowed
	 */
	public boolean allowRelay(String host, int port) {
		return true;
	}

	/**
	 * Requests allowing a UDP handshake to peer UDP address.
	 *
	 * <p>Default returns true, derived classes can override.</p>
	 *
	 * @param host IP address
	 * @param port UDP port number
	 * @return true if allowed
	 */
	public boolean allowPeer(String host, int port) {
		return true;
	}
}
