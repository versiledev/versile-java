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
 * Holds a VEntity consumer/producer pair.
 *
 * <p>Intended primarily as a convenience type for holding a consumer/producer pair
 * when returning from functions or passing as arguments; also it allows a
 * cleaner syntax for attaching two consumer/producer pairs.</p>
 */
public class VEntityIOPair {

	VEntityConsumer consumer;
	VEntityProducer producer;

	/**
	 * Sets up a consumer/producer pair.
	 *
	 * @param consumer
	 * @param producer
	 */
	public VEntityIOPair(VEntityConsumer consumer, VEntityProducer producer) {
		this.consumer = consumer;
		this.producer = producer;
	}

	/**
	 * Attaches to another consumer/producer pair.
	 * @param pair pair to connect to
	 * @throws IOException attach operation error
	 */
	public void attach(VEntityIOPair pair)
		throws IOException {
		consumer.attach(pair.getProducer());
		try {
			producer.attach(pair.getConsumer());
		} catch (IOException e) {
			consumer.detach();
			throw e;
		}
	}

	/**
	 * Attaches to another consumer/producer pair.
	 * @param pair pair to connect to
	 * @param safe must be false unless known to be executing in reactor thread
	 * @throws IOException attach operation error
	 */
	public void attach(VEntityIOPair pair, boolean safe)
		throws IOException {
		consumer.attach(pair.getProducer(), safe);
		try {
			producer.attach(pair.getConsumer(), safe);
		} catch (IOException e) {
			consumer.detach();
			throw e;
		}
	}

	/**
	 * Get associated consumer.
	 *
	 * @return consumer
	 */
	public VEntityConsumer getConsumer() {
		return consumer;
	}

	/**
	 * Get associated producer.
	 *
	 * @return producer
	 */
	public VEntityProducer getProducer() {
		return producer;
	}
}
