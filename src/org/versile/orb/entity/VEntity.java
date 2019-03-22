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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VCombiner.CombineException;
import org.versile.orb.entity.decoder.VEntityDecoder;
import org.versile.vse.VSEResolver;
import org.versile.vse.container.VArrayOfDouble;
import org.versile.vse.container.VArrayOfFloat;
import org.versile.vse.container.VArrayOfInt;
import org.versile.vse.container.VArrayOfLong;
import org.versile.vse.container.VArrayOfVFloat;
import org.versile.vse.container.VArrayOfVInteger;


/**
 * Base class for VFE types.
 *
 * <p>{@link VEntity} methods are prefixed with "_v_" in order to avoid cluttering
 * the available namespace for method names of derived classes. This differs from
 * conventional Java naming conventions, however it aligns with the Versile
 * Platform namespace recommendations.</p>
 */
public abstract class VEntity {

	/**
	 * Returns a native representation of the object.
	 *
	 * <p>Default performs conversion on a {@link #_v_native_converter} converter.</p>
	 *
	 * <p>The returned value may be the object itself if it has no other
	 * native representation.</p>
	 *
	 * @return native representation
	 */
	public Object _v_native() {
		VCombiner.Pair conv_pair = this._v_native_converter();
		try {
			return (conv_pair.getCombiner().combine(conv_pair.getObjects()));
		} catch (CombineException e) {
			// Return this as a fallback
			return this;
		}
	}

	/**
	 * Writes an explicit serialized entity representation.
	 *
	 * @param ctx serialization context
	 * @return serialized representation
	 */
	public final byte[] _v_write(VIOContext ctx) {
		return this._v_writer(ctx, true).writeAll();
	}

	/**
	 * Writes a serialized entity representation.
	 *
	 * @param ctx serialization context
	 * @param explicit if true use explicit encoding
	 * @return serialized representation
	 */
	public final byte[] _v_write(VIOContext ctx, boolean explicit) {
		return this._v_writer(ctx, explicit).writeAll();
	}

	/**
	 * Generates a writer for the entity's (explicit) serialized representation.
	 *
	 * @param ctx serialization context
	 * @return writer
	 */
	public final VEntityWriter _v_writer(VIOContext ctx) {
		return this._v_writer(ctx, true);
	}

	/**
	 * Generates a writer for the entity's serialized representation.
	 *
	 * @param ctx serialization context
	 * @param explicit if true use explicit encoding
	 * @return writer
	 */
	public final VEntityWriter _v_writer(VIOContext ctx, boolean explicit) {
		VEntityWriter writer = new VEntityWriter(ctx, explicit);
		try {
			writer.setEntity(this);
		} catch (VEntityWriterException e) {
			throw new RuntimeException();
		}
		return writer;
	}

	/**
	 * Lazy-converts object to a {@link VEntity}.
	 *
	 * <p>Convenience method for {@link #_v_lazy(Object, VTaggedParser)}.</p>
	 *
	 * @param obj object to convert
	 * @return converted value
	 * @throws VEntityError could not convert
	 */
	public static VEntity _v_lazy(Object obj)
		throws VEntityError {
		return VEntity._v_lazy(obj, null);
	}

	/**
	 * Lazy-converts object to a {@link VEntity}.
	 *
	 * <p>Performs the following conversions, in addition to any conversions
	 * handled by the provided parser:</p>
	 *
	 * <table border="1">
	 * <tr>
	 * <td>From</td>
     * <td>To</td>
     * </tr>
     * <tr>
     * <td>null</td>
     * <td>{@link VNone}</td>
     * </tr>
     * <tr>
     * <td>Byte[]</td>
     * <td>{@link VBytes}</td>
     * </tr>
     * <tr>
     * <td>Boolean</td>
     * <td>{@link VBoolean}</td>
     * </tr>
     * <tr>
     * <td>Float, Double, BigDecimal</td>
     * <td>{@link VFloat}</td>
     * </tr>
     * <tr>
     * <td>Int, Long, BigInteger</td>
     * <td>{@link VInteger}</td>
     * </tr>
     * <tr>
     * <td>Object[]</td>
     * <td>{@link VTuple}</td>
     * </tr>
     * <tr>
     * <td>String</td>
     * <td>{@link VString}</td>
     * </tr>
     * </table>
	 *
	 * @param obj object to convert
	 * @param parser parser for VTagged encoded entities (or null)
	 * @return converted value
	 * @throws VEntityError could not convert
	 */
	public static VEntity _v_lazy(Object obj, VTaggedParser parser)
		throws VEntityError {
		if (obj instanceof VEntity)
			return (VEntity)obj;

		Object agg_result = null;
		class Node {
			Vector<Object> items;
			int unprocessed_index = 0;
			Node parent;
			int parent_index;
			VCombiner aggregator;

			public Node(Vector<Object> items, Node parent, int parent_index, VCombiner aggregator) {
				this.items = items;
				this.parent = parent;
				this.parent_index = parent_index;
				this.aggregator = aggregator;
			}
		}

		Vector<Object> _items = new Vector<Object>();
		_items.add(obj);
		Node node = new Node(_items, null, -1, VCombiner.identity());

		while(true) {
			if(node.unprocessed_index<node.items.size()) {
				int index = node.unprocessed_index;
				node.unprocessed_index++;
				Object _obj = node.items.get(index);
				VCombiner.Pair agg_pair = VEntity._v_top_converter(_obj, parser);
				if (agg_pair.getCombiner() == null) {
					node.items.set(index, agg_pair.getObjects().get(0));
				}
				else {
					node.unprocessed_index = index;
					Node new_node = new Node(agg_pair.getObjects(), node, index, agg_pair.getCombiner());
					node = new_node;
				}
			}
			else if (node.parent != null) {
				Object converted;
				try {
					converted = node.aggregator.combine(node.items);
				} catch (CombineException e) {
					throw new VEntityError("Conversion error");
				}
				Node parent = node.parent;
				int parent_index = node.parent_index;
				node = parent;
				node.items.set(parent_index,  converted);
				node.unprocessed_index++;
			}
			else {
				try {
					agg_result = node.aggregator.combine(node.items);
				} catch (CombineException e) {
					throw new VEntityError("Conversion error");
				}
				break;
			}
		}
		try {
			return (VEntity) agg_result;
		} catch (Exception e) {
			throw new VEntityError("Could not convert");
		}
	}

	/**
	 * Lazy-converts a {@link VEntity} to a native type (if possible).
	 *
	 * <p>Convenience method for {@link #_v_lazy_native(Object, VTaggedParser)}.</p>
	 *
	 * @param obj object to convert
	 * @return converted object, or the object itself
	 */
	public static Object _v_lazy_native(Object obj) {
		return VEntity._v_lazy_native(obj, null);
	}

	/**
	 * Lazy-converts a {@link VEntity} to a native type (if possible).
	 *
	 * <p>Performs conversions of {@link #_v_lazy(Object, VTaggedParser)} in the opposite
	 * direction. In addition, performs any lazy native conversion which is implemented
	 * by the provided parser. If no conversion can be made returns the value itself.</p>
	 *
	 * @param obj object to convert
	 * @param parser parser for VTagged encoded entities (or null)
	 * @return converted object, or the object itself
	 */
	public static Object _v_lazy_native(Object obj, VTaggedParser parser) {
		Object agg_result = null;
		class Node {
			Vector<Object> items;
			int unprocessed_index = 0;
			Node parent;
			int parent_index;
			VCombiner aggregator;

			public Node(Vector<Object> items, Node parent, int parent_index, VCombiner aggregator) {
				this.items = items;
				this.parent = parent;
				this.parent_index = parent_index;
				this.aggregator = aggregator;
			}
		}

		Vector<Object> _items = new Vector<Object>();
		_items.add(obj);
		Node node = new Node(_items, null, -1, VCombiner.identity());

		while(true) {
			if (node.unprocessed_index<node.items.size()) {
				int index = node.unprocessed_index;
				node.unprocessed_index++;
				Object _obj = node.items.get(index);
				VCombiner.Pair agg_pair = VEntity._v_top_native_converter(_obj, parser);
				if (agg_pair.getCombiner() == null) {
					node.items.set(index, agg_pair.getObjects().get(0));
				}
				else {
					node.unprocessed_index = index;
					Node new_node = new Node(agg_pair.getObjects(), node, index, agg_pair.getCombiner());
					node = new_node;
				}
			}
			else if (node.parent != null) {
				Object converted;
				try {
					converted = node.aggregator.combine(node.items);
				} catch (CombineException e) {
					return obj;
				}
				Node parent = node.parent;
				int parent_index = node.parent_index;
				node = parent;
				node.items.set(parent_index,  converted);
				node.unprocessed_index++;
			}
			else {
				try {
					agg_result = node.aggregator.combine(node.items);
				} catch (CombineException e) {
					return obj;
				}
				break;
			}
		}
		return agg_result;
	}

	/**
	 * Generates combiner data for converting a native value to a {@link VEntity}.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param obj value to convert
	 * @param parser parser for VTagged formats (or null)
	 * @return combiner data structure for conversion
	 * @throws VEntityError cannot generate combiner for provided value
	 */
	public static VCombiner.Pair _v_top_converter(Object obj, VTaggedParser parser)
		throws VEntityError {
		if (obj instanceof VEntity) {
			Vector<Object> obj_list = new Vector<Object>();
			obj_list.add(obj);
			return new VCombiner.Pair(null, obj_list);
		}

		if (obj instanceof VProxy)
			return VProxy._v_converter(obj);

		if (obj instanceof Integer || obj instanceof Long || obj instanceof BigInteger)
			return VInteger._v_converter(obj);

		if (obj instanceof Float || obj instanceof Double || obj instanceof BigDecimal)
			return VFloat._v_converter(obj);

		if (obj instanceof Boolean)
			return VBoolean._v_converter(obj);

		if (obj instanceof byte[])
			return VBytes._v_converter(obj);

		if (obj instanceof Byte[])
			return VBytes._v_converter(obj);

		if (obj instanceof String)
			return VString._v_converter(obj);

		if (obj instanceof Object[]) {
			if (VSEResolver.lazyArrays()) {
				if (((Object[])obj).length == 0)
					return VTuple._v_converter(obj);

				// Perform any conversion which is enabled by simply inspecting array type
				if (obj instanceof Integer[])
					return VArrayOfInt._v_converter(obj);
				else if (obj instanceof Long[])
					return VArrayOfLong._v_converter(obj);
				else if (obj instanceof Float[])
					return VArrayOfFloat._v_converter(obj);
				else if (obj instanceof Double[])
					return VArrayOfDouble._v_converter(obj);
				else if (obj instanceof VFloat[])
					return VArrayOfVFloat._v_converter(obj);
				else if (obj instanceof VInteger[])
					return VArrayOfVInteger._v_converter(obj);

				// Inspect types of Object[]
				boolean complete = true;
				boolean _int = false;
				boolean _long = false;
				boolean _bigint = false;
				boolean _vinteger = false;
				boolean _float = false;
				boolean _double = false;
				boolean _vfloat = false;
				for (Object o : (Object[])obj) {
					if (o instanceof Integer)
						_int = true;
					else if (o instanceof Long)
						_long = true;
					else if (o instanceof BigInteger)
						_bigint = true;
					else if (o instanceof VInteger)
						_vinteger = true;
					else if (o instanceof Float)
						_float = true;
					else if (o instanceof Double)
						_double = true;
					else if (o instanceof VFloat)
						_vfloat = true;
					else {
						complete = false;
						break;
					}
				}
				if (!complete)
					return VTuple._v_converter(obj);

				boolean _ints = _int | _long | _bigint | _vinteger;
				boolean _floats = _float | _double | _vfloat;
				if (_ints && _floats) {
					// For simplified conversion, will not attempt conversion of arrays
					// of mixed integer and floating-point based numbers
					return VTuple._v_converter(obj);
				}
				else if (_ints) {
					Object[] _oarr = (Object[])obj;
					if (_vinteger || _bigint) {
						Number[] _arr = new Number[_oarr.length];
						for (int i = 0; i < _arr.length; i++) {
							if (_oarr[i] instanceof VInteger)
								_arr[i] = ((VInteger)_oarr[i]).getValue();
							else
								_arr[i] = (Number)_oarr[i];
						}
						return VArrayOfVInteger._v_converter(_arr);
					}
					else if (_long) {
						Long[] _arr = new Long[_oarr.length];
						for (int i = 0; i < _arr.length; i++)
							_arr[i] = (Long)_oarr[i];
						return VArrayOfLong._v_converter(_arr);
					}
					else {
						Integer[] _arr = new Integer[_oarr.length];
						for (int i = 0; i < _arr.length; i++)
							_arr[i] = (Integer)_oarr[i];
						return VArrayOfInt._v_converter(_arr);
					}
				}
				else if (_floats) {
					Object[] _oarr = (Object[])obj;
					if (_vfloat) {
						VFloat[] _arr = new VFloat[_oarr.length];
						for (int i = 0; i < _arr.length; i++)
							_arr[i] = (VFloat)_oarr[i];
						return VArrayOfVFloat._v_converter(_arr);
					}
					else if (_double) {
						Double[] _arr = new Double[_oarr.length];
						for (int i = 0; i < _arr.length; i++)
							_arr[i] = (Double)_oarr[i];
						return VArrayOfDouble._v_converter(_arr);
					}
					else {
						Float[] _arr = new Float[_oarr.length];
						for (int i = 0; i < _arr.length; i++)
							_arr[i] = (Float)_oarr[i];
						return VArrayOfFloat._v_converter(_arr);
					}
				}

				// If code ever reaches this point, convert as a tuple
				return VTuple._v_converter(obj);
			}
			else
				return VTuple._v_converter(obj);
		}

		if (obj == null)
			return VNone._v_converter(obj);

		if (parser != null) {
			try {
				return parser.converter(obj);
			} catch (VTaggedParseError e) {
				// SILENT
			} catch (VTaggedParseUnknown e) {
				// SILENT
			}
		}

		throw new VEntityError("Could not convert object");
	}

	/**
	 * Generates combiner data for converting to a native representation.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param obj value to convert
	 * @param parser parser for VTagged encoded entities (or null)
	 * @return combiner structure for performing conversion
	 */
	public static VCombiner.Pair _v_top_native_converter(Object obj, VTaggedParser parser) {
		if (obj instanceof VTagged && parser != null) {
			try {
				return parser.native_decoder((VTagged)obj);
			} catch (VTaggedParseError e) {
				// SILENT
			}
		}
		else if (obj instanceof VEntity) {
			return ((VEntity)obj)._v_native_converter();
		}
		else if (obj instanceof Number[] || obj instanceof Byte[] || obj instanceof Boolean[]) {
			// Already represented as a native type, return as-is
			Vector<Object> obj_list = new Vector<Object>();
			obj_list.add(obj);
			return new VCombiner.Pair(null, obj_list);
		}
		else if (obj instanceof VFloat[]) {
			// Already represented as a VEntity based type which cannot be further converted, return as-is
			Vector<Object> obj_list = new Vector<Object>();
			obj_list.add(obj);
			return new VCombiner.Pair(null, obj_list);
		}
		else if (obj instanceof Object[]) {
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
			for (Object o: (Object[])obj)
				vec.add(o);
			return new VCombiner.Pair(new Aggregator(), vec);
		}

		// Could not convert, fall back to returning converter for the object itself
		Vector<Object> obj_list = new Vector<Object>();
		obj_list.add(obj);
		return new VCombiner.Pair(null, obj_list);
	}

	/**
	 * Generates a native converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @return combiner structure for conversion.
	 */
	public VCombiner.Pair _v_native_converter() {
		Vector<Object> obj_list = new Vector<Object>();
		obj_list.add(this);
		return new VCombiner.Pair(null, obj_list);
	}

	/**
	 * Generate a reader for reading a {@link VEntity} from (explicit) serialized data.
	 *
	 * @param ctx serialization I/O context
	 * @return reader
	 */
	public static VEntityReader _v_reader(VIOContext ctx) {
		VEntityReader reader = new VEntityReader();
		try {
			reader.setDecoder(new VEntityDecoder(ctx));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Generates encoder parameters for serializing the object.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param ctx context for object serialization
	 * @param explicit if True use explicit VEntity encoding
	 * @return encoding parameters
	 * @throws VEntityWriterException writer error
	 */
	public abstract VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException;
}
