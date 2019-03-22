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

package org.versile.vse.container;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VTagged;


/**
 * Base class for VSE arrays with elements of certain type.
 *
 * @param <T> local associated type
 */
public abstract class VArrayOf<T> extends VEntity implements Iterable<T> {

	protected T[] array;

	/**
	 * Create array with provided elements.
	 *
	 * @param array array elements
	 */
	public VArrayOf(T[] array) {
		this.array = createArray(array.length);
		for (int i = 0; i < array.length; i++)
			this.array[i] = array[i];
	}

	@Override
	public Iterator<T> iterator() {
		return Arrays.asList(array).iterator();
	}

	/**
	 * Get array length.
	 *
	 * @return array length
	 */
	public int getLength() {
		return array.length;
	}

	/**
	 * Get array element at index.
	 *
	 * @param index array index
	 * @return array element
	 */
	public T getElement(int index) {
		return array[index];
	}

	@Override
	public String toString() {
		String result = "[";
		boolean first = true;
		for (T t: array) {
			if (first)
				first = false;
			else
				result += ", ";
			result += t;
		}
		result += "]";
		return result;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public abstract VTagged _v_as_tagged(VIOContext ctx);

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		T[] result = createArray(array.length);
		for (int i = 0; i < array.length; i++)
			result[i] = array[i];
		Vector<Object> comb_items = new Vector<Object>();
		comb_items.add(result);
		return new VCombiner.Pair(VCombiner.identity(), comb_items);
	}

	/**
	 * Create an array of type T
	 *
	 * @param length array length
	 * @return array of type T
	 */
	protected abstract T[] createArray(int length);
}
