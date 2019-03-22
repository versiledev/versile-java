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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
 * A set of VEntity data.
 */
public class VFrozenSet extends VEntity implements Iterable<VEntity> {

	/**
	 * VSE code for the VFrozenSet type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "frozenset"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 3));

	Set<VEntity> elements;

	public VFrozenSet(Iterable<? extends VEntity> elements) {
		this.elements = new HashSet<VEntity>();
		for (VEntity item: elements)
			this.elements.add(item);
	}

	public VFrozenSet(VEntity[] elements) {
		this.elements = new HashSet<VEntity>();
		for (VEntity item: elements)
			this.elements.add(item);
	}

	/**
	 * Get set elements as a native set class.
	 *
	 * @return native set
	 */
	public Set<VEntity> getValue() {
		Set<VEntity> result = new HashSet<VEntity>();
		for (VEntity item: elements)
			result.add(item);
		return result;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VFrozenSet.VSE_CODE.getTags(ctx);
		VEntity value = new VTuple(elements);
		return new VTagged(value, tags);
	}

	/**
	 * Get VSE decoder for tag data.
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
						return new VFrozenSet(elements);
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
					return VFrozenSet._v_converter(obj);
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
				return new VFrozenSet(elements);
			}
		}
		if (!(obj instanceof Set<?>))
			throw new VEntityError("Input type is not a Set");
		Set<?> s_obj = (Set<?>)obj;
		Vector<Object> comb_items = new Vector<Object>();
		for (Object item: s_obj)
			comb_items.add(item);
		return new VCombiner.Pair(new Combiner(), comb_items);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		class Combiner extends VCombiner {
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				Set<Object> result = new HashSet<Object>();
				for (Object item: objects)
					result.add(item);
				return result;
			}
		}
		Vector<Object> comb_items = new Vector<Object>();
		for (Object item: elements)
			comb_items.add(item);
		return new VCombiner.Pair(new Combiner(), comb_items);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	@Override
	public String toString() {
		return elements.toString();
	}

	@Override
	public Iterator<VEntity> iterator() {
		return elements.iterator();
	}

	/**
	 * Converts input to a {@link VFrozenSet}.
	 *
	 * <p>{@link VFrozenSet} is returned as-is, and Set is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * <p>Performs only standard VEntity conversions. For additional
	 * conversions use {@link #valueOf(Object, VTaggedParser)}</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VFrozenSet valueOf(Object value)
		throws VEntityError {
		return VFrozenSet.valueOf(value, null);
	}

	/**
	 * Converts input to a {@link VFrozenSet}.
	 *
	 * <p>{@link VFrozenSet} is returned as-is, and Set is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @param parser parser for lazy-conversion to VEntity types (or null)
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public VFrozenSet valueOf(Object value, VTaggedParser parser)
		throws VEntityError {
		if (value instanceof VFrozenSet)
			return (VFrozenSet) value;
		else if (value instanceof Set<?>) {
			LinkedList<VEntity> elements = new LinkedList<VEntity>();
			for (Object obj: (Set<?>)value) {
				// Throws VEntityError if conversion fails
				elements.addLast(VEntity._v_lazy(obj, parser));
			}
			return new VFrozenSet(elements);
		}
		else
			throw new VEntityError("Cannot convert input value");
	}

	/**
	 * Converts input to a HashSet.
	 *
	 * <p>Set<?> is returned as-is, and {@link VFrozenSet} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * <p>Performs lazy-conversion of VFrozenSet elements using default VEntity
	 * conversion.</p>
	 *
	 * @param value value to convert
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Set<?> nativeOf(Object value)
		throws VEntityError {
		return VFrozenSet.nativeOf(value, true, null);
	}

	/**
	 * Converts input to a HashSet.
	 *
	 * <p>Set<?> is returned as-is, and {@link VFrozenSet} is converted to the
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
	static public Set<?> nativeOf(Object value, Boolean lazyElements)
		throws VEntityError {
		return VFrozenSet.nativeOf(value, lazyElements, null);
	}

	/**
	 * Converts input to a HashSet.
	 *
	 * <p>Set<?> is returned as-is, and {@link VFrozenSet} is converted to the
	 * return type. Any other input raises an exception.</p>
	 *
	 * @param value value to convert
	 * @param lazyElements if true lazy-convert elements to native type
	 * @param parser parser for lazy-conversion of elements (or null)
	 * @return converted value
	 * @throws VEntityError cannot perform conversion
	 */
	static public Set<?> nativeOf(Object value, Boolean lazyElements, VTaggedParser parser)
		throws VEntityError {
		if (value instanceof Set<?>)
			return (Set<?>)value;
		else if (value instanceof VFrozenSet) {
			Set<Object> result = new HashSet<Object>();
			for (Object obj: (VFrozenSet)value) {
				if (lazyElements)
					obj = VEntity._v_lazy_native(obj, parser);
				result.add(obj);
			}
			return result;
		}
		else
			throw new VEntityError("Cannot convert input value");
	}
}
