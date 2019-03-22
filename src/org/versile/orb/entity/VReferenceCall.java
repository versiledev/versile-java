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

import org.versile.orb.link.VLink;


/**
 * Pending remote call to a {@link VReference}.
 */
public abstract class VReferenceCall extends VObjectCall {

	/**
	 * Link which handles call execution.
	 */
	protected VLink link;

	/**
	 * Call identifier on the handling link.
	 */
	protected Number call_id;

	/**
	 * Set up call with associated link call parameters.
	 *
	 * @param link owning link
	 * @param call_id call ID on link
	 */
	public VReferenceCall(VLink link, Number call_id) {
		this.link = link;
		this.call_id = call_id;
	}

	@Override
	protected void finalize() throws Throwable {
		this.unregister();
	}

	@Override
	protected void _cancel() {
		this.unregister();
	}

	@Override
	protected void _pushCleanup() {
		this.unregister();
	}

	/**
	 * Unregisters the call from its owning link.
	 */
	protected abstract void unregister();
}
