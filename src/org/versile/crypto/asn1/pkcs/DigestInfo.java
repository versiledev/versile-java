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

package org.versile.crypto.asn1.pkcs;

import org.versile.common.asn1.VASN1DefOctetString;
import org.versile.common.asn1.VASN1DefSequence;
import org.versile.common.asn1.VASN1Exception;
import org.versile.crypto.asn1.cert.AlgorithmIdentifier;



/**
 * DigestInfo ASN.1 type.
 */
public final class DigestInfo extends VASN1DefSequence {
	public DigestInfo() {
		super("DigestInfo");
		try {
			this.append(new AlgorithmIdentifier(), "digestAlgorithm");
			this.append(new VASN1DefOctetString(), "digest");
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}
}
