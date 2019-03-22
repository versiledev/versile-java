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

import org.versile.common.asn1.VASN1Tag.Decoded;
import org.versile.common.util.VByteBuffer;



/**
 * An ASN.1 tagged value.
 */
public class VASN1Tagged extends VASN1Base {

	VASN1Base value;
	VASN1Tag tag;
	boolean explicit;

	/**
	 * Set up tagged value.
	 *
	 *
	 * @param value the value to tag
	 * @param tag tag to set
	 * @param explicit if true DER encoding uses explicit mode (otherwise implicit)
	 */
	public VASN1Tagged(VASN1Base value, VASN1Tag tag, boolean explicit) {
		this.value = value;
		this.tag = tag;
		this.explicit = explicit;
	}

	/**
	 * Set up tagged value.
	 *
	 * <p>If tag encoding mode is explicit/implicit, then the tagged value is
	 * encoded with the associated encoding mode. If it is 'inherited' then
	 * the property is instead derived from the identifier octets of the
	 * DER encoding of the held 'value'.</p>
	 *
	 * @param value the value to tag
	 * @param tag tag to set
	 * @param explicit if true DER encoding uses explicit mode (otherwise implicit)
	 * @param name type name (or null)
	 * @param definition ASN.1 definition (or null)
	 */
	public VASN1Tagged(VASN1Base value, VASN1Tag tag, boolean explicit,
				       String name, VASN1Definition definition) {
		super(name, definition);
		this.value = value;
		this.tag = tag;
		this.explicit = explicit;
	}

	/**
	 * Get the held value.
	 *
	 * @return held value
	 */
	public VASN1Base getValue() {
		return value;
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return tag;
	}

	@Override
	public Object getNative(boolean deep) {
		return this;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		byte[] val_der = value.encodeDER();
		Decoded dec_tag = VASN1Tag.fromDER(val_der);

		VByteBuffer buf = new VByteBuffer();
		if (explicit) {
			// Use explicit encoding, with tag set to 'constructed'
			buf.append(VASN1Base.berEncTaggedContent(tag, true, val_der));
		}
		else {
			// Use implicit encoding, with same tag mode as the held value
			buf.append(val_der);
			buf.pop(dec_tag.getNumRead());
			byte[] _val_data = buf.popAll();
			buf.append(tag.encodeDER(dec_tag.isConstructed()));
			buf.append(_val_data);
		}
		if (!withTag) {
			// Strip tag from the result
			Decoded _dtag = VASN1Tag.fromDER(buf.peekAll());
			buf.pop(_dtag.getNumRead());
		}
		return buf.popAll();
	}

	@Override
	public String toString() {
		return tag.toString() + " " + value.toString();
	}
}
