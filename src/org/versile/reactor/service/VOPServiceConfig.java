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

import org.versile.reactor.io.link.VLinkAgentConfig;
import org.versile.reactor.io.vec.VEntityChannelConfig;
import org.versile.reactor.io.vts.VSecureChannelConfig;


/**
 * Configuration parameters for reactor-based VOP service.
 *
 * <ul>
 *
 *   <li><i>vecConfig</i> holds configuration parameters for entity
 *   serialization channels for new client connections. The
 *   default value is a default VEC configuration object.</li>
 *
 *   <li><i>linkConfig</i> holds configuration parameters for the
 *   connection's link end point. The default value is a default link
 *   configuration object.</li>
 *
 *   <li><i>vtsEnabled</i> if true means VTS transport is allowed for a VOP connection. The
 *   default is true.</li>
 *
 *   <li><i>vtsConfig</i> holds configuration parameters for a VTS
 *   transport if enabled and negotiated for a VOP connection. The
 *   default value is a default constructed configuration object.</li>
 *
 *   <li><i>insecureEnabled</i> if true means insecure plaintext transport is allowed for
 *   VOP connections. The default is false (only secure connections allowed).</li>
 *
 * </ul>
 */
public class VOPServiceConfig extends VReactorServiceConfig {

	// When adding fields remember to update copyTo()
	VEntityChannelConfig vecConfig;
	VLinkAgentConfig linkConfig;
	boolean vtsEnabled = true;
	VSecureChannelConfig vtsConfig;
	boolean insecureEnabled = false;

	public VOPServiceConfig() {
		vecConfig = new VEntityChannelConfig();
		linkConfig = new VLinkAgentConfig();
		vtsConfig = new VSecureChannelConfig();
	}

	@Override
	public VOPServiceConfig clone() {
		VOPServiceConfig result = new VOPServiceConfig();
		this.copyTo(result);
		return result;
	}

	public VEntityChannelConfig getVecConfig() {
		return vecConfig;
	}

	public void setVecConfig(VEntityChannelConfig vecConfig) {
		this.vecConfig = vecConfig;
	}

	public VLinkAgentConfig getLinkConfig() {
		return linkConfig;
	}

	public void setLinkConfig(VLinkAgentConfig linkConfig) {
		this.linkConfig = linkConfig;
	}

	public boolean isVtsEnabled() {
		return vtsEnabled;
	}

	public void setVtsEnabled(boolean vtsEnabled) {
		this.vtsEnabled = vtsEnabled;
	}

	public VSecureChannelConfig getVtsConfig() {
		return vtsConfig;
	}

	public void setVtsConfig(VSecureChannelConfig vtsConfig) {
		this.vtsConfig = vtsConfig;
	}

	public boolean isInsecureEnabled() {
		return insecureEnabled;
	}

	public void setInsecureEnabled(boolean insecureEnabled) {
		this.insecureEnabled = insecureEnabled;
	}

	protected void copyTo(VOPServiceConfig config) {
		super.copyTo(config);
		if (vecConfig != null)
			config.vecConfig = vecConfig.clone();
		else
			config.vecConfig = null;
		if (linkConfig != null)
			config.linkConfig = linkConfig.clone();
		else
			config.linkConfig = null;
		if (vtsConfig != null)
			config.vtsConfig = vtsConfig.clone();
		else
			config.vtsConfig = null;

		config.vtsEnabled = vtsEnabled;
		config.insecureEnabled = insecureEnabled;
	}
}
