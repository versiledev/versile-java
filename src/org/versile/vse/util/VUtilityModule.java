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

package org.versile.vse.util;

import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE utility classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VUtilityModule extends VSEModule {

	static VUtilityModule module = null;

	/**
	 * Get reference to utility module.
	 *
	 * @return module reference
	 */
	static public VUtilityModule getModule() {
		if (VUtilityModule.module == null)
			VUtilityModule.module = new VUtilityModule();
		return VUtilityModule.module;
	}

	VUtilityModule() {
		try {
			// Decoder for conversion from VTagged - VFunctionProxy
			VModuleDecoder.Decoder _decoder = VFunctionProxy._v_vse_decoder();
			VModuleDecoder decoder = VFunction.VSE_CODE.generateDecoder(_decoder);
			this.addDecoder(decoder);

			// Decoder for conversion from VTagged - VUDPRelay
			_decoder = VUDPRelay._v_vse_decoder();
			decoder = VUDPRelay.VSE_CODE.generateDecoder(_decoder);
			this.addDecoder(decoder);

			// Decoder for conversion from VTagged - VUDPRelayedVOP
			_decoder = VUDPRelayedVOP._v_vse_decoder();
			decoder = VUDPRelayedVOP.VSE_CODE.generateDecoder(_decoder);
			this.addDecoder(decoder);

			// Decoder for conversion from VTagged - VPasswordLoginHandler
			_decoder = VPasswordLogin._v_vse_decoder();
			decoder = VPasswordLogin.VSE_CODE.generateDecoder(_decoder);
			this.addDecoder(decoder);

		} catch (VModuleError e) {
			throw new RuntimeException();
		}
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 3;
	}
}
