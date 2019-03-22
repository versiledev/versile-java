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
 * VSE utility classes.
 *
 * <p>Includes the {@link org.versile.vse.util.VFunction} class for remote access to
 * a function. It provides a way of providing remote access to simple functions without the
 * complexity of defining a complete {@link org.versile.orb.external.VExternal} remote-object
 * interface. Function objects can be convenient for use e.g. for callbacks between two systems,
 * such as notifying a message is ready for processing.</p>
 *
 * <p>Below is a code example of a client/server link pair which passes a reference to a function object
 * over the link. The client makes a call to the server in which the client provides a reference to
 * a client-side function object, which is called by the server during the execution of its remote method.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.common.util.VExceptionProxy;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.external.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 * import org.versile.vse.VSEResolver;
 * import org.versile.vse.util.VFunction;
 * import org.versile.vse.util.VFunctionProxy;
 *
 * class Example {
 *     public static class Gateway extends VExternal {
 *         {@literal @}Publish(show=true, ctx=false)
 *         public String triggerCallback(VFunctionProxy func) throws VExceptionProxy {
 *             try {
 *                 String clientResult = (String) func.call("ciao");
 *                 return "Server says: " + clientResult;
 *             } catch (Exception e) {
 *                 throw new VException("Callback failed").getProxy();
 *             }
 *         }
 *     }
 *
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         try {
 *             // Set up a link
 *             VSEResolver.addToGlobalModules();
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new Gateway()).peerGateway();
 *
 *             // Create a function object and send as an argument to server, which is
 *             // actually called by the server during the server's remote method execution
 *             class Function extends VFunction {
 *                 public Function() {
 *                     super(1, 1, null);
 *                 }
 *                 {@literal @}Override
 *                 public Object function(Object... args) throws Exception {
 *                     return "Client says: " + args[0];
 *                 }
 *             }
 *             Object serverResult = gw.call("triggerCallback", new Function());
 *             System.out.println(serverResult);
 *
 *             // Shut down link
 *             VLink.shutdownForProxy(gw, true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Server says: Client says: ciao</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.vse.util;
