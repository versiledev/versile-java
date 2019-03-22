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

import org.versile.common.asn1.VASN1BitString;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Definition.ParseResult;
import org.versile.common.util.VBitfield;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.asn1.cert.KeyUsage;



/**
 * An X.509 KeyUsage extension.
 */
public class VX509KeyUsage extends VX509CertificateExtension {

    /**
     * Associated extension identifier.
     */
    public static VObjectIdentifier IDENTIFIER = KeyUsage.ID_CE;

	/**
	 * Extension flag for digitalSignature.
	 */
	public static int DIGITAL_SIGNATURE = 0x100;

	/**
	 * Extension flag for nonRepudiation.
	 */
	public static int NON_REPUDIATION = 0x080;

	/**
	 * Extension flag for keyEncipherment.
	 */
	public static int KEY_ENCIPHERMENT = 0x040;

	/**
	 * Extension flag for dataEncipherment.
	 */
	public static int DATA_ENCIPHERMENT = 0x020;

	/**
	 * Extension flag for keyAgreement.
	 */
	public static int KEY_AGREEMENT = 0x010;

	/**
	 * Extension flag for keyCertSign.
	 */
	public static int KEY_CERT_SIGN = 0x008;

	/**
	 * Extension flag for cRLSign.
	 */
	public static int CRL_SIGN = 0x004;

	/**
	 * Extension flag for encipherOnly.
	 */
	public static int ENCIPHER_ONLY = 0x002;

	/**
	 * Extension flag for decipherOnly.
	 */
	public static int DECIPHER_ONLY = 0x001;

	int usageBits;

	/**
	 * Set up extension.
	 *
	 * <p>The 'bits' parameter should be a bitwise OR of the static extension flags
	 * set on the class.</p>
	 *
	 * @param bits logical OR of key usage flags
	 */
	public VX509KeyUsage(int bits) {
		super(VX509KeyUsage.IDENTIFIER, true);
		if (bits < 0 || bits > 0x1ff)
			throw new IllegalArgumentException("Invalid key usage bits");
		usageBits = bits;

		VBitfield _bits = VBitfield.fromNumber(usageBits).newLength(9);
		VASN1BitString asn1 = new KeyUsage().create(_bits);
		try {
			this.setValue(asn1.encodeDER());
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get bitwise OR of set key usage bits.
	 *
	 * @return key usage bits
	 */
	public int getUsageBits() {
		return usageBits;
	}

	/**
	 * Create extension by parsing extension's DER value.
	 *
	 * @param isCritical true if extension is critical
	 * @param der DER representation of extension value
	 * @return parsed certificate
	 * @throws VCryptoException unable to create extension (e.g. extension type not supported)
	 */
	public static VX509KeyUsage parse(boolean isCritical, byte[] der)
			throws VCryptoException {
		if (!isCritical)
			throw new VCryptoException("Key Usage extension must have 'critical' set to true");

		ParseResult _dec;
		try {
			_dec = new KeyUsage().parseDER(der);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		if (_dec.getNumRead() != der.length)
			throw new VCryptoException("Bad extension value DER encoding");

		VBitfield bits = ((VASN1BitString)_dec.getResult()).getValue();
		if (bits.getLength() != 9)
			throw new VCryptoException("Invalid key usage bits");
		int flags = bits.toInteger().getValue().intValue();
		return new VX509KeyUsage(flags);
	}

	/**
	 * Checks whether 'digitalSignature' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasDigitalSignature() {
		return ((usageBits & DIGITAL_SIGNATURE) != 0);
	}

	/**
	 * Checks whether 'nonRepudiation' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasNonRepidiation() {
		return ((usageBits & NON_REPUDIATION) != 0);
	}

	/**
	 * Checks whether 'keyEncipherment' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasKeyEncipherment() {
		return ((usageBits & KEY_ENCIPHERMENT) != 0);
	}

	/**
	 * Checks whether 'dataEncipherment' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasDataEncipherment() {
		return ((usageBits & DATA_ENCIPHERMENT) != 0);
	}

	/**
	 * Checks whether 'keyAgreement' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasKeyAgreement() {
		return ((usageBits & KEY_AGREEMENT) != 0);
	}

	/**
	 * Checks whether 'keyCertSign' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasKeyCertSign() {
		return ((usageBits & KEY_CERT_SIGN) != 0);
	}

	/**
	 * Checks whether 'cRLSign' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasCRLSign() {
		return ((usageBits & CRL_SIGN) != 0);
	}

	/**
	 * Checks whether 'encipherOnly' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasEncipherOnly() {
		return ((usageBits & ENCIPHER_ONLY) != 0);
	}

	/**
	 * Checks whether 'decipherOnly' key usage property is set.
	 *
	 * @return true if property is set
	 */
	public boolean hasDecipherOnly() {
		return ((usageBits & DECIPHER_ONLY) != 0);
	}
}
