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

package org.versile.common.util;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Buffer for byte data.
 *
 * <p>Simplifies byte data FIFO handling by providing commonly required functionality. Retains a
 * list of the byte[] chunks appended to the buffer, retaining each chunk internally as-is until all its
 * data has been popped.</p>
 */
public class VByteBuffer {

	LinkedList<byte[]> data;
	int l_len = 0;
	int ipos = 0;

	/**
	 * Constructs an empty buffer.
	 */
	public VByteBuffer() {
		data = new LinkedList<byte[]>();
	}

	/**
	 * Constructs a buffer which holds the provided data.
	 *
	 * @param data initial buffer data
	 */
	public VByteBuffer(byte[] data) {
		this.data = new LinkedList<byte[]>();
		this.append(data);
	}

	/**
	 * Appends a single byte value to the end of the buffer.
	 *
	 * @param value single byte value to append
	 */
	public synchronized void append(byte value) {
		byte[] data = new byte[] {value};
		this.appendReference(data);
	}

	/**
	 * Appends data to the end of the buffer.
	 *
	 * <p>The buffer will create an internal copy of the data held in 'data'.</p>
	 *
	 * @param data data to append
	 */
	public synchronized void append(byte[] data) {
		if (data.length == 0)
			return;
		byte[] _data = new byte[data.length];
		for (int i = 0; i < _data.length; i++)
			_data[i] = data[i];
		this.data.add(_data);
		l_len += _data.length;
	}

	/**
	 * Appends a reference to the data to the end of the buffer.
	 *
	 * <p>The buffer will hold a reference to 'data' instead of creating an
	 * internal copy, so the caller must make sure not to overwrite or
	 * otherwise modify after sending. For a safe version, use {@link #append}.</p>
	 *
	 * @param data data to append
	 */
	public synchronized void appendReference(byte[] data) {
		if (data.length == 0)
			return;
		this.data.add(data);
		l_len += data.length;
	}

	/**
	 * Pops byte data from the front of the buffer.
	 *
	 * @param num maximum bytes to pop
	 * @return popped data
	 */
	public synchronized byte[] pop(int num) {
		int rlen = num;
		if (rlen > l_len)
			rlen = l_len;
		byte[] result = new byte[rlen];
		int pos = 0;
		int left = rlen;
		while (left > 0) {
			byte[] item = data.getFirst();
			int ilen = item.length;
			int _pop = ilen - ipos;
			if (_pop > left)
				_pop = left;
			System.arraycopy(item, ipos, result, pos, _pop);
			ipos += _pop;
			pos += _pop;
			left -= _pop;
			l_len -= _pop;
			if (ipos == ilen) {
				data.removeFirst();
				ipos = 0;
			}
		}
		return result;
	}

	/**
	 * Pops all buffer data from the buffer
	 *
	 * @return all buffer data
	 */
	public synchronized byte[] popAll() {
		return this.pop(l_len);
	}

	/**
	 * Returns data from front of the buffer without popping.
	 *
	 * @param num maximum number of bytes to return
	 * @return data from front of the buffer
	 */
	public synchronized byte[] peek(int num) {
		ListIterator<byte[]> iter = data.listIterator(0);
		int _ipos = ipos;

		int rlen = num;
		if (rlen > l_len)
			rlen = l_len;
		byte[] result = new byte[rlen];
		int pos = 0;
		int left = rlen;
		while (left > 0) {
			byte[] item = iter.next();
			int ilen = item.length;
			int _pop = ilen - _ipos;
			if (_pop > left)
				_pop = left;
			System.arraycopy(item, _ipos, result, pos, _pop);
			left -= _pop;
			pos += _pop;
			_ipos = 0;
		}
		return result;
	}

	/**
	 * Returns all buffer data without popping it from the buffer.
	 *
	 * @return buffer data
	 */
	public synchronized byte[] peekAll() {
		return this.peek(this.length());
	}

	/**
	 * Removes data from front of the buffer.
	 *
	 * @param num maximum number of bytes to remove
	 */
	public synchronized void remove(int num) {
		if (num >= l_len) {
			this.clear();
			return;
		}

		int rlen = num;
		int left = rlen;
		while (left > 0) {
			byte[] item = data.getFirst();
			int ilen = item.length;
			int _pop = ilen - ipos;
			if (_pop > left)
				_pop = left;
			ipos += _pop;
			left -= _pop;
			l_len -= _pop;
			if (ipos == ilen) {
				data.removeFirst();
				ipos = 0;
			}
		}
	}

	/**
	 * Buffer length.
	 *
	 * @return number of bytes in buffer
	 */
	public synchronized int length() {
		return this.l_len;
	}

	/**
	 * Check if the buffer is empty.
	 *
	 * @return true if buffer is empty
	 */
	public synchronized boolean isEmpty() {
		return (this.l_len == 0);
	}

	/**
	 * Check if the buffer has data.
	 *
	 * @return true if buffer is not empty
	 */
	public synchronized boolean hasData() {
		return (this.l_len > 0);
	}

	/**
	 * Clears the buffer.
	 */
	public synchronized void clear() {
		data.clear();
		ipos = 0;
		l_len = 0;
	}
}
