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

import org.versile.common.asn1.VASN1DefChoice;
import org.versile.common.asn1.VASN1DefIA5String;
import org.versile.common.asn1.VASN1DefObjectIdentifier;
import org.versile.common.asn1.VASN1DefOctetString;
import org.versile.common.asn1.VASN1Exception;

/**
 * GeneralName ASN.1 type.
 */
public final class GeneralName extends VASN1DefChoice {

	public GeneralName() {
		super("GeneralName");
		try {
			this.add(this.tagWithContext(0, new AnotherName()), "otherName");
			this.add(this.tagWithContext(1, new VASN1DefIA5String()), "rfc822Name");
			this.add(this.tagWithContext(2, new VASN1DefIA5String()), "dNSName");
			// context tag 3 not (yet) supported
			this.add(this.tagWithContext(4, new Name()), "directoryName");
			// context tag5 not (yet) supported
			this.add(this.tagWithContext(6, new VASN1DefIA5String()), "uniformResourceIdentifier");
			this.add(this.tagWithContext(7, new VASN1DefOctetString()), "iPAddress");
			this.add(this.tagWithContext(8, new VASN1DefObjectIdentifier()), "registeredID");
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

}
