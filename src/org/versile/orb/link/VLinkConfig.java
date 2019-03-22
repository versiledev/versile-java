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

import org.versile.orb.entity.VTaggedParser;
import org.versile.orb.module.VModuleResolver;


/**
 * Configuration data for a {@link VLink}.
 *
 * <p>'authorizer' holds an authorizer for post-handshake link activation.
 * The default value is null (no authorization).</p>
 *
 * <p>'parser' holds a parser for lazy-conversion between VEntity types and
 * native types. Default holds a module resolver with all globally loaded imports
 * (at the time the config object was generated).</p>
 *
 * <p>'initCallback' holds a callback function to be called when link handshake
 * has completed. The default value is 'null' (no callback). The value should be replaced
 * with 'null' on the config by the link object after it has been used, in order
 * to avoid holding a reference.</p>
 *
 * <p>'kepAlive' are link protocol keep-alive settings. Default value is a default
 * {@link VLinkKeepAlive} object.</p>
 *
 * <p>'holdPeer' is true if the link should hold a full reference to the peer's
 * gateway object after the handshake, default value is true. It can be set to false for
 * setting up links which are not interested in the peer's gateway object (e.g. a service),
 * in order to enable proper detection of zero remote references between peers (which will
 * never reach zero if the link holds a peer gateway reference which is never retrieved
 * from the link).</p>
 */
public class VLinkConfig {

	// When adding fields remember to update copyTo()
	VLinkAuth authorizer = null;
	VTaggedParser parser;
	VLinkCallback initCallback = null;
	VLinkKeepAlive keepAlive;
	boolean holdPeer = true;

	public VLinkConfig() {
		parser = new VModuleResolver(null, true);
		keepAlive = new VLinkKeepAlive();
	}

	@Override
	public VLinkConfig clone() {
		VLinkConfig result = new VLinkConfig();
		this.copyTo(result);
		return result;
	}

	public VLinkAuth getAuthorizer() {
		return authorizer;
	}

	public void setAuthorizer(VLinkAuth authorizer) {
		this.authorizer = authorizer;
	}

	public VTaggedParser getParser() {
		return parser;
	}

	public void setParser(VTaggedParser parser) {
		this.parser = parser;
	}

	public VLinkCallback getInitCallback() {
		return initCallback;
	}

	public void setInitCallback(VLinkCallback initCallback) {
		this.initCallback = initCallback;
	}

	public VLinkKeepAlive getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(VLinkKeepAlive keepAlive) {
		this.keepAlive = keepAlive;
	}

	public boolean getHoldPeer() {
		return holdPeer;
	}

	public void setHoldPeer(boolean holdPeer) {
		this.holdPeer = holdPeer;
	}

	protected void copyTo(VLinkConfig config) {
		config.authorizer = authorizer;
		config.parser = parser;
		config.initCallback = initCallback;
		config.holdPeer = holdPeer;
		config.keepAlive = keepAlive.clone();
	}
}
