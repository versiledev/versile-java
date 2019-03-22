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

package org.versile.vse.semantics;


import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;


/**
 * A VSE reference to a platform defined concept.
 */
public class VPlatformConcept extends VConcept {

	// Minimum allowed ID value
    static int MIN_ID_VAL = 1;

    // Maximum allowed ID value
    static int MAX_ID_VAL = 243;

	// Platform defined concept identifier
	int conceptId;

	/**
	 * Instantiate platform defined concept.
	 *
	 * <p>Platform defined concepts refer to concepts defined explicitly
	 * by the Versile Platform standard. They should not be used for referring
	 * to concepts in any other ID spaces.</p>
	 *
	 * @param conceptId associated concept ID
	 * @throws IllegalArgumentException invalid ID
	 */
	public VPlatformConcept(int conceptId)
		throws IllegalArgumentException {
		if (conceptId < MIN_ID_VAL || conceptId > MAX_ID_VAL)
			throw new IllegalArgumentException("Invalid concept ID");
		this.conceptId = conceptId;
	}

	/**
	 * Get the associated platform defined concept ID.
	 *
	 * @return concept ID
	 */
	public int getConceptId() {
		return conceptId;
	}

	@Override
	public int conceptType() {
		return VConcept.TYPE_VP_DEFINED;
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] _tags = VConcept.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[_tags.length + 1];
		for (int i = 0; i < _tags.length; i++)
			tags[i] = _tags[i];
		tags[_tags.length] = new VInteger(VConcept.TYPE_VP_DEFINED);
		VEntity value = new VInteger(conceptId);
		return new VTagged(value, tags);
	}

	@Override
	public String toString() {
		return "VPlatformConcept[" + conceptId + "]";
	}
}
