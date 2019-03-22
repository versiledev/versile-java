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

package org.versile.reactor.io;

import org.versile.common.auth.VCredentials;
import org.versile.common.peer.VPeer;


/**
 * Control interface for a {@link VConsumer} or {@link VProducer}.
 *
 * <p>The control interface enables control operations on a consumer/producer
 * chain. Control methods can be handled locally or propagated to the next
 * element in the chain (if intended for the chain end-point). Control methods
 * that are not supported raise {@link VIOMissingControl}.</p>
 */
public class VIOControl {

	/**
	 * Request an update on chain's current state.
	 *
	 * <p>Should be propagated along the chain, and should trigger each chain
	 * element to pass any previously unsent state control notifications.</p>
	 *
	 * @param consumer consumer requesting a state notification
	 * @throws VIOMissingControl
	 */
	public void requestProducerState(VConsumer consumer) throws VIOMissingControl {
		throw new VIOMissingControl();
	}

	/**
	 * Requests whether a connection can be made.
	 *
	 * <p>Can be sent by a chain end-point which is about to make a network
	 * connection, to get approval from a "higher level protocol" handler
	 * further down the chain whether connectivity to the peer is allowed
	 * and can proceed.</p>
	 *
	 * @param peer target communication peer
	 * @return true if allowed to make a connection to peer
	 * @throws VIOMissingControl
	 */
	public boolean canConnect(VPeer peer) throws VIOMissingControl {
		throw new VIOMissingControl();
	}

	/**
	 * Notifies the chain a connection was made to a communication peer.
	 *
	 * @param peer connected peer
	 * @throws VIOMissingControl
	 */
	public void notifyConnected(VPeer peer) throws VIOMissingControl {
		throw new VIOMissingControl();
	}

	/**
	 * Notifies a consumer was attached to the chain.
	 *
	 * @param consumer attached consumer
	 * @throws VIOMissingControl
	 */
	public void notifyConsumerAttached(VConsumer consumer) throws VIOMissingControl {
		throw new VIOMissingControl();
	}

	/**
	 * Notifies a producer was attached to the chain.
	 *
	 * @param producer attached producer
	 * @throws VIOMissingControl
	 */
	public void notifyProducerAttached(VProducer producer) throws VIOMissingControl {
		throw new VIOMissingControl();
	}

	/**
	 * Requests authorization to proceed with a connection for provided credentials.
	 *
	 * @param credentials peer credentials (or null)
	 * @param protocol name of requesting protocol
	 * @return true if authorized, otherwise false
	 * @throws VIOMissingControl
	 */
	public boolean authorize(VCredentials credentials, String protocol)
			throws VIOMissingControl {
		throw new VIOMissingControl();
	}
}
