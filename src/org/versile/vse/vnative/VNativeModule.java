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

package org.versile.vse.vnative;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.common.util.VCombiner;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSEModule;
import org.versile.vse.vnative.python.VPython2Exception;
import org.versile.vse.vnative.python.VPython2Proxy;
import org.versile.vse.vnative.python.VPython3Exception;
import org.versile.vse.vnative.python.VPython3Proxy;


/**
 * Module for VSE native type classes.
 *
 * <p>Implementations of native types can be registered with
 * {@link #addNativeHandlers(String, ProxyDecoder, ExceptionDecoder)}, which causes the
 * module to dispatch decoding of
 * encoded native objects to handlers with a matching 'native type' residual
 * tag.</p>
 *
 * <p>If the module is created with "activates" set to True, then decoded
 * {@link org.versile.vse.vnative.VNativeProxy} and
 * {@link org.versile.vse.vnative.VNativeException} objects are automatically
 * activated.</p>
 */
public class VNativeModule extends VSEModule {

	boolean activates = false;
	Map<String, ProxyDecoder> proxy_handlers;
	Map<String, ExceptionDecoder> exception_handlers;
	Lock handler_lock;


	/**
	 * Set up module.
	 *
	 * <p>Sets up module with disabled automatic activation and with all
	 * supported native types registered with the module.</p>
	 */
	public VNativeModule() {
		this.construct(false, true);
	}

	/**
	 * Set up module.
	 *
	 * @param activates if true decoded remote native objects are automatically activated
	 * @param addTypes if true all supported native types are registered with the module
	 */
	public VNativeModule(boolean activates, boolean addTypes) {
		this.construct(activates, addTypes);
	}

	void construct(boolean activates, boolean addTypes) {
		this.activates = activates;
		proxy_handlers = new Hashtable<String, ProxyDecoder>();
		exception_handlers = new Hashtable<String, ExceptionDecoder>();
		handler_lock = new ReentrantLock();

		VModuleDecoder.Decoder _decoder = this.vse_proxy_decoder();
		VModuleDecoder decoder = VNativeObject.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		_decoder = this.vse_exception_decoder();
		decoder = VNativeException.VSE_CODE.generateDecoder(_decoder);
		try {
			this.addDecoder(decoder);
		} catch (VModuleError e) {
			throw new RuntimeException();
		}

		if (addTypes) {
			// Register python 2.x objects
			String tag = "vse-python-2.x";
			ProxyDecoder p_dec = VPython2Proxy._v_vse_proxy_decoder();
			ExceptionDecoder e_dec = VPython2Exception._v_vse_exception_decoder();
			this.addNativeHandlers(tag, p_dec, e_dec);

			// Register python 3.x objects
			tag = "vse-python-3.x";
			p_dec = VPython3Proxy._v_vse_proxy_decoder();
			e_dec = VPython3Exception._v_vse_exception_decoder();
			this.addNativeHandlers(tag, p_dec, e_dec);
		}
	}

	/**
	 * Adds decoding handlers for a native object type tag.
	 *
	 * @param nativeTag tag code for the native object type
	 * @param proxyDecoder decoder for VNativeProxy encoded objects
	 * @param exceptionDecoder decoder for VNativeException encoded objects
	 */
	public void addNativeHandlers(String nativeTag, ProxyDecoder proxyDecoder, ExceptionDecoder exceptionDecoder) {
		synchronized(handler_lock) {
			proxy_handlers.put(nativeTag, proxyDecoder);
			exception_handlers.put(nativeTag, exceptionDecoder);
		}
	}

	/**
	 * Removes decoding handlers associated with a native object type tag.
	 *
	 * @param nativeTag tag code for the native type objects
	 */
	public void removeNativeHandlers(String nativeTag) {
		synchronized(handler_lock) {
			proxy_handlers.remove(nativeTag);
			exception_handlers.remove(nativeTag);
		}
	}

	/**
	 * Decoder which decodes native types with registered handlers, or resolves VNativeProxy.
	 *
	 * @return decoder for native encoded VNativeProxy
	 */
	VModuleDecoder.Decoder vse_proxy_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				if (tags.length != 1)
					throw new VModuleError("Invalid residual tag format");
				VProxy proxy = null;
				String native_type = null;
				try {
					proxy = VProxy.valueOf(value);
					native_type = VString.nativeOf(tags[0]);
				} catch (VEntityError e) {
					throw new VModuleError("Invalid residual tag format");
				}
				ProxyDecoder dec = null;
				synchronized(handler_lock) {
					dec = proxy_handlers.get(native_type);
				}
				if (dec == null) {
					Vector<Object> objs = new Vector<Object>();
					try {
						objs.add(new VNativeProxy(proxy, new VString(native_type)));
					} catch (VEntityError e) {
						throw new VModuleError("Error instantiating native proxy");
					}
					return new Pair(VCombiner.identity(), objs);
				}
				else {
					try {
						return dec.decode(proxy);
					} catch (VCombiner.CombineException e) {
						throw new VModuleError("Error instantiating native exception");
					}
				}
			}
		}
		return new Decoder();
	}

	/**
	 * Decoder which decodes native exception types with registered handlers, or resolves VNativeException.
	 *
	 * @return decoder for native encoded VNativeException
	 */
	VModuleDecoder.Decoder vse_exception_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				if (tags.length != 1)
					throw new VModuleError("Invalid residual tag format");
				VTuple tuple = null;
				String native_type = null;
				try {
					tuple = VTuple.valueOf(value);
					native_type = VString.nativeOf(tags[0]);
				} catch (VEntityError e) {
					throw new VModuleError("Invalid residual tag format");
				}
				ExceptionDecoder dec = null;
				synchronized(handler_lock) {
					dec = exception_handlers.get(native_type);
				}
				if (dec == null) {
					Vector<Object> objs = new Vector<Object>();
					objs.add(new VNativeException(new VString(native_type), tuple));
					return new Pair(VCombiner.identity(), objs);
				}
				else {
					try {
						return dec.decode(tuple);
					} catch (VCombiner.CombineException e) {
						throw new VModuleError("Error instantiating native exception");
					}
				}
			}
		}
		return new Decoder();
	}

	/**
	 * Decoder for a VSE native proxy.
	 *
	 * <p>Decodes a VSE native proxy of known native type.</p>
	 */
	static public abstract class ProxyDecoder {

		/**
		 * Decoder for the value of the VTagged native type representation.
		 *
		 * <p>Returned decoder should produce a {@link VNativeProxy} or
		 * native-converted representation.</p>
		 *
		 * @param proxy VTagged value proxy component
		 * @return decoder structure for native type
		 * @throws VCombiner.CombineException
		 */
		public abstract VCombiner.Pair decode(VProxy proxy)
			throws VCombiner.CombineException;
	}

	/**
	 * Decoder for a VSE native exception.
	 *
	 * <p>Decodes a VSE native exception of known native type.</p>
	 */
	static public abstract class ExceptionDecoder {

		/**
		 * Decode the value of the VTagged native type representation.
		 *
		 * <p>Returned decoder should produce a {@link VNativeException} or
		 * native-converted representation.</p>
		 *
		 * @param value VTagged value component
		 * @return decoder structure for native type
		 * @throws VCombiner.CombineException
		 */
		public abstract VCombiner.Pair decode(VTuple value)
				throws VCombiner.CombineException;
	}

	@Override
	public int getVSEModuleCode() {
		// Must be unique among VSE modules
		return 4;
	}
}
