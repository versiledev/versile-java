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
 * Versile Standard Entities.
 *
 * <p>The VSE framework provides a standard set of higher-level data types which
 * are available to any implementation of Versile Platform which fully implements
 * the platform specifications.</p>
 *
 * <p>In order to use VSE types with a Versile ORB Link, the VSE types must be
 * enabled as a set of resolving {@link org.versile.orb.module.VModule} modules for
 * the associated data types, which are registered with the link's VER parser.</p>
 *
 * <p>The default behavior of a link is to enable all modules which have been
 * globally registered with {@link org.versile.orb.module.VModuleResolver#addImport},
 * and the typical pattern for enabling VSE types on a link is to add all VSE types
 * as global imports before the link is negotiated.</p>
 *
 * <p>Below is an example which enables VSE types on a link, allowing the link to
 * handle the associated types. Note how due to lazy-conversion on the link, the
 * returned representation of the object is actually a lazy-converted native
 * representation of the VSE type, i.e. though sending as a
 * {@link org.versile.vse.container.VFrozenSet} it is returned as a HashSet.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.demo.Echoer;
 * import org.versile.orb.entity.VEntity;
 * import org.versile.orb.entity.VProxy;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 * import org.versile.vse.VSEResolver;
 * import org.versile.vse.container.VFrozenSet;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         VLink link = null;
 *         try {
 *             // Activate VSE types for new instantiated links
 *             VSEResolver.addToGlobalModules();
 *
 *             // Test passing a VSE type in a remote method call
 *             link = VLinkAgent.createVirtualServerLink(new Echoer());
 *             VProxy echoer = link.peerGateway();
 *             VEntity[] data = new VEntity[] {VEntity._v_lazy(4), VEntity._v_lazy(-100)};
 *             VFrozenSet send_arg = new VFrozenSet(data);
 *             Object ret_val = echoer.call("echo", send_arg);
 *             System.out.println("Return value  : " + ret_val);
 *             System.out.println("Returned class: " + ret_val.getClass());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         } finally {
 *             // Terminate link and shut down service
 *             if (link != null)
 *                 link.shutdown(true);
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Return value  : [4, -100]
 * Returned class: class java.util.HashSet</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Because VSE recognizes a HashSet<?> as a type which can be lazy-converted to a VSE type,
 * the example could also pass a HashSet instead of constructing a frozen set. Note the set's elements
 * must also be lazy-convertible. Below is a rewritten version which uses only native types.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.util.HashSet;
 *
 * import org.versile.Versile;
 * import org.versile.demo.Echoer;
 * import org.versile.orb.entity.VProxy;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 * import org.versile.vse.VSEResolver;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         VLink link = null;
 *         try {
 *             // Activate VSE types for new instantiated links
 *             VSEResolver.addToGlobalModules();
 *
 *             // Test passing a VSE type in a remote method call
 *             link = VLinkAgent.createVirtualServerLink(new Echoer());
 *             VProxy echoer = link.peerGateway();
 *             HashSet{@literal <}Object{@literal >} send_arg = new HashSet{@literal <}Object{@literal >}();
 *             send_arg.add(4);
 *             send_arg.add(-100);
 *             Object ret_val = echoer.call("echo", send_arg);
 *             System.out.println("Return value  : " + ret_val);
 *             System.out.println("Returned class: " + ret_val.getClass());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         } finally {
 *             // Terminate link and shut down service
 *             if (link != null)
 *                 link.shutdown(true);
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Return value  : [4, -100]
 * Returned class: class java.util.HashSet</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.vse;
