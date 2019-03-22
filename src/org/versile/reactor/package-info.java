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
 * Implementation of the reactor pattern.
 *
 * <p>Implements the reactor pattern, with a reactor dispatching generated
 * events to registered event handlers. At the core of the framework is
 * {@link org.versile.reactor.VReactor} which uses a Selector to react to
 * I/O events and scheduled/timed events.</p>
 *
 * <p>The reactor is a single-thread framework, with all event proecssing
 * performed in the reactor's thread. This is one of the strengths of the
 * reactor based framework as it allows some simplifying assumptions about
 * resource control, however this also means care must be taken not to
 * interfere with the reactor thread for tasks and resources which the
 * reactor assumes it has full control. In case a task needs to be executed
 * in the reactor thread, {@link org.versile.reactor.VReactor#schedule(VReactorFunction)}
 * can be used to get the reactor to execute a task.</p>
 *
 * <p>The reactor framework can also affect performance as all reactor operations
 * are performed in a single thread, which does not take advantage of
 * multiple threads. If multi-thread performance is an issue then heavy-duty tasks
 * can be shifted to other threads (which is effectively what VOL links do by
 * executing method calls in separate task processors), and if I/O single-thread
 * performance is an issue then it can be handled by running multiple parallell
 * reactors.</p>
 *
 * <p>A reactor must be started in order to operate, after which the reactor's
 * main event processing loop continues until the reactor is stopped. Below is
 * an example of starting and stopping a reactor.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.call.VCall;
 * import org.versile.reactor.VReactor;
 * import org.versile.reactor.VReactorFunction;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             // Start a reactor in a separate thread
 *             VReactor reactor = new VReactor(null);
 *             reactor.start();
 *
 *             // Schedule a job, schedule() is one of the few thread-safe reactor calls
 *             class Job implements VReactorFunction {
 *                 {@literal @}Override
 *                 public Object execute() throws Exception {
 *                     return "qwerty";
 *                 }
 *             }
 *             VCall{@literal <}Object{@literal >} call = reactor.schedule(new Job());
 *             call.waitResult();
 *             System.out.println("Got call result: " + call.getResult());
 *
 *             // Stop reactor (also thread-safe)
 *             reactor.stopReactor();
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Got call result: qwerty</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 */
package org.versile.reactor;
