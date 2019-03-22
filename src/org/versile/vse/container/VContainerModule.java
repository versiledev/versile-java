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

package org.versile.vse.container;

import java.util.HashSet;
import java.util.Hashtable;

import org.versile.orb.module.VModuleConverter;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;


/**
 * Module for VSE container classes.
 *
 * <p>The module should be retrieved with {@link #getModule}.</p>
 */
public class VContainerModule extends VSEModule {

	static VContainerModule module = null;

	/**
	 * Get a reference to the VSE container module.
	 *
	 * @return container module
	 */
	static public VContainerModule getModule() {
		if (VContainerModule.module == null)
			VContainerModule.module = new VContainerModule();
		return VContainerModule.module;
	}

	VContainerModule() {
		// Decoder for conversion from VTagged - VFrozenSet
		VModuleDecoder.Decoder _decoder = VFrozenSet._v_vse_decoder();
		VModuleDecoder decoder = VFrozenSet.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VFrozenDict
		_decoder = VFrozenDict._v_vse_decoder();
		decoder = VFrozenDict.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VFrozenMultiArray
		_decoder = VFrozenMultiArray._v_vse_decoder();
		decoder = VFrozenMultiArray.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfInt
		_decoder = VArrayOfInt._v_vse_decoder();
		decoder = VArrayOfInt.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfLong
		_decoder = VArrayOfLong._v_vse_decoder();
		decoder = VArrayOfLong.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfVInteger
		_decoder = VArrayOfVInteger._v_vse_decoder();
		decoder = VArrayOfVInteger.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfFloat
		_decoder = VArrayOfFloat._v_vse_decoder();
		decoder = VArrayOfFloat.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfDouble
		_decoder = VArrayOfDouble._v_vse_decoder();
		decoder = VArrayOfDouble.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Decoder for conversion from VTagged - VArrayOfVFloat
		_decoder = VArrayOfVFloat._v_vse_decoder();
		decoder = VArrayOfVFloat.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Encoder for entity lazy-conversion - VFrozenSet
		Class<?>[] classes = new Class<?>[] {(new HashSet<Object>()).getClass()};
		VModuleConverter.Converter _converter = VFrozenSet._v_vse_converter();
		VModuleConverter converter = new VModuleConverter(classes, _converter);
		try {
			this.addConverter(converter);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Encoder for entity lazy-conversion - VFrozenDict
		classes = new Class<?>[] {(new Hashtable<Object, Object>()).getClass()};
		_converter = VFrozenDict._v_vse_converter();
		converter = new VModuleConverter(classes, _converter);
		try {
			this.addConverter(converter);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		// Encoder for entity lazy-conversion - VFrozenMultiArray
		classes = new Class<?>[] {VMultiArray.class};
		_converter = VFrozenMultiArray._v_vse_converter();
		converter = new VModuleConverter(classes, _converter);
		try {
			this.addConverter(converter);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 1;
	}
}
