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

import org.versile.common.util.VByteBuffer;


/**
 * Definition for an ASN.1 tagged data element.
 */
public class VASN1DefTagged extends VASN1Definition {

	VASN1Tag tag;
	VASN1Definition def;
	boolean explicit;

	/**
	 * Set up tagged-value definition.
	 *
	 * @param tag associated tag
	 * @param definition definition of the tagged value
	 * @param explicit if true tagged value uses explicit encoding
	 */
	public VASN1DefTagged(VASN1Tag tag, VASN1Definition definition, boolean explicit) {
		this.tag = tag;
		this.def = definition;
		this.explicit = explicit;
	}

	/**
	 * Set up tagged-value definition.
	 *
	 * @param tag associated tag
	 * @param definition definition of the tagged value
	 * @param explicit if true tagged value uses explicit encoding
	 * @param name associated name (or null)
	 */
	public VASN1DefTagged(VASN1Tag tag, VASN1Definition definition, boolean explicit, String name) {
		super(name);
		this.tag = tag;
		this.def = definition;
		this.explicit = explicit;
	}

	@Override
	public VASN1Tag getTag() {
		return tag;
	}

	/**
	 * Get the associated tagged value definition.
	 *
	 * @return tagged value definition
	 */
	public VASN1Definition getDefinition() {
		return def;
	}

	/**
	 * Check if tag format is explicit.
	 *
	 * @return true if explicit tag format
	 */
	public boolean isExplicit() {
		return explicit;
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (!withTag)
			throw new VASN1Exception("VASN1DefTagged can only parse tagged data");

		byte[] content;
		int num_read;
		int tag_len;
		if (withTag) {
			DecodedTaggedContent _dec = VASN1Definition.berDecTaggedContent(data);
			content = _dec.getContent();
			VASN1Tag _tag = _dec.getTag();
			if (!(this.tag.equals(_tag)))
				throw new VASN1Exception("Tag mismatch");
			tag_len = VASN1Tag.fromDER(data).getNumRead();
			num_read = _dec.getNumRead();
		}
		else {
			VByteBuffer _buf = new VByteBuffer();
			DecodedLength _ldec = VASN1Definition.berDecLength(data);
			int _l_num;
			int _c_num;
			if (_ldec != null) {
				_l_num = _ldec.getBytesRead();
				_buf.append(data);
				_buf.pop(_l_num);
				DecodedContent _cdec = VASN1Definition.berDecContentDefinite(_buf.popAll(), _ldec.decodedLength);
				content = _cdec.getContent();
				_c_num = _cdec.getBytesRead();
			}
			else {
				_l_num = 1;
				_buf.append(data);
				_buf.pop(_l_num);
				DecodedContent _cdec = VASN1Definition.berDecContentIndefinite(_buf.popAll());
				content = _cdec.getContent();
				_c_num = _cdec.getBytesRead();
			}
			tag_len = 0;
			num_read = _l_num + _c_num;
		}

		// Below decoding does not validate 'constructed vs. primitive' encoding
		// requirement, ref. X.690 section 8.14
		VASN1Base result;
		int _num_parsed;
		if (explicit) {
			ParseResult _pdec = def.parseDER(content, withTag);
			result = _pdec.getResult();
			_num_parsed = _pdec.getNumRead();
			if (_num_parsed != content.length)
				throw new VASN1Exception("Illegal encoding");
		}
		else {
			VByteBuffer _buf = new VByteBuffer(data);
			_buf.pop(tag_len);
			ParseResult _pdec = def.parseDER(_buf.popAll(), false);
			result = _pdec.getResult();
			_num_parsed = _pdec.getNumRead();
			if (_num_parsed + tag_len != num_read)
				throw new VASN1Exception("Illegal encoding");
		}

		VASN1Tagged tagged_val = new VASN1Tagged(result, tag, explicit);
		return new ParseResult(tagged_val, num_read);
	}

}
