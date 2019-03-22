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

package org.versile.common.asn1;

import java.math.BigInteger;
import java.util.LinkedList;

import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VInteger;



/**
 * Definition for the ASN.1 ObjectIdentifier type.
 */
public class VASN1DefObjectIdentifier extends VASN1Definition {

	public VASN1DefObjectIdentifier() {
	}

	public VASN1DefObjectIdentifier(String name) {
		super(name);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1ObjectIdentifier.getUniversalTag();
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");
		int tot_length = 0;
		if (withTag) {
			if (data[0] != (byte)0x06)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_length += 1;
		}
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");
		DecodedLength dec_len = VASN1Definition.berDecLength(data);
		if (dec_len == null)
			throw new VASN1Exception("Indefinite length not supported");
		int l_len = dec_len.getBytesRead();
		int c_len = dec_len.getDecodedLength().getValue().intValue();
		tot_length += l_len + c_len;
		if (data.length < (l_len+c_len))
			throw new VASN1Exception("Incomplete data");
		byte[] content = new byte[c_len];
		for (int i = 0; i < content.length; i++)
			content[i] = data[l_len+i];

		LinkedList<VInteger> oids = new LinkedList<VInteger>();
		boolean decoding = false;
		String bin_str = "0";
		for (byte b: content) {
			decoding = true;
			int b_val = b & 0xff;
			String add_bits = Integer.toBinaryString(b_val & 0x7f);
			while(add_bits.length() < 7)
				add_bits = "0" + add_bits;
			bin_str += add_bits;
			if ((b_val & 0x80) == 0) {
				oids.addLast(new VInteger(new BigInteger(bin_str, 2)));
				decoding = false;
				bin_str = "0";
			}
		}
		if (decoding || oids.size() < 1)
			throw new VASN1Exception("Invalid Object Identifier encoding");

		BigInteger first = oids.removeFirst().getBigIntegerValue();
		oids.addFirst(new VInteger(first.remainder(BigInteger.valueOf(40))));
		oids.addFirst(new VInteger(first.divide(BigInteger.valueOf(40))));

		VObjectIdentifier oid = new VObjectIdentifier(oids.toArray(new VInteger[0]));
		return new ParseResult(new VASN1ObjectIdentifier(oid), tot_length);
	}

}
