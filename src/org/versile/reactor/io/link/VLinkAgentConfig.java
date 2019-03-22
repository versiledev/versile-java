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

package org.versile.reactor.io.link;

import java.util.logging.Logger;

import org.versile.orb.link.VLinkConfig;


/**
 * Configuration parameters for link agent.
 *
 * <p>In addition to {@link org.versile.orb.link.VLinkConfig} parameters,
 * holds some reactor specific parameters.</p>
 *
 * <p>'queueLength' is the maximum number of link messages to buffer
 * and the maximum number of output messages to pass per producer send
 * operation.</p>
 *
 * <p>'reactorLogger' is a logger for lazy-created reactors. Default
 * is null.</p>
 */
public class VLinkAgentConfig extends VLinkConfig {

	// When adding fields remember to update copyTo()
	int queueLength = 10;
	Logger reactorLogger = null;

	@Override
	public VLinkAgentConfig clone() {
		VLinkAgentConfig result = new VLinkAgentConfig();
		this.copyTo(result);
		return result;
	}

	public int getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}

	public Logger getReactorLogger() {
		return reactorLogger;
	}

	public void setReactorLogger(Logger reactorLogger) {
		this.reactorLogger = reactorLogger;
	}

	protected void copyTo(VLinkAgentConfig config) {
		super.copyTo(config);
		config.queueLength = queueLength;
		config.reactorLogger = reactorLogger;
	}
}
