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


/**
 * Base framework for reactor-driven I/O.
 *
 * <p>In order to help separate abstraction of data processing paths and
 * data processing logic from selectable I/O events or timed operations,
 * {@link org.versile.reactor.io} provides a consumer/producer processing
 * framework as an abstraction for data flows.</p>
 *
 * <p>A {@link org.versile.reactor.io.VByteProducer} can be connected with and push
 * data to a {@link org.versile.reactor.io.VByteConsumer}, and similarly
 * {@link org.versile.reactor.io.VEntityProducer} can be connected with and push
 * data to a {@link org.versile.reactor.io.VEntityConsumer} for passing VEntity
 * data.</p>
 *
 * <p>The producer/consumer framework is used by the reactor-based implementations
 * of Versile Object Protocol, e.g. a VOP connection with a negotiated VTS transport
 * will consist of:</p>
 *
 * <ul>
 *
 *   <li>A {@link org.versile.reactor.io.sock.VClientSocketAgent} exposing a producer/consumer
 *   interface to socket communication.</li>
 *
 *  <li>A {@link org.versile.reactor.io.vop.VOPClientBridge} or
 *  {@link org.versile.reactor.io.vop.VOPServerBridge} handling VOP protocol negotiation</li>
 *
 *   <li>When VTS has been negotiated, the VOP bridge adds a {@link org.versile.reactor.io.vts.VSecureClient}
 *   or {@link org.versile.reactor.io.vts.VSecureServer} as a transport</li>
 *
 *  <li>Transport plaintext data is connected to a {@link org.versile.reactor.io.vec.VEntityChannel}
 *  for (de)serializing VEntity data</li>
 *
 *  <li>The VEntity side of the entity channel connects to a
 *  {@link org.versile.reactor.io.link.VLinkAgent} which implements the Versile ORB Protocol</li>
 *
 * </ul>
 *
 * <p>The consumer/producer framework allows separation of functionality and business logic, e.g. the VTS
 * channel classes deal with generic byte communication and is entirely separate from e.g. managing
 * socket communication.</p>
 *
 * <p>The consumer/producer interfaces are assumed to be called only by the reactor's thread, with the
 * exception of the 'attach' and 'detach' operations, which must be implemented in a way so that they are
 * thread-safe (typically handled by dispatching the code to perform an attach/detach operation to be
 * executed by the reactor scheduler, rather than executing in the context of the calling thread).</p>
 */
package org.versile.reactor.io;
