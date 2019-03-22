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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTaggedParseUnknown;
import org.versile.orb.entity.VTaggedParser;
import org.versile.orb.entity.VTuple;


/**
 * Parser for Versile Entity Representation encoded data types.
 *
 * <p>Holds a set of {@link VModule} modules and dispatches VER
 * parsing to the appropriate module.</p>
 */
public class VModuleResolver extends VTaggedParser {
	// Globally imported modules
	static Set<VModule> imported_modules = new HashSet<VModule>();
	static Hashtable<Integer, VModule> imported_vse_modules = new Hashtable<Integer, VModule>();
	static Lock imported_modules_lock = new ReentrantLock();

	// Determines whether imported VSE modules are enabled globally
	// as modules, the default is True.
	static boolean enable_vse_modules = true;

	/**
	 * OID prefix for Versile Standard Entities
	 */
	public static final VObjectIdentifier VSE_OID_PREFIX = new VObjectIdentifier(1, 3, 6, 1, 4, 1, 38927, 1);

	Set<VModule> modules;
	VModule proxy_module;

	/**
	 * Set up resolver
	 *
	 * @param modules modules to register with the resolver (or null)
	 * @param addGlobal if True add globally registered modules
	 * @throws IllegalArgumentException cannot register modules (e.g. overlapping names)
	 */
	public VModuleResolver(VModule[] modules, boolean addGlobal)
			throws IllegalArgumentException {
		this.modules = new HashSet<VModule>();
		proxy_module = new VModule();

		if (modules != null)
			for (VModule mod: modules)
				try {
					this.addModule(mod);
				} catch (VModuleError e) {
					throw new IllegalArgumentException(e);
				}

		if (addGlobal)
			for (VModule mod: VModuleResolver.getImports())
				try {
					this.addModule(mod);
				} catch (VModuleError e) {
					throw new IllegalArgumentException(e);
				}
	}

	@Override
	public synchronized Pair decoder(VTagged obj)
		throws VTaggedParseError {
		VEntity value = obj.getValue();
		VEntity[] tags = obj.getTags();
		if (tags.length == 0)
			throw new VTaggedParseError("No tags set on tag object");

		VInteger encoding = null;
		try {
			encoding = VInteger.valueOf(tags[0]);
		} catch (VEntityError e) {
			throw new VTaggedParseError("Invalid VER encoding tag");
		}

		VModuleDecoder decoder = null;
		Object[] decode_tags = null;

		if (encoding.equals(-1)) {
			// Name and version encoded format
			if (tags.length < 3)
				throw new VTaggedParseError("Invalid VER encoding");

			String[] name = null;
			VInteger[] version;
			try {
				VTuple t_names = VTuple.valueOf(tags[1]);
				VTuple t_version = VTuple.valueOf(tags[2]);
				name = new String[t_names.length()];
				for (int i = 0 ; i < t_names.length(); i++)
					name[i] = VString.valueOf(t_names.get(i)).getValue();
				version = new VInteger[t_version.length()];
				for (int i = 0 ; i < t_version.length(); i++)
					version[i] = VInteger.valueOf(t_version.get(i));
			} catch (VEntityError e) {
				throw new VTaggedParseError("Invalid VER encoding");
			}

			try {
				decoder = proxy_module.decoderFromName(name, version);
			} catch (VModuleError e) {
				throw new VTaggedParseError("No associated decoder");
			}
			decode_tags = new Object[tags.length-3];
			for (int i = 0; i < tags.length-3; i++)
				decode_tags[i] = tags[i+3];
		}
		else {
			Integer oid_enc_type = null;
			Integer oid_enc_len = null;
			try {
				oid_enc_len = (Integer)encoding.lazyDivide(10).getValue();
				oid_enc_type = (Integer)encoding.lazyRemainder(10).getValue();
			} catch (Exception e) {
				throw new VTaggedParseError("Invalid OID encoding");
			}
			if (oid_enc_type != 0 && oid_enc_type != 1)
				throw new VTaggedParseError("Invalid Object Identifier encoding");
			if (tags.length < oid_enc_len+1)
				throw new VTaggedParseError("Invalid OID tag structure");
			VInteger[] _oid_tags = new VInteger[oid_enc_len];
			for (int i = 0; i < oid_enc_len; i++) {
				try {
					_oid_tags[i] = VInteger.valueOf(tags[i+1]);
				} catch (VEntityError e) {
					throw new VTaggedParseError("Invalid OID identifier tag(s)");
				}
			}

			decode_tags = new Object[tags.length-(1+oid_enc_len)];
			for (int i = 0; i < tags.length-(1+oid_enc_len) ; i++)
				decode_tags[i] = tags[i+1+oid_enc_len];

			VObjectIdentifier oid = null;
			if (oid_enc_type == 1) {
				VInteger[] prefix = VModuleResolver.VSE_OID_PREFIX.getIdentifiers();
				VInteger[] oid_tags = new VInteger[prefix.length + _oid_tags.length];
				for (int i = 0; i < prefix.length; i++)
					oid_tags[i] = prefix[i];
				for (int i = 0; i < _oid_tags.length ; i++)
					oid_tags[prefix.length+i] = _oid_tags[i];
				oid = new VObjectIdentifier(oid_tags);
			}
			else {
				oid = new VObjectIdentifier(_oid_tags);
			}

			try {
				decoder = proxy_module.decoderFromOID(oid);
			} catch (VModuleError e) {
				throw new VTaggedParseError("No associated decoder");
			}

		}

		try {
			return decoder.getDecoder().decode(value, decode_tags);
		} catch (VModuleError e) {
			throw new VTaggedParseError("Could not generate decoder");
		}
	}

	@Override
	public synchronized Pair converter(Object obj)
			throws VTaggedParseError, VTaggedParseUnknown {
		VModuleConverter conv = null;
		try {
			conv = proxy_module.converterFromType(obj.getClass());
		} catch (VModuleError e) {
			throw new VTaggedParseUnknown("Object class not recognized");
		}

		try {
			return conv.getConverter().convert(obj);
		} catch (VModuleError e) {
			throw new VTaggedParseError("Error generating converter");
		}
	}

	/**
	 * Adds a module to the set of modules handled by the resolver.
	 *
	 * <p>Adding the module will import module decoders defined on the module at
	 * the time of the import. The set of module decoders defined on the module
	 * should not be changed while the module is registered with a resolver.</p>
	 *
	 * @param module module to add
	 * @throws VModuleError conflict with already registered module
	 */
	public synchronized void addModule(VModule module)
		throws VModuleError {
		modules.add(module);
		for (VModuleDecoder dec: module.getDecoders()) {
			proxy_module.addDecoder(dec);
		}
		for (VModuleConverter conv: module.getConverters())
			proxy_module.addConverter(conv);
	}

	/**
	 * Removes a module from the set handled by the resolver.
	 *
	 * @param module module to remove
	 */
	public synchronized void removeModule(VModule module) {
		modules.remove(module);
		for (VModuleDecoder dec: module.getDecoders())
			try {
				proxy_module.removeDecoder(dec);
			} catch (VModuleError e) {
				// SILENT
			}
		for (VModuleConverter conv: module.getConverters())
			try {
				proxy_module.removeConverter(conv);
			} catch (VModuleError e) {
				// SILENT
			}
	}

	/**
	 * Registers a module as a globally available module.
	 *
	 * @param module module to register as import
	 */
	static public void addImport(VModule module) {
		synchronized (VModuleResolver.imported_modules_lock) {
			VModuleResolver.imported_modules.add(module);
		}
	}

	/**
	 * Returns status whether globally added VSE modules are enabled.
	 *
	 * @return true if loaded VSE modules are enabled
	 */
	static public boolean isVSEEnabled() {
		synchronized (VModuleResolver.imported_modules_lock) {
			return VModuleResolver.enable_vse_modules;
		}
	}

	/**
	 * Enables globally added VSE modules.
	 *
	 * Convenience method for calling enableVSE(true).
	 */
	static public void enableVSE() {
		VModuleResolver.enableVSE(true);
	}

	/**
	 * Enables or disables globally added VSE modules.
	 *
	 * Enables or disables VSE modules which have been registered by
     * calling {@link org.versile.vse.VSEResolver#addToGlobalModules()} or by
     * loading the various relevant VSE modules, as globally registered modules.
     *
     * The default behavior is that VSE modules are be enabled.
	 *
	 * @param status if true enable, otherwise disable
	 */
	static public void enableVSE(boolean status) {
		synchronized (VModuleResolver.imported_modules_lock) {
			if (VModuleResolver.enable_vse_modules ^ status)
				for (VModule mod : VModuleResolver.imported_vse_modules.values()) {
					if (status)
						VModuleResolver.imported_modules.add(mod);
					else
						VModuleResolver.imported_modules.remove(mod);
				}
			VModuleResolver.enable_vse_modules = status;
		}
	}

	/**
	 * Registers a VSE module as a globally available module.
	 *
	 * <p>Intended for internal use by the VSE framework, should normally
	 * not be called.</p>
	 *
	 * @param vseCode VSE module code
	 * @param vseModule module to register as import
	 */
	static public void _addVSEImport(int vseCode, VModule vseModule) {
		boolean perform_import;
		synchronized (VModuleResolver.imported_modules_lock) {
			perform_import = !VModuleResolver.imported_vse_modules.containsKey(vseCode);
			if (perform_import)
				VModuleResolver.imported_vse_modules.put(vseCode, vseModule);
		}
		if (perform_import && VModuleResolver.enable_vse_modules)
			VModuleResolver.imported_modules.add(vseModule);
	}

	/**
	 * Get modules registered globally with {@link #addImport(VModule)}.
	 *
	 * @return globally registered modules
	 */
	static public VModule[] getImports() {
		synchronized (VModuleResolver.imported_modules_lock) {
			return VModuleResolver.imported_modules.toArray(new VModule[0]);
		}
	}
}
