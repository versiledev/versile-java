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

import javax.security.auth.x500.X500Principal;

import org.versile.common.asn1.VASN1Base;
import org.versile.common.asn1.VASN1DefUnknown;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1OctetString;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.asn1.VASN1Tag;
import org.versile.common.asn1.VASN1Tagged;
import org.versile.common.asn1.VASN1Unknown;
import org.versile.common.asn1.VASN1Definition.ParseResult;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.asn1.cert.AuthorityKeyIdentifier;
import org.versile.crypto.asn1.cert.CertificateSerialNumber;
import org.versile.crypto.asn1.cert.KeyIdentifier;
import org.versile.orb.entity.VInteger;



/**
 * An X.509 AuthorityKeyIdentifier extension.
 */
public class VX509AuthorityKeyIdentifier extends VX509CertificateExtension {

    /**
     * Associated extension identifier.
     */
    public static VObjectIdentifier IDENTIFIER = AuthorityKeyIdentifier.ID_CE;

    byte[] identifier;
    X500Principal issuer;
    VInteger serial;


	/**
	 * Set up extension.
	 *
	 * <p>Issuer and serial must either both be set or both be null. If issuer and serial are set,
	 * 'identifier' must also be set.</p>
	 *
	 * @param identifier authority key identifier (or null)
	 * @param issuer certificate issuer (or null)
	 * @param serial certificate serial number (or null)
	 * @throws IllegalArgumentException mismatching issuer and serial parameters
	 */
	public VX509AuthorityKeyIdentifier(byte[] identifier, X500Principal issuer, VInteger serial)
			throws IllegalArgumentException {
		super(IDENTIFIER, false);
		if (identifier == null && issuer != null)
			throw new IllegalArgumentException("Cannot set issuer/serial without also setting identifier");
		if ((issuer == null) ^ (serial == null))
			throw new IllegalArgumentException("issuer and serial must either both be null or both set");
		if (identifier != null) {
			this.identifier = new byte[identifier.length];
			for (int i = 0; i < identifier.length; i++)
				this.identifier[i] = identifier[i];
		}
		else
			this.identifier = null;
		this.issuer = issuer;
		this.serial = serial;

		try {
			VASN1Sequence asn1 = new AuthorityKeyIdentifier().create();
			if (identifier != null) {
				VASN1Tagged _tagged = new VASN1Tagged(new KeyIdentifier().create(identifier), new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 0), true);
				asn1.append(_tagged, "keyIdentifier");
			}
			if (issuer != null) {
				VASN1Unknown _val = (VASN1Unknown) new VASN1DefUnknown().parseDER(issuer.getEncoded()).getResult();
				VASN1Tagged _tagged = new VASN1Tagged(_val, new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 1), true);
				asn1.append(_tagged, "authorityCertIssuer");
				_tagged = new VASN1Tagged(new CertificateSerialNumber().create(serial), new VASN1Tag(VASN1Tag.TagClass.CONTEXT, 1), true);
				asn1.append(_tagged, "authorityCertSerialNumber");
				this.setValue(asn1.encodeDER());
			}
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get authority key identifier
	 *
	 * @return key identifier (or null)
	 */
	public byte[] getIdentifier() {
		return identifier;
	}


	/**
	 * Get associated issuer
	 *
	 * @return issuer (or null)
	 */
	public X500Principal getIssuer() {
		return issuer;
	}


	/**
	 * Get associated serial number
	 *
	 * @return serial number (or null)
	 */
	public VInteger getSerial() {
		return serial;
	}

	/**
	 * Create extension by parsing extension's DER value.
	 *
	 * @param isCritical true if extension is critical
	 * @param der DER representation of extension value
	 * @return parsed certificate
	 * @throws VCryptoException unable to create extension (e.g. extension type not supported)
	 */
	public static VX509AuthorityKeyIdentifier parse(boolean isCritical, byte[] der)
			throws VCryptoException {
		if (isCritical)
			throw new VCryptoException("Extension requires 'critical' to be false");

		ParseResult _dec;
		try {
			_dec = new AuthorityKeyIdentifier().parseDER(der);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		if (_dec.getNumRead() != der.length)
			throw new VCryptoException("Bad extension value DER encoding");

		VASN1Sequence asn1 = (VASN1Sequence) _dec.getResult();
		VASN1OctetString _id = (VASN1OctetString) asn1.get("keyIdentifier");
		if (_id == null)
			return new VX509AuthorityKeyIdentifier(null, null, null);

		VASN1Base _issuer = asn1.get("authorityCertIssuer");
		if (_issuer == null)
			return new VX509AuthorityKeyIdentifier(_id.getValue(), null, null);

		VASN1Integer _serial = (VASN1Integer) asn1.get("authorityCertSerialNumber");
		if (_serial == null)
			throw new VCryptoException("encoding has issuer without serial");
		X500Principal _xissuer;
		try {
			_xissuer = new X500Principal(_issuer.encodeDER());
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		} catch (IllegalArgumentException e) {
			throw new VCryptoException(e);
		}
		return new VX509AuthorityKeyIdentifier(_id.getValue(), _xissuer, _serial.getValue());
	}

}
