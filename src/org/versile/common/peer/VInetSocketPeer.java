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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.versile.common.call.VCall;


/**
 * Represents an Internet communication peer for socket-based communication.
 */
public class VInetSocketPeer extends VSocketPeer {

	VInetSocketPeer(InetSocketAddress address) {
		super(address);
	}

	/**
	 * Get the associated Internet address.
	 *
	 * @return peer Internet address.
	 */
	public InetSocketAddress getInetAddress() {
		return (InetSocketAddress) this.getAddress();
	}

	/**
	 * Returns an Internet address associated with a host name.
	 *
	 * <p>Performs a blocking DNS lookup. If address is null or empty, a
	 * localhost address reference is returned.</p>
	 *
	 * @param address host name
	 * @return resolved address
	 * @throws UnknownHostException could not resolve address
	 */
	public static InetAddress lookup(String address)
		throws UnknownHostException {
		if (address == null || address.length() == 0)
			return InetAddress.getLocalHost();
		return InetAddress.getByName(address);
	}

	/**
	 * Performs a non-blocking DNS lookup.
	 *
	 * <p>Similar to {@link #lookup(String)} except the result is returned as
	 * an asynchronous call reference. Note that a thread is instantiated to
	 * perform the lookup.</p>
	 *
	 * @param address address host name
	 * @return call reference to the lookup() call result
	 */
	public static VCall<InetAddress> nowaitLookup(String address) {
		VCall<InetAddress> result = new VCall<InetAddress>();

		class Lookup extends Thread {
			String address;
			VCall<InetAddress> result;
			public Lookup(String address, VCall<InetAddress> result) {
				this.address = address;
				this.result = result;
			}
			@Override
			public void run() {
				try {
					result.silentPushResult(VInetSocketPeer.lookup(address));
				} catch (UnknownHostException e) {
					result.silentPushException(e);
				}
			}
		}
		Lookup lookup = new Lookup(address, result);
		lookup.start();
		return result;
	}
}
