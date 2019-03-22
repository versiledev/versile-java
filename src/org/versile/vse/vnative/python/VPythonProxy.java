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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.vse.vnative.VNativeException;
import org.versile.vse.vnative.VNativeProxy;


/**
 * Base class for proxy interfaces to remote python objects.
 */
public abstract class VPythonProxy extends VNativeProxy {

	/**
	 * Set up proxy to native python object.
	 *
	 * @param proxy proxy reference to object
	 * @param tag native type's tag
	 * @throws VEntityError invalid input proxy for operation
	 */
	public VPythonProxy(VProxy proxy, VEntity tag)
			throws VEntityError {
		super(proxy, tag);
	}

	/**
	 * Gets an attribute on the python object.
	 *
	 * @param attribute attribute name
	 * @return attribute
	 * @throws Exception
	 */
	public Object getattr(String attribute)
			throws Exception {
		if (!_v_active)
			throw new VCallError("Native object not activated");
		try {
			Object result = _v_proxy.call("getattribute", new VString(attribute));
			if (_v_active && result instanceof VNativeProxy)
				((VNativeProxy)result)._v_activate();
			return result;
		} catch (VExceptionProxy e) {
			if (e.getValue() instanceof VNativeException)
				((VNativeException)e.getValue())._v_activate();
			throw e;
		}
	}

	/**
	 * Sets an attribute on the python object.
	 *
	 * <p>Calls "__setattr__" on the remote object.</p>
	 *
	 * @param attribute attribute name
	 * @param value target value
	 * @return call result (normally null)
	 * @throws VCallError
	 */
	public Object getattr(String attribute, Object value)
			throws VCallError {
		try {
			return this.methodCall("__setattr__", new Object[] {value});
		} catch (Exception e) {
			throw new VCallError();
		}
	}

	/**
	 * Performs a call the python object (assuming it is callable).
	 *
	 * @param args arguments (or null)
	 * @return call result
	 * @throws Exception
	 */
	public Object call(Object... args)
			throws Exception {
		return this.callWithKeywords(args, null);
	}

	/**
	 * Performs a call the python object (assuming it is callable).
	 *
	 * @param args arguments (or null)
	 * @param kargs arguments (or null)
	 * @return call result
	 * @throws Exception
	 */
	public Object callWithKeywords(Object[] args, Map<String, ?> kargs)
			throws Exception {
		if (!_v_active)
			throw new VCallError("Native object not activated");
		if (args == null)
			args = new Object[0];
		if (kargs == null)
			kargs = new Hashtable<String, Object>();
		try {
			Object result = _v_proxy.call("call", args, kargs);
			if (_v_active && result instanceof VNativeProxy)
				((VNativeProxy)result)._v_activate();
			return result;
		} catch (VExceptionProxy e) {
			if (e.getValue() instanceof VNativeException)
				((VNativeException)e.getValue())._v_activate();
			throw e;
		}
	}


	/**
	 * Performs a method call the python object (assuming it is callable).
	 *
	 * <p>Equivalent to performing an {@link #getattr(String)} lookup followed by
	 * calling {@link #call(Object[])} on the result.</p>
	 *
	 * @param method method name
	 * @param args arguments (or null)
	 * @return call result
	 * @throws Exception
	 */
	public Object methodCall(String method, Object... args)
			throws Exception {
		return this.methodCallWithKeywords(method, args, null);
	}

	/**
	 * Performs a method call the python object (assuming it is callable).
	 *
	 * <p>Equivalent to performing an {@link #getattr(String)} lookup followed by
	 * calling {@link #callWithKeywords(Object[], Map)} on the result.</p>
	 *
	 * @param method method name
	 * @param args arguments (or null)
	 * @param kargs arguments (or null)
	 * @return call result
	 * @throws Exception
	 */
	public Object methodCallWithKeywords(String method, Object[] args, Map<String, ?> kargs)
			throws Exception {
		Object callable = this.getattr(method);
		VPythonProxy _callable;
		try {
			_callable = (VPythonProxy)callable;
		} catch (Exception e) {
			throw new VCallError();
		}
		return _callable.callWithKeywords(args, kargs);
	}

	/**
	 * Gets a list of names in the object's namespace.
	 *
	 * <p>Generates a list by calling "__dir__" on the object and iterating the result. The
	 * resulting iterator throws IllegalStateException if its hasNext or next operation
	 * has an unexpected failure.</p>
	 *
	 * @return names in object namespace
	 * @throws VCallError
	 */
	public abstract String[] dir() throws VCallError;

	/**
	 * Requests an iterator for the python object.
	 *
	 * <p>Calls "__iter__" on the remote object to generate an iterator, and internally calls
	 * "__next__" on the remote-generated iterator when moving to the next element.</p>
	 *
	 * @return iterator
	 * @throws VCallError
	 */
	public Iterator<?> iter()
		throws VCallError {
		class Iter implements Iterator<Object> {
			VPythonProxy proxy;
			VPythonProxy next_callable = null;
			boolean got_next = false;
			Object next_val = null;

			public Iter(VPythonProxy proxy) {
				this.proxy = proxy;
			}

			@Override
			public synchronized boolean hasNext() {
				if (!got_next)
					got_next = this.getNext();
				return got_next;
			}

			@Override
			public synchronized Object next() {
				if (!got_next)
					got_next = this.getNext();
				if (!got_next)
					throw new NoSuchElementException();
				got_next = false;
				return next_val;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			// Return false if no more elements, throw IllegalStateException if other error
			boolean getNext() {
				Object result = null;
				try {
					if (next_callable == null) {
						next_callable = (VPythonProxy)proxy.getattr("__next__");
					}
					result = next_callable.call();
				} catch (VExceptionProxy e) {
					if (e.getValue() instanceof VPythonException) {
						VPythonException p_exc = (VPythonException) e.getValue();
						if (p_exc.getArgs().length() >= 1) {
							try {
								String e_name = VString.nativeOf(p_exc.getArgs().get(0));
								if (e_name.equals("exceptions.StopIteration"))
									return false;
							} catch (VEntityError e1) {
								throw new IllegalStateException();
							}
						}
						else
							throw new IllegalStateException();
					}
					else
						throw new IllegalStateException();
				} catch (Exception e) {
					throw new IllegalStateException();
				}
				got_next = true;
				next_val = result;
				return true;
			}
		}
		try {
			VPythonProxy python_iterator = (VPythonProxy) this.methodCall("__iter__", new Object[0]);
			return new Iter(python_iterator);
		} catch (Exception e) {
			throw new VCallError(e);
		}
	}

}
