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

import org.versile.common.asn1.VASN1DefInteger;
import org.versile.common.asn1.VASN1DefSequence;
import org.versile.common.asn1.VASN1Exception;
import org.versile.crypto.asn1.cert.Version;



/**
 * RSAPrivateKey ASN.1 type.
 */
public final class RSAPrivateKey extends VASN1DefSequence {

	public RSAPrivateKey() {
		super("RSAPrivateKey");
		try {
			this.append(new Version(), "version");
			this.append(new VASN1DefInteger(), "modulus");
			this.append(new VASN1DefInteger(), "publicExponent");
			this.append(new VASN1DefInteger(), "privateExponent");
			this.append(new VASN1DefInteger(), "prime1");
			this.append(new VASN1DefInteger(), "prime2");
			this.append(new VASN1DefInteger(), "exponent1");
			this.append(new VASN1DefInteger(), "exponent2");
			this.append(new VASN1DefInteger(), "coefficient");
			this.append(new OtherPrimeInfos(), "otherPrimeInfos", true);
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}
}
