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

import org.versile.common.asn1.VASN1DefBitString;
import org.versile.common.util.VObjectIdentifier;



/**
 * KeyUsage ASN.1 type for KeyUsage extension value.
 *
 * <p>Significance of bits held by bit number: (0) digitalSignature, (1) nonRepudiation,
 * (2) keyEncipherment, (3) dataEncipherment, (4) keyAgreement, (5) keyCertSign,
 * (6) cRLSign, (7) encipherOnly, (8) decipherOnly.</p>
 */
public class KeyUsage extends VASN1DefBitString {

	/**
	 * Extension identifier.
	 */
	static public VObjectIdentifier ID_CE = new VObjectIdentifier(2, 5, 29, 15);

	public KeyUsage() {
		super("KeyUsage");
	}

}
