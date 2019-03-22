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

package org.versile.reactor.io.vudp;

import org.versile.common.processor.VProcessor;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.link.VLinkAgentConfig;
import org.versile.reactor.io.vec.VEntityChannelConfig;
import org.versile.reactor.io.vts.VSecureChannelConfig;


/**
 * Configuration data for a {@link VUDPRelayedVOPConnecter}.
 *
 * <p>Default set up with VTS enabled, TLS disabled (not yet supported by Versile Java), and insecure
 * (plaintext) channels disabled.</p>
 */
public class VUDPRelayedVOPConfig {

	// When adding fields remember to update copyTo()
	VLinkAgentConfig linkConfig;
	VProcessor processor = null;
	VReactor reactor = null;
	boolean enableVts = true;
	boolean enableTls = false;
	boolean allowInsecure = false;
	VUDPHostFilter udpFilter = null;
	VSecureChannelConfig vtsConfig;
	VEntityChannelConfig vecConfig;

	public VUDPRelayedVOPConfig() {
		linkConfig = new VLinkAgentConfig();
		vtsConfig = new VSecureChannelConfig();
		vecConfig = new VEntityChannelConfig();
	}

	@Override
	public VUDPRelayedVOPConfig clone() {
		VUDPRelayedVOPConfig result = new VUDPRelayedVOPConfig();
		this.copyTo(result);
		return result;
	}

	public VLinkAgentConfig getLinkConfig() {
		return linkConfig;
	}

	public void setLinkConfig(VLinkAgentConfig linkConfig) {
		this.linkConfig = linkConfig;
	}

	public VProcessor getProcessor() {
		return processor;
	}

	public void setProcessor(VProcessor processor) {
		this.processor = processor;
	}

	public VReactor getReactor() {
		return reactor;
	}

	public void setReactor(VReactor reactor) {
		this.reactor = reactor;
	}

	public boolean isEnableVts() {
		return enableVts;
	}

	public void setEnableVts(boolean enableVts) {
		this.enableVts = enableVts;
	}

	public boolean isEnableTls() {
		return enableTls;
	}

	public void setEnableTls(boolean enableTls) {
		this.enableTls = enableTls;
	}

	public boolean isAllowInsecure() {
		return allowInsecure;
	}

	public void setAllowInsecure(boolean allowInsecure) {
		this.allowInsecure = allowInsecure;
	}

	public VUDPHostFilter getUdpFilter() {
		return udpFilter;
	}

	public void setUdpFilter(VUDPHostFilter udpFilter) {
		this.udpFilter = udpFilter;
	}

	public VSecureChannelConfig getVtsConfig() {
		return vtsConfig;
	}

	public void setVtsConfig(VSecureChannelConfig vtsConfig) {
		this.vtsConfig = vtsConfig;
	}

	public VEntityChannelConfig getVecConfig() {
		return vecConfig;
	}

	public void setVecConfig(VEntityChannelConfig vecConfig) {
		this.vecConfig = vecConfig;
	}

	protected void copyTo(VUDPRelayedVOPConfig config) {
		config.linkConfig = linkConfig.clone();
		config.processor = processor;
		config.reactor = reactor;
		config.enableVts = enableVts;
		config.enableTls = enableTls;
		config.allowInsecure = allowInsecure;
		config.udpFilter = udpFilter;
		config.vtsConfig = vtsConfig.clone();
		config.vecConfig = vecConfig.clone();
	}
}
