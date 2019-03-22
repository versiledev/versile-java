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
 * Base classes for resolving Versile Resource Identifiers.
 *
 * <p>Base functionality for VRI (URL) based connection to Versile Object Protocol
 * resources. Versile Java includes a reactor-based implementation in
 * {@link org.versile.reactor.url}.</p>
 *
 * <p>See {@link org.versile.orb.service} for an example how to access a remote service
 * (because the example needs to include a service which the client can connect to).</p>
 *
 * <p>Note that global license configuration must be set before a link can be constructed.
 * See {@link org.versile.Versile} for details.<p>
 *
 * <p>Below is an example of resolving a relative VRI path. E.g. the VRI
 * <tt>'vop://example.com/some/path/'</tt> could be split into an absolute and relative
 * path component as <tt>('vop://example.com/', '/some/path/')</tt> or
 * <tt>('vop://example.com/some/', '/path/')</tt>. The example uses a remote
 * reference to a {@link org.versile.demo.SimpleGateway} and VRI resolution mechanisms to
 * resolve a resource defined by a relative path.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.demo.SimpleGateway;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 * import org.versile.reactor.url.VUrl;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         try {
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new SimpleGateway()).peerGateway();
 *             VProxy echoer = (VProxy) VUrl.relative(gw, "/text/echo/");
 *             System.out.println("Remote call result: " + echoer.call("echo", "relative resource"));
 *             VLink.shutdownForProxy(gw, true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Remote call result: relative resource</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>VRIs can also include a query component, which resolves as a method call on the resource at the VRI path's
 * end-point, with provided arguments and possibly named arguments. Below is an example of accessing the
 * gateway echoer's echo method directly via a query, and how to build the query string.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.demo.SimpleGateway;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.orb.url.VUrlBase;
 * import org.versile.reactor.io.link.VLinkAgent;
 * import org.versile.reactor.url.VUrl;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         try {
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new SimpleGateway()).peerGateway();
 *             String relUrl = "/text/echo/";
 *             relUrl += VUrlBase.createQuery("echo", new Object[] {"Some nice text here"}, null);
 *             System.out.println("Resolving relative path : " + relUrl);
 *             System.out.println("Resolved resource       : " + VUrl.relative(gw, relUrl));
 *             VLink.shutdownForProxy(gw, true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Resolving relative path : /text/echo/echo&Some+nice+text+here
 * Resolved resource       : Some nice text here</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is an example of building a complete absolute VRI which includes a query.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.util.Hashtable;
 *
 * import org.versile.orb.url.VUrlBase;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         try {
 *             Object[] q_args = new Object[] {"test_img.png", 640, 480};
 *             Hashtable{@literal <}String, Object{@literal >} q_named_args = new Hashtable{@literal <}String, Object{@literal >}();
 *             q_named_args.put("Quality", 80);
 *             String vri = VUrlBase.createVRI("vop", "example.com", -1,
 *                     new String[] {"images", "hi-res"},
 *                     "load", q_args, q_named_args);
 *             System.out.println("Generated VRI:\n" + vri);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Generated VRI:
 * vop://example.com/images/hi-res/load&test_img.png&int%3A640&int%3A480&Quality=int%3A80</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.orb.url;
