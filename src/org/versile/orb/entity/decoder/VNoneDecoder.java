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
import org.versile.orb.entity.VNone;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VNone} data.
 */
public final class VNoneDecoder extends VEntityDecoderBase {

	VEntity entity = null;

	public VNoneDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (entity != null)
			return true;
		if (data.length() == 0)
			return false;
		int code = (int)(data.pop(1)[0] & 0xff);
		if (code == 0xf8)
			entity = VNone.get();
		else {
			failed = true;
			throw new VEntityReaderException("Invalid code for VNone type.");
		}
		return true;
	}

	@Override
	public Integer payloadLength() throws VEntityReaderException {
		this.checkFailed();
		return 0;
	}

	@Override
	public LinkedList<VEntityDecoderBase> getEmbeddedDecoders()
			throws VEntityReaderException {
		this.checkFailed();
		return null;
	}

	@Override
	public void putEmbeddedEntities(LinkedList<VEntity> entities)
			throws VEntityReaderException {
		this.checkFailed();
	}

	@Override
	public boolean decodePayload(VByteBuffer data)
			throws VEntityReaderException {
		this.checkFailed();
		return true;
	}

	@Override
	public VEntity getResult() throws VEntityReaderException {
		this.checkFailed();
		return entity;
	}
}
