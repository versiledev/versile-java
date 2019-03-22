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
 * Decoder for explicit-encoded VEntity which resolves type.
 */
public final class VEntityDecoder extends VEntityDecoderBase {

	VEntityDecoderBase decoder = null;

	/**
	 * Initialize decoder.
	 *
	 * @param ctx serialization I/O context
	 */
	public VEntityDecoder(VIOContext ctx) {
		super(ctx, true);
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		if (decoder == null) {
			if (data.length() == 0)
				return false;
			int code = (int)(data.peek(1)[0] & 0xff);
			if (code < 0xef)
				decoder = new VIntegerDecoder(ctx, true);
			else if (code == 0xef || code == 0xf0)
				decoder = new VIntegerDecoder(ctx, true);
			else if (code == 0xf1 || code == 0xf2)
				decoder = new VBooleanDecoder(ctx, true);
			else if (code == 0xf3)
				decoder = new VBytesDecoder(ctx, true);
			else if (code == 0xf4 || code == 0xf5)
				decoder = new VStringDecoder(ctx, true);
			else if (code == 0xf6)
				decoder = new VTupleDecoder(ctx, true);
			else if (code == 0xf7)
				decoder = new VExceptionDecoder(ctx, true);
			else if (code == 0xf8)
				decoder = new VNoneDecoder(ctx, true);
			else if (code == 0xf9 || code == 0xfa || code == 0xfb)
				decoder = new VFloatDecoder(ctx, true);
			else if (code == 0xfc)
				decoder = new VReferenceDecoder(ctx, true);
			else if (code == 0xfd)
				decoder = new VObjectDecoder(ctx, true);
			else if (code == 0xfe)
				decoder = new VTaggedDecoder(ctx, true);
			else
				throw new VEntityReaderException("Unknown code");
		}

		return decoder.decodeHeader(data);
	}

	@Override
	public Integer payloadLength() throws VEntityReaderException {
		if (decoder == null)
			throw new VEntityReaderException("Decoder type not yet resolved");
		return decoder.payloadLength();
	}

	@Override
	public LinkedList<VEntityDecoderBase> getEmbeddedDecoders()
			throws VEntityReaderException {
		if (decoder == null)
			throw new VEntityReaderException("Decoder type not yet resolved");
		return decoder.getEmbeddedDecoders();
	}

	@Override
	public void putEmbeddedEntities(LinkedList<VEntity> entities)
			throws VEntityReaderException {
		if (decoder == null)
			throw new VEntityReaderException("Decoder type not yet resolved");
		decoder.putEmbeddedEntities(entities);
	}

	@Override
	public boolean decodePayload(VByteBuffer data)
			throws VEntityReaderException {
		if (decoder == null)
			throw new VEntityReaderException("Decoder type not yet resolved");
		return decoder.decodePayload(data);
	}

	@Override
	public VEntity getResult() throws VEntityReaderException {
		if (decoder == null)
			throw new VEntityReaderException("Decoder type not yet resolved");
		return decoder.getResult();
	}
}
