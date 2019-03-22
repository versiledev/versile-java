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
import java.util.Map;

import org.versile.common.asn1.VASN1Tag.Decoded;



/**
 * Definition for the ASN.1 Choice element.
 */
public class VASN1DefChoice extends VASN1Definition {

	Map<VASN1Tag, VASN1Definition> defs;
	Map<String, VASN1Definition> named_defs;
	boolean use_explicit;

	/**
	 * Sets up a choice type definition.
	 *
	 * <p>Sets up with default explicit encoding and no associated name.</p>
	 */
	public VASN1DefChoice() {
		defs = new Hashtable<VASN1Tag, VASN1Definition>();
		named_defs = new Hashtable<String, VASN1Definition>();
		use_explicit = true;
	}

	/**
	 * Sets up a choice type definition.
	 *
	 * <p>Sets up with default explicit encoding.</p>
	 *
	 * @param name associated name (or null)
	 */
	public VASN1DefChoice(String name) {
		defs = new Hashtable<VASN1Tag, VASN1Definition>();
		named_defs = new Hashtable<String, VASN1Definition>();
		use_explicit = true;
	}

	/**
	 * Sets up a choice type definition.
	 *
	 * @param name associated name (or null)
	 * @param useExplicit if true default use explicit encoding
	 */
	public VASN1DefChoice(String name, boolean useExplicit) {
		defs = new Hashtable<VASN1Tag, VASN1Definition>();
		named_defs = new Hashtable<String, VASN1Definition>();
		use_explicit = useExplicit;
	}

	/**
	 * Adds a tracked type definition without an associated name.
	 *
	 * @param definition definition to add
	 * @throws VASN1Exception
	 */
	public void add(VASN1Definition definition)
			throws VASN1Exception {
		this.add(definition, null);
	}

	/**
	 * Adds a tracked type definition.
	 *
	 * @param definition definition to add
	 * @param name associated name (or null)
	 * @throws VASN1Exception
	 */
	public synchronized void add(VASN1Definition definition, String name)
			throws VASN1Exception {
		VASN1Tag tag = definition.getTag();
		if (tag == null)
			throw new VASN1Exception("Tag required in provided definitions");
		if (defs.get(tag) != null)
			throw new VASN1Exception("Definition with same tag already registered");
		if (name != null && named_defs.get(name) != null)
			throw new VASN1Exception("Definition with same name already registered");
		defs.put(tag, definition);
		if (name != null)
			named_defs.put(name, definition);
	}

	/**
	 * Get named definition.
	 *
	 * @param name definition name
	 * @return associated definition (or null)
	 */
	public synchronized VASN1Definition getNamedDefinition(String name) {
		return named_defs.get(name);
	}

	@Override
	public ParseResult parseDER(byte[] data, boolean withTag)
			throws VASN1Exception {
		if (!withTag)
			throw new VASN1Exception("ASN.1 Choice cannot parse from implicit representation");
		Decoded t_dec = VASN1Tag.fromDER(data);
		VASN1Definition def = defs.get(t_dec.getTag());
		if (def == null)
			throw new VASN1Exception("Choice value's tag not recognized");
		return def.parseDER(data, withTag);
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

}
