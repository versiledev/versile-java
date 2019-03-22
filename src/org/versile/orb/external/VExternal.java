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

package org.versile.orb.external;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VCallContext;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;


/**
 * Base class for VOB conforming {@link org.versile.orb.entity.VObject} classes.
 *
 * <p>Implements the Versile Object Behavior specification for remote method
 * calls and offers convenient mechanisms for publishing native class methods
 * as external methods.</p>
 *
 * <p>Remote methods can be published with the {@link Publish} or {@link PublishAs}
 * annotations. Remote meta methods can be published with {@link Meta} or
 * {@link MetaAs}. Documentation strings for derived classes and remote methods can
 * be set with the {@link Doc} annotation.</p>
 */
public class VExternal extends VObject {

	String _v_class_doc = null;
	Map<String, Map<Method, _v_MethodData>> _v_methods;
	Map<String, Set<Method>> _v_meta_methods;
	Lock _v_methods_lock;

	public VExternal() {
		_v_methods = new Hashtable<String, Map<Method, _v_MethodData>>();
		_v_meta_methods = new Hashtable<String, Set<Method>>();
		_v_methods_lock = new ReentrantLock();

		// Check if class has documentation
		Annotation annotation = this.getClass().getAnnotation(Doc.class);
		if (annotation != null) {
			Doc p_ann = (Doc) annotation;
			_v_class_doc = p_ann.doc();
		}

		// Identify methods published with @Publish or @PublishAs
		for (Method method: this.getClass().getMethods()) {
			_v_MethodData mdata = null;
			String mname = null;
			annotation = method.getAnnotation(PublishAs.class);
			if (annotation != null) {
				PublishAs p_ann = (PublishAs) annotation;
				mdata = new _v_MethodData();
				mdata.show = p_ann.show();
				mdata.ctx = p_ann.ctx();
				mname = p_ann.name();
			}
			else {
				annotation = method.getAnnotation(Publish.class);
				if (annotation != null) {
					Publish p_ann = (Publish) annotation;
					mdata = new _v_MethodData();
					mdata.show = p_ann.show();
					mdata.ctx = p_ann.ctx();
					mname = method.getName();
				}
				else
					continue;
			}
			annotation = method.getAnnotation(Doc.class);
			if (annotation != null) {
				Doc p_ann = (Doc) annotation;
				mdata.doc = p_ann.doc();
			}
			Map<Method, _v_MethodData> _methods = _v_methods.get(mname);
			if (_methods == null) {
				_methods = new Hashtable<Method, _v_MethodData>();
				_v_methods.put(mname,  _methods);
			}
			_methods.put(method, mdata);
		}

		// Identify meta-methods published with @Meta or @MetaAs
		for (Method method: this.getClass().getMethods()) {
			String mname = null;
			annotation = method.getAnnotation(MetaAs.class);
			if (annotation != null) {
				MetaAs p_ann = (MetaAs) annotation;
				mname = p_ann.name();
			}
			else {
				annotation = method.getAnnotation(Meta.class);
				if (annotation != null)
					mname = method.getName();
				else
					continue;
			}
			Set<Method> _methods = _v_meta_methods.get(mname);
			if (_methods == null) {
				_methods = new HashSet<Method>();
				_v_meta_methods.put(mname, _methods);
			}
			_methods.add(method);
 		}
	}

	/**
	 * Dispatch remote call to a registered remote method.
	 *
	 * <p>{@link VExternal} overloads this internally called method to implement dispatching to
	 * registered external methods. Derived classes do not overload this method to implement
	 * remote method calls (as it is done when inheriting directly from
	 * {@link org.versile.orb.entity.VObject}), instead derived classes should register external methods
	 * or meta-methods with the dispatcher framework using annotations or methods for dynamically
	 * registering methods.</p>
	 *
	 * @see org.versile.orb.entity.VObject#_v_execute(java.util.List, org.versile.orb.entity.VCallContext)
	 */
	@Override
	protected final Object _v_execute(List<Object> args, VCallContext ctx)
		throws Exception {
		if (args.size() == 0)
			throw new VCallError();
		Object first = args.get(0);
		if (first == null || first instanceof VNone) {
			// Meta method, identify method
			if (args.size() < 2)
				throw new VCallError();
			Object second = args.get(1);
			String method_name = null;
			if (second instanceof String)
				method_name = (String)second;
			else if (second instanceof VString)
				method_name = ((VString)second).getValue();
			else
				throw new VCallError();
			Method method = null;
			synchronized(_v_methods_lock) {
				Set<Method> _methods = _v_meta_methods.get(method_name);
				if (_methods == null)
					throw new VCallError();

				// Try to identify a method without known incompatibilities and transform
				// the argument list with known required conversions performed
				for (Method m: _methods) {
					List<Object> _m_args = VExternal._v_transform_method_args(m, args, 2);
					if (_m_args != null) {
						method = m;
						args = _m_args;
						break;
					}
				}
				if (method == null)
					throw new VCallError();
			}
			// Prepare list arguments including context object (if required)
			LinkedList<Object> args2 = new LinkedList<Object>();
			Iterator<Object> it = args.iterator();
			it.next();
			it.next();
			while (it.hasNext())
				args2.addLast(it.next());
			// Execute method
			return this._v_int_exec(method, args2);
		}
		else {
			// Regular method call, identify method
			String method_name = null;
			if (first instanceof String)
				method_name = (String)first;
			else if (first instanceof VString)
				method_name = ((VString)first).getValue();
			else
				throw new VCallError();
			Method method = null;
			synchronized(_v_methods_lock) {
				Map<Method, _v_MethodData> _methods = _v_methods.get(method_name);
				if (_methods == null)
					throw new VCallError();

				// Try to identify a method without known incompatibilities and transform
				// the argument list with known required conversions performed
				Vector<Object> args_with_ctx = null;
				for (Method m: _methods.keySet()) {
					List<Object> _args = args;
					boolean _m_ctx = _methods.get(m).ctx;
					if (_m_ctx) {
						if (args_with_ctx == null) {
							args_with_ctx = new Vector<Object>();
							Iterator<Object> iter = args.iterator();
							args_with_ctx.add(iter.next());
							args_with_ctx.add(ctx);
							while(iter.hasNext())
								args_with_ctx.add(iter.next());
						}
						_args = args_with_ctx;
					}
					List<Object> _m_args = VExternal._v_transform_method_args(m, _args, 1);
					if (_m_args != null) {
						method = m;
						args = _m_args;
						break;
					}
				}
				if (method == null)
					throw new VCallError();
			}

			// Prepare list arguments including context object (if required)
			LinkedList<Object> args2 = new LinkedList<Object>();
			Iterator<Object> it = args.iterator();
			it.next();
			while (it.hasNext())
				args2.addLast(it.next());
			// Execute method
			return this._v_int_exec(method, args2);
		}
	}

	private Object _v_int_exec(Method method, List<Object> args)
			throws Exception {
		// Validate number of arguments
		Class<?>[] params = method.getParameterTypes();
		boolean var_args = method.isVarArgs();
		if (var_args) {
			if (args.size() < (params.length-1))
				throw new VCallError();
		}
		else if (args.size() != params.length)
			throw new VCallError();

		// Prepare argument structure for Method.invoke()
		Object[] i_args = new Object[params.length];
		int i = 0;
		Iterator<Object> it2 = args.iterator();
		while (i < params.length-1) {
			i_args[i] = it2.next();
			i += 1;
		}
		if (var_args) {
			try {
				Object var_array = Array.newInstance(params[params.length-1].getComponentType(),
													 args.size()-params.length+1);
				int j = 0;
				while(it2.hasNext()) {
					Array.set(var_array,  j, it2.next());
					j++;
				}
				i_args[i] = var_array;
			} catch (Exception e) {
				throw new VCallError(e);
			}
		}
		else if (it2.hasNext())
			i_args[i] = it2.next();

		// Execute method
		Object result = null;
		try {
			result = method.invoke(this, i_args);
		} catch (IllegalAccessException e) {
			throw new VCallError();
		} catch (IllegalArgumentException e) {
			throw new VCallError("Illegal arguments");
		} catch (InvocationTargetException e) {
			Throwable exc = e.getTargetException();
			if (exc instanceof Exception)
				throw (Exception)exc;
			else
				throw new VCallError();
		} catch (NullPointerException e) {
			throw new VCallError();

		} catch (ExceptionInInitializerError e) {
			throw new VCallError();
		}
		return result;
	}

	/**
	 * Implements the Versile Object Behavior "doc" meta-call.
	 *
	 * <p>If multiple local methods are exposed for the same external method name,
	 * a docstring of one of the methods is returned (null if no docstrings on
	 * any of the methods).</p>
	 *
	 * @param name method name (if null request class docstring)
	 * @return documentation string (or {@link org.versile.orb.entity.VNone})
	 * @throws VCallError invalid input arguments
	 */
	@MetaAs(name="doc")
	public final VEntity _v_doc(Object... name)
		throws VCallError {
		String result = null;
		if (name.length == 0)
			result = _v_class_doc;
		else if (name.length == 1){
			Object method_name = name[0];
			String mname = null;
			if (method_name instanceof String)
				mname = (String)method_name;
			else if (method_name instanceof VString)
				mname = ((VString)method_name).getValue();
			else
				throw new VCallError();
			synchronized(_v_methods_lock) {
				Map<Method, _v_MethodData> _methods = _v_methods.get(mname);
				if (_methods != null) {
					for (Method _m: _methods.keySet()) {
						_v_MethodData mdata = _methods.get(_m);
						if (mdata.doc != null) {
							result = mdata.doc;
							break;
						}
					}
				}
			}
		}
		else
			throw new VCallError();

		if (result == null)
			return VNone.get();
		else
			return new VString(result);
	}

	/**
	 * Implements the Versile Object Behavior "methods" meta-call.
	 *
	 * <p>If multiple local methods are exposed with the same external method name,
	 * the method name is included in the result if one or more of the methods
	 * has the "show" attribute set to true.</p>
	 *
	 * @return list of names of visible published remote methods
	 */
	@MetaAs(name="methods")
	public final VTuple _v_methods() {
		LinkedList<VEntity> names = new LinkedList<VEntity>();
		synchronized(_v_methods_lock) {
			for (String name: _v_methods.keySet()) {

				Map<Method, _v_MethodData> _methods = _v_methods.get(name);
				if (_methods != null) {
					for (Method _m: _methods.keySet()) {
						_v_MethodData mdata = _methods.get(_m);
						if (mdata.show) {
							names.addLast(new VString(name));
							break;
						}
					}
				}
			}
		}
		return new VTuple(names);
	}

	/**
	 * Publish external method using its local name.
	 *
	 * <p>Publishes the method without a doc string.</p>
	 *
	 * @param method method to publish
	 * @param show if true is exposed by the "methods" meta-call
	 * @param ctx if true the method takes an initial VCallContext argument
	 */
	protected void _v_publish(Method method, boolean show, boolean ctx) {
		this._v_publish(method, show, ctx, null);
	}

	/**
	 * Publish an external method using its local name.
	 *
	 * @param method method to publish
	 * @param show if true is exposed by the "methods" meta-call
	 * @param ctx if true the method takes an initial VCallContext argument
	 * @param doc documentation string (or null)
	 */
	protected void _v_publish(Method method, boolean show, boolean ctx, String doc) {
		this._v_publish(method, method.getName(), show, ctx, doc);
	}

	/**
	 * Publish an external method without a doc string.
	 *
	 * @param method method to publish
	 * @param name external method name
	 * @param show if true is exposed by the "methods" meta-call
	 * @param ctx if true the method takes an initial VCallContext argument
	 */
	protected void _v_publish(Method method, String name, boolean show, boolean ctx) {
		this._v_publish(method, name, show, ctx, null);
	}

	/**
	 * Publish an external method.
	 *
	 * @param method method to publish
	 * @param name external method name
	 * @param show if true is exposed by the "methods" meta-call
	 * @param ctx if true the method takes an initial VCallContext argument
	 * @param doc documentation string (or null)
	 */
	protected void _v_publish(Method method, String name, boolean show, boolean ctx, String doc) {
		_v_MethodData mdata = new _v_MethodData();
		mdata.show = show;
		mdata.ctx = ctx;
		mdata.doc = doc;
		synchronized(_v_methods_lock) {
			Map<Method, _v_MethodData> _methods = _v_methods.get(name);
			if (_methods == null) {
				_methods = new Hashtable<Method, _v_MethodData>();
				_v_methods.put(name, _methods);
			}
			_methods.put(method, mdata);
		}
	}

	/**
	 * Unpublish a method so it is no longer remotely available.
	 *
	 * @param method method to unpublish
	 */
	protected void _v_unpublish(Method method) {
		LinkedList<String> names = new LinkedList<String>();
		synchronized(_v_methods_lock) {
			for (String name: _v_methods.keySet()) {
				Map<Method, _v_MethodData> _methods = _v_methods.get(name);
				if (_methods.get(name) != null)
					names.addLast(name);
			}
			for (String key: names) {
				Map<Method, _v_MethodData> _methods = _v_methods.get(key);
				_methods.remove(method);
				if (_methods.isEmpty())
					_v_methods.remove(key);
			}
		}
	}

	/**
	 * Unpublish a method so it is no longer remotely available.
	 *
	 * @param name external name of the method to unpublish
	 */
	protected void _v_unpublish(String name) {
		synchronized(_v_methods_lock) {
			_v_methods.remove(name);
		}
	}

	/**
	 * Transform an argument list into one compatible for method.
	 *
	 * <p>Returns null if method is found to be definitely incompatible with the
	 * argument list. Otherwise returns a list of arguments which converts arguments
	 * that have known type compatibilities.</p>
	 *
	 * <p>If 'skipArgs' is non-zero, that number of initial arguments is considered
	 * separate from method input arguments, and are left untouched in the output list.</p>
	 *
	 * @param method method to test for
	 * @param args input method arguments
	 * @param skipArgs number of initial 'args' elements to skip
	 * @return compatibility-converted arguments (or null if definitely now compatible)
	 */
	protected static List<Object> _v_transform_method_args(Method method, List<Object> args, int skipArgs) {
		Class<?>[] m_types = method.getParameterTypes();

		// Determine various method argument parameters
		boolean has_var_args = method.isVarArgs();
		int num_fixed_args = m_types.length;
		Class<?> var_element_class = null;
		if (has_var_args) {
			num_fixed_args -= 1;
			Class<?> var_array_class = m_types[m_types.length-1];
			var_element_class = var_array_class.getComponentType();
		}

		// Check for length incompatibility
		if (!has_var_args && (args.size()-skipArgs) != num_fixed_args) {
			return null;
		}
		if (has_var_args && (args.size()-skipArgs) < num_fixed_args) {
			return null;
		}

		LinkedList<Object> result = new LinkedList<Object>();
		Iterator<Object> arg_it = args.iterator();

		// Skip arguments as needed
		for (int i = 0; i < skipArgs; i++) {
			result.addLast(arg_it.next());
		}

		// Process fixed arguments
		for (int i = 0; i < num_fixed_args; i++) {
			Object arg = arg_it.next();
			try {
				arg = VExternal._v_transform_argument(arg, m_types[i]);
			} catch (IllegalArgumentException e) {
				return null;
			}
			result.addLast(arg);
		}

		// Process variable arguments
		while (arg_it.hasNext()) {
			Object arg = arg_it.next();
			try {
				arg = VExternal._v_transform_argument(arg, var_element_class);
			} catch (IllegalArgumentException e) {
				return null;
			}
			result.addLast(arg);
		}

		return result;
	}

	class _v_MethodData {
		public boolean show = false;
		public String doc = null;
		public boolean ctx = true;
	}

	/**
	 * Attempts transforming an argument to a target type.
	 *
	 * <p>Converts if a known conversion can be performed. If the argument is
	 * known to be incompatible and cannot be converted, an exception is
	 * raised. Otherwise the argument itself is returned.</p>
	 *
	 * @param arg argument to convert
	 * @param targetType target type to convert to
	 * @return validated and possibly converted argument
	 * @throws IllegalArgumentException known incompatible argument
	 */
	protected static Object _v_transform_argument(Object arg, Class<?> targetType)
			throws IllegalArgumentException {

		// Handle 'null' argument
		if (arg == null || arg instanceof VNone) {
			if (targetType.isArray()) {
				// If target type is an array, return null
				return null;
			}
			else if (targetType.equals(VEntity.class)) {
				// If target is a non-array VEntity, send VNone
				return VNone.get();
			}
			else if (targetType.isPrimitive()) {
				throw new IllegalArgumentException("Cannot assign null references to primitive types");
			}
			else {
			// For any other type return null (assignable to any non-primitive object)
			return null;
			}
		}

		// If argument is assignable to target, return as-is
		if (targetType.isInstance(arg))
			return arg;

		// Handle target types that are arrays
		if (targetType.isArray()) {

			// Trigger possible VEntity conversion to array structure (i.e. VTuple)
			if (arg instanceof VEntity)
				arg = VEntity._v_lazy_native(arg);

			// Handle arrays for primitive types. Internal conversion may generate null pointers which cause
			// a null pointer exception when conversion back to primitive type is performed; we detect and re-raise
			try {
				if (targetType.equals(byte[].class)) {
					try {
						return VBytes.nativeOf(arg);
					} catch (VEntityError e) {
						throw new IllegalArgumentException(e);
					}
				}
				else if (targetType.equals(int[].class)) {
					Integer[] array = (Integer[]) VExternal._v_transform_argument(arg, Integer[].class);
					int[] result = new int[array.length];
					for (int i = 0; i < result.length; i++)
						result[i] = array[i];
					return result;
				}
				else if (targetType.equals(long[].class)){
					Long[] array = (Long[]) VExternal._v_transform_argument(arg, Long[].class);
					long[] result = new long[array.length];
					for (int i = 0; i < result.length; i++)
						result[i] = array[i];
					return result;
				}
				else if (targetType.equals(float[].class)){
					Float[] array = (Float[]) VExternal._v_transform_argument(arg, Float[].class);
					float[] result = new float[array.length];
					for (int i = 0; i < result.length; i++)
						result[i] = array[i];
					return result;
				}
				else if (targetType.equals(double[].class)){
					Double[] array = (Double[]) VExternal._v_transform_argument(arg, Double[].class);
					double[] result = new double[array.length];
					for (int i = 0; i < result.length; i++)
						result[i] = array[i];
					return result;
				}
				else if (targetType.equals(boolean[].class)){
					Boolean[] array = (Boolean[]) VExternal._v_transform_argument(arg, Boolean[].class);
					boolean[] result = new boolean[array.length];
					for (int i = 0; i < result.length; i++)
						result[i] = array[i];
					return result;
				}
			} catch (NullPointerException e) {
				throw new IllegalArgumentException(e);
			}

			// Assuming array holds objects, get component type
			Class<?> targetComponentType = null;
			try {
				targetComponentType = targetType.getComponentType();
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}

			// Handle Byte[] as a special case
			if (targetComponentType.equals(Byte.class)) {
				try {
					byte[] data = VBytes.nativeOf(arg);
					Byte[] result = new Byte[data.length];
					for (int i = 0 ; i < result.length; i++)
						result[i] = data[i];
					return result;
				} catch (VEntityError e) {
					throw new IllegalArgumentException(e);
				}
			}

			// Try to represent the input as an array, doing so if the argument is
			// either an array already, or implements List<?>
			Object[] arg_array = null;
			try {
				if (arg.getClass().isArray())
					arg_array = (Object[]) arg;
				else if (arg instanceof List<?>)
					arg_array = ((List<?>)arg).toArray();
				else
					throw new IllegalArgumentException("Cannot represent input as an array");
			} catch (ClassCastException e) {
				// Handle possible bad casts
				throw new IllegalArgumentException(e);
			}

			// Try to generate an array of the appropriate type
			try {
				Object result = Array.newInstance(targetComponentType, arg_array.length);
				for (int i = 0; i < arg_array.length; i++)
					Array.set(result,  i, VExternal._v_transform_argument(arg_array[i], targetComponentType));
				return result;
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		// Handle non-array object which should match a VEntity type
		if (VEntity.class.isAssignableFrom(targetType)) {
			// Convert to a VEntity data type
			VEntity result;
			try {
				result = VEntity._v_lazy(arg);
			} catch (VEntityError e) {
				throw new IllegalArgumentException(e);
			}
			if (!targetType.isInstance(result))
				throw new IllegalArgumentException();
			return result;
		}

		// Handle non-array object which should match a non-VEntity type
		if (arg instanceof VEntity) {
			// Make sure argument becomes a native type for further processing
			arg = VEntity._v_lazy_native(arg);
			if (arg instanceof VEntity)
				throw new IllegalArgumentException();
		}

		// If lazy-native converted argument is assignable to target, return as-is
		if (targetType.isInstance(arg))
			return arg;

		// Handle conversion to integer types; if a number cannot be represented
		// exactly without loss of precision then conversion fails
		if (targetType.equals(int.class) || targetType.equals(Integer.class)
				|| targetType.equals(long.class) || targetType.equals(Long.class)
				|| targetType.equals(BigInteger.class)) {

			// If argument is a floating point type, try to represent it first as a
			// BigInteger if it represents an integer number
			if (arg instanceof Float || arg instanceof Double || arg instanceof BigDecimal) {
				BigDecimal as_dec = null;
				if (arg instanceof Float)
					as_dec = BigDecimal.valueOf((Float)arg);
				else if (arg instanceof Double)
					as_dec = BigDecimal.valueOf((Double)arg);
				else
					as_dec = (BigDecimal)arg;
				try {
					arg = as_dec.toBigIntegerExact();
				} catch (ArithmeticException e) {
					throw new IllegalArgumentException(e);
				}
			}

			try {
				VInteger normalized = VInteger.valueOf(arg);
				if (targetType.equals(Integer.class) || targetType.equals(int.class)){
					int result = normalized.getValue().intValue();
					if (VInteger.valueOf(result).equals(normalized))
						return result;
					else
						throw new IllegalArgumentException("Number does not fit inside an 'Integer'");
				}
				else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
					long result = normalized.getValue().longValue();
					if (VInteger.valueOf(result).equals(normalized))
						return result;
					else
						throw new IllegalArgumentException("Number does not fit inside an 'Integer'");
				}
				else if (targetType.equals(BigInteger.class))
					return normalized.getBigIntegerValue();
			} catch (VEntityError e) {
				throw new IllegalArgumentException();
			}
		}

		// Handle conversion to floating point types; if a number cannot be represented
		// exactly without loss of precision then conversion fails
		if (targetType.equals(Float.class) || targetType.equals(float.class)
				|| targetType.equals(Double.class) || targetType.equals(double.class)
				|| targetType.equals(BigDecimal.class)) {

			// If argument is of an integer type, translate it to a BigDecimal
			if (arg instanceof Integer || arg instanceof Long || arg instanceof BigInteger) {
				try {
					arg = new BigDecimal(VInteger.valueOf(arg).getBigIntegerValue());
				} catch (VEntityError e) {
					throw new IllegalArgumentException(e);
				}
			}

			if (targetType.equals(BigDecimal.class)) {
				if (arg instanceof Float)
					return BigDecimal.valueOf((Float)arg);
				else if (arg instanceof Double)
					return BigDecimal.valueOf((Double)arg);
				else
					throw new IllegalArgumentException();
			}
			else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
				if (arg instanceof Float)
					return ((Float)arg).doubleValue();
				else if (arg instanceof BigDecimal) {
					double result = ((BigDecimal)arg).doubleValue();
					if (BigDecimal.valueOf(result).equals((BigDecimal)arg))
						return result;
					else
						throw new IllegalArgumentException();
				}
				else
					throw new IllegalArgumentException();
			}
			else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
				if (arg instanceof Double) {
					float result = ((Double)arg).floatValue();
					if ((Double)arg == ((Float)result).doubleValue())
						return result;
					else
						throw new IllegalArgumentException();
				}
				else if (arg instanceof BigDecimal) {
					float result = ((BigDecimal)arg).floatValue();
					if (BigDecimal.valueOf(result).equals((BigDecimal)arg))
						return result;
					else
						throw new IllegalArgumentException();
				}
				else
					throw new IllegalArgumentException();
			}
		}

		// Handle conversion to boolean types
		if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
			try {
				return VBoolean.nativeOf(arg);
			} catch (VEntityError e) {
				throw new IllegalArgumentException(e);
			}
		}

		throw new IllegalArgumentException("Known incompatible type that cannot be converted");
	}
}
