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
import org.versile.common.asn1.VASN1Definition;
import org.versile.common.asn1.VASN1Exception;
import org.versile.orb.entity.VInteger;



/**
 * TBSCertificate ASN.1 type.
 */
public final class TBSCertificate extends VASN1DefSequence {

	public TBSCertificate() {
		super(true, "TBSCertificate");
		try {
			VASN1DefTagged _def = this.tagWithContext(0,  new Version());
			this.append(_def, "version", true, new Version().create(new VInteger(0)));
			this.append(new CertificateSerialNumber(), "serialNumber");
			this.append(new AlgorithmIdentifier(), "signature");
			this.append(new Name(), "issuer");
			this.append(new Validity(), "validity");
			this.append(new Name(), "name");
			this.append(new SubjectPublicKeyInfo(), "subjectPublicKeyInfo");
			_def = VASN1Definition.tagWithContext(1, new UniqueIdentifier(), false);
			this.append(_def, "issuerUniqueId", true);
			_def = VASN1Definition.tagWithContext(2, new UniqueIdentifier(), false);
			this.append(_def, "subjectUniqueId", true);
			_def = this.tagWithContext(3, new Extensions());
			this.append(_def, "extensions", true);
		} catch (VASN1Exception e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}

}
