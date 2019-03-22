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
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTagged;


/**
 * A VSE reference to a 'concept'.
 */
public class VEnWikipediaConcept extends VConcept {

	// Wikipedia article name
	String name;

	/**
	 * Instantiate platform defined concept.
	 *
	 * <p>'name' refers to the article name of an English Wikipedia article, e.g.
	 * name="Hydrogen" would resolve to the concept defined or described by the article at
	 * URL "http://en.wikipedia.org/wiki/Hydrogen".</p>
	 *
	 * @param name article name
	 */
	public VEnWikipediaConcept(String name) {
		this.name = name;
	}

	@Override
	public int conceptType() {
		return VConcept.TYPE_EN_WIKIPEDIA;
	}

	/**
	 * Get the associated English Wikipedia article name.
	 *
	 * @return article name
	 */
	public String getName() {
		return name;
	}

    @Override
    public VConcept[] getEquivalents(int type) {
    	if (type == VConcept.TYPE_EN_WIKIPEDIA)
    		return new VConcept[] {this};
    	else if(type == VConcept.TYPE_URL) {
    		VConcept[] result = new VConcept[2];
    		try {
				result[0] = new VUrlConcept("http://en.wikipedia.org/wiki/", name);
	    		result[1] = new VUrlConcept("https://en.wikipedia.org/wiki/", name);
			} catch (Exception e) {
				// Should never happen
				return new VConcept[] {};
			}
    		return result;
    	}
    	return new VConcept[] {};
    }

    @Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] _tags = VConcept.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[_tags.length + 1];
		for (int i = 0; i < _tags.length; i++)
			tags[i] = _tags[i];
		tags[_tags.length] = new VInteger(VConcept.TYPE_EN_WIKIPEDIA);
		VEntity value = new VString(name);
		return new VTagged(value, tags);
	}

	@Override
	public String toString() {
		return "VEnWikipediaConcept[" + name + "]";
	}
}
