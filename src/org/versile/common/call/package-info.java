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
 * Mechanisms for passing asynchronous call results.
 *
 * <p>{@link org.versile.common.call.VCall} provides a mechanism for passing a
 * reference to a call result which may not yet be available because the
 * call has not yet completed. The class supports asynchronous call
 * results by providing call method resolution mechanisms for passing the
 * result of a completed call to the call object, and separate mechanisms
 * for the call result receiver to wait for a call result and retrieve
 * the result of a completed call.</p>
 *
 * <p>Below is a generic example of an asynchronous call, with one thread
 * waiting for a call result from a separate thread.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.call.VCall;
 * import org.versile.common.call.VCallOperationException;
 *
 * class Example {
 *
 *     static VCall{@literal <}String{@literal >} asyncGreeting(String name) {
 *         VCall{@literal <}String{@literal >} result = new VCall{@literal <}String{@literal >}();
 *         class Job implements Runnable {
 *             VCall{@literal <}String{@literal >} jobResult;
 *             String jobName;
 *             public Job(VCall{@literal <}String{@literal >} result, String name) {
 *                 jobResult = result;
 *                 jobName = name;
 *             }
 *             {@literal @}Override
 *             public void run() {
 *                 String greeting = "Hello " + jobName;
 *                 try {
 *                     jobResult.pushResult(greeting);
 *                 } catch (VCallOperationException e) {
 *                     // SILENT
 *                 }
 *             }
 *         }
 *         new Thread(new Job(result, name)).start();
 *         return result;
 *     }
 *
 *     public static void main(String[] args) {
 *         try {
 *             VCall{@literal <}String{@literal >} result = asyncGreeting("John Doe");
 *             result.waitResult();
 *             System.out.println("Got asynchronous call result: " + result.getResult());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Got asynchronous call result: Hello John Doe</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is an example of using asynchronous call references in a real scenario, which performs
 * a remote-object call as an asynchronous call which generates a call reference for the call. The
 * result is retrieved by waiting for the result to become ready before pulling the result value
 * from the call object.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.common.call.VCall;
 * import org.versile.demo.Echoer;
 * import org.versile.orb.entity.*;
 * import org.versile.orb.link.VLink;
 * import org.versile.reactor.io.link.VLinkAgent;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *         try {
 *             VProxy gw = VLinkAgent.createVirtualServerLink(new Echoer()).peerGateway();
 *             VCall{@literal <}Object{@literal >} call = gw.nowait("echo", 42);
 *             call.waitResult();
 *             System.out.println("Got asynchronous call result: " + call.getResult());
 *             VLink.shutdownForProxy(gw, true);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Got asynchronous call result: 42</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.common.call;
