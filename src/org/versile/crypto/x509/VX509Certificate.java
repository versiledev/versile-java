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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.security.auth.x500.X500Principal;

import org.versile.common.asn1.VASN1Base;
import org.versile.common.asn1.VASN1BitString;
import org.versile.common.asn1.VASN1Boolean;
import org.versile.common.asn1.VASN1DefUnknown;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1GeneralizedTime;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1Null;
import org.versile.common.asn1.VASN1ObjectIdentifier;
import org.versile.common.asn1.VASN1OctetString;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.asn1.VASN1SequenceOf;
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
import org.versile.crypto.asn1.cert.Certificate;
import org.versile.crypto.asn1.cert.CertificateSerialNumber;
import org.versile.crypto.asn1.cert.Extension;
import org.versile.crypto.asn1.cert.Extensions;
import org.versile.crypto.asn1.cert.SubjectPublicKeyInfo;
import org.versile.crypto.asn1.cert.TBSCertificate;
import org.versile.crypto.asn1.cert.Validity;
import org.versile.crypto.asn1.cert.Version;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VInteger;



/**
 * An X.509 certificate as specified by RFC 5280.
 */
public class VX509Certificate {

	byte[] der;
	VASN1Sequence asn1;

	int version;
	VInteger serial;
	VObjectIdentifier signAlgorithm;
	X500Principal issuer;
	VASN1Time notValidBefore;
	VASN1Time notValidAfter;
	X500Principal subject;
	RSAPublicKey subjectKey;
	VBitfield issuerUnique = null;
	VBitfield subjectUnique = null;
	VX509CertificateExtension[] extensions;
	VBitfield signature;

	/**
	 * Initialize from certificate ASN.1 data.
	 *
	 * @param der DER encoded certificate data
	 * @throws VCryptoException could not resolve ASN.1 data
	 */
	protected VX509Certificate(byte[] der)
			throws VCryptoException {
		try {
			ParseResult _dec = new org.versile.crypto.asn1.cert.Certificate().parseDER(der);
			if (_dec.getNumRead() != der.length)
				throw new VCryptoException("Invalid DER encoding");
			asn1 = (VASN1Sequence)_dec.getResult();

			this.der = new byte[der.length];
			for (int i = 0; i < der.length; i++)
				this.der[i] = der[i];

			VASN1Sequence tbs_cert = (VASN1Sequence) asn1.get("TbsCertificate");
			VASN1Sequence sign_alg = (VASN1Sequence) asn1.get("signatureAlgorithm");
			VASN1BitString sign_val = (VASN1BitString) asn1.get("signatureValue");

			VASN1Tagged _t_version = (VASN1Tagged) tbs_cert.get("version");
			version = ((VASN1Integer)_t_version.getValue()).getValue().getValue().intValue();
			if (version < 0 || version > 2)
				throw new VCryptoException("Invalid certificate version");

			serial = ((VASN1Integer) tbs_cert.get("serialNumber")).getValue();

			VASN1Sequence _sign_alg = (VASN1Sequence) tbs_cert.get("signature");
			signAlgorithm = ((VASN1ObjectIdentifier)_sign_alg.get("algorithm")).getValue();
			if (!(_sign_alg.get("parameters") instanceof VASN1Null))
				throw new VCryptoException("Invalid signature algorithm format");

			VASN1Base _issuer = tbs_cert.get("issuer");
			issuer = new X500Principal(_issuer.encodeDER());

			VASN1Sequence _validity = (VASN1Sequence)tbs_cert.get("validity");
			notValidBefore = (VASN1Time)_validity.get("notBefore");
			notValidAfter = (VASN1Time)_validity.get("notAfter");

			VASN1Base _subject = tbs_cert.get("name");
			subject = new X500Principal(_subject.encodeDER());

			VASN1Base _spki = tbs_cert.get("subjectPublicKeyInfo");
			subjectKey = VRSAKeyPair.importPublicDerSpki(_spki.encodeDER());

			// Read optional issuer/subject unique ID values
			VASN1Tagged _t_unique = (VASN1Tagged) tbs_cert.get("issuerUniqueId");
			if (_t_unique != null) {
				VASN1BitString _unique = (VASN1BitString) _t_unique.getValue();
				if (!(version == 1 || version == 2))
					throw new VCryptoException("Unique ID requires X.509 v2 or v3");
				issuerUnique = _unique.getValue();
			}
			_t_unique = (VASN1Tagged) tbs_cert.get("subjectUniqueId");
			if (_t_unique != null) {
				VASN1BitString _unique = (VASN1BitString) _t_unique.getValue();
				if (!(version == 1 || version == 2))
					throw new VCryptoException("Unique ID requires X.509 v2 or v3");
				subjectUnique = _unique.getValue();
			}

			// Read certificate extensions
			extensions = new VX509CertificateExtension[0];
			VASN1Tagged _t_extensions = (VASN1Tagged) tbs_cert.get("extensions");
			if (_t_extensions != null) {
				VASN1SequenceOf _extensions = (VASN1SequenceOf) _t_extensions.getValue();
				if (_extensions != null) {
					LinkedList<VX509CertificateExtension> _ext_list = new LinkedList<VX509CertificateExtension>();
					for (VASN1Base _ext: _extensions)
						_ext_list.addLast(VX509CertificateExtension.parse((VASN1Sequence)_ext));
					extensions = _ext_list.toArray(new VX509CertificateExtension[0]);
				}
			}

			// Decode certificate signature
			VObjectIdentifier _alg = ((VASN1ObjectIdentifier) sign_alg.get("algorithm")).getValue();;
			if (!(sign_alg.get("parameters") instanceof VASN1Null))
				throw new VCryptoException("Invalid signature algorithm encoding");
			if (!_alg.equals(signAlgorithm))
				throw new VX509InvalidSignature("Signature algorithm mismatch");
			signature = sign_val.getValue();
		} catch (VCryptoException e) {
			throw e;
		} catch (Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Returns a DER representation of the certificate.
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
	 * Returns an ASN.1 representation of the certificate.
	 *
	 * @return ASN.1 representation
	 */
	public VASN1Sequence exportASN1() {
		try {
			return (VASN1Sequence) new Certificate().parseDER(der).getResult();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns an ASCII-armored representation of the certificate.
	 *
	 * @return ASCII-armored representation
	 */
	public byte[] exportArmored() {
		try {
			return VBase64.encodeBlock("CERTIFICATE", der);
		} catch (VBase64Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Verify the key that was used for signing the certificate.
	 *
	 * @param signingKey signing key to test
	 * @return true if provided key matches with certificate's signature
	 * @throws VCryptoException unable to resolve
	 */
	public boolean isSignedWith(RSAPublicKey signingKey)
			throws VCryptoException {
		if (!signAlgorithm.equals(VRSACipher.SHA1_WITH_RSA_SIGNATURE))
			throw new VCryptoException("Can only validate RSASSA PKCS #1 v1.5 Verify signatures");
		byte[] msg;
		try {
			msg = asn1.get("TbsCertificate").encodeDER();
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		byte[] signature = this.signature.toOctets();
		return VRSACipher.rsassa_pkcs1_v1_5_verify(signingKey, VHash.getHashGenerator("sha1"), msg, signature);
	}


	/**
	 * Checks whether this certificate is certified by a provided certificate.
	 *
	 * <p>Performs a check which includes validation of issuer certificate against
	 * current time, and validation of issuer certification rights and CA rights
	 * set on issuer certificate.</p>
	 *
	 * @param certificate issuer certificate
	 * @return true if this certificate is certified by provided certificate
	 * @throws VCryptoException unable to perform certification check
	 */
	boolean certifiedBy(VX509Certificate certificate)
					throws VCryptoException {
		return this.certifiedBy(certificate, true, null, true, true);
	}

	/**
	 * Checks whether this certificate is certified by a provided certificate.
	 *
	 * @param certificate issuer certificate
	 * @param verifyTime if true validate issuer certificate timestamp
	 * @param time time stamp to validate issuer certificate against (if null use current time)
	 * @param validateExt if true validate issuer certificate has extensions granting it certifying rights
	 * @param validateCA if true validate issuer certificate has CA signature permission
	 * @return true if this certificate is certified by provided certificate
	 * @throws VCryptoException unable to perform certification check
	 */
	boolean certifiedBy(VX509Certificate certificate, boolean verifyTime, Date time,
			boolean validateExt, boolean validateCA)
					throws VCryptoException {

		// Verify match with issuer's key
		if (!this.isSignedWith(certificate.getSubjectKey()))
			return false;

		// Verify matching issuer principals
		if (!this.getIssuer().equals(certificate.getSubject()))
			return false;

		// Verify matching key identifiers
		Hashtable<VObjectIdentifier, VX509CertificateExtension> issuer_ext = new Hashtable<VObjectIdentifier, VX509CertificateExtension>();
		for (VX509CertificateExtension _ext: certificate.getExtensions())
			issuer_ext.put(_ext.getOid(), _ext);
		Hashtable<VObjectIdentifier, VX509CertificateExtension> subject_ext = new Hashtable<VObjectIdentifier, VX509CertificateExtension>();
		for (VX509CertificateExtension _ext: this.getExtensions())
			subject_ext.put(_ext.getOid(), _ext);
		if (issuer_ext.contains(VX509SubjectKeyIdentifier.IDENTIFIER)
			|| subject_ext.contains(VX509AuthorityKeyIdentifier.IDENTIFIER)) {
			VX509SubjectKeyIdentifier ski = (VX509SubjectKeyIdentifier) issuer_ext.get(VX509SubjectKeyIdentifier.IDENTIFIER);
			VX509AuthorityKeyIdentifier aki = (VX509AuthorityKeyIdentifier) subject_ext.get(VX509AuthorityKeyIdentifier.IDENTIFIER);
			if (ski == null || aki == null)
				return false;
			if (!(new VBytes(ski.getKeyIdentifier()).equals(new VBytes(aki.getIdentifier()))))
				return false;
		}

		// Verify timestamp (with second precision)
		if (verifyTime) {
			if (time == null)
				time = new Date();
			if (certificate.notValidBefore.getDate().after(time))
				return false;
			if (certificate.notValidAfter.getDate().before(time))
				return false;
		}

		// Validate issuer's signature rights
		if (validateCA) {
			VX509BasicConstraint basic = (VX509BasicConstraint) issuer_ext.get(VX509BasicConstraint.IDENTIFIER);
			VX509SubjectKeyIdentifier ski = (VX509SubjectKeyIdentifier) issuer_ext.get(VX509SubjectKeyIdentifier.IDENTIFIER);
			VX509KeyUsage usage = (VX509KeyUsage) issuer_ext.get(VX509KeyUsage.IDENTIFIER);
			if (basic == null || ski == null || usage == null)
				return false;
			if (!basic.isCertificateAuthority())
				return false;
			if (!usage.hasKeyCertSign())
				return false;
		}

		return true;
	}

	/**
	 * Creates a certificate from a TBS Certificate structure.
	 *
	 * @param tbsCertificate certificate data to sign (as DER structure)
	 * @param signatureKey issuer's signing key
	 * @return certificate
	 * @throws VCryptoException
	 */
	public static VX509Certificate signTBSCertificate(byte[] tbsCertificate, RSAPrivateKey signatureKey)
			throws VCryptoException {

		try {
			// Reconstruct TBS Certificate ASN.1 structure
			ParseResult _dec = new TBSCertificate().parseDER(tbsCertificate);
			if (_dec.getNumRead() != tbsCertificate.length)
				throw new VCryptoException("TBS Certificate data did not properly resolve");
			VASN1Sequence tbs_cert = (VASN1Sequence) _dec.getResult();

			// Compute signature
			VObjectIdentifier _alg_id = ((VASN1ObjectIdentifier)((VASN1Sequence)tbs_cert.get("signature")).get("algorithm")).getValue();
			if (!_alg_id.equals(VRSACipher.SHA1_WITH_RSA_SIGNATURE))
				throw new VCryptoException("Target signature algorithm not supported");
			byte[] msg = tbsCertificate;
			byte[] signature = VRSACipher.rsassa_pkcs1_v1_5_sign(signatureKey, VHash.getHashGenerator("sha1"), msg);

			// Create the certificate
			VASN1Sequence cert = new Certificate().create();
			cert.append(tbs_cert, "TbsCertificate");
			VASN1Sequence _alg = new AlgorithmIdentifier().create();
			_alg.append(new VASN1ObjectIdentifier(VRSACipher.SHA1_WITH_RSA_SIGNATURE), "algorithm");
			_alg.append(new VASN1Null(), "parameters");
			cert.append(_alg, "signatureAlgorithm");
			cert.append(new VASN1BitString(VBitfield.fromOctets(signature)), "signature");

			return VX509Certificate.importASN1(cert);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Creates an ASN.1 TBS Certificate object for provided arguments.
	 *
	 * <p>'version' is the actual X.509 version (represented as version-1 in the
	 * TBS Certificate structure).</p>
	 *
	 * @param version X.509 certificate version (automatically set if negative)
	 * @param serial serial number
	 * @param issuer issuer
	 * @param notBefore earliest time certificate is valid
	 * @param notAfter latest time certificate is valid
	 * @param subject subject
	 * @param subjectKey subject public key
	 * @param issuerUnique issuer unique ID (or null)
	 * @param subjectUnique subject unique ID (or null)
	 * @param extensions certificate extensions (or null)
	 * @return ASN.1 TBS Certificate structure for an associated certificate
	 * @throws VCryptoException unable to create certificate data
	 */
	public static VASN1Sequence createTbsCertificate(int version, VInteger serial, X500Principal issuer, Date notBefore,
													 Date notAfter, X500Principal subject, RSAPublicKey subjectKey,
													 VBitfield issuerUnique, VBitfield subjectUnique, VX509CertificateExtension[] extensions)
			throws VCryptoException {
		try {
			return VX509Certificate.createTbsCertificate(version, serial, issuer, new VASN1GeneralizedTime(notBefore), new VASN1GeneralizedTime(notAfter),
										                 subject, subjectKey, issuerUnique, subjectUnique, extensions);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Creates an ASN.1 TBS Certificate object for provided arguments.
	 *
	 * <p>'version' is the actual X.509 version (represented as version-1 in the
	 * TBS Certificate structure).</p>
	 *
	 * @param version X.509 certificate version (automatically set if negative)
	 * @param serial serial number
	 * @param issuer issuer
	 * @param notBefore earliest time certificate is valid
	 * @param notAfter latest time certificate is valid
	 * @param subject subject
	 * @param subjectKey subject public key
	 * @param issuerUnique issuer unique ID (or null)
	 * @param subjectUnique subject unique ID (or null)
	 * @param extensions certificate extensions (or null)
	 * @return ASN.1 TBS Certificate structure for an associated certificate
	 * @throws VCryptoException unable to create certificate data
	 */
	public static VASN1Sequence createTbsCertificate(int version, VInteger serial, X500Principal issuer, VASN1Time notBefore,
													 VASN1Time notAfter, X500Principal subject, RSAPublicKey subjectKey,
													 VBitfield issuerUnique, VBitfield subjectUnique, VX509CertificateExtension[] extensions)
			throws VCryptoException {

		VASN1Sequence tbs = new TBSCertificate().create();

		try {

			if (version < 0) {
				if (extensions != null && extensions.length > 0)
					version = 3;
				else if (issuerUnique != null || subjectUnique != null)
					version = 2;
				else
					version = 3;
			}
			if (version < 1 || version > 3)
				throw new VCryptoException("Invalid X.509 version number");
			version -= 1;
			VASN1Tagged _tagged = new VASN1Tagged(new Version().create(new VInteger(version)), new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 0), true);
			tbs.append(_tagged, "version");

			tbs.append(new CertificateSerialNumber().create(serial), "serialNumber");

			VASN1Sequence _sig = new AlgorithmIdentifier().create();
			_sig.append(new VASN1ObjectIdentifier(VRSACipher.SHA1_WITH_RSA_SIGNATURE), "algorithm");
			_sig.append(new VASN1Null(), "parameters");
			tbs.append(_sig, "signature");

			tbs.append(new VASN1DefUnknown().parseDER(issuer.getEncoded()).getResult(), "issuer");

			VASN1Sequence _validity = new Validity().create();
			_validity.append(notBefore.getTimeObject(), "notBefore");
			_validity.append(notAfter.getTimeObject(), "notAfter");
			tbs.append(_validity, "validity");

			tbs.append(new VASN1DefUnknown().parseDER(subject.getEncoded()).getResult(), "subject");

			VASN1Sequence _spki = new SubjectPublicKeyInfo().create();
			VASN1Sequence _alg = new AlgorithmIdentifier().create();
			_alg.append(new VASN1ObjectIdentifier(VRSAKeyPair.RSA_ENCRYPTION), "algorithm");
			_alg.append(new VASN1Null(), "parameters");
			_spki.append(_alg, "algorithm");
			_spki.append(new VASN1BitString(VBitfield.fromOctets(VRSAKeyPair.exportDerPkcs(subjectKey))), "subjectPublicKey");
			tbs.append(_spki, "subjectPublicKeyInfo");

			if (issuerUnique != null) {
				_tagged = new VASN1Tagged(new VASN1BitString(issuerUnique), new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 1), true);
				tbs.append(_tagged, "issuerUniqueId");
			}

			if (subjectUnique != null) {
				_tagged = new VASN1Tagged(new VASN1BitString(subjectUnique), new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 2), true);
				tbs.append(_tagged, "subjectUniqueId");
			}

			if (extensions != null && extensions.length > 0) {
				VASN1SequenceOf _exts = new Extensions().create();
				for (VX509CertificateExtension _xt: extensions) {
					VASN1Sequence _axt = new Extension().create();
					_axt.append(new VASN1ObjectIdentifier(_xt.getOid()), "extnID");
					_axt.append(new VASN1Boolean(_xt.isCritical()), "critical", !_xt.isCritical());
					_axt.append(new VASN1OctetString(_xt.getDer()), "extnValue");
					_exts.append(_axt);
				}
				_tagged = new VASN1Tagged(_exts, new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 3), true);
				tbs.append(_tagged, "extensions");
			}

			return tbs;

		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}

	}

	/**
	 * Imports certificate from a DER representation.
	 *
	 * @param der DER representation
	 * @return imported certificate
	 * @throws VCryptoException import error
	 */
	public static VX509Certificate importDer(byte[] der)
			throws VCryptoException {
		return new VX509Certificate(der);
	}

	/**
	 * Imports certificate from am ASN.1 representation.
	 *
	 * @param data ASN.1 representation
	 * @return imported certificate
	 * @throws VCryptoException import error
	 */
	public static VX509Certificate importASN1(VASN1Base data)
			throws VCryptoException {
		try {
			return VX509Certificate.importDer(data.encodeDER());
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports certificate from an ASCII armored representation.
	 *
	 * @param data DER representation
	 * @return imported certificate
	 * @throws VCryptoException import error
	 */
	public static VX509Certificate importArmored(byte[] data)
			throws VCryptoException {
		try {
			return new VX509Certificate(VBase64.decodeBlock("CERTIFICATE", data));
		} catch (VBase64Exception e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Imports a native Java X.509 certificate.
	 *
	 * @param certificate native representation
	 * @return imported representation
	 * @throws VCryptoException import error
	 */
	public static VX509Certificate importNative(X509Certificate certificate)
			throws VCryptoException {
		try {
			return VX509Certificate.importDer(certificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * Generate a Java-native version of the certificate.
	 *
	 * @return alternative representation
	 * @throws VCryptoException certificate translation error
	 */
	public X509Certificate getNative()
			throws VCryptoException {
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			InputStream data_as_stream = new ByteArrayInputStream(der);
			return (X509Certificate) cf.generateCertificate(data_as_stream);
		} catch (CertificateException e) {
			throw new VCryptoException(e);
		}
	}

	/**
	 * X.509 certificate version
	 *
	 * <p>Returned value is one higher than the ASN.1 encoded value, e.g. a "v3" certificate
	 * is returned as the number '3', whereas it is encoded as two in the ASN.1 structure.</p>
	 *
	 * @return certificate version
	 */
	public int getVersion() {
		return version+1;
	}

	/**
	 * Get certificate serial number.
	 *
	 * @return serial number
	 */
	public VInteger getSerial() {
		return serial;
	}

	/**
	 * Get ID of certificate signature algorithm.
	 *
	 * @return signature algorithm ID
	 */
	public VObjectIdentifier getSignAlgorithm() {
		return signAlgorithm;
	}

	/**
	 * Get certificate issuer.
	 *
	 * @return issuer
	 */
	public X500Principal getIssuer() {
		return issuer;
	}

	/**
	 * Get earliest time certificate is valid.
	 *
	 * @return time reference
	 */
	public VASN1Time getNotValidBefore() {
		return notValidBefore;
	}

	/**
	 * Get latest time certificate is valid.
	 *
	 * @return time reference
	 */
	public VASN1Time getNotValidAfter() {
		return notValidAfter;
	}

	/**
	 * Get certificate subject.
	 *
	 * @return subject
	 */
	public X500Principal getSubject() {
		return subject;
	}

	/**
	 * Get subject's public key.
	 *
	 * @return subject key
	 */
	public RSAPublicKey getSubjectKey() {
		return subjectKey;
	}

	/**
	 * Get unique issuer ID
	 *
	 * @return unique ID (or null)
	 */
	public VBitfield getIssuerUnique() {
		return issuerUnique;
	}

	/**
	 * Get unique subject ID
	 *
	 * @return unique ID (or null)
	 */
	public VBitfield getSubjectUnique() {
		return subjectUnique;
	}

	/**
	 * Get certificate's extensions
	 *
	 * @return extensions
	 */
	public VX509CertificateExtension[] getExtensions() {
		return extensions;
	}

	/**
	 * Get certificate's signature.
	 *
	 * @return signature
	 */
	public VBitfield getSignature() {
		return signature;
	}

}
