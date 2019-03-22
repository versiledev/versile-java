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

package org.versile.orb.module;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VInteger;



/**
 * Decoder for a VER encoded representation.
 */
public class VModuleDecoder {

	String[] name;
	VInteger[] version;
	VObjectIdentifier oid;
	Decoder decoder;

	/**
	 * Initialize decoder data.
	 *
	 * <p>At least one of 'name' and 'oid' must be set. 'name' and 'version' must either both
	 * be set or none of them can be set.</p>
	 *
	 * @param name encoded-format name list (or null)
	 * @param version name-encoded-format version (or null)
	 * @param oid encoded-format object identifier (or null)
	 * @param decoder decoder resolving provided name/version or oid
	 * @throws IllegalArgumentException invalid name/version/oid combination
	 */
	public VModuleDecoder(String[] name, VInteger[] version, VObjectIdentifier oid, Decoder decoder)
		throws IllegalArgumentException {
		if (name == null && oid == null)
			throw new IllegalArgumentException("name or oid must be set");
		if ((name == null && version != null) || (name != null && version == null))
			throw new IllegalArgumentException("name and path must either both be set or none");
		this.name = name;
		this.version = version;
		this.oid = oid;
		this.decoder = decoder;
	}

	/**
	 * Get VER-format name list if set.
	 *
	 * @return name list (or null)
	 */
	public String[] getName() {
		return name;
	}

	/**
	 * Get VER-format version if set.
	 *
	 * @return version (or null)
	 */
	public VInteger[] getVersion() {
		return version;
	}

	/**
	 * Get VER-format object identifier if set.
	 *
	 * @return identifier (or null)
	 */
	public VObjectIdentifier getOID() {
		return oid;
	}

	/**
	 * Get decoding handler.
	 *
	 * @return decoding handler
	 */
	public Decoder getDecoder() {
		return decoder;
	}

	/**
	 * Decoder for decoding a VER encoded data type.
	 */
	public abstract static class Decoder {
		/**
		 * Decode VTagged data as a VEntity.
		 *
		 * <p>Value and residual tags may be {@link org.versile.orb.entity.VEntity} however they
		 * may also have been converted to a native representation. The residual tags are the tags
		 * following the VER tags identifying the data type.</p>
		 *
		 * @param value tag-object value
		 * @param tags tag-object residual tags
		 * @return decoder for performing a decode operation
		 * @throws VModuleError error decoding
		 */
		public abstract VCombiner.Pair decode(Object value, Object[] tags)
			throws VModuleError;
	}
}
