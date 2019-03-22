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
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTuple;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VTagged} data.
 */
public final class VTaggedDecoder extends VEntityDecoderBase {
	VEntity entity = null;
	boolean have_code = false;
	int elements = 0;

	public VTaggedDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (!explicit || have_code)
			return true;

		if (data.length() == 0)
			return false;
		int code = (int)(data.pop(1)[0] & 0xff);
		if (code == 0xfe)
			have_code = true;
		else
			throw new VEntityReaderException("Invalid code for VTagged");
		return true;
	}

	@Override
	public Integer payloadLength() throws VEntityReaderException {
		this.checkFailed();
		if (explicit && !have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		return 0;
	}

	@Override
	public LinkedList<VEntityDecoderBase> getEmbeddedDecoders()
			throws VEntityReaderException {
		this.checkFailed();
		if (explicit && !have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		LinkedList<VEntityDecoderBase> result = new LinkedList<VEntityDecoderBase>();
		result.addLast(new VTupleDecoder(ctx, false));
		return result;
	}

	@Override
	public void putEmbeddedEntities(LinkedList<VEntity> entities)
			throws VEntityReaderException {
		this.checkFailed();
		if (explicit && !have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		if (entities.size() != 1)
			throw new VEntityReaderException("Invalid encoded format");
		VEntity entity = entities.getFirst();
		if (!(entity instanceof VTuple))
			throw new VEntityReaderException("Invalid encoded format");
		VTuple tuple = (VTuple) entity;
		VEntity[] items = tuple._v_native();
		if (items.length == 0)
			throw new VEntityReaderException("Invalid encoded format");
		VEntity value = items[0];
		LinkedList<VEntity> tags = new LinkedList<VEntity>();
		for (int i = 1; i < items.length; i++)
			tags.addLast(items[i]);
		this.entity = new VTagged(value, tags);
	}

	@Override
	public boolean decodePayload(VByteBuffer data)
			throws VEntityReaderException {
		this.checkFailed();
		if (explicit && !have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		return true;
	}

	@Override
	public VEntity getResult() throws VEntityReaderException {
		this.checkFailed();
		if (explicit && !have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		return entity;
	}
}
