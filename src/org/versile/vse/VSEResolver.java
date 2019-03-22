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

import org.versile.orb.module.VModule;
import org.versile.orb.module.VModuleError;
import org.versile.orb.module.VModuleResolver;
import org.versile.vse.container.VContainerModule;
import org.versile.vse.math.VMathModule;
import org.versile.vse.semantics.VSemanticsModule;
import org.versile.vse.stream.VStreamModule;
import org.versile.vse.time.VTimeModule;
import org.versile.vse.util.VUtilityModule;
import org.versile.vse.vnative.VNativeModule;


/**
 * Resolver for Versile Standard Entities
 *
 * @param modules modules to add in addition to VSE types
 */
public class VSEResolver extends VModuleResolver {

	static boolean _lazy_arrays = false;

	/**
	 * Set up resolver which includes all VSE types.
	 *
	 * <p>Registers all VSE modules with the resolver, in addition to modules
	 * defined in constructor arguments.</p>
	 *
	 * @param modules modules to include
	 * @param addGlobal if true add globally registered modules
	 */
	public VSEResolver(VModule[] modules, boolean addGlobal) {
		super(null, addGlobal);
		VModule[] vse_modules = VSEResolver.vseModules();
		for (VModule module: vse_modules)
			try {
				this.addModule(module);
			} catch (VModuleError e) {
				// Should never happen
				throw new RuntimeException();
			}
		for (VModule module: modules)
			try {
				this.addModule(module);
			} catch (VModuleError e) {
				// SILENT
			}
	}

	/**
	 * Registers all VSE modules as global modules
	 */
	public static void addToGlobalModules() {
		for (VSEModule mod: VSEResolver.vseModules())
			VModuleResolver._addVSEImport(mod.getVSEModuleCode(), mod);
	}

	/**
	 * Generates instances of all VSE modules
	 *
	 * @return VSE modules
	 */
	public static VSEModule[] vseModules() {
		LinkedList<VSEModule> mods = new LinkedList<VSEModule>();
		mods.add(VContainerModule.getModule());
		mods.add(VMathModule.getModule());
		mods.add(new VNativeModule());
		mods.add(VSemanticsModule.getModule());
		mods.add(VStreamModule.getModule());
		mods.add(VTimeModule.getModule());
		mods.add(VUtilityModule.getModule());
		return mods.toArray(new VSEModule[0]);
	}

	/**
	 * Get global lazy arrays conversion status.
	 *
	 * <p>See {@link #enableLazyArrays(boolean)}.</p>
	 *
	 * @return true if enabled, otherwise false
	 */
	public static boolean lazyArrays() {
		return _lazy_arrays;
	}

	/**
	 * Enables lazy arrays globally.
	 *
	 * <p>See {@link #enableLazyArrays(boolean)}. Equivalent to
	 * enableLazyArrays(true).</p>.
	 */
	public static void enableLazyArrays() {
		VSEResolver.enableLazyArrays(true);
	}

	/**
	 * Set global lazy arrays status.
	 *
	 * <p>Enabling lazy arrays causes
	 * {@link org.versile.orb.entity.VEntity#_v_top_converter(Object,
	 * org.versile.orb.entity.VTaggedParser)} to inspect elements of Object[]
	 * structures for type, and when possible lazy convert to
	 * the appropriate VSE type derived from
	 * {@link org.versile.vse.container.VArrayOf}. If such conversion is not
	 * possible, a conversion to {@link org.versile.orb.entity.VTuple} is
	 * attempted instead (similar to when lazy arrays are disabled).</p>
	 *
	 * @param status if true enable (globally), otherwise disable
	 */
	public static void enableLazyArrays(boolean status) {
		_lazy_arrays = status;
	}
}
