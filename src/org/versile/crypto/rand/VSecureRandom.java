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

package org.versile.crypto.rand;

import java.security.SecureRandom;

import org.versile.common.util.VByteGenerator;



/**
 * Secure random number generator.
 */
public class VSecureRandom extends VByteGenerator {

	SecureRandom rand;

	/**
	 * Sets up with the system's default SecureRandom random data provider.
	 */
	public VSecureRandom() {
		rand = new SecureRandom();
	}

	/**
	 * Set up on the a specified secure random data provider.
	 *
	 * @param rand random data provider
	 */
	public VSecureRandom(SecureRandom rand) {
		this.rand = rand;
	}

	@Override
	public byte[] getBytes(int numBytes) {
		if (numBytes == 0)
			return new byte[0];
		byte[] result = new byte[numBytes];
		rand.nextBytes(result);
		return result;
	}
}
