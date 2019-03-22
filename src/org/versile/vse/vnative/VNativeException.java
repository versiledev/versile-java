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

import org.versile.common.util.VExceptionProxy;
import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTuple;
import org.versile.vse.VSECode;


/**
 * Exception raised by a remote native-type object.
 *
 * <p>The {@link VNativeException} mechanism allows raising exceptions
 * which can be identified as native exceptions, and native types may
 * define standards for how the exception is encoded and interpreted.</p>
 *
 * <p>Similar to {@link VNativeProxy} a native exception must be 'activated'
 * before it is allowed to enable any native-handling interfaces, or
 * activate remote objects provided via this object.</p>
 */
public class VNativeException extends VEntity {

	/**
	 * VSE code for the VNativeException type.
	 */
	public static VSECode VSE_CODE = new VSECode(new String[] {"native", "exception"},
							                     new VInteger[] {new VInteger(0), new VInteger(8)},
							                     new VObjectIdentifier(3, 2));

	/**
	 * Native exception's residual tag.
	 */
	protected VEntity _v_tag;
	/**
	 * Native exception's held value.
	 */
	protected VTuple _v_args;
	/**
	 * True if activated.
	 */
	protected boolean _v_active = false;

	/**
	 * Set up native exception.
	 *
	 * @param tag native-type tag
	 * @param args exception arguments
	 */
	public VNativeException(VEntity tag, VEntity... args) {
		_v_tag = tag;
		_v_args = new VTuple(args);
	}

	/**
	 * Returns a proxy to the {@link VNativeException}.
	 *
	 * @return proxy exception
	 */
	public VExceptionProxy getProxy() {
		return new VExceptionProxy(this);
	}

	/**
	 * Activates the native exception, enabling all proxy mechanisms.
	 */
	public void _v_activate() {
		_v_active = true;
	}

	/**
	 * Get the exception's arguments.
	 *
	 * @return values held on exception
	 */
	public VTuple getArgs() {
		return _v_args;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VNativeException.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[code_tags.length+1];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		tags[code_tags.length] = this._v_tag;
		return new VTagged(_v_args, tags);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}
}
