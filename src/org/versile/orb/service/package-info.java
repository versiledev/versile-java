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
 * Services for initiating (VOP) links.
 *
 * <p>Base functionality for running services which provide access to Versile
 * Object Protocol resources. Versile Java includes a reactor-based implementation in
 * {@link org.versile.reactor.service}.</p>
 *
 * <p>Service classes provide a mechanism for setting up socket-based listening services
 * which accept incoming connections and negotiate a Versile Object Protocol (VOP)
 * connection, which provides access to a Versile ORB Link using a negotiated transport.
 * The byte transport is normally an encrypted VTS transport. The protocol also allows
 * TLS connections (not currently implemented by Versile Java) and unencrypted connections
 * (strongly discouraged).</p>
 *
 * <p>Note that global license configuration must be set before a service object can be
 * constructed. See {@link org.versile.Versile} for details.<p>
 *
 * <p>Below is a simple example of running a listening service, accessing the service by
 * connecting to the service with a client by resolving a URL, and shutting down the service.
 * The main input for service creation is a set of server credentials (in this case using
 * an RSA key pair without certificates), and a factory for the gateway objects which are
 * provided to connecting clients.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.common.auth.VPrivateCredentials;
 * import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VRSAKeyPair;
 * import org.versile.demo.SimpleGateway;
 * import org.versile.orb.entity.VProxy;
 * import org.versile.orb.link.VLink;
 * import org.versile.orb.service.VService;
 * import org.versile.reactor.service.VOPService;
 * import org.versile.reactor.url.VUrl;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         VService service = null;
 *         VProxy echoer = null;
 *         try {
 *             // Set up and start a service (using a decentral identity as the key)
 *             VRSAKeyPair keypair = VDecentralIdentity.dia(1024, "", "", "someVdiPasswd");
 *             VPrivateCredentials credentials = new VPrivateCredentials(keypair);
 *             service = new VOPService(new SimpleGateway.Factory(), credentials, null);
 *             service.start();
 *
 *             // Test a client connection accessing the service
 *             echoer = VProxy.valueOf(VUrl.resolve("vop://localhost/text/echo/"));
 *             System.out.println("Resolved VOP resource");
 *             System.out.println("Echoer call result: " + echoer.call("echo", 1234));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         } finally {
 *             // Terminate link and shut down service
 *             VLink.shutdownForProxy(echoer);
 *             if (service != null)
 *                 service.stop(true, true);
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Resolved VOP resource
 * Echoer call result: 1234</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>The above example used an unauthenticated client connection. The client can also authenticate itself with
 * a keypair, similar to server credentials. Authentication is only performed with secure transports, and the
 * standard pattern is to use transport authentication credentials also at the Versile ORB Link layer for client
 * authentication. Below is an updated example which includes client credentials.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import org.versile.Versile;
 * import org.versile.common.auth.VPrivateCredentials;
 * import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VRSAKeyPair;
 * import org.versile.demo.SimpleGateway;
 * import org.versile.orb.entity.VProxy;
 * import org.versile.orb.link.VLink;
 * import org.versile.orb.service.VService;
 * import org.versile.reactor.service.VOPService;
 * import org.versile.reactor.url.VUrl;
 *
 * public class Example {
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         VService service = null;
 *         VProxy echoer = null;
 *         try {
 *             // Set up and start a service (using a decentral identity as the key)
 *             VRSAKeyPair keypair = VDecentralIdentity.dia(1024, "", "", "someVdiPasswd");
 *             VPrivateCredentials credentials = new VPrivateCredentials(keypair);
 *             service = new VOPService(new SimpleGateway.Factory(), credentials, null);
 *             service.start();
 *
 *             // Test an authenticated client connection accessing the service
 *             VRSAKeyPair clientKey = VDecentralIdentity.dia(1024, "", "", "someClientVdiPasswd");
 *             VPrivateCredentials clientCredentials = new VPrivateCredentials(clientKey);
 *             echoer = VProxy.valueOf(VUrl.resolve("vop://localhost/text/echo/", clientCredentials));
 *             System.out.println("Resolved VOP resource");
 *             System.out.println("Echoer call result: " + echoer.call("echo", 1234));
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         } finally {
 *             // Terminate link and shut down service
 *             VLink.shutdownForProxy(echoer);
 *             if (service != null)
 *                 service.stop(true, true);
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Resolved VOP resource
 * Echoer call result: 1234</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>The above example performed a connection for which the client provided client credentials which were
 * used for negotiating the secure transport, however the server ignored those credentials allowing any
 * client to "log in". Authorization can be performed by adding an "authorizer" to the service or the
 * connecting client, which provides the necessary logic whether to allow a Versile ORB Link to be
 * initiated.</p>
 *
 * <p>A link authorizer takes advantage of the call context object which is set on the link being negotiated. During
 * VOP handshake information about the peer is set on the link object, including information about the peer
 * communication channel, a public key provided by the peer, or a certificate chain for the peer's key. If
 * a RSA public key is provided, it is used in the setup of the secure transport which guarantees the peer has
 * possession of the private key component for that key.</p>
 *
 * <p>Below is an example service which is configured with a link authorizer that validates a connecting
 * client by comparing the RSA public key the client used to connect with a list of authorized keys. A matching
 * key is accepted, any other connection is rejected.</p>
 *
 * <!--CODE WITH OUTPUT-->
 * <div style="margin-left:50px;margin-right:50px;padding-top:5px;padding-bottom:5px;background-color:#e6e6e6;">
 * <pre><code> import java.security.interfaces.RSAPublicKey;
 * import java.util.HashSet;
 *
 * import org.versile.Versile;
 * import org.versile.common.auth.VPrivateCredentials;
 * import org.versile.crypto.VCryptoException;
 * import org.versile.crypto.VDecentralIdentity;
 * import org.versile.crypto.VRSAKeyPair;
 * import org.versile.demo.SimpleGateway;
 * import org.versile.orb.entity.VCallContext;
 * import org.versile.orb.entity.VProxy;
 * import org.versile.orb.link.VLink;
 * import org.versile.orb.link.VLinkAuth;
 * import org.versile.orb.service.VService;
 * import org.versile.orb.url.VUrlException;
 * import org.versile.reactor.service.VOPService;
 * import org.versile.reactor.url.VUrl;
 *
 * public class Example {
 *     static class Authorizer extends VLinkAuth {
 *         HashSet{@literal <}RSAPublicKey{@literal >} acceptedKeys;
 *         public Authorizer() {
 *             acceptedKeys = new HashSet{@literal <}RSAPublicKey{@literal >}();
 *             try {
 *                 VRSAKeyPair _key = VDecentralIdentity.dia(1024, "", "", "someClientVdiPasswd");
 *                 acceptedKeys.add(_key.getPublic());
 *             } catch (VCryptoException e) {
 *                 throw new RuntimeException(e);
 *             }
 *         }
 *         {@literal @}Override
 *         public boolean authorize(VLink link) {
 *             VCallContext ctx = link.getContext();
 *             RSAPublicKey clientKey = ctx.getPublicKey();
 *             if (clientKey == null)
 *                 return false;
 *             return (acceptedKeys.contains(clientKey));
 *         }
 *     }
 *
 *     public static void main(String[] args) {
 *         Versile.setInternalUseAGPL();
 *
 *         VService service = null;
 *         VProxy echoer = null;
 *         try {
 *             // Set up and start a service (using a decentral identity as the key)
 *             VRSAKeyPair keypair = VDecentralIdentity.dia(1024, "", "", "someVdiPasswd");
 *             VPrivateCredentials credentials = new VPrivateCredentials(keypair);
 *             service = new VOPService(new SimpleGateway.Factory(), credentials, new Authorizer());
 *             service.start();
 *
 *             // Test an authenticated client connection accessing the service
 *             VRSAKeyPair clientKey = VDecentralIdentity.dia(1024, "", "", "someClientVdiPasswd");
 *             VPrivateCredentials clientCredentials = new VPrivateCredentials(clientKey);
 *             System.out.println("Connecting with valid client credentials");
 *             echoer = VProxy.valueOf(VUrl.resolve("vop://localhost/text/echo/", clientCredentials));
 *             System.out.println("Resolved VOP resource, example call: " + echoer.call("echo", 1234));
 *             VLink.shutdownForProxy(echoer);
 *
 *             // Client with unauthorized credentials is rejected by the service
 *             clientKey = VDecentralIdentity.dia(1024, "", "", "INVALIDClientVdiPasswd");
 *             clientCredentials = new VPrivateCredentials(clientKey);
 *             try {
 *                 echoer = VProxy.valueOf(VUrl.resolve("vop://localhost/text/echo/", clientCredentials));
 *                 throw new RuntimeException("Should never happen");
 *             } catch (VUrlException e2) {
 *                 System.out.println("Client connection with illegal credentials was rejected");
 *             }
 *
 *             // Client which connects without credentials is rejected by the service
 *             try {
 *                 echoer = VProxy.valueOf(VUrl.resolve("vop://localhost/text/echo/"));
 *                 throw new RuntimeException("Should never happen");
 *             } catch (VUrlException e2) {
 *                 System.out.println("Client connection without credentials was rejected");
 *             }
 *
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         } finally {
 *             // Terminate link and shut down service
 *             VLink.shutdownForProxy(echoer);
 *             if (service != null)
 *                 service.stop(true, true);
 *         }
 *     }
 * }</code></pre>
 * <div style="margin-left:25px;margin-right:25px;color:#222222;background-color:#d6d6d6;">
 * <tt> <u>Expected output:</u></tt>
 * <tt><pre> Connecting with valid client credentials
 * Resolved VOP resource, example call: 1234
 * Client connection with illegal credentials was rejected
 * Client connection without credentials was rejected</pre></tt>
 * </div></div>
 * <!--END CODE-->
 *
 * <p>Though not included in the above example, the {@link org.versile.orb.link.VLinkAuth} could perform
 * additional authorization by inspecting additional {@link org.versile.orb.entity.VCallContext} parameters.
 * This includes e.g. inspecting a certificate chain provided for the public key (if any), and possibly
 * validating that certificate chain vs. the key and root signature vs. an accepted CA root certificate.
 * It could also inspect information about channel peer, e.g. to filter clients based on IP address.</p>
 */
package org.versile.orb.service;
