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

import java.util.Iterator;
import java.util.LinkedList;

import org.versile.vse.stream.VStream.PosBase;


/**
 * Streamer data interface to an iterator.
 *
 * @param <T> stream data type
 */
public abstract class VIteratorStreamerData<T> extends VStreamerData<T> {

	Iterator<T> iter;
	LinkedList<T> buffer;
	int buf_len;
	int pos = 0;
	boolean finished = false;
	boolean failed = false;

	/**
	 * Set up streamer data.
	 *
	 * <p>Sets up streamer data with a default size of the internal stream element
	 * buffer of 1000 elements.</p>
	 *
	 * @param iterator iterator which provides data to stream
	 */
	public VIteratorStreamerData(Iterator<T> iterator) {
		iter = iterator;
		buf_len = 1000; // HARDCODED
		buffer = new LinkedList<T>();
		this._update_buffer();
	}

	/**
	 * Set up streamer data.
	 *
	 * @param iterator iterator which provides data to stream
	 * @param bufLen max elements to hold in internal buffer (or null)
	 */
	public VIteratorStreamerData(Iterator<T> iterator, int bufLen) {
		iter = iterator;
		buf_len = bufLen;
		buffer = new LinkedList<T>();
		this._update_buffer();
	}

	@Override
	public synchronized T[] read(int maxNum) throws VStreamError, VStreamFailure {
		if (failed)
			throw new VStreamFailure();
		if (finished)
			return this.createArray(0);
		LinkedList<T> res_data = new LinkedList<T>();
		int num_left = maxNum;
		while (!buffer.isEmpty() && num_left > 0) {
			res_data.addLast(buffer.removeFirst());
			num_left -= 1;
			if (buffer.isEmpty())
				this._update_buffer();
		}
		pos += res_data.size();
		return res_data.toArray(this.createArray(0));
	}

	@Override
	public synchronized void write(T[] data) throws VStreamError, VStreamFailure {
		this.handleNotSupported();
		throw new VStreamFailure();
	}

	@Override
	public synchronized long seek(long pos, PosBase posRef)
			throws VStreamError, VStreamFailure {
		this.handleNotSupported();
		throw new VStreamFailure();
	}

	@Override
	public synchronized void truncateBefore() throws VStreamError, VStreamFailure {
		this.handleNotSupported();
		throw new VStreamFailure();
	}

	@Override
	public synchronized void truncateAfter() throws VStreamError, VStreamFailure {
		this.handleNotSupported();
		throw new VStreamFailure();
	}

	@Override
	public synchronized void close() {
		finished = true;
		buffer = null;
	}

	@Override
	public long getPosition()
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		return pos;
	}

	@Override
	public org.versile.vse.stream.VStreamerData.Endpoint[] getEndpoints()
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		Endpoint[] result = new Endpoint[2];
		result[0] = new Endpoint(true, pos);
		result[1] = new Endpoint(true, pos+buffer.size());
		return result;
	}

	@Override
	public VStreamMode[] getRequiredMode() {
		VStreamMode mode = new VStreamMode();
		mode.setReadable(true);
		mode.setStartBounded(true);
		mode.setEndBounded(true);
		mode.setFixedData(true);

		VStreamMode mask = new VStreamMode().inverse();

		return new VStreamMode[] {mode, mask};
	}

	@Override
	public VStreamMode getOptionalMode() {
		return new VStreamMode();
	}

	/**
	 * Creates an unitialized array for the class' element data type.
	 *
	 * @param len array length
	 * @return uninitialized array of given length
	 */
	public abstract T[] createArray(int len);

	synchronized void _update_buffer() {
		if (!finished) {
			int max_add = buf_len - buffer.size();
			for (int i = 0; i < max_add; i++) {
				if (iter.hasNext())
					buffer.addLast(iter.next());
				else {
					break;
				}
			}
		}
	}

	/**
	 * Called internally if unsupported stream operation was called.
	 */
	protected void handleNotSupported() {
		finished = true;
		buffer = null;
		failed = true;
	}

	/**
	 * Called internally to check if stream was closed or failed.
	 *
	 * @throws VStreamError
	 * @throws VStreamFailure
	 */
	protected void check_closed_or_failed()
			throws VStreamError, VStreamFailure {
		if (finished)
			if (failed)
				throw new VStreamFailure("Streamer data had a failure");
			else
				throw new VStreamError("Streamer data error");
	}
}
