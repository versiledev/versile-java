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

package org.versile.vse.time;

import java.sql.Timestamp;
import java.util.Date;

import org.versile.orb.module.VModuleConverter;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE time related classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VTimeModule extends VSEModule {

	static VTimeModule module = null;

	/**
	 * Get a reference to the VSE time module.
	 *
	 * @return time module
	 */
	static public VTimeModule getModule() {
		if (VTimeModule.module == null)
			VTimeModule.module = new VTimeModule();
		return VTimeModule.module;
	}

	VTimeModule() {
		// Decoder for conversion from VTagged - VUTCTime
		VModuleDecoder.Decoder _decoder = VUTCTime._v_vse_decoder();
		VModuleDecoder decoder = VUTCTime.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Encoder for entity lazy-conversion - VUTCTime
		Class<?>[] classes = new Class<?>[] {new Date(0L).getClass(), new Timestamp(0L).getClass()};
		VModuleConverter.Converter _converter = VUTCTime._v_vse_converter();
		VModuleConverter converter = new VModuleConverter(classes, _converter);
		try {
			this.addConverter(converter);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 5;
	}
}
