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

package org.versile.vse.stream;

import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE stream classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VStreamModule extends VSEModule {

	static VStreamModule module = null;

	/**
	 * Get reference to the module.
	 *
	 * @return module reference
	 */
	static public VStreamModule getModule() {
		if (VStreamModule.module == null)
			VStreamModule.module = new VStreamModule();
		return VStreamModule.module;
	}

	VStreamModule() {
		// Decoder for conversion from VTagged - VByteStreamProxy
		VModuleDecoder.Decoder _decoder = VByteStreamerProxy._v_vse_decoder();
		VModuleDecoder decoder = VByteStreamerProxy.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VByteStreamProxy
		_decoder = VEntityStreamerProxy._v_vse_decoder();
		decoder = VEntityStreamerProxy.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 2;
	}
}
