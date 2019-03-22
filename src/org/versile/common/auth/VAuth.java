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

package org.versile.common.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.versile.common.peer.VPeer;
import org.versile.crypto.VRSAKeyPair;



/**
 * Authorizes communication peers.
 *
 * <p>Performs authorization of communication peer credentials and
 * communication channel information.</p>
 */
public class VAuth {

	boolean require_key;
	boolean require_cert;
	boolean validate_root;
	Set<X509Certificate> root_certificates;
	boolean added_public_ca = false;

	/**
	 * Set up authorizer.
	 *
	 * @param requireKey if true require peer to provide credentials with a key
	 * @param requireCert if true require peer to include a certificate with credentials
	 */
	public VAuth(boolean requireKey, boolean requireCert) {
		this.initialize(requireKey,  requireCert, false, null);
	}

	/**
	 * Set up authorizer.
	 *
	 * @param requireKey if true require peer to provide credentials with a key
	 * @param requireCert if true require peer to include a certificate with credentials
	 * @param validateRoot if true validate certificate has a valid root-CA path
	 * @param rootCertificates accepted root CA certificates
	 */
	public VAuth(boolean requireKey, boolean requireCert, boolean validateRoot,
				 Iterable<X509Certificate> rootCertificates) {
		this.initialize(requireKey,  requireCert, validateRoot, rootCertificates);
	}

	void initialize(boolean requireKey, boolean requireCert, boolean validateRoot,
					Iterable<X509Certificate> rootCertificates) {
		require_key = requireKey;
		require_cert = requireCert;
		validate_root = validateRoot;
		root_certificates = new HashSet<X509Certificate>();
		if (rootCertificates != null)
			for (X509Certificate item: rootCertificates)
				root_certificates.add(item);
	}

	/**
	 * Check peer authorization for a communication channel.
	 *
	 * <p>Default returns true, derived classes can overload.</p>
	 *
	 * @param peer peer host
	 * @return true if allowed to communicate with peer
	 */
	public synchronized boolean acceptPeer(VPeer peer) {
		return true;
	}


	/**
	 * Check peer authorization for a set of peer credentials.
	 *
	 * <p>Default returns true, derived classes can overload.</p>
	 *
	 * @param credentials peer's provided credentials
	 * @return true if allowed to interact
	 */
	public synchronized boolean acceptCredentials(VCredentials credentials) {
		return true;
	}

	/**
	 * Add an accepted root certificate.
	 *
	 * @param certificate root certificate
	 */
	public synchronized void addRootCertificate(X509Certificate certificate) {
		root_certificates.add(certificate);
	}

	/**
	 * Adds the Versile Platform public CA as an approved root CA.
	 *
	 * <p>Should be used with caution as anyone may sign certificate chains with the public
	 * CA as the CA keypair is publicly known. Should only be used for situations which
	 * require a known root CA due to technical constraint.</p>
	 */
	public synchronized void addPublicCA() {
		if (!added_public_ca) {
			this.addRootCertificate(VAuth.getPublicCACertificate());
			added_public_ca = true;
		}
	}

	/**
	 * Check whether key is required.
	 *
	 * @return true if required
	 */
	public boolean requiresKey() {
		return require_key;
	}

	/**
	 * Check whether certificate chain is required.
	 *
	 * @return true if required
	 */
	public boolean requiresCertificate() {
		return require_cert;
	}

	/**
	 * Check whether certificates must be signed by a root CA.
	 *
	 * @return true if signature required
	 */
	public boolean requireRootSignature() {
		return validate_root;
	}

	/**
	 * Get accepted root certificates.
	 *
	 * @return accepted certificates
	 */
	public synchronized List<X509Certificate> getRootCertificates() {
		List<X509Certificate> result = new Vector<X509Certificate>();
		for (X509Certificate item: root_certificates)
			result.add(item);
		return result;
	}

	/**
	 * Get the Versile Platform public Certificate Authority keypair
	 *
	 * <p>The Versile Platform public CA is defined by the platform as keypair
	 * which can be used in contexts where a CA is technically required, but it
	 * is not actually used for validation.</p>
	 *
	 * <p>The public CA keypair should not be used in any way to secure or sign
	 * data, as it is a key which is openly published so anyone is able to
	 * decrypt data or force signatures using this key.</p>
	 *
	 * @return Versile Platform defined public CA keypair
	 */
	static public VRSAKeyPair getPublicCAKeypair() {
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1) {
			// Should never happen
			throw new RuntimeException();
		}

		BigInteger n = new BigInteger("140054528373697259711052893057241270983139244677578240022393689127315918503436497938575203584403591315780022240362711021470454961818154752305845872362386094764204050684505955598561861096430240842554860957953199833812103058111733475674538871936008024820439795457392357748454689476701885351561272859991975585379");
		BigInteger e = BigInteger.valueOf(65537);
		BigInteger d = new BigInteger("13106404359611903105236544137205864091117887416384444604686520521504318601424783585719238347546381823087399124161077051050220490422673346291282065630079402076398341353840458127722331518809361463387488385226820712160175964388928786096683313481535523610286317361379146167396682886616439834432370142744570152433");
		BigInteger p = new BigInteger("12378067688701492203149933907673274826306619610010170445273912571898784340936891055840132487697324713949712588697193382399192014875707645238873713277423931");
		BigInteger q = new BigInteger("11314732791575930415693933930799935273947964302586033178656941524934289527569858269544824979057179838941959917780500828623834306833966564942832675095670009");
		BigInteger exp_p = d.mod(p.subtract(BigInteger.ONE));
		BigInteger exp_q = d.mod(q.subtract(BigInteger.ONE));
		BigInteger coeff = q.modInverse(p);
		RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(n, e, d, p, q, exp_p, exp_q, coeff);

		RSAPublicKey pubkey = null;
		try {
			pubkey = (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(n, e));
		} catch (Exception e1) {
			// Should never happen
			throw new RuntimeException();
		}

		RSAPrivateKey privatekey = null;
		try {
			privatekey = (RSAPrivateKey) factory.generatePrivate(spec);
		} catch (InvalidKeySpecException e1) {
			// Should never happen
			throw new RuntimeException();
		}

		return new VRSAKeyPair(pubkey, privatekey);
	}

	/**
	 * Get the Versile Platform public Certificate Authority certificate
	 *
	 * <p>The Versile Platform public CA is defined by the platform as certificate
	 * which can be used in contexts where a CA is technically required, but it
	 * is not actually used for validation.</p>
	 *
	 * <p>The public CA certificate should not be used in any way to secure or sign
	 * data, as it uses the openly published public CA key allowing anyone to
	 * force signatures for this certificate.</p>
	 *
	 * @return Versile Platform defined public CA certificate
	 */
	static public X509Certificate getPublicCACertificate() {
		String hex_str = "3082021d30820186a003020102020900cab850b2bdc1a63b300d06092a86"
					     + "4886f70d0101050500302c312a302806035504060c2144756d6d792056"
					     + "544c53205369676e6174757265204f7267616e697a6174696f6e302218"
					     + "0f32303131303130313030303030305a180f3230353030313031303030"
					     + "3030305a302c312a302806035504060c2144756d6d792056544c532053"
					     + "69676e6174757265204f7267616e697a6174696f6e30819f300d06092a"
					     + "864886f70d010101050003818d0030818902818100c771bb9801f4b476"
					     + "e37735046a20f0f6faf327c1f6a2381aac4925b954ce0340c5ce0ab233"
					     + "16756514fd63ec1b77daae58b4ef38c5e4c3150d12af0f0c3905bbe164"
					     + "db561d95b1cd65c037d63bd400fad25e817a7c1f404c4cfea814fda028"
					     + "89028452f7c4338388eba8c663dfeb34bb28e286fcc161b50274beccd3"
					     + "7ba2e6630203010001a3433041300f0603551d130101ff040530030101"
					     + "ff301d0603551d0e0416041475934d4a29b5fd8f1eff282ef0b719cfd0"
					     + "0fbff2300f0603551d0f0101ff04050303070600300d06092a864886f7"
					     + "0d0101050500038181004362b0ffb5d433db28ac0a9bf40d589da3ac52"
					     + "06fdaff50814d468796267b238655e18f179ca347efe7c564b890d32d3"
					     + "e8c8b1595b8d7aeab95b112ea51aeeefbb7040f75404b6cfe522af185e"
					     + "0c4f40f838dbae1b585ea31ada4e9bf462768af7fd975fc6623b79eccf"
					     + "5d7415e3fd579bb7c870edd195001656baf6947cf5c1";
		byte[] der = new byte[hex_str.length()/2];
		for (int i = 0; i < der.length; i++) {
			der[i] = (byte)(Integer.valueOf(hex_str.substring(2*i, 2*i+2), 16).intValue());
		}
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e1) {
	    	// This should never happen
	    	throw new RuntimeException();
		}
	    try {
	    	InputStream data_as_stream = new ByteArrayInputStream(der);
	    	return (X509Certificate)(cf.generateCertificate(data_as_stream));
	    } catch (Exception e) {
	    	// This should never happen
	    	throw new RuntimeException();
	    }
	}
}
