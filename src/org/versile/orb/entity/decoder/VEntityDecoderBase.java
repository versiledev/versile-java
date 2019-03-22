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

package org.versile.orb.entity.decoder;

import java.util.LinkedList;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityReaderException;
import org.versile.orb.entity.VIOContext;


/**
 * Decoder for serialized VEntity data.
 *
 * <p>Intended primarily for internal use by the Versile Java framework.</p>
 */
public abstract class VEntityDecoderBase {

	protected VIOContext ctx;
	protected boolean explicit;
	protected boolean failed = false;

	/**
	 * Initialize decoder.
	 *
	 * @param ctx serialization I/O context
	 * @param explicit if true decode explicit serialization
	 */
	public VEntityDecoderBase(VIOContext ctx, boolean explicit) {
		this.ctx = ctx;
		this.explicit = explicit;
	}

	/**
	 * Decode header data.
	 *
	 * @param data serialized data (decoded data is popped off the buffer)
	 * @return true if header was fully decoded
	 * @throws VEntityReaderException header decode error
	 */
	public abstract boolean decodeHeader(VByteBuffer data)
			throws VEntityReaderException;

	/**
	 * Return payload length defined by header.
	 *
	 * @return payload length
	 * @throws VEntityReaderException payload length not available
	 */
	public abstract Integer payloadLength()
			 throws VEntityReaderException;

	/**
	 * Return decoders for embedded entities in serialized structure.
	 *
	 * @return decoders
	 * @throws VEntityReaderException decode error
	 */
	public abstract LinkedList<VEntityDecoderBase> getEmbeddedDecoders()
			throws VEntityReaderException;

	/**
	 * Provide decoded embedded entities to this decoder.
	 *
	 * @param entities decoded embedded entities
	 * @throws VEntityReaderException decoder error
	 */
	public abstract void putEmbeddedEntities(LinkedList<VEntity> entities)
			 throws VEntityReaderException;

	/**
	 * Decode payload data.
	 *
	 * @param data data to decode (decoded data is popped off the buffer)
	 * @return true if payload was fully decoded
	 * @throws VEntityReaderException decoder error
	 */
	public abstract boolean decodePayload(VByteBuffer data)
			 throws VEntityReaderException;

	/**
	 * Return decoding result.
	 *
	 * @return decoded entity
	 * @throws VEntityReaderException decoder error
	 */
	public abstract VEntity getResult()
			 throws VEntityReaderException;

	/**
	 * Checks if decoder has a failure state registered.
	 *
	 * @throws VEntityReaderException failure state is set on the decoder
	 */
	protected void checkFailed() throws VEntityReaderException {
		if (failed)
			throw new VEntityReaderException("Decoder had an earlier failure");
	}
}
