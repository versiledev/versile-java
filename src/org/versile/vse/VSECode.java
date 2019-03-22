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

package org.versile.vse;

import java.util.LinkedList;

import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleDecoder;


/**
 * Tag code used in VSE encoding.
 */
public class VSECode {

	/**
	 * OID prefix for VSE types
	 */
	static VObjectIdentifier VSE_PREFIX = new VObjectIdentifier(1, 3, 6, 1, 4, 1, 38927, 1);

	String[] name;
	VInteger[] version;
	VObjectIdentifier oid;

	/**
	 * Set up meta-data for VSE encoding.
	 *
	 * <p>One of name and oid must be set. Name and version must either both be set or both be null.</p>
	 *
	 * @param name encoding name (or null)
	 * @param version version (or null)
	 * @param oid identifier (or null)
	 */
	public VSECode(String[] name, VInteger[] version, VObjectIdentifier oid) {
		this.name = name;
		this.version = version;
		this.oid = oid;
	}

	/**
	 * Get VER prefix tags for the OID encoding.
	 *
	 * @return tags (or null)
	 */
	public VEntity[] getOIDCode() {
		if (oid == null)
			return null;

		VInteger[] obj_id = oid.getIdentifiers();
		VInteger[] result = new VInteger[obj_id.length + 1];
		result[0] = new VInteger(10*obj_id.length + 1);
		for (int i = 0; i < obj_id.length; i++)
			result[i+1] = obj_id[i];
		return result;
	}

	/**
	 * Get VER prefix tags for the name/version encoding.
	 *
	 * @return tags (or null)
	 */
	public VEntity[] getNameCode() {
		if (name == null)
			return null;
		VEntity[] result = new VEntity[3];
		result[0] = new VInteger(-1);
		LinkedList<VEntity> v_name = new LinkedList<VEntity>();
		v_name.addLast(new VString("versile"));
		for (String s: name)
			v_name.addLast(new VString(s));
		result[1] = new VTuple(v_name);
		result[2] = new VTuple(version);
		return result;
	}

	/**
	 * Get VER prefix tags for the I/O context.
	 *
	 * @param ctx I/O context
	 * @return tags (or null)
	 */
	public VEntity[] getTags(VIOContext ctx) {
		if (ctx.isPreferOIDEncoding()) {
			if (oid != null)
				return this.getOIDCode();
			else if (name != null)
				return this.getNameCode();
		}
		else {
			if (name != null)
				return this.getNameCode();
			else if (oid != null)
				return this.getOIDCode();
		}
		// This should never happen with properly initialized VSECode
		throw new RuntimeException();
	}

	/**
	 * Create a VRE tagged-value decoder.
	 *
	 * <p>Creates a tagged-value decoder which includes VER tag information.</p>
	 *
	 * @param handler handler for decoding embedded tagged object data
	 * @return decoder for full VER tagged object representation
	 */
	public VModuleDecoder generateDecoder(VModuleDecoder.Decoder handler) {
		String[] _name = null;
		if (name != null) {
			_name = new String[name.length+1];
			_name[0] = "versile";
			for (int i = 0; i < name.length; i++)
				_name[i+1] = name[i];
		}
		VObjectIdentifier _oid = null;
		if (oid != null) {
			VInteger[] prefix = VSECode.VSE_PREFIX.getIdentifiers();
			VInteger[] after = oid.getIdentifiers();
			VInteger[] oid_items = new VInteger[prefix.length + after.length];
			for (int i = 0; i < prefix.length; i++)
				oid_items[i] = prefix[i];
			for (int i = 0; i < after.length; i++)
				oid_items[prefix.length+i] = after[i];
			_oid = new VObjectIdentifier(oid_items);
		}
		return new VModuleDecoder(_name, version, _oid, handler);
	}
}
