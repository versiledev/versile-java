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

import java.util.BitSet;

import org.versile.common.util.VBitfield;



/**
 * Definition for the ASN.1 BitString type.
 */
public class VASN1DefBitString extends VASN1Definition {

	public VASN1DefBitString() {
	}

	public VASN1DefBitString(String name) {
		super(name);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param value value
	 */
	public VASN1BitString create(VBitfield value) {
		return new VASN1BitString(value, this.name, this);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1BitString.getUniversalTag();
	}

	/*
    def _create(self, *args, **kargs):
        return VASN1BitString(*args, **kargs)
	 */

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("Incomplete data");

		int tot_length = 0;
		if (withTag) {
			if (data[0] != (byte)0x03)
				throw new VASN1Exception("Explicit tag mismatch");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
		}

		DecodedLength dec_len = VASN1Definition.berDecLength(data);
		byte[] content;
		if (dec_len != null) {
			int _bread = dec_len.getBytesRead();
			tot_length = _bread + dec_len.getDecodedLength().getValue().intValue();
			content = new byte[tot_length-_bread];
			for (int i = 0; i < content.length; i++)
				content[i] = data[_bread+i];
		}
		else {
			tot_length += 1;
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			DecodedContent _cdec = VASN1Definition.berDecContentIndefinite(_data);
			content = _cdec.getContent();
			tot_length += _cdec.getBytesRead();
		}
		if (content.length == 0)
			throw new VASN1Exception("Incomplete data");

		VASN1BitString dec_value;
		if (content.length == 1) {
			if (content[0] == (byte)0x00)
				dec_value = new VASN1BitString(new VBitfield(new BitSet(), 0));
			else
				throw new VASN1Exception("Invalid encoding");
		}
		else {
			if (data.length < tot_length)
				throw new VASN1Exception("Incomplete data");
			int pad_len = content[0] & 0xff;
			if (pad_len > 7)
				throw new VASN1Exception("Invalid encoding");
			BitSet _bstr = new BitSet();
			int pos = 0;
			for (int i = 1; i < content.length; i++) {
				byte c_byte = content[i];
				for (int j = 0; j < 8; j++) {
					int mask = 0x80 >>> j;
					if ((c_byte & mask) != 0)
						_bstr.set(pos);
					else
						_bstr.clear(pos);
					pos += 1;
				}
			}
			dec_value = new VASN1BitString(new VBitfield(_bstr, pos-pad_len));
		}

		if (withTag)
			tot_length += 1;

		return new ParseResult(dec_value, tot_length);
	}
}
