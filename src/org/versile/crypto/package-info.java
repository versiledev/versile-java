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
 * Versile Crypto Algorithms.
 *
 * <p>Provides functionality for providing VCA capabilities defined in the
 * Versile Platform specification, such as referring to hash methods or
 * ciphers using their VCA defined standard name, providing the VCA RSA block
 * cipher transform, or implementing the VMessage plaintext integrity
 * protection format. Native Java crypto is used in the implementation
 * whenever possible.</p>
 *
 * <p>This package also includes the implementation of Versile Decentral
 * Identities, see {@link org.versile.crypto.VDecentralIdentity} for more
 * information.</p>
 *
 * <p>Because the Versile Object Protocol relies on public key and certificate
 * chains for authentication and negotiating secure connections, is is important
 * for programs using Versile Java to be able to not only parse certificates, but
 * also create certificates. See {@link org.versile.crypto.x509} for more information
 * how this is supported.</p>
 *
 * <p>Example use of the hash-method framework:</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.security.MessageDigest;
 *
 * import org.versile.crypto.VHash;
 * import org.versile.orb.entity.VBytes;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VHash hashMethod = VHash.getHashGenerator("sha1");
 *
 *             byte[] digest = hashMethod.digestOf("Input string".getBytes());
 *             System.out.println("Hash1: " + new VBytes(digest));
 *
 *             MessageDigest hasher = hashMethod.getInstance();
 *             hasher.update("Input string".getBytes());
 *             System.out.println("Hash2: " + new VBytes(hasher.digest()));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Hash1: b'\x21\x94\x41\x36\x09\xb4\xf9\x71\x53\x49\x53\xbb\x2e\xe6\x56\xc0\x0a\x1d\xe5\x6c'
 * Hash2: b'\x21\x94\x41\x36\x09\xb4\xf9\x71\x53\x49\x53\xbb\x2e\xe6\x56\xc0\x0a\x1d\xe5\x6c'</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is an example of working with block ciphers (implicitly using zero-bytes initialization vector).</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import javax.crypto.SecretKey;
 *
 * import org.versile.crypto.VBlockCipher;
 * import org.versile.crypto.VBlockTransform;
 * import org.versile.orb.entity.VBytes;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VBlockCipher cipher = VBlockCipher.getCipher("aes128", "cbc");
 *             SecretKey key = cipher.importKey("abcdefghijklmnop".getBytes());
 *
 *             byte[] input = "1234567890123456".getBytes();
 *             System.out.println("Input         : " + new VBytes(input));
 *
 *             VBlockTransform enc = cipher.getEncrypter(key);
 *             byte[] ciphertext = enc.transform(input);
 *             System.out.println("Ciphertext    : " + new VBytes(ciphertext));
 *             VBlockTransform dec = cipher.getDecrypter(key);
 *
 *             byte[] rec = dec.transform(ciphertext);
 *             System.out.println("Reconstructed : " + new VBytes(rec));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Input         : b'\x31\x32\x33\x34\x35\x36\x37\x38\x39\x30\x31\x32\x33\x34\x35\x36'
 * Ciphertext    : b'\x33\x7a\xb7\x73\xce\x4b\x19\xd1\x23\xf6\x4f\x22\x7a\x9f\xc9\xd7'
 * Reconstructed : b'\x31\x32\x33\x34\x35\x36\x37\x38\x39\x30\x31\x32\x33\x34\x35\x36'</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is an example of using a cipher with the VCA message-based plaintext wrapper for
 * performing plaintext integrity validation.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import javax.crypto.SecretKey;
 *
 * import org.versile.crypto.VBlockCipher;
 * import org.versile.crypto.VBlockTransform;
 * import org.versile.crypto.VHash;
 * import org.versile.crypto.VMessageDecrypter;
 * import org.versile.crypto.VMessageEncrypter;
 * import org.versile.crypto.rand.VSecureRandom;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VBlockCipher cipher = VBlockCipher.getCipher("aes128", "cbc");
 *             SecretKey key = cipher.importKey("abcdefghijklmnop".getBytes());
 *             byte[] macSecret = "mac_secret".getBytes();
 *
 *             String input = "abcdefghijklmnopqrstuvxyz1234567890";
 *             System.out.println("Input      : " + input);
 *
 *             VBlockTransform _enc = cipher.getEncrypter(key);
 *             VMessageEncrypter enc = new VMessageEncrypter(_enc, VHash.getHashGenerator("sha1"),
 *                                                           new VSecureRandom(), macSecret);
 *             byte[] ciphertext = enc.encrypt(input.getBytes());
 *             System.out.println("Ciphertext : (" + ciphertext.length + " bytes of data)");
 *
 *             VBlockTransform _dec = cipher.getDecrypter(key);
 *             VMessageDecrypter dec = new VMessageDecrypter(_dec, VHash.getHashGenerator("sha1"),
 *                                                           macSecret);
 *             int numUnresolved = dec.decrypt(ciphertext);
 *             if (numUnresolved {@literal <} 0)
 *                 throw new RuntimeException("Integrity check failed");
 *             if (numUnresolved {@literal >} 0)
 *                 System.out.println("  " + numUnresolved + " pending unresolved ciphertext bytes");
 *             byte[] rec = dec.getDecrypted();
 *             System.out.println("Result     : " + new String(rec));
 *             dec.getDecrypted();
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Input      : abcdefghijklmnopqrstuvxyz1234567890
 * Ciphertext : (64 bytes of data)
 * Result     : abcdefghijklmnopqrstuvxyz1234567890</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Below is a code example for creating and validating RSA key signatures.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VHash;
 * import org.versile.crypto.VRSACipher;
 * import org.versile.crypto.VRSACrtKeyPair;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             VRSACrtKeyPair keyPair = VDecentralIdentity.dia(1024, "", "", "someVDIpassword");
 *             byte[] msg = "Important message to be signed".getBytes();
 *             VHash hash = VHash.getHashGenerator("sha1");
 *
 *             // Signing message
 *             byte[] signature = VRSACipher.rsassa_pkcs1_v1_5_sign(keyPair.getPrivate(), hash, msg);
 *             System.out.println("Signature: (" + signature.length + " bytes)");
 *
 *             // Validate signature
 *             boolean valid;
 *             valid = VRSACipher.rsassa_pkcs1_v1_5_verify(keyPair.getPublic(), hash, msg, signature);
 *             System.out.println("Valid signature: " + valid);
 *
 *             // Tamper with message
 *             for (int i = 0; i {@literal <} 5; i++)
 *                 msg[i] = (byte)0xff;
 *             valid = VRSACipher.rsassa_pkcs1_v1_5_verify(keyPair.getPublic(), hash, msg, signature);
 *             System.out.println("Valid signature for tampered message: " + valid);
 *
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Signature: (128 bytes)
 * Valid signature: true
 * Valid signature for tampered message: false</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.crypto;
