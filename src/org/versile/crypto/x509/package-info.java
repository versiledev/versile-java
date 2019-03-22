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
 * X.509 support including signing certificates.
 *
 * <p>Because Versile Object Protocol relies on public keys and X.509 certificate
 * chains for authenticating clients or servers, it is important for programs
 * using Versile Platform to be able to not only parse but also create certificates
 * without having to rely on an external tool chain or key chains stored in files.
 * As this is not part of the Java standard, Versile Java adds such support.</p>
 *
 * <p>Below is example code for creating and signing a Certification Request.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.security.cert.X509Certificate;
 * import java.security.interfaces.RSAPrivateKey;
 * import java.text.SimpleDateFormat;
 *
 * import javax.security.auth.x500.X500Principal;
 *
 * import org.versile.common.asn1.VASN1Base;
 * import org.versile.common.asn1.VASN1Time;
 * import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VRSAKeyPair;
 * import org.versile.crypto.x509.VX509Certificate;
 * import org.versile.crypto.x509.VX509CertificationRequest;
 * import org.versile.orb.entity.VInteger;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             // Create certification request
 *             X500Principal subjectName = new X500Principal("CN=ExampleSubject");
 *             VRSAKeyPair subjectKey = VDecentralIdentity.dia(1024, "", "", "someSubjectKeyPwd");
 *             VX509CertificationRequest csr = VX509CertificationRequest.create(subjectName, subjectKey);
 *
 *             // Create signed certificate and print encoded representation
 *             VInteger serial = VInteger.valueOf(123456123456L);
 *             X500Principal issuerName = new X500Principal("CN=ExampleIssuer");
 *             VRSAKeyPair issuerKey = VDecentralIdentity.dia(1024, "", "", "someIssuerSignatureKeyPwd");
 *             RSAPrivateKey issuerSignKey = issuerKey.getPrivate();
 *             SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
 *             VASN1Time notBefore = (VASN1Time) VASN1Base.lazy(dateFormat.parse("2000-01-01 00:00 UTC"));
 *             VASN1Time notAfter = (VASN1Time) VASN1Base.lazy(dateFormat.parse("2050-01-01 00:00 UTC"));
 *             VX509Certificate cert = csr.sign(3, serial, issuerName, issuerSignKey,
 *             	                                notBefore, notAfter, null, null, null);
 *             System.out.println(new String(cert.exportArmored()));
 *
 *             // Example how to convert to a native Java certificate
 *             Object javaCert = cert.getNative();
 *             System.out.println("Got X509Certificate: " + (javaCert instanceof X509Certificate));
 *
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> -----BEGIN CERTIFICATE-----
 * MIIBrTCCARagAwIBAgIFHL6O8kAwDQYJKoZIhvcNAQEFBQAwGDEWMBQGA1UEAxMNRXhhbXBsZUlz
 * c3VlcjAiGA8yMDAwMDEwMTAwMDAwMFoYDzIwNTAwMTAxMDAwMDAwWjAZMRcwFQYDVQQDEw5FeGFt
 * cGxlU3ViamVjdDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEApbwnNhV4Mgn1VrQctOaSkoTg
 * S6VqDulOAgGvDxwGMaAILWn0lNU50P7mkyFemGLCAWT+JGxgk7XLmaJtTGdA+9z7GnxRtxRpsnEL
 * yAleVvAVNvZxecMIYyuUM2ZRBZWAnyDig4HomVAXz3rA0B32L37qmf+e3Z/Yie2ft4l41FcCAwEA
 * ATANBgkqhkiG9w0BAQUFAAOBgQBE9aUWNuXhRGK225tGPzMKOa3WkltBiZOv8RNCavvKkp6/WIle
 * KWtoNwSwUGjq+VhVcGqBfPEqDN8eC5DeCIxmQqkvxVQLe8hAZ4o5upSvfxvttj1NbSJMBf6NtDrB
 * aVjjgqxSubteb6th+cqTsPdUsn5WfDbDjeuSa5d0fOEBzw==
 * -----END CERTIFICATE-----
 *
 * Got X509Certificate: true</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>The framework also includes additional features for working with some X.509 certificate
 * extensions including CA certificates. Below is an example of creating a self-signed
 * CA root certificate.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.text.SimpleDateFormat;
 *
 * import javax.security.auth.x500.X500Principal;
 *
 * import org.versile.common.asn1.VASN1Base;
 * import org.versile.common.asn1.VASN1Time;
 * import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VRSAKeyPair;
 * import org.versile.crypto.x509.VX509Certificate;
 * import org.versile.crypto.x509.VX509CertificationRequest;
 * import org.versile.crypto.x509.VX509KeyUsage;
 * import org.versile.orb.entity.VInteger;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         try {
 *             // Create certification request
 *             X500Principal subjectName = new X500Principal("CN=ExampleCASubject");
 *             VRSAKeyPair subjectKey = VDecentralIdentity.dia(1024, "", "", "someCAKeyPwd");
 *             VX509CertificationRequest csr = VX509CertificationRequest.create(subjectName, subjectKey);
 *
 *             // Create self-signed CA certificate and print encoded representation
 *             VInteger serial = VInteger.valueOf(123456123456L);
 *             SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
 *             VASN1Time notBefore = (VASN1Time) VASN1Base.lazy(dateFormat.parse("2000-01-01 00:00 UTC"));
 *             VASN1Time notAfter = (VASN1Time) VASN1Base.lazy(dateFormat.parse("2050-01-01 00:00 UTC"));
 *             VX509Certificate cert = csr.selfSignCA(3, serial, subjectKey.getPrivate(),
 *                                              notBefore, notAfter, null,
 *                                              -1, VX509KeyUsage.KEY_CERT_SIGN, null);
 *             System.out.println(new String(cert.exportArmored()));
 *
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> -----BEGIN CERTIFICATE-----
 * MIIB9zCCAWCgAwIBAgIFHL6O8kAwDQYJKoZIhvcNAQEFBQAwGzEZMBcGA1UEAxMQRXhhbXBsZUNB
 * U3ViamVjdDAiGA8yMDAwMDEwMTAwMDAwMFoYDzIwNTAwMTAxMDAwMDAwWjAbMRkwFwYDVQQDExBF
 * eGFtcGxlQ0FTdWJqZWN0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9hhKcHJRcK4Iag6G
 * F+hSEKf84nshHKTfCwKZU5d1xZGw2BOiicCDAmpHB/73nSGFZI9l8Gepi6Ng5cW5I4gkw7q21u4k
 * 2sOMP94romZ4j+NbGjNi6RzXOrFMsZ6HSI8Z2/vgn5qfsdJ86VZlllVC5E41NC9u0oP6klmi8EUG
 * ZwIDAQABo0MwQTAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTFrT/SWDb+SEr/KT+5+6BHyQie
 * kTAPBgNVHQ8BAf8EBQMDBwQAMA0GCSqGSIb3DQEBBQUAA4GBABVkWuQgRfiXAjZ1Ui0VeRzPNtIV
 * jxP4pE0gxEuLGsTBQugccSAej1IXaLBZwQtS6DE7ItguucsO2pxT470B7Y/5qR8MuMB2pN6lxH6r
 * SBpyJnCJNE+no/4YkvfLkHEsLJG4/eB/N1RjytGMazU/bURkkxWc1uMvd5HeS33NwL4W
 * -----END CERTIFICATE-----</pre></tt>
 * </div></div>
 * <!--END CODE-->
 */
package org.versile.crypto.x509;
