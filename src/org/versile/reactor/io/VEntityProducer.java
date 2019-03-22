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

import java.io.IOException;


/**
 * Producer of VEntity data.
 *
 * <p>Can be connected to a {@link VEntityConsumer}.  Implementing objects must be
 * connected to a reactor and be operated by the reactor's main thread.</p>
 */
public interface VEntityProducer extends VProducer {

	/**
	 * Informs the producer it can deliver data to its consumer.
	 *
	 * <p>Should only be called by owning reactor thread. Implementations of
	 * this method may not call the consume method on the connected
	 * consumer within the context of this method call.</p>
	 *
	 * @param limit cumulative byte consumption limit
	 * @throws IOException
	 */
	public void canProduce(long limit)
			throws IOException;

	/**
	 * Abort processing chain operation.
	 *
	 * <p>Aborts any further processing, which should also abort and disconnect any
	 * producers/consumers in the processing chain which depend on input from this
	 * producer.</p>
	 *
	 * <p>Should only be called by the owning reactor thread.</p>
	 */
	public void abort();

	/**
	 * Attach to a consumer.
	 *
	 * <p>Implementations must be thread-safe, allowing it to be called also form
	 * other threads than the owning reactor's thread.</p>
	 *
	 * @param consumer target consumer
	 * @throws IOException cannot attach
	 */
	public void attach(VEntityConsumer consumer)
			throws IOException;

	/**
	 * Attach to a consumer.
	 *
	 * <p>Implementations must provide a thread-safe implementation for when the
	 * 'safe' argument is false. If 'safe' is true then implementations may
	 * assume they are executed by the owning reactor's thread.</p>
	 *
	 * @param consumer target consumer
	 * @param safe if false may not have been called by reactor thread
	 * @throws IOException cannot attach
	 */
	public void attach(VEntityConsumer consumer, boolean safe)
			throws IOException;

	/**
	 * Detach from any connected consumer.
	 *
	 * <p>Must be thread-safe allowing the method to also be called from other threads
	 * than the main reactor thread.</p>
	 */
	public void detach();

	/**
	 * Detach from any connected consumer.
	 *
	 * <p>Implementations must provide a thread-safe implementation for when the
	 * 'safe' argument is false. If 'safe' is true then implementations may
	 * assume they are executed by the owning reactor's thread.</p>
	 *
	 * @param safe if false may not have been called by reactor thread
	 */
	public void detach(boolean safe);

	/**
	 * Get the connected consumer.
	 *
	 * @return connected consumer (or null)
	 */
	public VEntityConsumer getConsumer();

}
