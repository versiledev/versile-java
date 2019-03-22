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


/**
 * Authorization of communication peers.
 *
 * <p>Provides {@link org.versile.common.auth.VCredentials} and
 * {@link org.versile.common.auth.VPrivateCredentials} as mechanisms for providing
 * sets of credentials for an identity, which can combine RSA key information,
 * certificiate path certfifying a key, or a claimed identity. This allows
 * such information to be conveniently passed together in Versile Java APIs.</p>
 *
 * <p>Establishing communication with a peer typically involves making a decision
 * whether the connection is authorized to take place over the particular communication
 * channel, and with the communication partner in the other end of the communication
 * channel. Reactor producer/consumer mechanisms such as VTS channels can be configured
 * with a {@link org.versile.common.auth.VAuth} authorizer which performs a separate layer
 * of abstraction for performing such authorization.</p>
 *
 * <p>The {@link org.versile.common.peer.VPeer} class represents communication peers, which
 * are implemented in derived classes for different channels such as sockets. The
 * {@link org.versile.common.auth.VAuth#acceptPeer} method performs an evaluation whether
 * a peer is approved, e.g. based on IP address filtering.</p>
 *
 * <p>The {@link org.versile.common.auth.VAuth#acceptCredentials} method inspects a set of
 * credentials for the communication partner, and evaluates whether credentials are
 * authorized for the operation in question. In the case of VTS, a public key included
 * with credentials is confirmed at the VTS handshake level (meaning the communication
 * partner has verified access to the corresponding private key) and can be used as
 * a basis for authorization and "login" for higher-level protocols.</p>
 *
 * <p>Links use a separate higher-level mechanism {@link org.versile.orb.link.VLinkAuth} for
 * handling link authentication, however they can be configured to handle authorization
 * by redirecting to a {@link org.versile.common.auth.VAuth}.</p>
 */
package org.versile.common.auth;
