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

package org.versile.vse.util;

import org.versile.orb.entity.VCallContext;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VTagged;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;


/**
 * Handler for user/password authentication.
 */
public abstract class VPasswordLoginHandler extends VExternal {

	/**
	 * External method for performing a login operation.
	 *
	 * @param ctx call context
	 * @param username username
	 * @param password user's password
	 * @return resource granted by successful login, or null/VNone
	 */
	@Publish(show=true, ctx=true)
	public final Object login(VCallContext ctx, String username, String password) {
		VPasswordLogin.LoginResult result = this.handleLogin(username,  password, ctx);
		return new Object[] {result.isAuthorized(), result.getResource()};
	}

	/**
	 * Implements login operation.
	 *
	 * <p>Called internally by {@link #login(VCallContext, String, String)} to perform
	 * a login operation.</p>
	 *
	 * @param username username
	 * @param password user's password
	 * @param ctx call context
	 * @return login result and authorized resource
	 */
	protected abstract VPasswordLogin.LoginResult handleLogin(String username, String password, VCallContext ctx);

	/**
	 * Get a Versile Entity Representation of the associated password login type.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VPasswordLogin.VSE_CODE.getTags(ctx);
		return new VTagged(this._v_fake_object(), tags);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}
}
