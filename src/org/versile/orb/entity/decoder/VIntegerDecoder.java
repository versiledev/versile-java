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

import java.math.BigInteger;
import java.util.LinkedList;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityReaderException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VInteger} data.
 */
public final class VIntegerDecoder extends VEntityDecoderBase {

	VEntity entity = null;
	Integer code = null;
	boolean have_header = false;
	int netbytes_read = 0;
	VByteBuffer _data;
	boolean decode_signed = true;
	int offset = 0;
	boolean sign_change = false;

	public VIntegerDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
		_data = new VByteBuffer();
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (have_header)
			return true;
		if (data.length() == 0)
			return false;

		if (explicit && code == null) {
			code = (int)(data.pop(1)[0] & 0xff);
			if (code < 0xef) {
				have_header = true;
				entity = new VInteger(code-1);
				return true;
			}
			else if (code == 0xef) {
				decode_signed = false;
				offset = 0xee;
			}
			else if (code == 0xf0) {
				decode_signed = false;
				offset = 2;
				sign_change = true;
			}
			else
				throw new VEntityReaderException("Invalid header code");
		}

		_data.append(data.peekAll());
		if (_data.length() == 0)
			return false;
		VInteger.NetbytesResult conv_res;
		if (decode_signed)
			conv_res = VInteger.netbytes_to_signedint(_data.peekAll());
		else
			conv_res = VInteger.netbytes_to_posint(_data.peekAll());
		int tot_bytes_read = conv_res.getBytesRead();
		if (tot_bytes_read > netbytes_read) {
			data.pop(tot_bytes_read - netbytes_read);
			netbytes_read = tot_bytes_read;
		}
		if (conv_res.hasValue()) {
			Number value = conv_res.getValue();
			if (value instanceof Integer) {
				int num = (Integer) value;
				num += offset;
				if (sign_change)
					num = -num;
				entity = new VInteger(num);
			}
			else if (value instanceof Long) {
				long num = (Long) value;
				num += offset;
				if (sign_change)
					num = -num;
				entity = new VInteger(num);
			}
			else {
				BigInteger num = (BigInteger) value;
				if (offset != 0)
					num = num.add(BigInteger.valueOf(offset));
				if (sign_change)
					num = num.multiply(BigInteger.valueOf(-1));
				entity = new VInteger(num);
			}
			return true;
		}
		else
			return false;
	}

	@Override
	public Integer payloadLength()
		throws VEntityReaderException {
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
		throw new RuntimeException("VInteger decoder does not accept embedded entities");
	}

	@Override
	public boolean decodePayload(VByteBuffer data)
			throws VEntityReaderException {
		this.checkFailed();
		return true;
	}

	@Override
	public VEntity getResult()
			throws VEntityReaderException {
		this.checkFailed();
		if (entity == null)
			throw new VEntityReaderException("Read operation not finished");
		return entity;
	}
}
