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
 * VCA random and pseudo-random number methods.
 *
 * <p>{@link org.versile.crypto.rand.VSecureRandom} is a convenient interface to Java's
 * native secure random number providers, allowing it to be used in interfaces
 * which require a {@link org.versile.common.util.VByteGenerator} as a data source.</p>
 *
 * <p>{@link org.versile.crypto.rand.VPseudoRandomHMAC} implements the pseudo-random
 * function defined by RFC 5246. It enables constructing pseudo-random data from
 * a set of secrets. Pseudo-random data has the property it will always produce the same
 * sequence from the same set of input data, allowing it to be used e.g. in handshake
 * operations where two peers need to generate seemingly random data for one-time use
 * from a set of secrets known to both parties.</p>
 *
 * <p>Below is an example of setting up a pseudo-random function to generate output data.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.common.util.VByteGenerator;
 * import org.versile.crypto.VHash;
 * import org.versile.crypto.rand.VPseudoRandomHMAC;
 * import org.versile.orb.entity.*;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VHash hash = VHash.getHashGenerator("sha1");
 *             byte[] secret = "someSecret".getBytes();
 *             byte[] seed = "someSeeed".getBytes();
 *             VByteGenerator pseudo = new VPseudoRandomHMAC(hash, secret, seed);
 *             System.out.println("Generating random data   : " + new VBytes(pseudo.getBytes(10)));
 *             System.out.println("Generating random number : " + pseudo.getNumber(1, 10));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Generating random data   : b'\xd6\xc2\x38\x5c\x14\xd6\x96\x0f\x59\x59'
 * Generating random number : 4</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 */
package org.versile.crypto.rand;
