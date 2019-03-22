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

import org.versile.orb.entity.VEntity;


/**
 * Streamer data interface to an iterator of VEntity data.
 *
 * <p>Operates on VEntity data elements.</p>
 */
public class VEntityIteratorStreamerData extends VIteratorStreamerData<VEntity> {

	/**
	 * Set up streamer data.
	 *
	 * <p>Sets up streamer data with a default size of internal stream element
	 * buffer.</p>
	 *
	 * @param iterator iterator which provides data to stream
	 */
	public VEntityIteratorStreamerData(Iterator<VEntity> iterator) {
		super(iterator);
	}

	/**
	 * Set up streamer data.
	 *
	 * @param iterator iterator which provides data to stream
	 * @param bufLen max elements to hold in internal buffer (or null)
	 */
	public VEntityIteratorStreamerData(Iterator<VEntity> iterator, int bufLen) {
		super(iterator, bufLen);
	}

	@Override
	public VEntity[] createArray(int len) {
		return new VEntity[len];
	}
}
