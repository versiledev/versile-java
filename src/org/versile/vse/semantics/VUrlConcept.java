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
import org.versile.orb.entity.VTuple;


/**
 * A VSE reference to a URL defined concept.
 */
public class VUrlConcept extends VConcept {

	// URL domain
	String domain;

	// URL postfix
	String postfix;

	/**
	 * Instantiate concept.
	 *
	 * <p>The associated URL is the string concatenation of 'domain' and 'postfix'.
	 * Domain must contain "://" and end with "/". It is assumed that the
	 * concept being referred to is uniquely identified by 'postfix' within the
	 * name space of 'domain'.</p>
	 *
	 * @param domain URL domain (i.e. prefix)
	 * @param postfix URL prefix
	 * @throws Exception illegal domain value
	 */
	public VUrlConcept(String domain, String postfix)
		throws Exception {
		if (!(domain.contains("://") && domain.endsWith("/")))
				throw new Exception("Illegal domain value");
		this.domain = domain;
		this.postfix = postfix;
	}

	@Override
	public int conceptType() {
		return VConcept.TYPE_URL;
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] _tags = VConcept.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[_tags.length + 1];
		for (int i = 0; i < _tags.length; i++)
			tags[i] = _tags[i];
		tags[_tags.length] = new VInteger(VConcept.TYPE_URL);
		VEntity value = new VTuple(new VEntity[] {new VString(domain), new VString(postfix)});
		return new VTagged(value, tags);
	}

	@Override
	public String toString() {
		return "VUrlConcept[\"" + domain+ "\" + \"" + postfix + "\"]";
	}
}
