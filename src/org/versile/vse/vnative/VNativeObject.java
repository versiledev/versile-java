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

package org.versile.vse.vnative;

import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.external.VExternal;
import org.versile.vse.VSECode;


/**
 * VSE native access to a local object.
 *
 * <p>A reference to this object is typically included in the object's Versile
 * Entity Rerpresentation encoding, and it is the gateway for accessing or
 * performing operations on the local object.</p>
 *
 * <p>The external interface is individual to each native type, and this base class
 * does not impose any particular interface.</p>
 *
 * <p>The object must have a 'native tag' defined which specifies the native type
 * (interface) implemented by this class. Tag values prefixed with 'vse-' are
 * reserved for use by the VSE standard. Derived classes must implement
 * {@link #_v_native_tag} so it provides the correct tag value.</p>
 *
 * <p>When decoding a {@link VNativeObject} representation, it should be resolved
 * as a {@link VNativeProxy} which provides a proxy-type interface for accessing the
 * native object. Derived classes must implement {@link #_v_native_ref} which
 * should instantiate the appropriave sub-class of {@link VNativeProxy}.</p>
 */
public abstract class VNativeObject extends VExternal {

	/**
	 * VSE code for the VNative type.
	 */
	public static VSECode VSE_CODE = new VSECode(new String[] {"native", "object"},
							                     new VInteger[] {new VInteger(0), new VInteger(8)},
							                     new VObjectIdentifier(3, 1));

	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VNativeObject.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[code_tags.length+1];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		tags[code_tags.length] = this._v_native_tag();
		return new VTagged(this._v_fake_object(), tags);
	}

	/**
	 * Get a native proxy alias to this object.
	 *
	 * @return native proxy alias
	 */
	public abstract VNativeProxy _v_native_ref();

	/**
	 * Get the tag name for this particular native type.
	 *
	 * @return native tag name
	 */
	protected abstract VEntity _v_native_tag();
}
