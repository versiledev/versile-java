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

import org.versile.common.asn1.VASN1Boolean;
import org.versile.common.asn1.VASN1DefBoolean;
import org.versile.common.asn1.VASN1DefObjectIdentifier;
import org.versile.common.asn1.VASN1DefOctetString;
import org.versile.common.asn1.VASN1DefSequence;
import org.versile.common.asn1.VASN1Exception;


/**
 * Extension ASN.1 type.
 */
public class Extension extends VASN1DefSequence {

	public Extension() {
		super(true, "Extension");
		try {
			this.append(new VASN1DefObjectIdentifier(), "extnID");
			this.append(new VASN1DefBoolean(), "critical", true, new VASN1Boolean(false));
			this.append(new VASN1DefOctetString(), "extnValue");
		} catch (VASN1Exception e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}

}
