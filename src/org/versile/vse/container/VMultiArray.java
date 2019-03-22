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

import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VNone;


/**
 * N-dimensional array of VEntity elements.
 */
public class VMultiArray {

	int[] dim;
	VEntity[] elements;
	int[] multipliers;

	/**
	 * Constructs an empty multi-dimensional array.
	 *
	 * @param dim array dimensions
	 * @throws IllegalArgumentException
	 */
	public VMultiArray(int[] dim)
			throws IllegalArgumentException {
		this.construct(dim, null, null);
	}

	/**
	 * Constructs an initialized multi-dimensional array.
	 *
	 * <p>The provided data must have the same layout as the VMultiFrozenArray
	 * Versile Entity Representation encoding layout, and must have the same
	 * length as the number of array elements.</p>
	 *
	 * @param dim array dimensions
	 * @param data array input data (following VER layout)
	 * @throws IllegalArgumentException
	 */
	public VMultiArray(int[] dim, VEntity[] data)
			throws IllegalArgumentException {
		this.construct(dim, data, null);
	}

	/**
	 * Constructs a filled multi-dimensional array.
	 *
	 * @param dim array dimensions
	 * @param fill value to set for all array elements
	 * @throws IllegalArgumentException
	 */
	public VMultiArray(int[] dim, VEntity fill)
			throws IllegalArgumentException {
		this.construct(dim, null, fill);
	}

	void construct(int[] dim, VEntity[] data, VEntity fill)
			throws IllegalArgumentException {
		if (dim.length == 0)
			throw new IllegalArgumentException("Must have at least one dimension");
		int num_elements = 1;
		for (int d: dim) {
			if (d < 1)
				throw new IllegalArgumentException("Dimensions must be positive");
			num_elements *= d;
		}
		this.dim = dim;
		elements = new VEntity[num_elements];

		multipliers = new int[dim.length];
		int mul = 1;
		for (int i = 0; i < dim.length; i++) {
			multipliers[i] = mul;
			mul *= dim[i];
		}

		if (data != null) {
			if (fill != null)
				throw new IllegalArgumentException("data and fill cannot both be set");
			if (data.length != elements.length)
				throw new IllegalArgumentException("Invalid length of provided data");
			for (int i = 0; i < data.length; i++) {
				VEntity value = data[i];
				if (value == null)
					value = VNone.get();
				elements[i] = data[i];
			}
		}
		else if (fill != null) {
			for (int i = 0; i < elements.length; i++)
				elements[i] = fill;
		}
		else {
			for (int i = 0; i < elements.length; i++)
				elements[i] = VNone.get();
		}
	}

	/**
	 * Get array dimensions.
	 *
	 * @return dimensions
	 */
	public int[] getDimensions() {
		int[] result = new int[dim.length];
		for (int i = 0; i < result.length; i++)
			result[i] = dim[i];
		return result;
	}

	/**
	 * Get a flattened representation.
	 *
	 * <p>Generates a representation which complies with the Versile
	 * Entity Representation encoded format.</p>
	 *
	 * @return flattened array data
	 */
	public synchronized VEntity[] flatten() {
		VEntity[] result = new VEntity[elements.length];
		for (int i = 0; i < result.length; i++)
			result[i] = elements[i];
		return result;
	}

	/**
	 * Get an array element.
	 *
	 * @param index array index
	 * @return element at index
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public synchronized VEntity get(int... index)
			throws ArrayIndexOutOfBoundsException {
		if (index.length != dim.length)
			throw new ArrayIndexOutOfBoundsException("Invalid number of dimensions");
		for (int i = 0; i < index.length; i++) {
			if (index[i] < 0 || index[i] >= dim[i])
				throw new ArrayIndexOutOfBoundsException("Invalid index");
		}
		int pos = 0;
		for (int i = 0; i < index.length; i++)
			pos += index[i]*multipliers[i];
		return elements[pos];
	}

	/**
	 * Set an array element.
	 *
	 * @param value value to set
	 * @param index array index
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public synchronized void set(VEntity value, int... index)
			throws ArrayIndexOutOfBoundsException {
		if (index.length != dim.length)
			throw new ArrayIndexOutOfBoundsException("Invalid number of dimensions");
		for (int i = 0; i < index.length; i++) {
			if (index[i] < 0 || index[i] >= dim[i])
				throw new ArrayIndexOutOfBoundsException("Invalid index");
		}
		int pos = 0;
		for (int i = 0; i < index.length; i++)
			pos += index[i]*multipliers[i];
		if (value == null)
			value = VNone.get();
		elements[pos] = value;
	}

	/**
	 * Generates a frozen representation of this array.
	 *
	 * @return frozen array
	 */
	public VFrozenMultiArray getFrozen() {
		return new VFrozenMultiArray(this);
	}

	@Override
	public String toString() {
		String result = "[\n";
		int[] index = new int[dim.length];
		for (int i = 0; i < index.length; i++)
			index[i] = 0;
		while (true) {
			boolean first = true;
			for (int i = 0; i < index.length; i++) {
				if (first) {
					result += "  ";
					first = false;
				}
				else
					result += ", ";
				result += index[i];
			}
			result += ": " + this.get(index);

			boolean updated = false;
			for (int i = 0; i < index.length; i++) {
				index[i] += 1;
				if (index[i] == dim[i]) {
					index[i] = 0;
					if (i == dim.length-1)
						break;
				}
				else {
					updated = true;
					break;
				}
			}
			result += "\n";
			if (!updated)
				break;
		}
		result += "]\n";
		return result;
	}
}
