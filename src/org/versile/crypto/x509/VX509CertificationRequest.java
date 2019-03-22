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

package org.versile.crypto.x509;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.LinkedList;

import javax.security.auth.x500.X500Principal;

import org.versile.common.asn1.VASN1Base;
import org.versile.common.asn1.VASN1BitString;
import org.versile.common.asn1.VASN1DefInteger;
import org.versile.common.asn1.VASN1DefUnknown;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1GeneralizedTime;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1Null;
import org.versile.common.asn1.VASN1ObjectIdentifier;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.asn1.VASN1Tag;
import org.versile.common.asn1.VASN1Tagged;
import org.versile.common.asn1.VASN1Time;
import org.versile.common.asn1.VASN1Definition.ParseResult;
import org.versile.common.util.VBase64;
import org.versile.common.util.VBitfield;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VBase64.VBase64Exception;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VHash;
import org.versile.crypto.VRSACipher;
import org.versile.crypto.VRSAKeyPair;
import org.versile.crypto.asn1.cert.AlgorithmIdentifier;
import org.versile.crypto.asn1.cert.SubjectPublicKeyInfo;
import org.versile.crypto.asn1.pkcs.Attributes;
import org.versile.crypto.asn1.pkcs.CertificationRequest;
import org.versile.crypto.asn1.pkcs.CertificationRequestInfo;
import org.versile.orb.entity.VInteger;



/**
 * Certification request, implementing request as per PKCS#10.
 *
 * <p>The CSR 'attributes' property is not supported.</p>
 */
public class VX509CertificationRequest {

	byte[] der;
	VASN1Sequence asn1;

	int version;
	X500Principal subject;
	RSAPublicKey subjectKey;
	VASN1Base attributes;
	VObjectIdentifier signAlgorithm;
	VBitfield signature;

	protected VX509CertificationRequest(byte[] der)
			throws VCryptoException {

		try {
			ParseResult _dec = new org.versile.crypto.asn1.pkcs.CertificationRequest().parseDER(der);
			if (_dec.getNumRead() != der.length)
				throw new VCryptoException("Invalid DER encoding");
			asn1 = (VASN1Sequence)_dec.getResult();

			this.der = new byte[der.length];
			for (int i = 0; i < der.length; i++)
				this.der[i] = der[i];

			VASN1Sequence _cr_info = (VASN1Sequence) asn1.get("certificationRequestInfo");

			version = ((VASN1Integer)_cr_info.get("version")).getValue().getValue().intValue();
			if (version != 0)
				throw new VCryptoException("Invalid version number");

			VASN1Base _subject = _cr_info.get("subject");
			subject = new X500Principal(_subject.encodeDER());

			VASN1Base _spki = _cr_info.get("subjectPKInfo");
			subjectKey = VRSAKeyPair.importPublicDerSpki(_spki.encodeDER());

			attributes = _cr_info.get("attributes");

			VASN1Sequence _sign_alg = (VASN1Sequence) asn1.get("signatureAlgorithm");
			signAlgorithm = ((VASN1ObjectIdentifier)_sign_alg.get("algorithm")).getValue();
			if (!(_sign_alg.get("parameters") instanceof VASN1Null))
				throw new VCryptoException("Invalid signature algorithm format");

			signature = ((VASN1BitString)asn1.get("signature")).getValue();

		} catch (VCryptoException e) {
			throw e;
		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Create a certification request without attributes.
	 *
	 * @param subject subject
	 * @param keypair subject keypair
	 * @return signed certification request
	 * @throws VCryptoException unable to create certification request
	 */
	public static VX509CertificationRequest create(X500Principal subject, VRSAKeyPair keypair)
			throws VCryptoException {
		return VX509CertificationRequest.create(subject, keypair, null);
	}

	/**
	 * Create a certification request.
	 *
	 * @param subject subject
	 * @param keypair subject keypair
	 * @param attributes request attributes (or null)
	 * @return signed certification request
	 * @throws VCryptoException unable to create certification request
	 */
	public static VX509CertificationRequest create(X500Principal subject, VRSAKeyPair keypair, VASN1Base attributes)
			throws VCryptoException {

		try {
			// Generate CR info structure
			CertificationRequestInfo _crinfo_def = new CertificationRequestInfo();
			VASN1Sequence _cr_info = (VASN1Sequence) _crinfo_def.create();
			// Version
			VASN1DefInteger _ver_def = (VASN1DefInteger) _crinfo_def.get("version");
			_cr_info.append(_ver_def.create(new VInteger(0)), "version");
			// Subject
			VASN1Base _sub_asn1 = new VASN1DefUnknown().parseDER(subject.getEncoded()).getResult();
			_cr_info.append(_sub_asn1, "subject");
			// Public key
			VASN1Sequence _spki = new SubjectPublicKeyInfo().create();
			VASN1Sequence _key_alg = new AlgorithmIdentifier().create();
			_key_alg.append(new VASN1ObjectIdentifier(VRSAKeyPair.RSA_ENCRYPTION), "algorithm");
			_key_alg.append(new VASN1Null(), "parameters");
			_spki.append(_key_alg, "algorithm");
			VASN1BitString _key_data = new VASN1BitString(VBitfield.fromOctets(VRSAKeyPair.exportDerPkcs(keypair.getPublic())));
			_spki.append(_key_data, "subjectPublicKey");
			_cr_info.append(_spki, "subjectPKInfo");
			// Attributes (note - not yet properly supported)
			if (attributes == null)
				attributes = new Attributes().create();
			VASN1Tag _attr_tag = new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 0);
			_cr_info.append(new VASN1Tagged(attributes, _attr_tag, false), "attributes");

			// Generate CR structure
			VASN1Sequence _cr = new CertificationRequest().create();
			// CR info
			_cr.append(_cr_info, "certificationRequestInfo");
			// signature algorithm
			VASN1Sequence _cr_alg = new AlgorithmIdentifier().create();
			_cr_alg.append(new VASN1ObjectIdentifier(VRSACipher.SHA1_WITH_RSA_SIGNATURE), "algorithm");
			_cr_alg.append(new VASN1Null(), "parameters");
			_cr.append(_cr_alg, "signatureAlgorithm");
			// signature
			byte[] _msg = _cr_info.encodeDER();
			byte[] _signature = VRSACipher.rsassa_pkcs1_v1_5_sign(keypair.getPrivate(), VHash.getHashGenerator("sha1"), _msg);
			VASN1BitString _cr_signature = new VASN1BitString(VBitfield.fromOctets(_signature));
			_cr.append(_cr_signature, "signature");

			return new VX509CertificationRequest(_cr.encodeDER());

		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Verifies the certification request signature.
	 *
	 * <p>Verifies the certification request is self-signed with the subjec key
	 * held by the request.</p>
	 *
	 * @return true if valid
	 * @throws VCryptoException unable to perform signature validation
	 */
	public boolean verify()
			throws VCryptoException {
		if (!signAlgorithm.equals(VRSACipher.SHA1_WITH_RSA_SIGNATURE))
			throw new VCryptoException("Signature method not supported");
		byte[] msg;
		try {
			msg = asn1.get("certificationRequestInfo").encodeDER();
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		byte[] _signature = signature.toOctets();
		return VRSACipher.rsassa_pkcs1_v1_5_verify(subjectKey, VHash.getHashGenerator("sha1"), msg, _signature);
	}

	/**
	 * Signs the certification request.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate sign(int version, VInteger serial, X500Principal issuer, RSAPrivateKey signatureKey,
							     Date notBefore, Date notAfter, VBitfield issuerUnique,
							     VBitfield subjectUnique, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		try {
			return this.sign(version, serial, issuer, signatureKey, new VASN1GeneralizedTime(notBefore), new VASN1GeneralizedTime(notAfter),
					issuerUnique, subjectUnique, extensions);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Signs the certification request.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate sign(int version, VInteger serial, X500Principal issuer, RSAPrivateKey signatureKey,
							     VASN1Time notBefore, VASN1Time notAfter, VBitfield issuerUnique,
							     VBitfield subjectUnique, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		VASN1Sequence tbsCert = VX509Certificate.createTbsCertificate(version, serial, issuer, notBefore, notAfter, this.getSubject(),
																	  this.getSubjectKey(), issuerUnique, subjectUnique, extensions);
		try {
			return VX509Certificate.signTBSCertificate(tbsCert.encodeDER(), signatureKey);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Self-signs the certification request.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate selfSign(int version, VInteger serial, RSAPrivateKey signatureKey,
							     Date notBefore, Date notAfter, VBitfield uniqueID, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		try {
			return this.selfSign(version, serial, signatureKey, new VASN1GeneralizedTime(notBefore), new VASN1GeneralizedTime(notAfter),
					uniqueID, extensions);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Self-signs the certification request.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate selfSign(int version, VInteger serial, RSAPrivateKey signatureKey,
							     VASN1Time notBefore, VASN1Time notAfter,
							     VBitfield uniqueID, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		if (signatureKey.getModulus().compareTo(this.getSubjectKey().getModulus()) != 0)
			throw new VCryptoException("Signature key does not match certification request key");
		VASN1Sequence tbsCert = VX509Certificate.createTbsCertificate(version, serial, this.getSubject(), notBefore, notAfter, this.getSubject(),
				this.getSubjectKey(), uniqueID, uniqueID, extensions);
		try {
			return VX509Certificate.signTBSCertificate(tbsCert.encodeDER(), signatureKey);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Self-signs the certification request as a Certificate Authority.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Similar to {@link #selfSign} with the difference that this method automatically
	 * generates a set of root CA certificate extensions and appends to the other
	 * provided extensions (which must not conflict with generated CA extensions).</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate selfSignCA(int version, VInteger serial, RSAPrivateKey signatureKey,
							     Date notBefore, Date notAfter, VBitfield uniqueID,
							     int pathLength, int usageBits, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		try {
			return this.selfSignCA(version, serial, signatureKey, new VASN1GeneralizedTime(notBefore), new VASN1GeneralizedTime(notAfter),
					               uniqueID, pathLength, usageBits, extensions);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Self-signs the certification request as a Certificate Authority.
	 *
	 * <p>Parameters are similar to
	 * {@link VX509Certificate#createTbsCertificate(int, VInteger, X500Principal, Date, Date, X500Principal,
	 * RSAPublicKey, VBitfield, VBitfield, VX509CertificateExtension[])}
	 * and {@link VX509Certificate#signTBSCertificate(byte[], RSAPrivateKey)}.</p>
	 *
	 * <p>Similar to {@link #selfSign} with the difference that this method automatically
	 * generates a set of root CA certificate extensions and appends to the other
	 * provided extensions (which must not conflict with generated CA extensions).</p>
	 *
	 * <p>Currently ignores the 'attributes' property of the certification request.</p>
	 *
	 * @return signed certificate
	 * @throws VCryptoException unable to create certificate
	 */
	public VX509Certificate selfSignCA(int version, VInteger serial, RSAPrivateKey signatureKey,
							     VASN1Time notBefore, VASN1Time notAfter, VBitfield uniqueID,
							     int pathLength, int usageBits, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		LinkedList<VX509CertificateExtension> exts = new LinkedList<VX509CertificateExtension>();
		for (VX509CertificateExtension _ext: VX509CertificateExtension.createCAExtensions(this.subjectKey, pathLength, usageBits))
			exts.addLast(_ext);
		if (extensions != null) {
			for (VX509CertificateExtension _ext	: extensions)
				for (VX509CertificateExtension _ext2 : exts)
					if (_ext.getOid().equals(_ext2.getOid()))
						throw new VCryptoException("Generated CA extensions conflict with provided extensions");
			for (VX509CertificateExtension _ext	: extensions)
				exts.addLast(_ext);
		}
		return this.selfSign(version, serial, signatureKey, notBefore, notAfter, uniqueID, exts.toArray(new VX509CertificateExtension[0]));
	}

	/**
	 * Import certification request from PKCS#10 DER data.
	 *
	 * @param der DER data
	 * @return imported request
	 * @throws VCryptoException unable to import
	 */
	public static VX509CertificationRequest importDer(byte[] der)
			throws VCryptoException {
		return new VX509CertificationRequest(der);
	}

	/**
	 * Import certification request from PKCS#10 ASN.1 data.
	 *
	 * @param data ASN.1 request data
	 * @return imported request
	 * @throws VCryptoException unable to import
	 */
	public static VX509CertificationRequest importASN1(VASN1Base data)
			throws VCryptoException {
		try {
			return new VX509CertificationRequest(data.encodeDER());
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Import certification request from ASCII armored PKCS#10 DER data.
	 *
	 * @param data armored DER data
	 * @return imported request
	 * @throws VCryptoException unable to import
	 */
	public static VX509CertificationRequest importArmored(byte[] data)
			throws VCryptoException {
		byte[] der;
		try {
			der = VBase64.decodeBlock("CERTIFICATE REQUEST", data);
		} catch (VBase64Exception e) {
			throw new VCryptoException(e);
		}
		return new VX509CertificationRequest(der);
	}

	/**
	 * Returns a DER representation of the request.
	 *
	 * @return DER representation
	 */
	public byte[] exportDer() {
		byte[] result = new byte[der.length];
		for (int i = 0; i < result.length; i++)
			result[i] = der[i];
		return der;
	}

	/**
	 * Returns an ASN.1 representation of the request.
	 *
	 * @return ASN.1 representation
	 */
	public VASN1Sequence exportASN1() {
		try {
			return (VASN1Sequence) new org.versile.crypto.asn1.pkcs.CertificationRequest().parseDER(der).getResult();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns an ASCII-armored representation of the request.
	 *
	 * @return ASCII-armored representation
	 */
	public byte[] exportArmored() {
		try {
			return VBase64.encodeBlock("CERTIFICATE REQUEST", der);
		} catch (VBase64Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get request version number.
	 *
	 * @return version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Get request subject.
	 *
	 * @return subject
	 */
	public X500Principal getSubject() {
		return subject;
	}

	/**
	 * Get request subject public key.
	 *
	 * @return subject public key
	 */
	public RSAPublicKey getSubjectKey() {
		return subjectKey;
	}

	/**
	 * Get request attributes.
	 *
	 * @return 'attributes' parameter of request
	 */
	public VASN1Base getAttributes() {
		return attributes;
	}

	/**
	 * Get identifier of signature algorithm.
	 *
	 * @return identifier
	 */
	public VObjectIdentifier getSignAlgorithm() {
		return signAlgorithm;
	}

	/**
	 * Get request signature.
	 *
	 * @return signature
	 */
	public VBitfield getSignature() {
		return signature;
	}

}
