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
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleConverter;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * Frozen N-dimensional array of VEntity elements.
 *
 * <p>Differs from {@link VMultiArray} in that frozen multi-array element references
 * cannot be changed.</p>
 */
public class VFrozenMultiArray extends VEntity {

	/**
	 * VSE code for the VFrozenSet type
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"container", "frozenmultiarray"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(1, 1));

	VMultiArray array;

	/**
	 * Construct a frozen representation of an existing array.
	 *
	 * @param array non-frozen array
	 */
	public VFrozenMultiArray(VMultiArray array) {
		this.array = new VMultiArray(array.getDimensions(), array.flatten());
	}

	/**
	 * Constructs an initialized frozen multi-dimensional array.
	 *
	 * <p>The provided data must have the same layout as the VMultiFrozenArray
	 * Versile Entity Representation encoding layout, and must have the same
	 * length as the number of array elements.</p>
	 *
	 * @param dim array dimensions
	 * @param data array input data (following VER layout)
	 * @throws IllegalArgumentException
	 */
	public VFrozenMultiArray(int[] dim, VEntity[] data)
			throws IllegalArgumentException {
		this.array = new VMultiArray(dim, data);
	}

	/**
	 * Get array dimensions.
	 *
	 * @return dimensions
	 */
	public int[] getDimensions() {
		return array.getDimensions();
	}

	/**
	 * Get a flattened representation.
	 *
	 * <p>Generates a representation which complies with the Versile
	 * Entity Representation encoded format.</p>
	 *
	 * @return flattened array data
	 */
	public VEntity[] flatten() {
		return array.flatten();
	}

	/**
	 * Get an array element.
	 *
	 * @param index array index
	 * @return element at index
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public VEntity get(int... index)
			throws ArrayIndexOutOfBoundsException {
		return array.get((int[])index);
	}

	@Override
	public String toString() {
		return array.toString();
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VFrozenMultiArray.VSE_CODE.getTags(ctx);
		VEntity value = new VTuple(array.elements);
		int[] dim = this.getDimensions();
		VEntity[] tags = new VEntity[code_tags.length + dim.length];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		for (int i = 0; i < dim.length; i++)
			tags[code_tags.length + i] = new VInteger(dim[i]);
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
					int[] dim;
					public Combiner(int[] dim) {
						this.dim = dim;
					}
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						VEntity[] elements = new VEntity[objects.size()];
						for (int i = 0; i < elements.length; i++)
							try {
							elements[i] = VEntity._v_lazy(objects.get(i));
							} catch (VEntityError e) {
								throw new CombineException();
							}
						try {
							return new VFrozenMultiArray(dim, elements);
						} catch (IllegalArgumentException e) {
							throw new CombineException();
						}
					}
				}
				if (tags.length == 0)
					throw new VModuleError("Invalid residual tag data (no dimensions set)");
				int[] dim = new int[tags.length];
				for (int i = 0; i < tags.length; i++) {
					try {
						dim[i] = VInteger.nativeOf(tags[i]).intValue();
					} catch (VEntityError e) {
						throw new VModuleError("Invalid residual tag data (bad dimension type)");
					}
					if (dim[i] <= 0)
						throw new VModuleError("Invalid residual tag data (zero or negative dimension)");
				}

				VTuple elements = null;
				try {
					elements = VTuple.valueOf(value);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
				Vector<Object> comb_items = new Vector<Object>();
				for (Object obj: elements)
					comb_items.add(obj);
				return new VCombiner.Pair(new Combiner(dim), comb_items);
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
					return VFrozenMultiArray._v_converter(obj);
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
		if (!(obj instanceof VMultiArray))
			throw new VEntityError("Input type is not a VMultiArray");
		Vector<Object> comb_items = new Vector<Object>();
		comb_items.add(new VFrozenMultiArray((VMultiArray)obj));
		return new VCombiner.Pair(VCombiner.identity(), comb_items);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

}
