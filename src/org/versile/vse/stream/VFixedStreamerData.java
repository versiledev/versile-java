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

import org.versile.vse.stream.VStream.PosBase;


/**
 * Memory-cached fixed-size read-only streamer data set.
 *
 * @param <T> streaming data type
 */
public abstract class VFixedStreamerData<T> extends VStreamerData<T> {

	int pos = 0;
	T[] data;
	boolean failed = false;

	/**
	 * Set up streamer data on fixed data array.
	 *
	 * @param data fixed data source for streamer data
	 */
	public VFixedStreamerData(T[] data) {
		this.data = data;
	}

	@Override
	public synchronized T[] read(int maxNum) throws VStreamError, VStreamFailure {
		if (failed)
			throw new VStreamFailure();

		// If closed (reached end), return empty array
		if (data == null)
			return this.createArray(0);

		int start_pos = pos;
		int end_pos = start_pos + maxNum;
		if (end_pos > data.length)
			end_pos = data.length;
		int result_len = end_pos - start_pos;
		T[] result = this.createArray(result_len);
		for (int i = 0; i < result_len; i++)
			result[i] = data[start_pos+i];
		pos += result_len;
		return result;
	}

	@Override
	public synchronized void write(T[] data) throws VStreamError, VStreamFailure {
		// Illegal operation
		this.data = null;
		failed = true;
		this.check_closed_or_failed();
	}

	@Override
	public long seek(long pos, PosBase posRef)
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		if (posRef == PosBase.END)
			pos += data.length;
		else if (posRef == PosBase.CURRENT)
			pos += this.pos;
		if (pos < 0 || pos > data.length)
			throw new VStreamError("Out-of-range seek position");
		this.pos = (int)pos;
		return this.pos;
	}

	@Override
	public void truncateBefore() throws VStreamError, VStreamFailure {
		// Illegal operation
		this.data = null;
		failed = true;
		this.check_closed_or_failed();
	}

	@Override
	public void truncateAfter() throws VStreamError, VStreamFailure {
		// Illegal operation
		this.data = null;
		failed = true;
		this.check_closed_or_failed();
	}

	@Override
	public synchronized void close() {
		data = null;
	}

	@Override
	public long getPosition()
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		return pos;
	}

	@Override
	public VStreamerData.Endpoint[] getEndpoints()
			throws VStreamError, VStreamFailure {
		this.check_closed_or_failed();
		Endpoint[] result = new Endpoint[2];
		result[0] = new Endpoint(true, 0);
		result[1] = new Endpoint(true, data.length);
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
		mask = mask.subtract(this.getOptionalMode());

		return new VStreamMode[] {mode, mask};
	}

	@Override
	public VStreamMode getOptionalMode() {
		VStreamMode mode = new VStreamMode();
		mode.setSeekRew(true);
		mode.setSeekFwd(true);
		return mode;
	}

	/**
	 * Creates an unitialized array for the class' element data type.
	 *
	 * @param len array length
	 * @return uninitialized array of given length
	 */
	public abstract T[] createArray(int len);

	void check_closed_or_failed()
			throws VStreamError, VStreamFailure {
		if (data == null)
			if (failed)
				throw new VStreamFailure("Streamer data had a failure");
			else
				throw new VStreamError("Streamer data error");
	}
}
