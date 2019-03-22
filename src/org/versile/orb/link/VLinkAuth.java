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

import java.security.cert.CertPath;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.x500.X500Principal;

import org.versile.common.auth.VAuth;
import org.versile.common.auth.VCredentials;
import org.versile.orb.entity.VCallContext;


/**
 * Authorizer for setting up a link.
 *
 * <p>Can be registered with a link to approve link peer credentials during link handshake. Default
 * performs authorization by calling {@link org.versile.common.auth.VAuth#acceptPeer(org.versile.common.peer.VPeer)}
 * and {@link org.versile.common.auth.VAuth#acceptCredentials(VCredentials)} on a registered
 * {@link org.versile.common.auth.VAuth}, however derived classes can overload {@link #authorize(VLink)}
 * to change this behavior.</p>
 */
public class VLinkAuth {
	/**
	 * Authorizer used for performing {@link #authorize(VLink)} by base class.
	 */
	protected VAuth auth;

	/**
	 * Set up with a default authorizer.
	 *
	 *  <p>Sets up with a default {@link org.versile.common.auth.VAuth} which approves any peer
	 *  or credentials.</p>
	 */
	public VLinkAuth() {
		auth = null;
	}

	/**
	 * Set up with provided authorizer.
	 *
	 * @param auth authorizer invoked by {@link #authorize(VLink)}.
	 */
	public VLinkAuth(VAuth auth) {
		this.auth = auth;
	}

	/**
	 * Requests authorization for a link.
	 *
	 * <p>If false is returned the link is not allowed to continue
	 * communication with its peer and must shut down.</p>
	 *
	 * <p>The default implementations inspects the link's configuration object for a
	 * {@link org.versile.common.auth.VAuth} authorizer, and if not null checks whether
	 * peer and credentials are accepted by that authorizer. Derived classes may
	 * overload this behavior.</p>
	 *
	 * @param link link to authorize
	 * @return true if authorized
	 */
	public boolean authorize(VLink link) {
		if (auth == null)
			return true;
		else {
			VCallContext ctx = link.context;
			if (!auth.acceptPeer(ctx.getPeer()))
				return false;

			RSAPublicKey key = ctx.getPublicKey();
			X500Principal identity = ctx.getClaimedIdentity();
			CertPath certificates = ctx.getCertificates();
			VCredentials credentials = new VCredentials(key, identity, certificates);
			if (!auth.acceptCredentials(credentials))
				return false;

			return true;
		}
	}
}
