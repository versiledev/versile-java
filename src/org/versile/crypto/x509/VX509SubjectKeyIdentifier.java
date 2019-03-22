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

import java.security.interfaces.RSAPublicKey;

import org.versile.common.asn1.VASN1BitString;
import org.versile.common.asn1.VASN1Definition;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1OctetString;
import org.versile.common.asn1.VASN1Definition.DecodedTaggedContent;
import org.versile.common.asn1.VASN1Definition.ParseResult;
import org.versile.common.util.VBitfield;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VHash;
import org.versile.crypto.VRSAKeyPair;
import org.versile.crypto.asn1.cert.SubjectKeyIdentifier;



/**
 * An X.509 SubjectKeyIdentifier extension.
 */
public class VX509SubjectKeyIdentifier extends VX509CertificateExtension {

    /**
     * Associated extension identifier.
     */
    public static VObjectIdentifier IDENTIFIER = SubjectKeyIdentifier.ID_CE;

    byte[] identifier;

	/**
	 * Set up extension.
	 *
	 * @param identifier key identifier
	 */
	public VX509SubjectKeyIdentifier(byte[] identifier) {
		super(VX509SubjectKeyIdentifier.IDENTIFIER, false);
		this.identifier = new byte[identifier.length];
		for (int i = 0; i < identifier.length; i++)
			this.identifier[i] = identifier[i];
		VASN1OctetString asn1 = new SubjectKeyIdentifier().create(this.identifier);
		try {
			this.setValue(asn1.encodeDER());
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create extension by parsing extension's DER value.
	 *
	 * @param isCritical true if extension is critical
	 * @param der DER representation of extension value
	 * @return parsed certificate
	 * @throws VCryptoException unable to create extension (e.g. extension type not supported)
	 */
	public static VX509SubjectKeyIdentifier parse(boolean isCritical, byte[] der)
			throws VCryptoException {
		ParseResult _dec;
		try {
			_dec = new SubjectKeyIdentifier().parseDER(der);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		if (_dec.getNumRead() != der.length)
			throw new VCryptoException("Bad extension value DER encoding");

		byte[] identifier = ((VASN1OctetString) _dec.getResult()).getValue();
		return new VX509SubjectKeyIdentifier(identifier);
	}

	/**
	 * Generates key identifier octets for a public key.
	 *
	 * <p>Implements the key identifier generation scheme (1) defined
	 * in RDC 5280 section 4.2.1.2.</p>
	 *
	 * @param key input key
	 * @return generated identifier
	 */
	public static byte[] generateKeyIdentifier(RSAPublicKey key) {
		try {
			byte[] der = VRSAKeyPair.exportDerPkcs(key);
			VBitfield bits = VBitfield.fromOctets(der);
			byte[] bits_der = new VASN1BitString(bits).encodeDER();
			DecodedTaggedContent _dec = VASN1Definition.berDecTaggedContent(bits_der);
			byte[] content = _dec.getContent();
			byte[] hash_input = new byte[content.length-1];
			for (int i = 0; i < hash_input.length; i++)
				hash_input[i] = content[i+1];
			return VHash.getHashGenerator("sha1").digestOf(_dec.getContent());
		} catch (Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the associated subject key identifier.
	 *
	 * @return key identifier
	 */
	byte[] getKeyIdentifier() {
		byte[] result = new byte[identifier.length];
		for (int i = 0; i < result.length; i++)
			result[i] = identifier[i];
		return result;
	}
}
