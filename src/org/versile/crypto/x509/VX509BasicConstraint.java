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

import org.versile.common.asn1.VASN1Boolean;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.asn1.VASN1Integer;
import org.versile.common.asn1.VASN1Sequence;
import org.versile.common.asn1.VASN1Definition.ParseResult;
import org.versile.common.util.VObjectIdentifier;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.asn1.cert.BasicConstraints;



/**
 * An X.509 BasicConstraint extension.
 */
public class VX509BasicConstraint extends VX509CertificateExtension {

    /**
     * Associated extension identifier.
     */
    public static VObjectIdentifier IDENTIFIER = BasicConstraints.ID_CE;

	boolean certificateAuthority;
	int maxPathLength;

	/**
	 * Set up extension.
	 *
	 * @param isCA if true public key belongs to a CA
	 * @param isCritical if true the extension is critical
	 * @param maxPathLength maximum following certificates (no limit if negative)
	 */
	public VX509BasicConstraint(boolean isCA, boolean isCritical, int maxPathLength) {
		super(VX509BasicConstraint.IDENTIFIER, isCritical);
		certificateAuthority = isCA;
		this.maxPathLength = maxPathLength;

		VASN1Sequence asn1 = new BasicConstraints().create();
		try {
			asn1.append(new VASN1Boolean(isCA), "cA");
			if (maxPathLength >= 0)
				asn1.append(new VASN1Integer(maxPathLength), "pathLenConstraint");
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
	public static VX509BasicConstraint parse(boolean isCritical, byte[] der)
			throws VCryptoException {
		ParseResult _dec;
		try {
			_dec = new BasicConstraints().parseDER(der);
		} catch (VASN1Exception e) {
			throw new VCryptoException(e);
		}
		if (_dec.getNumRead() != der.length)
			throw new VCryptoException("Bad extension value DER encoding");

		VASN1Sequence asn1 = (VASN1Sequence) _dec.getResult();
		boolean isCA = ((VASN1Boolean)asn1.get("cA")).getValue();
		int path_len = -1;
		VASN1Integer _p_len = (VASN1Integer)asn1.get("pathLenConstraint");
		if (_p_len != null)
			path_len = _p_len.getValue().getValue().intValue();
		return new VX509BasicConstraint(isCA, isCritical, path_len);
	}

	/**
	 * Check if certifying authority.
	 *
	 * @return true if stated to be a CA
	 */
	public boolean isCertificateAuthority() {
		return certificateAuthority;
	}

	/**
	 * Check maximum path length of following certificates.
	 *
	 * @return max path length (negative if unlimited)
	 */
	public int getMaxPathLength() {
		return maxPathLength;
	}

}
