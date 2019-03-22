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

package org.versile.vse.stream;


/**
 * Base class for receiving notifications from a {@link VStream}.
 */
public abstract class VStreamObserver<T> {

	protected VStream<T> stream;

	/**
	 * Set up observer (registers observer with stream)
	 *
	 * @param stream stream to observer
	 */
	public VStreamObserver(VStream<T> stream) {
		this.stream = stream;
		stream.addObserver(this);
	}

	public void finalize() {
		if (stream != null)
			stream.removeObserver(this);
	}

	/**
	 * Disable this observer, unregistering from any observed stream.
	 */
	public void disable() {
		if (stream != null) {
			stream.removeObserver(this);
			stream = null;
		}
	}

	/**
	 * Called by observed stream when data available for reading.
	 *
	 * <p>Only called as an 'edge' call, when the stream receives data
	 * available for reading after it had no data available.</p>
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	public void canReceive() {
	}

	/**
	 * Called by observed stream when data available for writing.
	 *
	 * <p>Only called as an 'edge' call, when the stream receives data
	 * available for reading after it had no data available.</p>
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	public void canSend() {
	}

	/**
	 * Called by observed stream when peer streamer acknowledges closed.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	public void closed() {
	}

	/**
	 * Called by observed stream when an error is set on the current context.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	public void contextError() {
	}

	/**
	 * Called by observed stream when a failure condition is set on stream.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	public void failed() {
	}

	/**
	 * Get a reference to the observed stream.
	 *
	 * @return observed stream (or null)
	 */
	public VStream<T> getStream() {
		return stream;
	}
}
