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

package org.versile.vse.semantics;

import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE semantics related classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VSemanticsModule extends VSEModule {

	static VSemanticsModule module = null;

	/**
	 * Get a reference to the VSE semantics module.
	 *
	 * @return semantics module
	 */
	static public VSemanticsModule getModule() {
		if (VSemanticsModule.module == null)
			VSemanticsModule.module = new VSemanticsModule();
		return VSemanticsModule.module;
	}

	VSemanticsModule() {
		// Decoder for conversion from VTagged - VUrlConcept
		VModuleDecoder.Decoder _decoder = VUrlConcept._v_vse_decoder();
		VModuleDecoder decoder = VUrlConcept.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 6;
	}
}
