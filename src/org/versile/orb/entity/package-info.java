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
 * VFE types and supporting classes.
 *
 * <p>Implements data types of the VFE standard, which provide basic data types
 * with standard byte serialized representation. Below is example code for creating,
 * serializing and reconstructing an entity.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.util.*;
 * import org.versile.orb.entity.*;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         VInteger num = new VInteger(42);
 *         System.out.println(num);
 *
 *         VIOContext ctx = new VIOContext();
 *         byte[] serialized = num._v_write(ctx);
 *         System.out.println(new VBytes(serialized));
 *
 *         VEntityReader reader = VEntity._v_reader(ctx);
 *         VByteBuffer data = new VByteBuffer(serialized);
 *         try {
 *             reader.read(data);
 *             VEntity result = reader.getResult();
 *             System.out.println(result.getClass());
 *             System.out.println(result);
 *         } catch (VEntityReaderException e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> 42
 * b'\x2b'
 * class org.versile.orb.entity.VInteger
 * 42</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Types can also be instantiated via lazy-conversion.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.orb.entity.*;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         VEntity entity;
 *         try {
 *             entity = VEntity._v_lazy(42);
 *             System.out.println(entity.getClass());
 *             System.out.println(entity);
 *             Object nat = VEntity._v_lazy_native(entity);
 *             System.out.println(nat.getClass());
 *             System.out.println(nat);
 *         } catch (VEntityError e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> class org.versile.orb.entity.VInteger
 * 42
 * class java.lang.Integer
 * 42</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>{@link org.versile.orb.entity.VObject} is an object which can be remotely called. Note that
 * {@link org.versile.orb.external.VExternal} provides a much cleaner interface with a higher level
 * of abstraction for defining remote-object types, and is normally a better choice of base class.</p>
 *
 * <p>The class {@link org.versile.orb.entity.VProxy} can hold a reference to a local or remote VObject,
 * allowing it to be called using the same remote-method interface regardless where the object exists.
 * Below is an example of accessing an object's remote-call interface via a proxy reference, performing
 * both a blocking and asynchronous version of the same call. Here the accessed object happens to be a local
 * object, however it could just as well have been a reference to remote object.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.util.List;
 *
 * import org.versile.common.call.VCall;
 * import org.versile.orb.entity.*;
 *
 * class Example {
 *     public static class MyObject extends VObject {
 *         {@literal @}Override
 *         protected Object _v_execute(List{@literal <}Object{@literal >} args, VCallContext ctx)
 *                 throws Exception {
 *             String result = "_v_execute called with arguments ";
 *             for (Object obj: args)
 *                 result += "{@literal <}" + obj + "{@literal >} ";
 *             return result;
 *         }
 *     }
 *     public static void main(String[] args) {
 *         VProxy proxy = new MyObject()._v_proxy();
 *         try {
 *             // Simulates remote-object call by accessing via a proxy
 *             Object result = proxy.call("dummyMethod", "blocking call", 42, false);
 *             System.out.println(result);
 *             // Perform same call as an asynchronous call
 *             VCall{@literal <}Object{@literal >} call = proxy.nowait("dummyMethod", "asynchronous call", 42, false);
 *             call.waitResult();
 *             System.out.println(call.getResult());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }
 * </code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> _v_execute called with arguments {@literal <}dummyMethod{@literal >} {@literal <}blocking call{@literal >} {@literal <}42{@literal >} {@literal <}false{@literal >}
 * _v_execute called with arguments {@literal <}dummyMethod{@literal >} {@literal <}asynchronous call{@literal >} {@literal <}42{@literal >} {@literal <}false{@literal >}
 * </pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Whereas entity types like {@link org.versile.orb.entity.VInteger} can be serialized with a
 * {@link org.versile.orb.entity.VIOContext}, the serialized representation of a
 * {@link org.versile.orb.entity.VObject} is only valid within an ID space of a specific object context,
 * and no longer holds any meaning when that context ceases to exist. {@link org.versile.orb.entity.VObject},
 * {@link org.versile.orb.entity.VReference} and composite types containing such objects must be
 * encoded with a {@link org.versile.orb.entity.VObjectIOContext} which provides such an ID space (typically
 * an object link).</p>
 *
 * <p>Serialized object data is assumed to always be exchanged between two communication peers, with
 * data serialized by one peer being reconstructed by the other peer. Serialization and decoding of serialized
 * data must be performed on separate context objects, corresponding to the communication contexts of each peer.
 * A serialized locally implemented {@link org.versile.orb.entity.VObject} would be decoded as a
 * {@link org.versile.orb.entity.VReference} by the peer (as it is remote in the peer's context).</p>
 */
package org.versile.orb.entity;
