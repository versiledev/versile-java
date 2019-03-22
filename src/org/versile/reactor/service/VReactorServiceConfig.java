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

package org.versile.reactor.service;

import java.util.logging.Logger;

import org.versile.common.auth.VAuth;
import org.versile.orb.service.VServiceConfig;
import org.versile.reactor.io.sock.VClientSocketConfig;



/**
 * Configuration parameters for reactor-based services.
 *
 * <ul>
 *
 *   <li><i>clientSocketConfig</i> holds configuration data for the
 *   reactor service's client socket end point. Default holds a
 *   config object created with the default constructor.</li>
 *
 *   <li><i>peerAuthorizer</i> is an authorization that should be performed for
 *   a service client socket peer (peer only, not credentials). The
 *   default value is a default VAuth object.</li>
 *
 *   <li><i>reactorLogger</i> is a logger for lazy-created reactors. Default
 *   is null.</li>
 *
 *   <li><i>reuseAddress</i> specifies whether a lazy-created server socket
 *   should be set up with the reuse address property. Default is true.</li>
 *
 * </ul>
 */
public class VReactorServiceConfig extends VServiceConfig {

	// When adding fields remember to update copyTo()
	VClientSocketConfig socketConfig;
	VAuth peerAuthorizer;
	Logger reactorLogger = null;
	boolean reuseAddress = true;

	public VReactorServiceConfig() {
		socketConfig = new VClientSocketConfig();
		peerAuthorizer = new VAuth(false, false);
	}

	@Override
	public VReactorServiceConfig clone() {
		VReactorServiceConfig result = new VReactorServiceConfig();
		this.copyTo(result);
		return result;
	}

	public VClientSocketConfig getSocketConfig() {
		return socketConfig;
	}

	public void setSocketConfig(VClientSocketConfig socketConfig) {
		this.socketConfig = socketConfig;
	}

	public VAuth getPeerAuthorizer() {
		return peerAuthorizer;
	}

	public void setPeerAuthorizer(VAuth peerAuthorizer) {
		this.peerAuthorizer = peerAuthorizer;
	}

	public Logger getReactorLogger() {
		return reactorLogger;
	}

	public void setReactorLogger(Logger reactorLogger) {
		this.reactorLogger = reactorLogger;
	}

	public boolean reuseAddress() {
		return reuseAddress;
	}

	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

	protected void copyTo(VReactorServiceConfig config) {
		super.copyTo(config);
		if (socketConfig != null)
			config.socketConfig = socketConfig.clone();
		else
			config.socketConfig = null;
		config.peerAuthorizer = peerAuthorizer;
		config.reactorLogger = reactorLogger;
		config.reuseAddress = reuseAddress;
	}
}
