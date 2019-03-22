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

package org.versile.vse.vnative.python;

import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VCombiner.CombineException;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.vse.vnative.VNativeException;
import org.versile.vse.vnative.VNativeModule;


/**
 * Represents a native python 2.x exception.
 */
public class VPython2Exception extends VPythonException {

	/**
	 * Set up exception object.
	 *
	 * @param args exception arguments
	 */
	public VPython2Exception(VEntity[] args) {
		super(new VString("vse-python-2.x"), args);
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		// Currently no native conversion, however could later do type conversion to some convenience mechanisms
		// for instantiating different classes for different known standard python exception encodings.
		Object result = this;
		Vector<Object> objs = new Vector<Object>();
			objs.add(result);
		return new VCombiner.Pair(null, objs);
	}

	/**
	 * Create a decoder for the exception type.
	 *
	 * @return decoder
	 */
	public static VNativeModule.ExceptionDecoder _v_vse_exception_decoder() {
		class Decoder extends VNativeModule.ExceptionDecoder {
			@Override
			public VCombiner.Pair decode(VTuple tuple) throws CombineException {
				VNativeException result = null;
				result = new VPython2Exception(tuple._v_native());
				Vector<Object> objs = new Vector<Object>();
				objs.add(result);
				return new VCombiner.Pair(VCombiner.identity(), objs);
			}
		}
		return new Decoder();
	}
}
