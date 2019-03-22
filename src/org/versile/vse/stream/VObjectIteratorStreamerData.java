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


/**
 * Streamer data interface to an object iterator.
 *
 * <p>Operates on objects which are lazy-convertible to VEntity data elements.</p>
 */
public class VObjectIteratorStreamerData extends VIteratorStreamerData<Object> {

	/**
	 * Set up streamer data.
	 *
	 * <p>Sets up streamer data with a default size of internal stream element
	 * buffer.</p>
	 *
	 * @param iterator iterator which provides data to stream
	 */
	public VObjectIteratorStreamerData(Iterator<Object> iterator) {
		super(iterator);
	}

	/**
	 * Set up streamer data.
	 *
	 * @param iterator iterator which provides data to stream
	 * @param bufLen max elements to hold in internal buffer (or null)
	 */
	public VObjectIteratorStreamerData(Iterator<Object> iterator, int bufLen) {
		super(iterator, bufLen);
	}

	@Override
	public Object[] createArray(int len) {
		return new Object[len];
	}
}
