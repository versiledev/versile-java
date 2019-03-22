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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VCombiner.CombineException;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.vse.vnative.VNativeModule;
import org.versile.vse.vnative.VNativeProxy;


/**
 * Proxy interfaces to a remote python 2.x object.
 */
public class VPython2Proxy extends VPythonProxy {

	/**
	 * Set up proxy to native python object.
	 *
	 * @param proxy proxy reference to object
	 * @throws VEntityError invalid input proxy for operation
	 */
	public VPython2Proxy(VProxy proxy) throws VEntityError {
		super(proxy, new VString("vse-python-2.x"));
	}

	@Override
	public String[] dir()
			throws VCallError {
		LinkedList<String> result = new LinkedList<String>();
		try {
			VPythonProxy list = (VPythonProxy) this.methodCall("__dir__", new Object[0]);
			Iterator<?> it = list.iter();
			while (it.hasNext()) {
				Object _nxt = it.next();
				result.addLast(new String(VBytes.nativeOf(_nxt), "utf8"));
			}
		} catch (Exception e) {
			throw new VCallError(e);
		}
		return result.toArray(new String[0]);
	}

	@Override
	public String toString() {
		if (!_v_active)
			return super.toString();
		try {
			VBytes b_str = VBytes.valueOf(this.methodCall("__str__"));
			try {
				return new String(b_str.getValue(), "utf8");
			} catch (Exception e) {
				return "" + b_str;
			}
		} catch (Exception e) {
			return super.toString();
		}
	}

	/**
	 * Create a decoder for the proxy type.
	 *
	 * @return decoder
	 */
	public static VNativeModule.ProxyDecoder _v_vse_proxy_decoder() {
		class Decoder extends VNativeModule.ProxyDecoder {
			@Override
			public VCombiner.Pair decode(VProxy proxy) throws CombineException {

				VNativeProxy result = null;
				try {
					result = new VPython2Proxy(proxy);
				} catch (VEntityError e) {
					throw new CombineException();
				}
				Vector<Object> objs = new Vector<Object>();
				objs.add(result);
				return new VCombiner.Pair(VCombiner.identity(), objs);
			}
		}
		return new Decoder();
	}
}
