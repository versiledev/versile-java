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
 * Remote-object classes implementing Versile Object Behavior.
 *
 * <p>{@link org.versile.orb.external.VExternal} is a base class for objects implementing VOB, which provides
 * convenient use of annotations for defining an object's remote interface and meta-data. It is normally
 * a better choice than {@link org.versile.orb.entity.VObject} as a base class for remote-enabled objects.
 * Below is a code example which demonstrates definition of a class derived from
 * {@link org.versile.orb.external.VExternal} and how it can be accessed remotely over a link.</p>
 *
 * <p>The remote method exposed as 'getGreeting' shows a simple remote method declaration for a method which
 * is not included in the object's published 'methods' meta-information, and which does not take a call
 * context argument. Except the {@link org.versile.orb.external.Publish} annotation the method is essentially
 * defined as as any other Java method. Arguments and return values are lazy-converted between VEntity and
 * native representations when possible, which is why the method can use "String" argument and return values
 * instead of VString (which is how a string is represented in a remote call via lazy-conversion).</p>
 *
 * <p>The remote method with remote name 'square' demonstrates most options available through configuration.
 * Setting 'ctx=true' means the method takes an initial argument which is a locally provided call context object
 * for the particular context. It is typically held by the local link node and
 * can contain information such as login credentials of the communicating peer.
 * {@link org.versile.orb.external.PublishAs} allows publishing the method with a different remote name than the
 * native Java method name. The {@link org.versile.orb.external.Doc} tag can be used to set a docstring for a class
 * or method which is remotely available as VOB meta-information.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.external.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 *
 * class Example {
 *
 *     {@literal @}Doc(doc="Test gateway docstring")
 *     public static class Gateway extends VExternal {
 *
 *         {@literal @}Publish(show=true, ctx=false)
 *         public String getGreeting(String name) {
 *             return "Hello, " + name + "!";
 *         }
 *
 *         {@literal @}Doc(doc="Returns square of provided argument")
 *         {@literal @}PublishAs(name="square", show=true, ctx=true)
 *         public VInteger mathFunction(VCallContext ctx, VInteger number) {
 *             System.out.println("    [method received call context arg " + ctx.getClass() + "]");
 *             return number.multiply(number);
 *         }
 *     }
 *
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         try {
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new Gateway()).peerGateway();
 *
 *             // Test remote method calls
 *             System.out.println("Calling remote 'getGreeting' method");
 *             System.out.println("  Result: " + gw.call("getGreeting", "John Doe"));
 *             System.out.println("Calling remote 'square' method");
 *             System.out.println("  Result: " + gw.call("square", 16));
 *
 *             // Meta calls
 *             System.out.println("\nRemotely available meta-information:\n  Class docstring: " + gw.doc());
 *             for (String method: gw.methods())
 *                 System.out.println("  Remote method '" + method + "', docstring: " + gw.doc(method));
 *
 *             VLink.shutdownForProxy(gw, true);
 *             //clientLink.shutdown(true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }
 * </code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Calling remote 'getGreeting' method
 *   Result: Hello, John Doe!
 * Calling remote 'square' method
 *     [method received call context arg class org.versile.orb.link.VLinkCallContext]
 *   Result: 256
 *
 * Remotely available meta-information:
 *   Class docstring: Test gateway docstring
 *   Remote method 'getGreeting', docstring: null
 *   Remote method 'square', docstring: Returns square of provided argument
 * </pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>The {@link org.versile.orb.entity.VProxy} remote-object proxy interface has methods for accessing VOB standard
 * meta-information, which is why remote method names and doc strings can easily be retrieved by calling
 * the appropriate proxy methods.</p>
 *
 * <p>Though the above example looks almost as a native Java program, all interaction between the 'gw'
 * remote-object reference and the instantiated 'Gateway' object is performed remotely over a VOL link
 * between two link nodes, and all data passed between nodes is transmitted in serialized form. Though the
 * example looks "local", the same mechanisms apply when performing this interaction remotely.</p>
 */
package org.versile.orb.external;
