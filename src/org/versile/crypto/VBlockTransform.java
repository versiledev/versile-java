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


/**
 * Block transform operating on byte data.
 *
 * <p>Transforms input data to associated output data. The transform has a
 * fixed blocksize for input data and output data (which do not necessarily
 * have to be the same), and input/output data must always align to the
 * relevant block size.</p>
 *
 * <p>Transform output may be affected by earlier transform operations (such
 * as block ciphers with a chain mode), so the sequence that transform
 * operations are performed may have to be taken into consideration.</p>
 */
public abstract class VBlockTransform {
	/**
	 * Number of bytes per block transform input block.
	 */
	protected int inputBlocksize;
	/**
	 * Number of bytes per block transform output block.
	 */
	protected int outputBlocksize;

	/**
	 * Set up base transform parameters.
	 *
	 * @param inputBlockSize transform input blocks ize
	 * @param outputBlockSize transform output block size
	 */
	protected VBlockTransform(int inputBlockSize, int outputBlockSize) {
		inputBlocksize = inputBlockSize;
		outputBlocksize = outputBlockSize;
	}

	/**
	 * Get block size of transform input.
	 *
	 * @return input block size
	 */
	public int getInputBlockSize() {
		return inputBlocksize;
	}

	/**
	 * Get block size of transform output.
	 *
	 * @return output block size
	 */
	public int getOutputBlockSize() {
		return outputBlocksize;
	}

	/**
	 * Transform block(s) of data.
	 *
	 * <p>Input data must be aligned to the input blocksize.</p>
	 *
	 * @param data input data to transform
	 * @return transformed data
	 * @throws VCryptoException input data not aligned to blocksize
	 */
	public final byte[] transform(byte[] data)
		throws VCryptoException {
		if (data.length % inputBlocksize != 0)
			throw new VCryptoException("Input not aligned to blocksize");
		byte[] result = this._transform(data);
		if (result == null || (data.length/inputBlocksize)*outputBlocksize != result.length)
			throw new RuntimeException("Internal VTransform error");
		return result;
	}

	/**
	 * Internal implementation of the block transform.
	 *
	 * <p>Note that 'data' may include multiple blocks of data, however it must be
	 * aligned to the input block size.</p>
	 *
	 * @param data input data
	 * @return output data
	 * @throws VCryptoException transform error
	 */
	protected abstract byte[] _transform(byte[] data)
		throws VCryptoException;
}
