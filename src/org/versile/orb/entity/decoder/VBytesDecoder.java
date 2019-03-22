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
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityReaderException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VBytes} data.
 */
public final class VBytesDecoder extends VEntityDecoderBase {

	VEntity entity = null;
	boolean have_code = false;
	boolean have_header = false;
	int payload_len = 0;
	VByteBuffer buf;

	public VBytesDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
		buf = new VByteBuffer();
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (have_header)
			return true;

		if (explicit && !have_code) {
			if (data.length() == 0)
				return false;
			int code = (int)(data.pop(1)[0] & 0xff);
			if (code == 0xf3)
				have_code = true;
			else
				throw new VEntityReaderException("Invalid code for VBytes");
		}

		if (data.length() == 0)
			return false;
		int old_len = buf.length();
		buf.append(data.peekAll());
		VInteger.NetbytesResult nresult = VInteger.netbytes_to_posint(buf.peekAll());
		int total_read = nresult.getBytesRead();
		if (total_read > old_len)
			data.pop(total_read - old_len);
		if (nresult.hasValue()) {
			payload_len = nresult.getValue().intValue();
			buf.clear();
			have_header = true;
			return true;
		}
		else
			return false;
	}

	@Override
	public Integer payloadLength() throws VEntityReaderException {
		this.checkFailed();
		if (!have_header)
			throw new VEntityReaderException("Header not yet decoded");
		return payload_len;
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
		if (!have_header)
			throw new VEntityReaderException("Header not yet decoded");
		if (entity != null)
			throw new VEntityReaderException("Already fully decoded");

		int bytes_left = payload_len - buf.length();
		buf.append(data.pop(bytes_left));
		if (buf.length() == payload_len) {
			entity = new VBytes(buf.popAll());
			return true;
		}
		else
			return false;
	}

	@Override
	public VEntity getResult() throws VEntityReaderException {
		return entity;
	}
}
