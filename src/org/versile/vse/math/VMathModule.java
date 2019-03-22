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

package org.versile.vse.math;

import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE math related classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VMathModule extends VSEModule {

	static VMathModule module = null;

	/**
	 * Get a reference to the VSE math module.
	 *
	 * @return math module
	 */
	static public VMathModule getModule() {
		if (VMathModule.module == null)
			VMathModule.module = new VMathModule();
		return VMathModule.module;
	}

	VMathModule() {
		// Decoder for conversion from VTagged - VPrefixedUnit
		VModuleDecoder.Decoder _decoder = VPrefixedUnit._v_vse_decoder();
		VModuleDecoder decoder = VPrefixedUnit.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VDimensionalQuantity
		_decoder = VDimensionalQuantity._v_vse_decoder();
		decoder = VDimensionalQuantity.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 7;
	}
}
