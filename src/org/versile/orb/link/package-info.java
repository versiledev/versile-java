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
 * Base classes for Versile ORB Link (VOL).
 *
 * <p>A VOL link is a serialized protocol for remote-object interaction.
 * It is normally accessed via the Versile Object Protocol (VOP) which
 * negotiates a byte transport for a link connection. Versile Java
 * includes a reactor-based implementation in {@link org.versile.reactor.io.link}.</p>
 *
 * <p>Note that global license configuration must be set before a link can be constructed.
 * See {@link org.versile.Versile} for details.<p>
 *
 * <p>The simplest type of link that can be set up is a local link
 * between two local link end-points, illustrated in the below example.
 * Note the link should be shut down when completed (this may
 * also some times happen automatically via garbage collection, however
 * this is not a reliable mechanism) by calling one of the link shutdown
 * methods. A link will normally use several threads for its operation
 * (processor and reactor) which may otherwise be left running.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.demo.Echoer;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         try {
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new Echoer()).peerGateway();
 *             System.out.println("Remote call result: " + gw.call("echo", "just some string"));
 *             VLink.shutdownForProxy(gw, true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Remote call result: just some string</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Links are typically established over a socket connection between a connecting 'client'
 * and a listening 'server', using the VOP protocol to negotiate the connection. For information
 * about URL-based client links see {@link org.versile.orb.url}, and for listening services
 * see {@link org.versile.orb.service}.</p>
 *
 */
package org.versile.orb.link;
