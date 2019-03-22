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
import org.versile.orb.entity.VString;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VString} data.
 */
public class VStringDecoder extends VEntityDecoderBase {

	VEntity entity = null;
	boolean have_code = false;
	boolean with_encoding = false;

	public VStringDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (have_code)
			return true;
		if (data.length() == 0)
			return false;

		int code = (int)(data.pop(1)[0] & 0xff);
		if (code == 0xf4) {
			with_encoding = false;
			have_code = true;
		}
		else if (code == 0xf5) {
			with_encoding = true;
			have_code = true;
		}
		else {
			failed = true;
			throw new VEntityReaderException("Invalid code for VBytes");
		}

		return true;
	}

	@Override
	public Integer payloadLength() throws VEntityReaderException {
		this.checkFailed();
		if (!have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		return 0;
	}

	@Override
	public LinkedList<VEntityDecoderBase> getEmbeddedDecoders()
			throws VEntityReaderException {
		LinkedList<VEntityDecoderBase> decoders = new LinkedList<VEntityDecoderBase>();
		this.checkFailed();
		if (!have_code) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}

		decoders.addLast(new VBytesDecoder(ctx, false));
		if (with_encoding)
			decoders.addLast(new VBytesDecoder(ctx, false));
		return decoders;
	}

	@Override
	public void putEmbeddedEntities(LinkedList<VEntity> entities)
			throws VEntityReaderException {
		this.checkFailed();
		if (!have_code || entity != null) {
			failed = true;
			throw new VEntityReaderException("Header not yet decoded");
		}
		if ((with_encoding && entities.size() != 2) || (!with_encoding && entities.size() != 1)) {
			failed = true;
			throw new VEntityReaderException("Invalid embedded entities");
		}

		VBytes v_str_data = (VBytes)(entities.peekLast());
		byte[] str_data = (byte[])(v_str_data._v_native());
		String encoding;
		if (with_encoding) {
			VBytes v_enc_data = (VBytes)(entities.peekFirst());
			byte[] enc_data = (byte[])(v_enc_data._v_native());
			try {
				encoding = new String(enc_data, "UTF-8");
			} catch (Exception e) {
				throw new VEntityReaderException("String encoding parsing error: " + e);
			}
		}
		else if (ctx.getStrDecoding() != null)
			encoding = ctx.getStrDecoding();
		else
			throw new VEntityReaderException("Unknown string encoding");

		try {
			entity = new VString(new String(str_data, encoding));
		}
		catch (Exception e) {
			throw new VEntityReaderException("String data could not be decoded");
		}
	}

	@Override
	public boolean decodePayload(VByteBuffer data)
			throws VEntityReaderException {
		this.checkFailed();
		if (entity == null) {
			failed = true;
			throw new VEntityReaderException("Embedded data not yet decoded");
		}
		return true;
	}

	@Override
	public VEntity getResult() throws VEntityReaderException {
		this.checkFailed();
		return entity;
	}
}
