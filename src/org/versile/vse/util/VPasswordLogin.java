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
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.entity.VTuple;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * References a login object for user/password authentication.
 *
 * <p>This type is normally returned when a client is attempting to access a
 * resource e.g. via VRI resolution which requires the client to be
 * authenticated, and the client is not authenticated via other means (such as
 * public-key verification on a secure transport).</p>
 *
 * <p>This class is typically not instantiated directly, but is resolved by a
 * VSE resolver when decoding a reference to a remote
 * {@link VPasswordLoginHandler}.</p>
 */
public class VPasswordLogin extends VEntity {

	/**
	 * VSE code for the VPasswordLogin type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"util", "login"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(4, 4));

	VProxy handler;

	/**
	 * Set up login proxy for an implementing handler.
	 *
	 * @param handler reference to implementing handler
	 */
	VPasswordLogin(VProxy handler) {
		this.handler = handler;
	}

	/**
	 * Perform a blocking login attempt.
	 *
	 * @param username username
	 * @param password user's password
	 * @return login result and authorized resource
	 * @throws Exception raised exception
	 */
	public LoginResult login(String username, String password)
		throws Exception {
		Object[] result = VTuple.nativeOf(handler.call("login", username, password));
		if (result.length != 2)
			throw new VCallError("Illegal result of remote login operation");
		boolean success = VBoolean.nativeOf(result[0]);
		Object resource = result[1];
		if (resource instanceof VNone)
			resource = null;
		return new LoginResult(success, resource);
	}

	/**
	 * Perform a non-blocking login attempt.
	 *
	 * @param username username
	 * @param password user's password
	 * @return reference to login result
	 */
	public VCall<LoginResult> nowaitLogin(String username, String password) {
		VCall<LoginResult> asyncResult = new VCall<LoginResult>();

		class ResultHandler implements VCallResultHandler<Object> {
			VCall<LoginResult> asyncResult;
			public ResultHandler(VCall<LoginResult> asyncResult) {
				this.asyncResult = asyncResult;
			}
			@Override
			public void callback(Object result) {
				try {
					Object[] _result = VTuple.nativeOf(result);
					if (_result.length != 2)
						throw new VCallError("Illegal result of remote login operation");
					boolean success = VBoolean.nativeOf(_result[0]);
					Object resource = _result[1];
					if (resource instanceof VNone)
						resource = null;
					asyncResult.silentPushResult(new LoginResult(success, resource));
				}
				catch (Exception e) {
					asyncResult.silentPushException(e);
				}
			}

		}
		class ExceptionHandler implements VCallExceptionHandler {
			VCall<LoginResult> asyncResult;
			public ExceptionHandler(VCall<LoginResult> asyncResult) {
				this.asyncResult = asyncResult;
			}
			@Override
			public void callback(Exception e) {
				asyncResult.silentPushException(e);
			}
		}

		VCall<Object> call = handler.nowait("login", username, password);
		call.addHandlerPair(new ResultHandler(asyncResult), new ExceptionHandler(asyncResult));
		return asyncResult;
	}

	/**
	 * Get the associated password login handler.
	 *
	 * @return handler
	 */
	public final VProxy getHandler() {
		return handler;
	}

	/**
	 * Get a Versile Entity Representation of the proxied function object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VFunction.VSE_CODE.getTags(ctx);
		return new VTagged(handler.get(), tags);
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
				if (tags.length > 0)
					throw new VModuleError("Encoding takes no residual tags");
				VProxy handler = null;
				try {
					handler = VProxy.valueOf(value);
				} catch (VEntityError e1) {
					throw new VModuleError("Encoding value must be an object reference", e1);
				}

				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(new VPasswordLogin(handler));
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	/**
	 * Result of a login operation.
	 */
	public static class LoginResult {
		boolean authorized;
		Object resource;
		/**
		 * Set up with login result.
		 *
		 * @param authorized if true login was authorized
		 * @param resource authorized resource if login successful, otherwise null
		 */
		public LoginResult(boolean authorized, Object resource) {
			this.authorized = authorized;
			this.resource = resource;
		}
		/**
		 * Get login status.
		 *
		 * @return true if login was successful
		 */
		public boolean isAuthorized() {
			return authorized;
		}
		/**
		 * Holds authorized resource if login was successful.
		 *
		 * @return authorized resource (otherwise null)
		 */
		public Object getResource() {
			return resource;
		}
	}
}
