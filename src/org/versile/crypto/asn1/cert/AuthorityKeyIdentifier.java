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

package org.versile.crypto.asn1.cert;

import org.versile.common.asn1.VASN1DefSequence;
import org.versile.common.asn1.VASN1DefTagged;
import org.versile.common.asn1.VASN1Exception;
import org.versile.common.util.VObjectIdentifier;



/**
 * AuthorityKeyIdentifier ASN.1 type for certificate extensions.
 *
 * <p>The extension must be included for CA-generated certificates, except for
 * self-signed certficates.</p>
 *
 * </p>AuthorityCertIssuer and authorityCertSerialNumber must either both be
 * present, or both be absent. The extension *must not* be marked critical.</p>
 */
public class AuthorityKeyIdentifier extends VASN1DefSequence {

	/**
	 * Extension identifier.
	 */
	static public VObjectIdentifier ID_CE = new VObjectIdentifier(2, 5, 29, 35);

	public AuthorityKeyIdentifier() {
		super(false, "AuthorityKeyIdentifier");
		try {
			VASN1DefTagged _def = this.tagWithContext(0, new KeyIdentifier());
			this.append(_def, "keyIdentifier", true);
			_def = this.tagWithContext(1, new GeneralNames());
			this.append(_def, "authorityCertIssuer", true);
			_def = this.tagWithContext(2, new CertificateSerialNumber());
			this.append(_def, "authorityCertSerialNumber", true);

		} catch (VASN1Exception e) {
			// should never happen
			throw new RuntimeException(e);
		}

	}

}
