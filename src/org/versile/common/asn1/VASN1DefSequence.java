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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.versile.common.asn1.VASN1Tag.Decoded;
import org.versile.common.util.VByteBuffer;



/**
 * Definition for the ASN.1 Sequence type.
 */
public class VASN1DefSequence extends VASN1Definition {

	Vector<Element> types;
	Map<String, VASN1Definition> named_types;
	boolean use_explicit;

	/**
	 * Set up type definition.
	 *
	 * <p>Sets up with explicit default tagged value encoding.</p>
	 */
	public VASN1DefSequence() {
		types = new Vector<Element>();
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
	public VASN1DefSequence(boolean useExplicit) {
		types = new Vector<Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = useExplicit;
	}

	/**
	 * Set up type definition.
	 *
	 * <p>Sets up with explicit default tagged value encoding.</p>
	 *
	 * @param name type name (or null)
	 */
	public VASN1DefSequence(String name) {
		super(name);
		types = new Vector<Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = true;
	}

	public VASN1DefSequence(boolean useExplicit, String name) {
		super(name);
		types = new Vector<Element>();
		named_types = new Hashtable<String, VASN1Definition>();
		use_explicit = useExplicit;
	}

	/**
	 * Get type associated with a sequence element name.
	 *
	 * @param name sequence element name
	 * @return type definition (null if no such element name)
	 */
	public synchronized VASN1Definition get(String name) {
		return named_types.get(name);
	}

	/**
	 * Append a sequence element type.
	 *
	 * <p>Appends an element as an non-optional element with no default value and no associated name.</p>
	 *
	 * @param def type definition
	 * @throws VASN1Exception element name already in use
	 */
	public void append(VASN1Definition def)
			throws VASN1Exception {
		this.append(def, null, false, null);
	}

	/**
	 * Append a sequence element type.
	 *
	 * <p>Appends an element as an non-optional element with no default value.</p>
	 *
	 * @param def type definition
	 * @param name associated element name (or null)
	 * @throws VASN1Exception element name already in use
	 */
	public void append(VASN1Definition def, String name)
			throws VASN1Exception {
		this.append(def, name, false, null);
	}

	/**
	 * Append a sequence element type.
	 *
	 * <p>Appends without an associated default value.</p>
	 *
	 * @param def type definition
	 * @param name associated element name (or null)
	 * @param isOptional if true element is optional
	 * @throws VASN1Exception element name already in use
	 */
	public synchronized void append(VASN1Definition def, String name, boolean isOptional)
			throws VASN1Exception {
		this.append(def, name, isOptional, null);
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
		Element e = new Element();
		e.definition = def;
		e.name = name;
		e.optional = isOptional;
		e.defaultValue = defaultValue;
		types.add(e);
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
		return VASN1Sequence.getUniversalTag();
	}

	public synchronized List<String> getDefinitionNames() {
		Vector<String> names = new Vector<String>();
		for (String _name: named_types.keySet()) {
			names.add(_name);
		}
		return names;
	}

	protected static ExtractedData berExtractSequenceElements(byte[] data)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		DecodedLength l_dec = VASN1Definition.berDecLength(data);
		if (l_dec == null)
			throw new VASN1Exception("Indefinite sequence data length not supported");
		int l_num = l_dec.getBytesRead();
		int c_num = l_dec.getDecodedLength().getValue().intValue();
		int total_length = l_num + c_num;
		byte[] content = new byte[c_num];
		for (int i = 0; i < content.length; i++)
			content[i] = data[l_num+i];

		// Parse content, splitting up into separate encoded elements
		ExtractedData result = new ExtractedData();
		result.elements = new Vector<byte[]>();
		result.numDecodedBytes = total_length;
		VByteBuffer _buf = new VByteBuffer();
		while(content.length > 0) {
			Decoded d_id = VASN1Tag.fromBER(content);

			int _tag_num = d_id.getNumRead();
			_buf.append(content);
			_buf.pop(_tag_num);
			byte[] _content = _buf.popAll();
			if (_content.length == 0)
				throw new VASN1Exception("Incomplete data");

			int _len_num;
			int _content_num;
			l_dec = VASN1Definition.berDecLength(_content);
			if (l_dec == null) {
				_len_num = 1;
				_buf.append(_content);
				_buf.pop(1);
				_content = _buf.popAll();
				if (_content.length == 0)
					throw new VASN1Exception("Incomplete data");
				DecodedContent c_dec = VASN1Definition.berDecContentIndefinite(_content);
				_content_num = c_dec.getBytesRead();
			}
			else {
				_len_num = l_dec.getBytesRead();
				_content_num = l_dec.getDecodedLength().getValue().intValue();
			}
			_buf.append(content);
			_content = _buf.pop(_tag_num+_len_num+_content_num);
			if (_content.length < _tag_num+_len_num+_content_num)
				throw new VASN1Exception("Invalid encoding");
			result.elements.add(_content);
			content = _buf.popAll();
		}
		return result;
	}

	@Override
	public synchronized ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (data.length == 0)
			throw new VASN1Exception("No data");
		int tot_length = 0;
		if (withTag) {
			if (data[0] != (byte)0x30)
				throw new VASN1Exception("Invalid explicit tag");
			byte[] _data = new byte[data.length-1];
			for (int i = 0; i < _data.length; i++)
				_data[i] = data[i+1];
			data = _data;
			tot_length += 1;
		}
		ExtractedData dec_elements = VASN1DefSequence.berExtractSequenceElements(data);
		tot_length += dec_elements.numDecodedBytes;
		Iterator<byte[]> iter = dec_elements.elements.iterator();

		VASN1Sequence seq = new VASN1Sequence();
		byte[] item = null;
		for (Element type_data: types) {
			try {
				if (item == null)
					item = iter.next();
			} catch (NoSuchElementException e) {
				if (type_data.defaultValue != null) {
					seq.append(type_data.defaultValue, type_data.name, true);
					continue;
				} else if (type_data.optional)
					continue;
				else
					throw new VASN1Exception("Required element missing");
			}
			Decoded dec_tag = VASN1Tag.fromDER(item);
			VASN1Tag _def_tag = type_data.definition.getTag();
			if (_def_tag != null && !_def_tag.equals(dec_tag.getTag())) {
				// Handle non-matching tag
				if (type_data.defaultValue != null) {
					seq.append(type_data.defaultValue, type_data.name, true);
					continue;
				}
				else if (type_data.optional)
					continue;
				else
				    throw new VASN1Exception("Required element missing");
			}
			ParseResult dec = type_data.definition.parseDER(item);
			seq.append(dec.getResult(), type_data.name, false);
			item = null;
		}

		// Ensure no unprocessed sequence elements left
		try {
			iter.next();
			throw new VASN1Exception("Unprocessed sequence elements");
		} catch (Exception e) {
		}

		return new ParseResult(seq, tot_length);
	}

	/**
	 * Creates an object for this type.
	 */
	public VASN1Sequence create() {
		return this.create(use_explicit);
	}

	/**
	 * Creates an object for this type.
	 *
	 * @param defaultExplicit if true use default explicit encoding
	 */
	public VASN1Sequence create(boolean defaultExplicit) {
		return new VASN1Sequence(defaultExplicit, this.name, this);
	}

	/**
	 * Sequence element information.
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

	/**
	 * Extracted byte-encoded sequence elements.
	 */
	protected static class ExtractedData {
		/**
		 * Sequence elements' byte encoded values (in order).
		 */
		public List<byte[]> elements;
		/**
		 * Number of sequence content bytes decoded to produce element list.
		 */
		public int numDecodedBytes;
	}
}
