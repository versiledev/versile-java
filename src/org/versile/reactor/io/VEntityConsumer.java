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

import org.versile.orb.entity.VEntity;


/**
 * Consumer of VEntity data.
 *
 * <p>Can be connected to a {@link VEntityProducer}. Implementing objects must be
 * connected to a reactor and be operated by the reactor's main thread.</p>
 */
public interface VEntityConsumer extends VConsumer {

	/**
	 * Consume VEntity data.
	 *
	 * <p>The consumer must accept all data provided to the method, as long as the
	 * cumulative consumption limit set with the producer is not exceeded.</p>
	 *
	 * <p>It is allowed to call {@link VEntityProducer#canProduce(long)} on a
	 * connected producer within the scope this method call.</p>
	 *
	 * <p>The method should only be called by the owning reactor's main thread.</p>
	 *
	 * @param data data to consume
	 * @return current cumulative consumption limit
	 * @throws IOException consumption error
	 */
	public long consume(VEntity[] data)
			throws IOException;

	/**
	 * Signals end-of-data for producer data output.
	 *
	 * <p>After this method has been called the stream should not accept any more
	 * data to {@link #consume(VEntity[])}. The consumer is responsible for
	 * performing any end-of-data processing including e.g. any notifications to other
	 * producers/consumers further down the processing chain.</p>
	 *
	 * <p>The method should only be called by the owning reactor's main thread.</p>
	 *
	 * @param clean if false the data stream has error conditions.
	 */
	public void endConsume(boolean clean);

	/**
	 * Abort processing chain operation.
	 *
	 * <p>Aborts any further processing, which should also abort and disconnect any
	 * producers/consumers in the processing chain which depend on input received
	 * by this consumer.</p>
	 *
	 * <p>Should only be called by the owning reactor thread.</p>
	 */
	public void abort();

	/**
	 * Attach to a producer.
	 *
	 * <p>Implementations must be thread-safe, allowing it to be called also form
	 * other threads than the owning reactor's thread.</p>
	 *
	 * @param producer target producer
	 * @throws IOException cannot attach
	 */
	public void attach(VEntityProducer producer)
			throws IOException;

	/**
	 * Attach to a producer.
	 *
	 * <p>Implementations must provide a thread-safe implementation for when the
	 * 'safe' argument is false. If 'safe' is true then implementations may
	 * assume they are executed by the owning reactor's thread.</p>
	 *
	 * @param producer target producer
	 * @param safe if false may not have been called by reactor thread
	 * @throws IOException cannot attach
	 */
	public void attach(VEntityProducer producer, boolean safe)
			throws IOException;


	/**
	 * Detach from any connected producer.
	 *
	 * <p>Must be thread-safe allowing the method to also be called from other threads
	 * than the main reactor thread.</p>
	 */
	public void detach();

	/**
	 * Detach from any connected producer.
	 *
	 * <p>Implementations must provide a thread-safe implementation for when the
	 * 'safe' argument is false. If 'safe' is true then implementations may
	 * assume they are executed by the owning reactor's thread.</p>
	 *
	 * @param safe if false may not have been called by reactor thread
	 */
	public void detach(boolean safe);

	/**
	 * Get the connected producer.
	 *
	 * @return connected producer (or null)
	 */
	public VEntityProducer getProducer();

}
