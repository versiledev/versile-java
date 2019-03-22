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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.versile.common.asn1.VASN1DefSequence.ExtractedData;
import org.versile.common.asn1.VASN1Tag.Decoded;



/**
 * Definition for the ASN.1 Set type.
 */
public class VASN1DefSet extends VASN1Definition {

	Map<VASN1Tag, Element> types;
	Map<String, VASN1Definition> named_types;
	boolean use_explicit;

	/**
	 * Set up type definition.
	 *
	 * <p>Sets up with explicit default tagged value encoding.</p>
	 */
	public VASN1DefSet() {
		types = new Hashtable<VASN1Tag, Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = true;
	}

	/**
	 * Set up type definition.
	 *
	 * <p>If using default explicit encoding then tagged values by default
	 * are explicit, otherwise they are by default implicit. This can be used
	 * as a mechanism to specify tag defaults as specified by an X.680 module.</p>
	 *
	 * @param useExplicit if true default use explicit tagged value encoding
	 */
	public VASN1DefSet(boolean useExplicit) {
		types = new Hashtable<VASN1Tag, Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = useExplicit;
	}

	/**
	 * Set up type definition.
	 *
	 * @param useExplicit if true default use explicit tagged value encoding
	 * @param name type name (or null)
	 */
	public VASN1DefSet(boolean useExplicit, String name) {
		super(name);
		types = new Hashtable<VASN1Tag, Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = useExplicit;
	}

	/**
	 * Get type associated with a sequence element name.
	 *
	 * @param name sequence element name
	 * @return type definition (null if no such element name)
	 */
	public synchronized VASN1Definition getNamedDefinition(String name) {
		return named_types.get(name);
	}

	/**
	 * Append a sequence element type.
	 *
	 * <p>Appends without an element name, as a required (not optional) element, and
	 * without a default value.</p>
	 *
	 * @param def type definition
	 * @throws VASN1Exception element name already in use
	 */
	public synchronized void append(VASN1Definition def)
			throws VASN1Exception {
		this.append(def, null, false, null);
	}

	/**
	 * Append a sequence element type.
	 *
	 * @param def type definition
	 * @param name associated element name (or null)
	 * @param isOptional if true element is optional
	 * @param defaultValue default type definition value
	 * @throws VASN1Exception element name already in use
	 */
	public synchronized void append(VASN1Definition def, String name, boolean isOptional, VASN1Base defaultValue)
			throws VASN1Exception {
		if (name != null && named_types.get(name) != null)
			throw new VASN1Exception("Element name already in use");
		VASN1Tag tag = def.getTag();
		if (tag == null)
			throw new VASN1Exception("Set definitions must have a tag");
		Element e = new Element();
		e.definition = def;
		e.name = name;
		e.optional = isOptional;
		e.defaultValue = defaultValue;
		types.put(tag, e);
		if (name != null)
			named_types.put(name, def);
	}

	/**
	 * Creates a tagged value definition for an associated context tag number and value definition.
	 *
	 * <p>Uses the same explicit encoding property for the tagged value as the default explicit
	 * encoding true/false value set on this object.</p>
	 *
	 * @param tagNumber tag number for a context tag
	 * @param def value definition
	 * @return tagged value definition
	 */
	public VASN1DefTagged tagWithContext(int tagNumber, VASN1Definition def) {
		return VASN1Definition.tagWithContext(tagNumber, def, use_explicit);
	}

	@Override
	public VASN1Tag getTag() {
		return VASN1Set.getUniversalTag();
	}

	/**
	 * Get names of named set elements.
	 *
	 * @return named set elements
	 */
	public synchronized List<String> getDefinitionNames() {
		Vector<String> names = new Vector<String>();
		for (String _name: named_types.keySet())
			names.add(_name);
		return names;
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

		VASN1Set set = new VASN1Set();
		Set<VASN1Tag> decoded_tags = new HashSet<VASN1Tag>();

		for (byte[] item: dec_elements.elements) {
			Decoded d_tag = VASN1Tag.fromDER(item);
			if (d_tag.getTag() == null)
				throw new VASN1Exception("Invalid element tag for the Set");
			Element type_data = types.get(d_tag.getTag());
			if (type_data == null)
				throw new VASN1Exception("Invalid element tag for the Set");
			decoded_tags.add(d_tag.getTag());
			ParseResult dec = type_data.definition.parseDER(item);
			set.append(dec.getResult(), type_data.name, false);
		}

		// Fill in default values and validate no required elements missing
		for (VASN1Tag tag: types.keySet()) {
			if (!decoded_tags.contains(tag)) {
				Element _tdata = types.get(tag);
				if (_tdata.defaultValue != null)
					set.append(_tdata.defaultValue, _tdata.name, true);
				else if (!_tdata.optional)
					throw new VASN1Exception("Set is missing required component(s)");
			}
		}

		return new ParseResult(set, tot_length);
	}

	/**
	 * Creates an object for this type.
	 */
	public VASN1Set create() {
		return this.create(use_explicit);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param defaultExplicit if true use default explicit encoding
	 */
	public VASN1Set create(boolean defaultExplicit) {
		return new VASN1Set(defaultExplicit, this.name, this);
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
