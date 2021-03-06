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

package org.versile.vse.math;


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
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;
import org.versile.vse.semantics.VConcept;


/**
 * A VSE prefixed unit.
 *
 * <p>A prefixed unit is a 'unit' (e.g. 'litre') with a prefix which scales
 * that unit (e.g. 'milli'). The prefix and unit should normally both be
 * VSE concepts.</p>
 *
 * <p>The API of this class is subject to change. Currently the API allows a unit or prefix to be any
 * {@link VEntity}, however this may change in later versions as the specification related to concepts
 * and mathematics evolves. The intention for the API is that the prefix and unit should currently be
 * {@link VConcept} objects.</p>
 */
public class VPrefixedUnit extends VEntity {

	/**
	 * VSE code for the VSE concept type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"math", "prefixedunit"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(7, 1));

	// Prefix
	VEntity prefix;

	// Unit
	VEntity unit;

	/**
	 * Instantiate prefixed unit object.
	 *
	 * @param prefix concept which refers to the prefix
	 * @param unit unit being prefixed (scaled) by 'prefix'
	 */
	public VPrefixedUnit(VEntity prefix, VEntity unit) {
		this.prefix = prefix;
		this.unit = unit;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
	VEntity[] tags = VSE_CODE.getTags(ctx);
	VEntity value = new VTuple(new VEntity[] {prefix, unit});
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
					Object value;
					Object[] tags;
					public Combiner(Object value, Object[] tags) {
						this.value = value;
						this.tags = tags;
					}
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						if (tags.length != 0)
							throw new CombineException("VPrefixedUnit takes no residual tags");
							VTuple _tuple;
							VEntity _prefix, _unit;
							try {
								_tuple = VTuple.valueOf(value);
								if (_tuple.length() != 2)
									throw new CombineException("Illegal VPrefixedUnit encoding");
								_prefix = _tuple.get(0);
								_unit = _tuple.get(1);
							} catch (VEntityError e) {
								throw new CombineException("Illegal VPrefixedUnit encoding");
							}
							return new VPrefixedUnit(_prefix, _unit);
					}
				}
				if (tags.length != 0)
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
				return new VCombiner.Pair(new Combiner(value, tags), comb_items);
			}
		}
		return new Decoder();
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	@Override
	public String toString() {
		return "" + prefix + "" + unit;
	}
}
