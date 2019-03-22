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

import org.versile.common.asn1.VASN1DefSequence.ExtractedData;


/**
 * Definition for the ASN.1 Set Of type.
 */
public class VASN1DefSetOf extends VASN1Definition {

	VASN1Definition def;

	/**
	 * Set up Set Of definition.
	 *
	 * @param definition type definition of sequence elements
	 */
	public VASN1DefSetOf(VASN1Definition definition) {
		def = definition;
	}

	/**
	 * Set up Set Of definition.
	 *
	 * @param definition type definition of sequence elements
	 * @param name type name (or null)
	 */
	public VASN1DefSetOf(VASN1Definition definition, String name) {
		super(name);
		def = definition;
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Set.getUniversalTag();
	}

	/**
	 * Get type definition of Set Of elements.
	 *
	 * @return element definition
	 */
	public VASN1Definition getElementDefinition() {
		return def;
	}

	@Override
	public synchronized ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int tot_length = 0;
		if (withTag) {
			if (data[0] != (byte)0x31)
				throw new VASN1Exception("Invalid explicit tag");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_length += 1;
		}
		ExtractedData dec_elements = VASN1DefSequence.berExtractSequenceElements(data);
		tot_length += dec_elements.numDecodedBytes;

		VASN1Set set = new VASN1SetOf();
		for (byte[] item : dec_elements.elements) {
			ParseResult item_dec = def.parseDER(item);
			set.append(item_dec.getResult());
		}
		return new ParseResult(set, tot_length);
	}

	/**
	 * Creates an object for this type with explicit encoding.
	 */
	public VASN1SetOf create() {
		return this.create(true);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param defaultExplicit if true use default explicit encoding
	 */
	public VASN1SetOf create(boolean defaultExplicit) {
		return new VASN1SetOf(defaultExplicit, this.name, this);
	}

	/**
	 * Set element information.
	 */
	protected static class Element {
		/**
		 * Element definition.
		 */
		public VASN1Definition definition;
		/**
		 * Element name (or null).
		 */
		public String name;
		/**
		 * True if element is optional.
		 */
		public boolean optional;
		/**
		 * Default element value (or null).
		 */
		public VASN1Base defaultValue;
	}

}
