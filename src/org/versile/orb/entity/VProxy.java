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

package org.versile.orb.entity;

import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.call.VCall;
import org.versile.common.util.VCombiner;


/**
 * Proxy to a {@link VObject} or {@link VReference}.
 *
 * <p>Provides a seamless uniform remote-access interface to a {@link VObject}
 * regardless of whether the proxied object is local or remote. Provides
 * methods for performing Versile Object Behavior compliant remote calls and
 * meta-calls.</p>
 */
public class VProxy {

	VObject _v_obj;

	/**
	 * Set up as proxy to the provided object.
	 *
	 * @param obj proxied object
	 */
	public VProxy(VObject obj) {
		this._v_obj = obj;
	}

	/**
	 * Proxied object.
	 *
	 * @return proxied object
	 */
	public VObject get() {
		return _v_obj;
	}

	/**
	 * Perform blocking remote method call on the proxied object.
	 *
	 * <p>Calling remote methods on a Versile Object Link requires passing arguments
	 * that are derived from {@link VEntity}, however when VOL lazy-conversion is enabled
	 * (currently this is always enabled), arguments can also be passed which are of a type
	 * that are lazy-convertible with {@link VEntity#_v_lazy(Object)}, including other
	 * lazy-convertible types from modules that have been registered with the link (such
	 * as globally loaded VSE types).</p>
	 *
	 * <p>Similarly, if VOL lazy-native conversion is enabled (currently this is always
	 * enabled), the {@link VEntity} return value may be lazy-native converted to a native
	 * type with {@link VEntity#_v_lazy_native(Object)} or a lazy-native-convertible type
	 * from modules which has been registered with a link (such as globally loaded
	 * VSE types).</p>
	 *
	 * <p>Keep in mind the 'args' argument is a variable length argument list for
	 * arguments of type Object, which means a single argument of type Object[] is
	 * interpreted by java as a variable length set of argument. If this method is
	 * intended to be called with a single argument of type Object[], that argument
	 * must first be cast to Object, in order to be interpreted by java as a single
	 * argument.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return result of remote method call
	 * @throws VCallError error invoking remote method call
	 * @throws Exception exception raised by remote method
	 */
	public Object call(String name, Object... args)
			throws Exception {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call(send_args, VObject._v_CallType.NORMAL, null);
	}

	/**
	 * Perform blocking void-result remote method call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments and technical considerations related to using variable-length
	 * arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @throws VCallError error invoking remote method call
	 * @throws Exception exception raised by remote method
	 */
	public void voidCall(String name, Object... args)
			throws Exception {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		_v_obj._v_call(send_args, VObject._v_CallType.NORESULT, null);
	}

	/**
	 * Perform oneway remote method call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments and technical considerations related to using variable-length
	 * arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 */
	public void oneway(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		_v_obj._v_call_nowait(send_args, VObject._v_CallType.ONEWAY, null);
	}

	/**
	 * Perform non-blocking remote method call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments, lazy-native conversion of return value, and technical
	 * considerations related to using variable-length arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return reference to call result
	 */
	public VCall<Object> nowait(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call_nowait(send_args, VObject._v_CallType.NORMAL, null);
	}

	/**
	 * Perform non-blocking void-result remote method call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments, lazy-native conversion of return value, and technical
	 * considerations related to using variable-length arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return reference to call result
	 */
	public VCall<Object> nowaitVoid(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call_nowait(send_args, VObject._v_CallType.NORESULT, null);
	}

	/**
	 * Perform blocking remote VOB meta-call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments, lazy-native conversion of return value, and technical
	 * considerations related to using variable-length arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return result of remote method call
	 * @throws VCallError error invoking remote method call
	 * @throws Exception exception raised by remote method
	 */
	public Object metaCall(String name, Object... args)
			throws Exception {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(null);
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call(send_args, VObject._v_CallType.NORMAL, null);
	}

	/**
	 * Perform blocking void-result remote VOB meta-call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments and technical considerations related to using variable-length
	 * arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @throws VCallError error invoking remote method call
	 * @throws Exception exception raised by remote method
	 */
	public void metaVoidCall(String name, Object... args)
			throws Exception {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(null);
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		_v_obj._v_call(send_args, VObject._v_CallType.NORESULT, null);
	}

	/**
	 * Perform blocking oneway remote VOB meta-call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments and technical considerations related to using variable-length
	 * arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 */
	public void metaOneway(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(null);
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		_v_obj._v_call_nowait(send_args, VObject._v_CallType.ONEWAY, null);
	}

	/**
	 * Perform non-blocking remote VOB meta-call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments, lazy-native conversion of return value, and technical
	 * considerations related to using variable-length arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return reference to call result
	 */
	public VCall<Object> metaNowaitCall(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(null);
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call_nowait(send_args, VObject._v_CallType.NORMAL, null);
	}

	/**
	 * Perform non-blocking void-result remote VOB meta-call on the proxied object.
	 *
	 * <p>See {@link #call(String, Object...)} for comments regarding lazy-conversion
	 * of passed arguments and technical considerations related to using variable-length
	 * arguments.</p>
	 *
	 * @param name remote method name (VOB standard)
	 * @param args remote method arguments
	 * @return reference to call result
	 */
	public VCall<Object> metaNowaitVoid(String name, Object... args) {
		LinkedList<Object> send_args = new LinkedList<Object>();
		send_args.addLast(null);
		send_args.addLast(new VString(name));
		for (Object o: args)
			send_args.addLast(o);
		return _v_obj._v_call_nowait(send_args, VObject._v_CallType.NORESULT, null);
	}

	/**
	 * List published method names.
	 *
	 * <p>Performs the Versile Object Behavior "methods" meta-call to list
	 * method names of methods (visibly) published by the object's remote-method
	 * interface.</p>
	 *
	 * @return method names of remotely exposed methods
	 * @throws VCallError error retrieving method name list
	 */
	public String[] methods()
		throws VCallError {
		LinkedList<String> list = new LinkedList<String>();
		Object remote_list = null;
		try {
			remote_list = this.metaCall("methods");
		} catch (Exception e) {
			throw new VCallError("Could not retrieve remote methods list");
		}
		if (remote_list instanceof VTuple) {
			for (VEntity item: (VTuple)remote_list) {
				if (item instanceof VString)
					list.addLast(((VString)item).getValue());
				else
					throw new VCallError("Invalid remote method list");
			}
		}
		else if (remote_list instanceof Object[]) {
			for (Object item: (Object[])remote_list) {
				if (item instanceof String)
					list.addLast((String)item);
				else
					throw new VCallError("Invalid remote method list");
			}
		}
		else
			throw new VCallError("Invalid remote method list");
		String[] result = new String[0];
		result = list.toArray(result);
		return result;
	}

	/**
	 * Get object's documentation string
	 *
	 * <p>Performs the Versile Object Behavior "doc" meta-call to get a
	 * documentation string for the object.</p>
	 *
	 * @return object documentation string (or null)
	 * @throws VCallError documentation meta-call error
	 */
	public String doc()
		throws VCallError {
		Object remote_result = null;
		try {
			remote_result = this.metaCall("doc");
		} catch (Exception e) {
			throw new VCallError("Error performing doc meta-call");
		}
		if (remote_result == null || remote_result instanceof VNone)
			return null;
		else if (remote_result instanceof String)
			return (String)remote_result;
		else if (remote_result instanceof VString)
			return ((VString)remote_result).getValue();
		else
			throw new VCallError("Error performing doc meta-call");
	}

	/**
	 * Get the documentation string of a remote method.
	 *
	 * <p>Performs the Versile Object Behavior "doc" meta-call to get a
	 * documentation string for the method.</p>
	 *
	 * @param method method name
	 * @return object documentation string (or null)
	 * @throws VCallError documentation meta-call error
	 */
	public String doc(String method)
		throws VCallError {
		Object remote_result = null;
		try {
			remote_result = this.metaCall("doc", new VString(method));
		} catch (Exception e) {
			throw new VCallError("Error performing doc meta-call");
		}
		if (remote_result == null || remote_result instanceof VNone)
			return null;
		else if (remote_result instanceof String)
			return (String)remote_result;
		else if (remote_result instanceof VString)
			return ((VString)remote_result).getValue();
		else
			throw new VCallError("Error performing doc meta-call");
	}

	/**
	 * Lazy-converts a {@link VProxy} or {@link VObject} to a proxy.
	 *
	 * <p>Raises an exception if the input object is not one of the above types.</p>
	 *
	 * @param obj object to represent as a proxy
	 * @return proxy for object
	 * @throws VEntityError cannot represent as a proxy
	 */
	public static VProxy valueOf(Object obj)
		throws VEntityError {
		if (obj instanceof VProxy)
			return (VProxy) obj;
		else if (obj instanceof VObject)
			return ((VObject)obj)._v_proxy();
		else
			throw new VEntityError("Cannot represent as a VProxy");
	}

	/**
	 * Generates a native converter structure for the entity's type.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @return combiner structure for conversion.
	 */
	public static VCombiner.Pair _v_converter(Object obj)
			throws VEntityError {
		VObject value;
		try {
			value = ((VProxy)obj).get();
		} catch (Exception e) {
			throw new VEntityError("Cannot convert object");
		}
		Vector<Object> val_list = new Vector<Object>();
		val_list.add(value);
		return new VCombiner.Pair(null, val_list);
	}
}
