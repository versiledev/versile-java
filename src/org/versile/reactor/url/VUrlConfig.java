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

package org.versile.reactor.url;

import org.versile.common.processor.VProcessor;
import org.versile.orb.entity.VObject;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.link.VLinkAgentConfig;
import org.versile.reactor.io.sock.VClientSocketConfig;
import org.versile.reactor.io.vec.VEntityChannelConfig;
import org.versile.reactor.io.vts.VSecureChannelConfig;


/**
 * Configuration data for a {@link org.versile.reactor.url.VUrl}.
 *
 * <p>'linkConfig' is configuration parameters for the link agent of
 * a resolved link. The default value is the default constructed config object.</p>
 *
 * <p>'vecConfig' is configuration parameters for the VEC entity serializer of
 * a resolved link. The default value is the default constructed config object.</p>
 *
 * <p>'vtsConfig' is configuration parameters for the negotiated VTS transport
 * of a VOP connection. The default value is the default constructed config object.</p>
 *
 * <p>'socketConfig' is configuration parameters for the socket to a
 * a resolved link. The default value is the default constructed config object.</p>
 *
 * <p>'vtsEnabled' if true means VTS transport is allowed for a VOP connection. The
 * default is true.</p>
 *
 * <p>'insecureEnabled' if true means insecure plaintext transport is allowed for
 * VOP connections. The default is false (only secure connections allowed).</p>
 *
 * <p>'localGateway' is not null is a local gateway object provided to the link peer
 * during link handshake.</p>
 *
 * <p>'reactor' if not null means the specified reactor is used for establishing the
 * link (which also means the caller takes ownership of the reactor and is responsible
 * for reactor start and shutdown). Otherwise a reactor is lazy-created for the link
 * (the link takes ownership of the reactor).</p>
 *
 * <p>'processor' if not null means the specified processor is used for establishing the
 * link (which also means the caller takes ownership of the processor and is responsible
 * for its start and shutdown). Otherwise a processor is lazy-created for the link
 * (the link takes ownership of the processor).</p>
 */
public class VUrlConfig {

	// When adding fields remember to update copyTo()
	VLinkAgentConfig linkConfig;
	VEntityChannelConfig vecConfig;
	VSecureChannelConfig vtsConfig;
	VClientSocketConfig socketConfig;

	boolean vtsEnabled = true;
	boolean insecureEnabled = false;
	VObject localGateway = null;
	VReactor reactor = null;
	VProcessor processor = null;

	public VUrlConfig() {
		linkConfig = new VLinkAgentConfig();
		vecConfig = new VEntityChannelConfig();
		vtsConfig = new VSecureChannelConfig();
		socketConfig = new VClientSocketConfig();
	}

	@Override
	public VUrlConfig clone() {
		VUrlConfig result = new VUrlConfig();
		this.copyTo(result);
		return result;
	}

	public VLinkAgentConfig getLinkConfig() {
		return linkConfig;
	}

	public void setLinkConfig(VLinkAgentConfig linkConfig) {
		this.linkConfig = linkConfig;
	}

	public VEntityChannelConfig getVecConfig() {
		return vecConfig;
	}

	public void setVecConfig(VEntityChannelConfig vecConfig) {
		this.vecConfig = vecConfig;
	}

	public VSecureChannelConfig getVtsConfig() {
		return vtsConfig;
	}

	public void setVtsConfig(VSecureChannelConfig vtsConfig) {
		this.vtsConfig = vtsConfig;
	}

	public VClientSocketConfig getSocketConfig() {
		return socketConfig;
	}

	public void setSocketConfig(VClientSocketConfig socketConfig) {
		this.socketConfig = socketConfig;
	}


	public boolean isVtsEnabled() {
		return vtsEnabled;
	}

	public void setVtsEnabled(boolean vtsEnabled) {
		this.vtsEnabled = vtsEnabled;
	}

	public boolean isInsecureEnabled() {
		return insecureEnabled;
	}

	public void setInsecureEnabled(boolean insecureEnabled) {
		this.insecureEnabled = insecureEnabled;
	}

	public VObject getLocalGateway() {
		return localGateway;
	}

	public void setLocalGateway(VObject gw) {
		localGateway = gw;
	}

	public VReactor getReactor() {
		return reactor;
	}

	public void setReactor(VReactor reactor) {
		this.reactor = reactor;
	}

	public VProcessor getProcessor() {
		return processor;
	}

	public void setProcessor(VProcessor processor) {
		this.processor = processor;
	}

	protected void copyTo(VUrlConfig config) {
		if (linkConfig != null)
			config.linkConfig = linkConfig.clone();
		else
			config.linkConfig = null;

		if (vecConfig != null)
			config.vecConfig = vecConfig.clone();
		else
			config.vecConfig = null;

		if (vtsConfig != null)
			config.vtsConfig = vtsConfig.clone();
		else
			config.vtsConfig = null;

		if (socketConfig != null)
			config.socketConfig = socketConfig.clone();
		else
			config.socketConfig = null;

		config.vtsEnabled = vtsEnabled;
		config.insecureEnabled = insecureEnabled;
		config.localGateway = localGateway;
		config.reactor = reactor;
		config.processor = processor;
	}
}
