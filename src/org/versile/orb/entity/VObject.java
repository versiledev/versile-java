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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.versile.common.call.VCall;
import org.versile.common.util.VCombiner;
import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.decoder.VObjectDecoder;


/**
 * Represents the VFE VObject type.
 *
 * <p>An object which can be remotely referenced. The default implementation does
 * not provide any external remote-method capabilities. Derived classes can override
 * this behavior.</p>
 */
public class VObject extends VEntity {

	// HARDCODED: max 10 threads for object non-blocking remote call thread pool
	static ThreadPoolExecutor _v_default_executor =
		new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS,
							   new LinkedBlockingQueue<Runnable>() );

	public VObject() {
	}

	@Override
	public Object _v_native() {
		return this._v_proxy();
	}

	/**
	 * Call type for a remote call.
	 *
	 * <p>For a "normal" call result and exceptions are provided. A "noresult" call will always
	 * return "null" as the call result, but throws exceptions similar to a "normal" call.
	 * A "oneway" call does not result provide a call result or exception.</p>
	 */
	public enum _v_CallType {
		/**
		 * Normal call with call result or exception.
		 */
		NORMAL,
		/**
		 * Call which returns VNone in place of any call result.
		 *
		 * <p>Exceptions are still returned as normal.</p>
		 */
		NORESULT,
		/**
		 * Call which does return any call result or exception.
		 */
		ONEWAY};

	/**
	 * Perform a blocking remote method call on the object.
	 *
	 * <p>Initiates a call to {@link #_v_execute(List, VCallContext)} to
	 * perform the method call.</p>
	 *
	 * @param args method arguments
	 * @param type call type
	 * @param ctx call context (or null)
	 * @return method call result (if call type is "normal")
	 * @throws VCallError error performing method call (unless "oneway")
	 * @throws Exception exception raised by method call (unless type "oneway")
	 */
	public Object _v_call(List<Object> args, _v_CallType type, VCallContext ctx)
		throws Exception {
		Object result = null;
		try {
			result = this._v_execute(args,  ctx);
		} catch (VCallError e) {
			if (type != _v_CallType.ONEWAY)
				throw e;
		} catch (VExceptionProxy e) {
			if (type != _v_CallType.ONEWAY)
				throw e;
		}
		if (type == _v_CallType.NORMAL)
			return result;
		else
			return null;
	}

	/**
	 * Perform a non-blocking remote method call on the object.
	 *
	 * <p>Schedules an asynchronous call with the object's associated task processor.
	 * When the call is executed. performs {@link #_v_execute(List, VCallContext)} to
	 * perform the method call.</p>
	 *
	 * @param args method arguments
	 * @param type call type
	 * @param ctx call context (or null)
	 * @return reference to a call result (or null if call is "oneway")
	 */
	public VCall<Object> _v_call_nowait(List<Object> args, _v_CallType type, VCallContext ctx) {
		VCall<Object> result = null;
		if (type != _v_CallType.ONEWAY)
			result = new VObjectCall();
		_NoWaitCall call = new _NoWaitCall(this, result, args, type, ctx);
		_v_default_executor.execute(call);
		return result;
	}

	/**
	 * Executes a remote method call on the object.
	 *
	 * <p>Default implementation throws VCallError, derived classes can override.</p>
	 *
	 * @param args method call arguments
	 * @param ctx method call context (or null)
	 * @return result
	 * @throws VCallError remote method could not be properly called
	 * @throws Exception exception raised by method
	 */
	protected Object _v_execute(List<Object> args, VCallContext ctx)
		throws Exception {
		throw new VCallError();
	}

	@Override
	public VCombiner.Pair _v_native_converter() {
		Vector<Object> obj_list = new Vector<Object>();
		obj_list.add(this._v_proxy());
		return new VCombiner.Pair(null, obj_list);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		if (!(ctx instanceof VObjectIOContext))
			throw new VEntityWriterException("Encoding VObject requires a VObjectIOContext");
		VObjectIOContext o_ctx = (VObjectIOContext) ctx;
		VEncoderData result = new VEncoderData(new byte[] {(byte)0xfc}, new byte[0]);
		Number peer_id;
		try {
			peer_id = o_ctx.localToPeerID(this, true);
		} catch (VEntityError e) {
			throw new VEntityWriterException("Could not generate peer ID");
		}
		result.addEmbedded(new VInteger(peer_id), false);
		return result;
	}

	/**
	 * Generate a reader for reading this entity class from (explicit) serialized data.
	 *
	 * @param ctx serialization I/O context
	 * @return reader
	 */
	public static VEntityReader _v_reader(VIOContext ctx) {
		VEntityReader reader = new VEntityReader();
		try {
			reader.setDecoder(new VObjectDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	/**
	 * Create a proxy reference to this object.
	 *
	 * @return proxy reference
	 */
	public VProxy _v_proxy() {
		return new VProxy(this);
	}

	/**
	 * Creates a fake object for use with object encoding.
	 *
	 * <p>Derived classes of VObject may override entity serialization, however
	 * if the serialized format contains the "raw" serialized format, a
	 * reference to a version of the object which provides that format may
	 * need to be provided to nested encoders. Calling this method generates a
	 * "fake" representation of the VObject which generates the raw entity
	 * encoding.</p>
	 *
	 * @return fake object for use with encoders.
	 */
	public VEntity _v_fake_object() {
		return new FakeObject(this);
	}

	// Standard proxy factory for VObject
	static class _ProxyFactory implements VProxyFactory {
		@Override
		public VProxy create(VObject obj) {
			return new VProxy(obj);
		}
	}

	// Internal structure for handling non-blocking calls
	class _NoWaitCall implements Runnable {
		VObject obj;
		VCall<Object> result;
		List<Object> args;
		_v_CallType type;
		VCallContext ctx;

		public _NoWaitCall(VObject obj, VCall<Object> result, List<Object> args,
							_v_CallType type, VCallContext ctx) {
			this.obj = obj;
			this.result = result;
			this.args = args;
			this.type = type;
			this.ctx = ctx;
		}

		public void run() {
			try {
				Object _result = obj._v_call(args, type, ctx);
				if (type != _v_CallType.ONEWAY)
					result.silentPushResult(_result);
			}
			catch (Exception e) {
				if (type != _v_CallType.ONEWAY)
					result.silentPushException(e);
			}
			finally {
				obj = null;
				result = null;
				args = null;
				type = null;
				ctx = null;
			}
		}
	}

	class FakeObject extends VObject {
		VObject obj;

		public FakeObject(VObject obj) {
			this.obj = obj;
		}

		@Override
		protected Object _v_execute(List<Object> args, VCallContext ctx)
				throws Exception {
			return obj._v_execute(args, ctx);
		}

		@Override
		public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
				throws VEntityWriterException {
			if (!(ctx instanceof VObjectIOContext))
				throw new VEntityWriterException("Encoding VObject requires a VObjectIOContext");
			VObjectIOContext o_ctx = (VObjectIOContext) ctx;
			VEncoderData result = new VEncoderData(new byte[] {(byte)0xfc}, new byte[0]);
			Number peer_id;
			try {
				peer_id = o_ctx.localToPeerID(obj, true);
			} catch (VEntityError e) {
				throw new VEntityWriterException("Could not generate peer ID");
			}
			result.addEmbedded(new VInteger(peer_id), false);
			return result;
		}
	}
}
