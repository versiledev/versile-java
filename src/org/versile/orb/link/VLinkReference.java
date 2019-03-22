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

package org.versile.orb.link;

import java.util.List;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallCancelled;
import org.versile.common.call.VCallException;
import org.versile.common.call.VCallOperationException;
import org.versile.orb.entity.VCallContext;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VReference;



/**
 * A {@link org.versile.orb.entity.VReference} which references a {@link VLink} peer object.
 */
public class VLinkReference extends VReference {
	public VLinkReference(VLink link, Number peer_id) {
		super(link, peer_id);
	}

	@Override
	public final Object _v_call(List<Object> args, _v_CallType type, VCallContext ctx)
			throws Exception {
		VCall<Object> call = this._v_call_nowait(args, type, ctx);
		try {
			call.waitResult();
		} catch (VCallCancelled e) {
			throw new VCallError();
		}
		try {
			return call.getResult();
		}
		catch (VCallException e) {
			throw e.getException();
		}
	}

	@Override
	public VCall<Object> _v_call_nowait(List<Object> args, _v_CallType type, VCallContext ctx) {
		try {
			if (((VLink)this.ctx).active)
				return ((VLink)this.ctx).sendRemoteCall(this, args, type);
			else {
				// If link not active cannot make call; throw exception to trigger below handling
				throw new VLinkException();
			}
		}
		catch (VLinkException e) {
			VCall<Object> result = new VCall<Object>();
			try {
				result.pushException(new VCallError());
			}
			catch (VCallOperationException e2) {
				// Should never happen
				throw new RuntimeException();
			}
			return result;
		}
	}

	/**
	 * Get the link holding the reference.
	 *
	 * @return owning link
	 */
	public VLink _v_link() {
		return (VLink)ctx;
	}
}
