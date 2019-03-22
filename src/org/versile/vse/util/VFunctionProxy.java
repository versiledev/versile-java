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

import java.util.Vector;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.util.VCombiner;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.container.VFrozenSet;


/**
 * References a VSE remote function.
 */
public class VFunctionProxy extends VEntity {

	VProxy peer;
	int min_args;
	int max_args;

	/**
	 * Set up proxy to (possibly) remote function object.
	 *
	 * @param peer remote object reference
	 * @param min_args minimum function arguments
	 * @param max_args maximum function arguments (no limit if negative)
	 */
	VFunctionProxy(VProxy peer, int min_args, int max_args) {
		this.peer = peer;
		this.min_args = min_args;
		this.max_args = max_args;
	}

	/**
	 * Performs a blocking call to referenced function.
	 *
	 * @param args arguments
	 * @return function result
	 * @throws Exception raised exception
	 */
	public Object call(Object... args)
			throws Exception {
		return peer.call("peer_call", (Object[])args);
	}

	/**
	 * Performs a void-result blocking call to referenced function.
	 *
	 * @param args arguments
	 * @throws Exception raised exception
	 */
	public void voidCall(Object... args)
			throws Exception {
		peer.voidCall("peer_call", (Object[])args);
	}

	/**
	 * Performs a oneway call to referenced function.
	 *
	 * @param args arguments
	 */
	public void oneway(Object... args) {
		peer.oneway("peer_call", (Object[])args);
	}

	/**
	 * Performs a non-blocking call to referenced function.
	 *
	 * @param args arguments
	 * @return reference to function result
	 */
	public VCall<Object> nowait(Object... args) {
		return peer.nowait("peer_call", (Object[])args);
	}

	/**
	 * Performs a non-blocking void-result call to referenced function.
	 *
	 * @param args arguments
	 * @return reference to function result
	 */
	public VCall<Object> nowaitVoid(Object... args) {
		return peer.nowaitVoid("peer_call", (Object[])args);
	}

	/**
	 * Get documentation string for function (blocking).
	 *
	 * @return doc string (or null)
	 * @throws VCallError call error
	 */
	public String getDoc()
			throws VCallError {

		try {
			Object result = peer.call("peer_doc");
			if (result == null || result instanceof VNone)
				return null;
			else
				return VString.nativeOf(result);
		} catch (Exception e) {
			throw new VCallError();
		}
	}

	/**
	 * Get documentation string for function (blocking).
	 *
	 * @return doc string (or null)
	 * @throws VCallError call error
	 */
	public VCall<String> nowaitGetDoc()
			throws VCallError {
		class ResultHandler implements VCallResultHandler<Object> {
			VCall<String> result;
			public ResultHandler(VCall<String> result) {
				this.result = result;
			}
			@Override
			public void callback(Object result) {
				if (result == null || result instanceof VNone)
					this.result.silentPushResult(null);
				else
					try {
						this.result.silentPushResult(VString.nativeOf(result));
					} catch (Exception e) {
						this.result.silentPushException(new VCallError());
					}
			}
		}
		class ExceptionHandler implements VCallExceptionHandler {
			VCall<String> result;
			public ExceptionHandler(VCall<String> result) {
				this.result = result;
			}
			@Override
			public void callback(Exception e) {
				result.silentPushException(e);
			}
		}
		VCall<Object> call= peer.nowait("peer_doc");
		VCall<String> result = new VCall<String>();
		call.addHandlerPair(new ResultHandler(result), new ExceptionHandler(result));
		return result;
	}

	/**
	 * Get a Versile Entity Representation of the proxied function object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VFunction.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[code_tags.length+2];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		code_tags[code_tags.length] = new VInteger(min_args);
		if (max_args >= 0)
			code_tags[code_tags.length+1] = new VInteger(max_args);
		else
			code_tags[code_tags.length+1] = VNone.get();
		return new VTagged(peer.get(), tags);
	}

	/**
	 * Get VSE decoder for tag data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				class Combiner extends VCombiner {
					@Override
					public Object combine(Vector<Object> objects)
							throws CombineException {
						VTuple elements = null;
						try {
							elements = VTuple.valueOf(objects);
						} catch (VEntityError e) {
							throw new CombineException();
						}
						return new VFrozenSet(elements);
					}
				}
				if (tags.length != 2)
					throw new VModuleError("Illegal use of residual tags");
				VProxy peer = null;
				int min_args = 0;
				int max_args = 0;
				try {
					if (tags[0] == null || tags[0] instanceof VNone)
						min_args = 0;
					else
						min_args = VInteger.nativeOf(tags[0]).intValue();
					if (tags[0] == null || tags[1] instanceof VNone)
						max_args = -1;
					else
						max_args = VInteger.nativeOf(tags[1]).intValue();
					peer = VProxy.valueOf(value);
				} catch (VEntityError e) {
					throw new VModuleError();
				}
				if (min_args < 0 || (max_args >= 0 && min_args > max_args))
					throw new VModuleError();
				VFunctionProxy result = new VFunctionProxy(peer, min_args, max_args);
				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(result);
				return new VCombiner.Pair(Combiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}
}
