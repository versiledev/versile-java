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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VInteger;



/**
 * An ASN.1 Sequence.
 */
public class VASN1Sequence extends VASN1Base implements Iterable<VASN1Base> {

	Vector<Element> elements;
	Map<String, VASN1Base> named_elements;
	boolean default_explicit;

	/**
	 * Set up sequence.
	 *
	 * <p>Sets up with default explicit encoding.</p>
	 */
	public VASN1Sequence() {
		if (this.name == null)
			this.name = "SEQUENCE";
		elements = new Vector<Element>();
		named_elements = new Hashtable<String, VASN1Base>();
		default_explicit = true;
	}

	/**
	 * Set up sequence.
	 *
	 * <p>If 'defaultExplicit' is true, tagged values are default encoded with
	 * explicit encoding; if false they are default encoded with implicit encoding.</p>
	 *
	 * @param defaultExplicit if true tagged values are default explicit encoded
	 */
	public VASN1Sequence(boolean defaultExplicit) {
		if (this.name == null)
			this.name = "SEQUENCE";
		elements = new Vector<Element>();
		named_elements = new Hashtable<String, VASN1Base>();
		default_explicit = defaultExplicit;
	}

	/**
	 * Set up sequence.
	 *
	 * <p>If 'defaultExplicit' is true, tagged values are default encoded with
	 * explicit encoding; if false they are default encoded with implicit encoding.</p>
	 *
	 * @param defaultExplicit if true tagged values are default explicit encoded
	 * @param name type name (or null)
	 * @param definition type definition (or null)
	 */
	public VASN1Sequence(boolean defaultExplicit, String name, VASN1Definition definition) {
		super(name, definition);
		if (this.name == null)
			this.name = "SEQUENCE";
		elements = new Vector<Element>();
		named_elements = new Hashtable<String, VASN1Base>();
		default_explicit = defaultExplicit;
	}

	/**
	 * Get number of elements in sequence.
	 *
	 * @return number of elements
	 */
	public synchronized int length() {
		return elements.size();
	}

	/**
	 * Get element at index.
	 *
	 * @param index sequence index
	 * @return element at position
	 */
	public synchronized VASN1Base get(int index) {
		Element e = elements.get(index);
		if (e == null)
			return null;
		else
			return e.value;
	}

	/**
	 * Get named element of given name.
	 *
	 * @param name named element
	 * @return element (or null if not found)
	 */
	public synchronized VASN1Base get(String name) {
		return named_elements.get(name);
	}

	/**
	 * Get the names of named elements.
	 *
	 * @return names of named elements
	 */
	public synchronized List<String> getNames() {
		Vector<String> result = new Vector<String>();
		for (String _name: named_elements.keySet())
			result.add(_name);
		return result;
	}

	/**
	 * Appends an element to the sequence.
	 *
	 * <p>Adds an element with 'isDefault' set to false, and without an associated name.</p>
	 *
	 * @param value value to append
	 */
	public synchronized void append(VASN1Base value) {
		try {
			this.append(value, null, false);
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Appends an element to the sequence.
	 *
	 * <p>Adds an element with 'isDefault' set to false, and without an associated name.</p>
	 *
	 * @param value value to append
	 * @param name name of named element (or null)
	 * @throws VASN1Exception element name already in use
	 */
	public synchronized void append(VASN1Base value, String name)
			throws VASN1Exception {
		this.append(value, name, false);
	}

	/**
	 * Appends an element to the sequence.
	 *
	 * <p>If 'isDefault' is true, then the sequence is informed that the value of
	 * the provided element is the default for that value in the type's ASN.1
	 * definition, which means the value is not encoded in a DER representation
	 * of the sequence. If set incorrectly, the DER encoded representation will
	 * not decode correctly for that type.</p>
	 *
	 * @param value value to append
	 * @param name name of named element (or null)
	 * @param isDefault if true element's value is default for the ASN.1 type
	 * @throws VASN1Exception element name already in use
	 */
	public synchronized void append(VASN1Base value, String name, boolean isDefault)
			throws VASN1Exception {
		if (name != null && named_elements.get(name) != null)
			throw new VASN1Exception("Name already in use");
		Element e = new Element();
		e.value = value;
		e.isDefault = isDefault;
		elements.add(e);
		if (name != null)
			named_elements.put(name, value);
	}

	/**
	 * Tag for the associated universal type.
	 *
	 * @return tag
	 */
	public static VASN1Tag getUniversalTag() {
		try {
			return VASN1Tag.fromDER(new byte[] {(byte)0x30}).getTag();
		} catch (VASN1Exception e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public VASN1Tag getTag() throws VASN1Exception {
		return VASN1Sequence.getUniversalTag();
	}

	@Override
	public Object getNative(boolean deep) {
		return this;
	}

	@Override
	public byte[] encodeDER(boolean withTag) throws VASN1Exception {
		VByteBuffer buf = new VByteBuffer();
		if (withTag) {
			// X.690: Sequence always has constructed encoding
			buf.append(this.getTag().encodeDER(true));
		}
		VByteBuffer _buf = new VByteBuffer();
		for (Element e: elements) {
			if (!e.isDefault)
				_buf.append(e.value.encodeDER());
		}
		byte[] content = _buf.popAll();
		buf.append(VASN1Base.berEncLengthDefinite(new VInteger(content.length)));
		buf.append(content);
		return buf.popAll();
	}

	@Override
	public synchronized Iterator<VASN1Base> iterator() {
		LinkedList<VASN1Base> list = new LinkedList<VASN1Base>();
		for (Element e: elements)
			list.addLast(e.value);
		return list.iterator();
	}

	protected class Element {
		public VASN1Base value;
		public boolean isDefault;
	}
}
