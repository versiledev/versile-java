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

import java.util.Vector;

import org.versile.common.util.VCombiner;


/**
 * Parser for Versile Entity Representation encoded entities.
 */
public class VTaggedParser {

	/**
	 * Generate decoder for a {@link VTagged} to a {@link VEntity}.
	 *
	 * <p>Default throws {@link VTaggedParseError}, derived classes can override.</p>
	 *
	 * @param obj object to convert
	 * @return conversion structure
	 * @throws VTaggedParseError cannot create converter
	 */
	public VCombiner.Pair decoder(VTagged obj)
		throws VTaggedParseError {
		throw new VTaggedParseError();
	}

	/**
	 * Generate decoder for decoding a {@link VTagged} to a native representation.
	 *
	 * <p>Performs {@link #decoder(VTagged)} and performs an additional follow-on
	 * step to attempt lazy-converting the result to a native format.</p>
	 *
	 * @param obj object to convert
	 * @return conversion structure
	 * @throws VTaggedParseError cannot create converter
	 */
	public final VCombiner.Pair native_decoder(VTagged obj)
			throws VTaggedParseError {
		class NativeCombiner extends VCombiner {
			VCombiner entity_combiner;
			public NativeCombiner(VCombiner entity_combiner) {
				this.entity_combiner = entity_combiner;
			}
			@Override
			public Object combine(Vector<Object> objects)
					throws CombineException {
				Object entity = entity_combiner.combine(objects);
				return VEntity._v_lazy_native(entity);
			}
		}
		VCombiner.Pair dec = this.decoder(obj);
		VCombiner entity_combiner = dec.getCombiner();

		// If combiner is null the decoder objects should be passed straight through; otherwise
		// wrap inside a tag-parser combiner
		if (entity_combiner != null)
			return new VCombiner.Pair(new NativeCombiner(entity_combiner), dec.getObjects());
		else
			return new VCombiner.Pair(null, dec.getObjects());
	}

	/**
	 * Generate converter for conversion to {@link VEntity}.
	 *
	 * <p>Default raises {@link VTaggedParseUnknown}, derived classes can override.</p>
	 *
	 * @param obj object to convert
	 * @return conversion structure
	 * @throws VTaggedParseUnknown object tag format not known
	 * @throws VTaggedParseError error creating converter
	 */
	public VCombiner.Pair converter(Object obj)
		throws VTaggedParseError, VTaggedParseUnknown {
			throw new VTaggedParseUnknown();
	}
}
