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
 * ASN.1 data structures.
 *
 * <p>Provides support for representing ASN.1 data structures, and for
 * encoding and decoding DER encoded representations. The scope of the
 * implementation is providing required support to handle Versile Platform
 * ASN.1 structures such as X.509 certificates, and so it is not a
 * complete implementation of ASN.1 specifications. It does however provide
 * support for common data structures.</p>
 *
 * <p>ASN.1 types are derived from {@link org.versile.common.asn1.VASN1Base} and
 * definitions for parsing from encoded representations are derived from
 * {@link org.versile.common.asn1.VASN1Definition}. Below is a simple example
 * of encoding/decoding a VASN.1 data type.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.asn1.*;
 * import org.versile.common.asn1.VASN1Definition.ParseResult;
 * import org.versile.orb.entity.VBytes;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VASN1Base obj = new VASN1Integer(42);
 *             System.out.println("Input value " + obj + " class " + obj.getClass());
 *             byte[] der = obj.encodeDER();
 *             System.out.println("DER encoding: " + new VBytes(der));
 *             VASN1Definition def = new VASN1DefInteger();
 *             ParseResult dec = def.parseDER(der);
 *             System.out.println("Parsed " + dec.getNumRead() + " bytes of " + der.length);
 *             VASN1Base parsed = dec.getResult();
 *             System.out.println("Parsed object " + parsed + " class " + parsed.getClass());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Input value 42 class class org.versile.common.asn1.VASN1Integer
 * DER encoding: b'\x02\x01\x2a'
 * Parsed 3 bytes of 3
 * Parsed object 42 class class org.versile.common.asn1.VASN1Integer</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is a rewrite of the code example which uses some convenience features for lazy-creation of
 * ASN.1 objects and generic definition for ASN.1 universal types.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.asn1.*;
 * import org.versile.common.asn1.VASN1Definition.ParseResult;
 * import org.versile.orb.entity.VBytes;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VASN1Base obj = VASN1Base.lazy(42);
 *             System.out.println("Input value " + obj + " class " + obj.getClass());
 *             byte[] der = obj.encodeDER();
 *             System.out.println("DER encoding: " + new VBytes(der));
 *             VASN1Definition def = new VASN1DefUniversal();
 *             ParseResult dec = def.parseDER(der);
 *             System.out.println("Parsed " + dec.getNumRead() + " bytes of " + der.length);
 *             VASN1Base parsed = dec.getResult();
 *             System.out.println("Parsed object " + parsed + " class " + parsed.getClass());
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Input value 42 class class org.versile.common.asn1.VASN1Integer
 * DER encoding: b'\x02\x01\x2a'
 * Parsed 3 bytes of 3
 * Parsed object 42 class class org.versile.common.asn1.VASN1Integer</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is a simple example of using composite type definitions to parse more complex ASN.1
 * data structures.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.asn1.*;
 * import org.versile.common.asn1.VASN1Definition.ParseResult;
 *
 * public class Example {
 *     static class MyASN1Type extends VASN1DefSequence {
 *         public MyASN1Type() {
 *             super("MyType");
 *             try {
 *                 append(new VASN1DefInteger(), "modulus");
 *                 append(new VASN1DefInteger(), "exponent");
 *             } catch (VASN1Exception e) {
 *                 throw new RuntimeException(e);
 *             }
 *         }
 *     }
 *
 *     public static void main(String[] args) {
 *         try {
 *             // Instantiate and encode an ASN.1 object of type
 *             MyASN1Type def = new MyASN1Type();
 *             VASN1Sequence seq = def.create();
 *             seq.append(VASN1Base.lazy(101));
 *             seq.append(VASN1Base.lazy(13));
 *             byte[] der = seq.encodeDER();
 *
 *             // Parse DER using the definition
 *             ParseResult dec = def.parseDER(der);
 *             VASN1Sequence parsed = (VASN1Sequence) dec.getResult();
 *             System.out.println(parsed.get("modulus"));
 *             System.out.println(parsed.get("exponent"));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> 101
 * 13</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.common.asn1;
