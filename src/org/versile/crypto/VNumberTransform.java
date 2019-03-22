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

package org.versile.crypto;

import java.math.BigInteger;


/**
 * Integer modular transform.
 *
 * <p>Operates on integers 0..N-1 where N is the modulus and N-1 is the largest
 * number that can be transformed.</p>
 */
public abstract class VNumberTransform {

	/**
	 * Perform transform on a number.
	 *
	 * @param number transform input
	 * @return transform output
	 * @throws VCryptoException invalid number
	 */
	public abstract BigInteger transform(BigInteger number)
		throws VCryptoException;

	/**
	 * Get maximum number that can be transformed.
	 *
	 * @return maximum transform input
	 */
	public BigInteger getMaxTransformInput() {
		return this.getTransformModulus().subtract(BigInteger.ONE);
	}

	/**
	 * Get transform modulus.
	 *
	 * @return modulus
	 */
	public abstract BigInteger getTransformModulus();
}
