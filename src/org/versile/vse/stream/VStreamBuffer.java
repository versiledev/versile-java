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
 * Generic stream data buffer.
 *
 * <p>Internal buffering features have been abstracted in order to separate stream
 * handling of 'data elements' from the type-specific handling of those
 * elements.</p>
 *
 * <p>Buffer methods for reading or writing data always operate on an absolute data
 * element position in the stream, so it would be possible e.g. to implement
 * derived classes which cache data which has been read from or written to a
 * stream, if the stream mode indicate the stream can be cached.</p>
 *
 * @param <T> the data type transferred by the stream
 */
public abstract class VStreamBuffer<T> {

	/**
	 * Changes to a new buffer context and repositions the buffer.
	 *
	 * <p>If 'canCache' is true then any data pushed to the buffer can be cached for later
	 * reading (however the buffer is not required to perform such caching).</p>
	 *
	 * <p>The method may be called while another context is already active, without
	 * calling {@link #endContext} first.</p>
	 *
	 * @param pos new (absolute) position in buffer
	 * @param canCache if true buffer may cache data
	 */
	public abstract void newContext(long pos, boolean canCache);

	/**
	 * Ends the current buffer context.
	 *
     * <p>Calling this method enables the buffer to release any
     * resources tied to the previously active context. It is allowed
     * to call this method also when not in an active context.</p>
	 */
	public abstract void endContext();

	/**
	 * Write data onto the buffer's current write position.
	 *
	 * <p>Does not advance the read position.</p>
	 *
	 * @param data data elements to write
	 */
	public void write(T[] data) {
		this.write(data, false);
	}

	/**
	 * Write data onto the buffer's current write position.
	 *
	 * @param data data elements to write
	 * @param advance if true advance read position to write position
	 */
	public abstract void write(T[] data, boolean advance);

	/**
	 * Read data from current read position.
	 *
	 * <p>Returns an empty data set if no data could be read.</p>
	 *
	 * <p>Advances the read position to the end of the data read. If the read
	 * position was moved past the previous write position during read, then the
	 * write position should be automatically moved to read position.</p>
	 *
	 * @param maxRead max elements to read
	 * @return max data read
	 */
	public abstract T[] read(int maxRead);

	/**
	 * Get max number of data elements that can be read from current position
	 *
	 * @return max readable elements
	 */
	public abstract long getMaxRead();

	/**
	 * Get current read position.
	 *
	 * @return read position
	 */
	public abstract long getReadPosition();

	/**
	 * Get current write position.
	 *
	 * @return write position
	 */
	public abstract long getWritePosition();

	/**
	 * Get an empty data elements object.
	 *
	 * @return empty data structure
	 */
	public T[] emptyData() {
		return this.createArray(0);
	}

	/**
	 * Internal call to create an array of requested length
	 *
	 * @param len
	 */
	public abstract T[] createArray(int len);
}
