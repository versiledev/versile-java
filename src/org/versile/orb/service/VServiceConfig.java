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

package org.versile.orb.service;

import org.versile.common.auth.VAuth;


/**
 * Configuration parameters for listening service.
 *
 * <p>'lazyProcessorThreads' is the number of threads to set up for a lazy-created
 * service processor. Default is 5.</p>
 *
 * <p>'transportAuthorizer' is an authorizer for the communication channel with a
 * link peer of an instantiated link. Default is null.</p>
 */
public class VServiceConfig {

	// When adding fields remember to update copyTo()
	int lazyProcessorWorkers = 5;
	VAuth transportAuthorizer = null;

	@Override
	public VServiceConfig clone() {
		VServiceConfig result = new VServiceConfig();
		this.copyTo(result);
		return result;
	}

	public int getLazyProcessorWorkers() {
		return lazyProcessorWorkers;
	}

	public void setLazyProcessorWorkers(int lazyProcessorWorkers) {
		this.lazyProcessorWorkers = lazyProcessorWorkers;
	}

	public VAuth getTransportAuthorizer() {
		return transportAuthorizer;
	}

	public void setTransportAuthorizer(VAuth transportAuthorizer) {
		this.transportAuthorizer = transportAuthorizer;
	}

	protected void copyTo(VServiceConfig config) {
		config.lazyProcessorWorkers = lazyProcessorWorkers;
		config.transportAuthorizer = transportAuthorizer;
	}
}
