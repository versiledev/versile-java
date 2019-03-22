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

import org.versile.orb.entity.VReferenceCall;


/**
 * References a pending remote call to a {@link VLinkReference}.
 */
public class VLinkReferenceCall extends VReferenceCall {

	public VLinkReferenceCall(VLink link, Number call_id) {
		super(link, call_id);
	}

	protected void unregister() {
		if (link != null) {
			link.unregisterRemoteCall(call_id);
			link = null;
		}
	}
}
