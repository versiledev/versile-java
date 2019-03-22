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

package org.versile.orb.entity;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VCombiner;
import org.versile.orb.entity.decoder.VTupleDecoder;


/**
 * Represents the VFE VTuple type.
 *
 * <p>Holds an ordered list of {@link VEntity} entities.</p>
 */
public final class VTuple extends VEntity implements Iterable<VEntity> {

	VEntity[] value;

	/**
	 * Initialize empty tuple.
	 */
	public VTuple() {
		this.value = new VEntity[0];
	}

	/**
	 * Initialize with provided elements.
	 *
	 * @param value iterable with tuple values
	 */
	public VTuple(Iterable<? extends VEntity> value) {
		int size = 0;
		LinkedList<VEntity> tmp_list = new LinkedList<VEntity>();
		for (VEntity item: value) {
			tmp_list.addLast(item);
			size++;
		}
		this.value = new VEntity[size];
		for (int i = 0; i < size; i++)
			this.value[i] = tmp_list.removeFirst();
	}

	/**
	 * Initialize with provided elements.
	 *
	 * @param value tuple values
	 */
	public VTuple(VEntity[] value) {
		int size = 0;
		LinkedList<VEntity> tmp_list = new LinkedList<VEntity>();
		for (VEntity item: value) {
			tmp_list.addLast(item);
			size++;
		}
		this.value = new VEntity[size];
		for (int i = 0; i < size; i++)
			this.value[i] = tmp_list.removeFirst();
	}

	/**
	 * Generates a tuple from a variable argument list.
	 *
	 * @param items tuple elements
	 * @return constructed tuple
	 */
	public static VTuple fromElements(VEntity... items) {
		return new VTuple(items);
	}

	/**
	 * Value represented by this object.
	 *
	 * @return value
	 */
	public VEntity[] getValue() {
		VEntity[] result = new VEntity[value.length];
		for (int i = 0; i < result.length; i++)
			result[i] = value[i];
		return result;
	}

	/**
	 * Return tuple element at index
	 *
	 * @param index index
	 * @return element
	 */
	public VEntity get(int index) {
		return value[index];
	}

	/**
	 * Number of tuple elements.
	 *
	 * @return number of elements
	 */
	public int length() {
		return value.length;
	}

	@Override
	public VEntity[] _v_native() {
		VEntity[] result = new VEntity[value.length];
		for (int i = 0; i < result.length; i++)
			result[i] = value[i];
		return result;
	}

	class VTupleIterator implements Iterator<VEntity> {
		VEntity[] items;
		int pos;

		public VTupleIterator(VEntity[] items) {
			this.items = items;
			pos = 0;
		}

		@Override
		public boolean hasNext() {
			return (pos < value.length);
		}

		@Override
		public VEntity next() {
			pos++;
			return value[pos-1];
		}

		@Override
		public void remove() {
			// Does nothing
		}
	}

	@Override
	public Iterator<VEntity> iterator() {
		return new VTupleIterator(value);
	}

	/**
	 * Tuple content as a new array (shallow copy).
	 *
	 * @return array with tuple elements
	 */
	public VEntity[] toArray() {
		VEntity[] result = new VEntity[this.length()];
		for (int i = 0; i < result.length; i++)
			result[i] = value[i];
		return result;
	}

	@Override
	public String toString() {
		String result = "(";
		boolean first = true;
		for (VEntity item: value) {
			if (first)
				first = false;
			else
				result += ", ";
			result += item;
		}
		result += ")";
		return result;
	}

	/**
	 * Generates a native converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
			throws VEntityError {
		class Aggregator extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				LinkedList<VEntity> entities = new LinkedList<VEntity>();
				for (Object obj: objects) {
					try {
						entities.addLast((VEntity)obj);
					} catch (Exception e) {
						throw new CombineException();
					}
				}
				return new VTuple(entities);
			}
		}
		Vector<Object> objs = new Vector<Object>();
		try {
			for (Object o: ((Object[])obj))
				objs.add(o);
		} catch (Exception e) {
			throw new VEntityError("Input object is not an array");
		}
		return new VCombiner.Pair(new Aggregator(), objs);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		class Aggregator extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				Object[] result = new Object[objects.size()];
				for (int i = 0; i < objects.size(); i++)
					result[i] = objects.get(i);
				return result;
			}
		}
		Vector<Object> vec = new Vector<Object>();
		for (Object o: this.getValue())
			vec.add(o);
		return new VCombiner.Pair(new Aggregator(), vec);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		VByteBuffer header = new VByteBuffer();
		if (explicit)
			header.append(new byte[] {(byte)0xf6});
		header.append(VInteger.posint_to_netbytes(value.length));
		VEncoderData result = new VEncoderData(header.popAll(), new byte[0]);
		for (VEntity item: value)
			result.addEmbedded(item,  true);
		return result;
	}

	/**
	 * Generate a reader for reading this entity class from (explicit) serialized data.
	 *
	 * @param ctx serialization I/O context
	 * @return reader
	 */
	public static VEntityReader _v_reader(VIOContext ctx) {
		VEntityReader reader = new VEntityReader();
		try {
			reader.setDecoder(new VTupleDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Converts input to an array.
	 *
	 * <p>Object[] is returned as-is, and {@link VTuple} is converted to an
	 * array holding internal elements. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	public static Object[] asArray(Object value)
		throws VEntityError {
		if (value instanceof Object[])
			return (Object[]) value;
		else if (value instanceof VTuple)
			return ((VTuple)value).toArray();
		else
			throw new VEntityError("Cannot convert to Object[]");
	}

	/**
	 * Converts input to a tuple.
	 *
	 * <p>A {@link VTuple} is returned as-is. Iv 'tuple' is an array or an iterable
	 * an attempt is made to represent its elements as a VTuple structure.
	 * Any other input results in an exception.</p>
	 *
	 * <p>Performs only standard VEntity conversions. For additional
	 * conversions use {@link #valueOf(Object, VTaggedParser)}</p>
	 *
	 * @param tuple input number object
	 * @return number as VInteger
	 * @throws VEntityError cannot convert
	 */
	public static VTuple valueOf(Object tuple)
		throws VEntityError {
		return VTuple.valueOf(tuple, null);
	}

	/**
	 * Converts input to a tuple.
	 *
	 * <p>A {@link VTuple} is returned as-is. Iv 'tuple' is an array or an iterable
	 * an attempt is made to represent its elements as a VTuple structure.
	 * Any other input results in an exception.</p>
	 *
	 * @param tuple input number object
	 * @param parser parser for lazy-converting elements (or null)
	 * @return number as VInteger
	 * @throws VEntityError cannot convert
	 */
	public static VTuple valueOf(Object tuple, VTaggedParser parser)
		throws VEntityError {
		if (tuple instanceof VTuple)
			return (VTuple)tuple;

		LinkedList<VEntity> items = new LinkedList<VEntity>();
		if (tuple instanceof Object[]) {
			for (Object obj: (Object[]) tuple) {
				if (obj instanceof VEntity)
					items.addLast((VEntity)obj);
				else {
					// This will throw an exception if cannot convert
					items.addLast(VEntity._v_lazy(obj, parser));
				}
			}
		}
		else if (tuple instanceof Iterable<?>) {
			for (Object obj: (Iterable<?>)tuple) {
				if (obj instanceof VEntity)
					items.addLast((VEntity)obj);
				else {
					// This will throw an exception if cannot convert
					items.addLast(VEntity._v_lazy(obj, parser));
				}
			}
		}
		else
			throw new VEntityError("Cannot represent as VTuple");

		return new VTuple(items);
	}

	/**
	 * Converts input to a tuple.
	 *
	 * <p>An Object[] is returned as-is. A List<?> is returned as an Object[].
     * {@link VTuple} is converted to an Object[] representation.
	 * Any other input results in an exception.</p>
	 *
	 * <p>Performs lazy-conversion of VTuple elements using default VEntity
	 * conversion.</p>
	 *
	 * @param tuple input number object
	 * @return number as VInteger
	 * @throws VEntityError cannot convert
	 */
	public static Object[] nativeOf(Object tuple)
		throws VEntityError {
		return VTuple.nativeOf(tuple, true, null);
	}

	/**
	 * Converts input to a tuple.
	 *
	 * <p>An Object[] is returned as-is. A List<?> is returned as an Object[].
     * {@link VTuple} is converted to an Object[] representation.
	 * Any other input results in an exception.</p>
	 *
	 * <p>Performs only standard VEntity conversions. For additional
	 * conversions use {@link #nativeOf(Object, Boolean, VTaggedParser)}</p>
	 *
	 * @param tuple input number object
	 * @return number as VInteger
	 * @throws VEntityError cannot convert
	 */
	public static Object[] nativeOf(Object tuple, Boolean lazyElements)
		throws VEntityError {
		return VTuple.nativeOf(tuple, lazyElements, null);
	}

	/**
	 * Converts input to a tuple.
	 *
	 * <p>An Object[] is returned as-is. A List<?> is returned as an Object[].
     * {@link VTuple} is converted to an Object[] representation.
	 * Any other input results in an exception.</p>
	 *
	 * @param tuple input number object
	 * @param parser parser for lazy-converting elements (or null)
	 * @return number as VInteger
	 * @throws VEntityError cannot convert
	 */
	public static Object[] nativeOf(Object tuple, Boolean lazyElements, VTaggedParser parser)
		throws VEntityError {
		if (tuple instanceof Object[])
			return (Object[])tuple;
		else if (tuple instanceof List<?>) {
			List<?> ltup = (List<?>)tuple;
			Object[] result = new Object[ltup.size()];
			int i = 0;
			for (Object obj: ltup) {
				result[i] = obj;
				i += 1;
			}
			return result;
		}
		else if (tuple instanceof VTuple) {
			VTuple vtup = (VTuple) tuple;
			Object[] result = new Object[vtup.length()];
			for (int i = 0; i < vtup.length(); i++) {
				Object item = vtup.get(i);
				if (lazyElements)
					item = VEntity._v_lazy_native(item, parser);
				result[i] = item;
			}
			return result;
		}
		else
			throw new VEntityError("Cannot represent as VTuple");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VTuple) {
			VTuple t_other = (VTuple)other;
			if (t_other.length() != value.length)
				return false;
			for (int i = 0; i < value.length; i++)
				if (!t_other.get(i).equals(value[i]))
					return false;
			return true;
		}
		else if (other instanceof Object[]) {
			Object[] o_other = (Object[]) other;
			if (o_other.length != value.length)
				return false;
			for (int i = 0; i < value.length; i++)
				if (!o_other[i].equals(value[i]))
					return false;
			return true;
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
