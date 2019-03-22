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

import org.versile.reactor.VReactor;


/**
 * Consumer of data.
 *
 * <p>Can be connected to a compatible {@link VProducer}. Implementing objects must
 * be connected to a {@link org.versile.reactor.VReactor} and be operated by the reactor's
 * main thread.</p>
 */
public interface VConsumer {
	/**
	 * Get a control object for the producer/consumer chain.
	 *
	 * @return control object
	 */
	public VIOControl getControl();

	/**
	 * Get the consumer's owning reactor.
	 *
	 * @return owning reactor
	 */
	public VReactor getReactor();
}
