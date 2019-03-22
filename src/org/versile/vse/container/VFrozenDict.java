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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTaggedParser;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleConverter;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * A dictionary of VEntity keys and values.
 */
public class VFrozenDict extends VEntity {

	/**
	 * VSE code for the VFrozenDict type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "frozendict"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 2));

	Map<VEntity, VEntity> dict;

	public VFrozenDict(Map<? extends VEntity, ? extends VEntity> dict) {
		this.dict = new Hashtable<VEntity, VEntity>();
		for (VEntity key: dict.keySet())
			this.dict.put(key, dict.get(key));
	}

	/**
	 * Get a shallow copy of the held dictionary.
	 *
	 * @return native set
	 */
	public Map<VEntity, VEntity> getValue() {
		Map<VEntity, VEntity> result = new Hashtable<VEntity, VEntity>();
		for (VEntity key: dict.keySet())
			result.put(key, dict.get(key));
		return result;
	}

	/**
	 * Get a Versile Entity Representation of this object
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VFrozenDict.VSE_CODE.getTags(ctx);
		LinkedList<VEntity> _elements = new LinkedList<VEntity>();
		for (VEntity key: dict.keySet()) {
			_elements.addLast(key);
			_elements.addLast(dict.get(key));
		}
		VEntity value = new VTuple(_elements);
		return new VTagged(value, tags);
	}

	/**
	 * Get VSE decoder for tagged-value data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				class Combiner extends VCombiner {
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						VTuple elements = null;
						try {
							elements = VTuple.valueOf(objects);
						} catch (VEntityError e) {
							throw new CombineException();
						}
						if ((elements.length() % 2) != 0)
							throw new CombineException();
						Map<VEntity, VEntity> dict = new Hashtable<VEntity, VEntity>();
						Iterator<VEntity> iter = elements.iterator();
						while (iter.hasNext()) {
							VEntity key = iter.next();
							VEntity val = iter.next();
							dict.put(key, val);
						}
						return new VFrozenDict(dict);
					}
				}
				if (tags.length > 0)
					throw new VModuleError("Illegal use of residual tags");
				VTuple elements = null;
				try {
					elements = VTuple.valueOf(value);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
				Vector<Object> comb_items = new Vector<Object>();
				for (Object obj: elements)
					comb_items.add(obj);
				return new VCombiner.Pair(new Combiner(), comb_items);
			}
		}
		return new Decoder();
	}

	/**
	 * Get VSE converter for native type.
	 *
	 * @return converter
	 * @throws VTaggedParseError
	 */
	static public VModuleConverter.Converter _v_vse_converter() {
		class Converter extends VModuleConverter.Converter {
			@Override
			public Pair convert(Object obj) throws VModuleError {
				try {
					return VFrozenDict._v_converter(obj);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
			}
		}
		return new Converter();
	}

	/**
	 * Generates an entity converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param obj native object to convert
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
			throws VEntityError {
		class Combiner extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				VTuple elements = null;
				try {
					elements = VTuple.valueOf(objects);
				} catch (VEntityError e) {
					throw new CombineException();
				}
				if ((elements.length() % 2) != 0)
					throw new CombineException();
				Map<VEntity, VEntity> dict = new Hashtable<VEntity, VEntity>();
				Iterator<VEntity> iter = elements.iterator();
				while (iter.hasNext()) {
					VEntity key = iter.next();
					VEntity val = iter.next();
					dict.put(key, val);
				}
				return new VFrozenDict(dict);
			}
		}
		if (!(obj instanceof Map<?, ?>))
			throw new VEntityError("Input type is not a Map");
		Map<?, ?> d_obj = (Map<?, ?>)obj;
		Vector<Object> comb_items = new Vector<Object>();
		for (Object key: d_obj.keySet()) {
			comb_items.add(key);
			comb_items.add(d_obj.get(key));
		}
		return new VCombiner.Pair(new Combiner(), comb_items);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		class Combiner extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				Map<Object, Object> result = new Hashtable<Object, Object>();
				Iterator<Object> iter = objects.iterator();
				while (iter.hasNext()) {
					Object key = iter.next();
					Object val = iter.next();
					result.put(key, val);
				}
				return result;
			}
		}
		Vector<Object> comb_items = new Vector<Object>();
		for (VEntity key: dict.keySet()) {
			comb_items.add(key);
			comb_items.add(dict.get(key));
		}
		return new VCombiner.Pair(new Combiner(), comb_items);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	/**
	 * Get a set view of dictionary keys.
	 *
	 * @return dictionary keys
	 */
	public Set<VEntity> keys() {
		return dict.keySet();
	}

	/**
	 * Get a dictionary element
	 *
	 * @param key dictionary key to look up
	 * @return resulting value (or null if key not found)
	 */
	public VEntity get(VEntity key) {
		return dict.get(key);
	}

	@Override
	public String toString() {
		return dict.toString();
	}

	/**
	 * Converts input to a {@link VFrozenDict}.
	 *
	 * <p>{@link VFrozenDict} is returned as-is, and Map is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * <p>Performs only standard VEntity conversions. For additional
	 * conversions use {@link #valueOf(Object, VTaggedParser)}</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VFrozenDict valueOf(Object value)
		throws VEntityError {
		return VFrozenDict.valueOf(value, null);
	}

	/**
	 * Converts input to a {@link VFrozenDict}.
	 *
	 * <p>{@link VFrozenDict} is returned as-is, and Map is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @param parser parser for lazy-conversion to VEntity types (or null)
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VFrozenDict valueOf(Object value, VTaggedParser parser)
		throws VEntityError {
		if (value instanceof VFrozenDict)
			return (VFrozenDict) value;
		else if (value instanceof Map<?, ?>) {
			Map<VEntity, VEntity> _elements = new Hashtable<VEntity, VEntity>();
			Map<?, ?> _dict = (Map<?, ?>)value;
			for (Object key: _dict.keySet()) {
				// Throws VEntityError if conversion fails
				Object val = _dict.get(key);
				_elements.put(VEntity._v_lazy(key, parser), VEntity._v_lazy(val, parser));
			}
			return new VFrozenDict(_elements);
		}
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a Map.
	 *
	 * <p>Map<?, ?> is returned as-is, and {@link VFrozenDict} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * <p>Performs lazy-conversion of VFrozenDict elements using default VEntity
	 * conversion.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Map<?, ?> nativeOf(Object value)
		throws VEntityError {
		return VFrozenDict.nativeOf(value, true, null);
	}

	/**
	 * Converts input to a HashSet.
	 *
	 * <p>Map<?, ?> is returned as-is, and {@link VFrozenDict} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * <p>Performs only standard VEntity conversions. For additional
	 * conversions use {@link #nativeOf(Object, Boolean, VTaggedParser)}</p>
	 *
	 * @param value value to convert
	 * @param lazyElements if true lazy-convert elements to native type
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Map<?, ?> nativeOf(Object value, Boolean lazyElements)
		throws VEntityError {
		return VFrozenDict.nativeOf(value, lazyElements, null);
	}

	/**
	 * Converts input to a HashSet.
	 *
	 * <p>Map<?, ?> is returned as-is, and {@link VFrozenDict} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @param lazyElements if true lazy-convert elements to native type
	 * @param parser parser for lazy-conversion of elements (or null)
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Map<?, ?> nativeOf(Object value, Boolean lazyElements, VTaggedParser parser)
		throws VEntityError {
		if (value instanceof Map<?, ?>)
			return (Map<?, ?>)value;
		else if (value instanceof VFrozenDict) {
			VFrozenDict _dict = (VFrozenDict) value;
			Map<Object, Object> result = new Hashtable<Object, Object>();
			for (VEntity _key: _dict.keys()) {
				Object key = VEntity._v_lazy_native(_key, parser);
				Object val = VEntity._v_lazy_native(_dict.get(_key), parser);
				result.put(key, val);
			}
			return result;
		}
		else
			throw new VEntityError("Cannot convert input value");
	}
}
