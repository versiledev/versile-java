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
import org.versile.orb.entity.VFloat;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;


/**
 * Decoder for serialized {@link org.versile.orb.entity.VFloat} data.
 */
public class VFloatDecoder extends VEntityDecoderBase {
	VEntity entity = null;
	boolean have_code = false;
	int code = 0;

	public VFloatDecoder(VIOContext ctx, boolean explicit) {
		super(ctx, explicit);
	}

	@Override
	public boolean decodeHeader(VByteBuffer data) throws VEntityReaderException {
		this.checkFailed();
		if (!explicit || have_code)
			return true;

		if (data.length() == 0)
			return false;
		code = (int)(data.pop(1)[0] & 0xff);
		if (code == 0xf9 || code == 0xfa || code == 0xfb)
			have_code = true;
		else {
			failed = true;
			throw new VEntityReaderException("Invalid code for VTagged");
		}
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
		int num_elements = 2;
		if (code == 0xfb)
			num_elements++;

		LinkedList<VEntityDecoderBase> result = new LinkedList<VEntityDecoderBase>();
		for (int i = 0; i < num_elements; i++)
			result.addLast(new VIntegerDecoder(ctx, false));
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

		int req_num = 2;
		VInteger base = null;
		if (code == 0xf9)
			base = new VInteger(10);
		else if (code == 0xfa)
			base = new VInteger(2);
		else
			req_num++;

		if (entities.size() != req_num)
			throw new VEntityReaderException("Invalid encoded format");

		VInteger digits = (VInteger)entities.peekFirst();
		VInteger exp = (VInteger)entities.peekLast();
		if (base == null)
			base = (VInteger)entities.get(1);

		BigInteger b_dig = VInteger.asBigInt(digits.getValue());
		BigInteger b_base = VInteger.asBigInt(base.getValue());
		BigInteger b_exp = VInteger.asBigInt(exp.getValue());
		if (b_base.compareTo(BigInteger.valueOf(2)) < 0)
			throw new VEntityReaderException("Illegal VFloat base");
		entity = new VFloat(b_dig, b_base, b_exp);
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
