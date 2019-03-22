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

import java.util.LinkedList;


/**
 * Simple non-caching stream buffer.
 *
 * @param <T> the data type transferred by the stream
 */
public abstract class VSimpleStreamBuffer<T> extends VStreamBuffer<T> {

	LinkedList<T[]> chunks;
	int chunk_pos = 0;
	long num_elements = 0;
	long read_pos = 0;
	long write_pos = 0;

	public VSimpleStreamBuffer() {
		chunks = new LinkedList<T[]>();
	}

	@Override
	public void newContext(long pos, boolean canCache) {
		read_pos = pos;
		write_pos = pos;
		chunks.clear();
	}

	@Override
	public void endContext() {
		read_pos = 0;
		write_pos = 0;
		chunks.clear();
	}

	@Override
	public synchronized void write(T[] data, boolean advance) {
		write_pos += data.length;
		if (advance) {
			read_pos = write_pos;
			chunks.clear();
			chunk_pos = 0;
		}
		else {
			chunks.addLast(data);
			num_elements += data.length;
		}
	}

	@Override
	public synchronized long getMaxRead() {
		return num_elements;
	}

	public synchronized T[] read(int maxRead) {
		int num_read = maxRead;
		if (num_read > num_elements)
			num_read = (int) num_elements;
		T[] result = this.createArray(num_read);
		int r_pos = 0;
		while (num_read > 0) {
			T[] chunk = chunks.getFirst();
			int c_num = chunk.length - chunk_pos;
			if (c_num > num_read)
				c_num = num_read;
			for (int i = 0; i < c_num; i++)
				result[r_pos+i] = chunk[chunk_pos+i];
			r_pos += c_num;
			chunk_pos += c_num;
			num_elements -= c_num;
			num_read -= c_num;
			if (chunk_pos == chunk.length) {
				chunks.removeFirst();
				chunk_pos = 0;
			}
		}
		read_pos += result.length;
		if (write_pos < read_pos)
			write_pos = read_pos;
		return result;
	}

	@Override
	public long getReadPosition() {
		return read_pos;
	}

	@Override
	public long getWritePosition() {
		return write_pos;
	}
}
