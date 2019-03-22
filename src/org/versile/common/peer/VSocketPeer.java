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

package org.versile.common.peer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Represents a communication peer for socket-based communication.
 */
public class VSocketPeer extends VPeer {
	SocketAddress address;

	VSocketPeer(SocketAddress address) {
		this.address = address;
	}

	/**
	 * Get associated socket address.
	 *
	 * @return socket address
	 */
	public SocketAddress getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return address.toString();
	}

	/**
	 * Create a socket peer from a socket address.
	 *
	 * <p>Constructs the appropriate sub-class of {@link VSocketPeer} for
	 * the provided address type.</p>
	 *
	 * @param address socket address
	 * @return peer object
	 */
	public static VSocketPeer fromAddress(SocketAddress address) {
		if (address instanceof InetSocketAddress)
			return new VInetSocketPeer((InetSocketAddress)address);
		else
			return new VSocketPeer(address);
	}
}
