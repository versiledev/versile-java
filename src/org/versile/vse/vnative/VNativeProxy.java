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

import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VReference;
import org.versile.orb.entity.VTagged;


/**
 * Proxy interface to a local or remote native object.
 *
 * <p>A {@link VNativeProxy} should provide mechanisms for accessing a (remotely
 * referenced) native object. This base class does not provide any such
 * mechanisms, as the appropriate mechanisms are specific to the particular
 * native type. However, the remote native-object reference can always be
 * retreived via {@link #_v_native_obj()}.</p>
 *
 * <p>The proxy initially starts in a 'passive' state and should only provide
 * remote-object interfaces once it has been 'activated'. Activation is
 * performed by calling {@link #_v_activate()}. This is a security measure in
 * order to ensure provided native-object interfaces are not unintentionally
 * accessed.</p>
 *
 * <p>When the object is in an 'active' state, it is permitted to instantiate other
 * {@link VNativeProxy} objects and set them to be 'active' (this is the typical
 * pattern). Otherwise, instantiated objects may not be 'active'. Also, when the
 * object is 'active' it is allowed to convert return values from remote
 * operations to native types.</p>
 */
public class VNativeProxy extends VEntity {

	/**
	 * Reference to implementing native object.
	 */
	protected VProxy _v_proxy;
	/**
	 * Native object tag for the associated native type.
	 */
	protected VEntity _v_tag;
	/**
	 * If true native object proxy has been activated.
	 */
	protected boolean _v_active = false;

	/**
	 * Set up native object proxy.
	 *
	 * @param proxy proxy to remote-object implementing native object interface
	 * @param tag native-object type tag
	 * @throws VEntityError invalid or mismatching parameters
	 */
	public VNativeProxy(VProxy proxy, VEntity tag)
			throws VEntityError {
		VObject obj = proxy.get();
		if (!(obj instanceof VReference)) {
			if (obj instanceof VNativeObject) {
				if (!((VNativeObject)obj)._v_native_tag().equals(tag))
					throw new VEntityError("Native type tag does not match implementing class");
			}
			else
				throw new VEntityError("Not a VReference or VNativeObject");
		}

		this._v_proxy = proxy;
		this._v_tag = tag;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VNativeObject.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[code_tags.length+1];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		tags[code_tags.length] = this._v_tag;
		return new VTagged(_v_proxy.get()._v_fake_object(), tags);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	/**
	 * Activates the native object proxy, enabling all proxy mechanisms.
	 */
	public void _v_activate() {
		this._v_active = true;
	}

	/**
	 * Get a reference the peer object which provides native object services.
	 *
	 * @return referenced peer
	 */
	public VProxy _v_native_obj() {
		return _v_proxy;
	}
}
