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

import org.versile.common.util.VObjectIdentifier;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VTagged;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;
import org.versile.vse.VSECode;


/**
 * Implements a VSE remote function.
 */
public abstract class VFunction extends VExternal {

	/**
	 * VSE code for the VFunction type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"util", "function"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(4, 1));

	int min_args;
	int max_args;
	String doc;
	boolean enabled = true;

	/**
	 * Set up function object.
	 *
	 * @param min_args minimum function arguments
	 * @param max_args maximum function arguments (negative if no limit)
	 * @param doc external documentation string (or null)
	 */
	public VFunction(int min_args, int max_args, String doc) {
		if (min_args < 0 || (max_args >= 0 && min_args < max_args))
			throw new IllegalArgumentException("Invalid min/max argument numbers");
		this.min_args = min_args;
		this.max_args = max_args;
		this.doc = doc;
	}

	/**
	 * Function executed by remote calls to this function.
	 *
	 * @param args function arguments
	 * @return function return value
	 * @throws Exception thrown function exception
	 */
	public abstract Object function(Object... args) throws Exception;

	/**
	 * External method for calling function as remote method call.
	 *
	 * @param args function arguments
	 * @return function return value
	 * @throws Exception thrown function exception
	 */
	@Publish(show=true, ctx=false)
	public Object peer_call(Object... args)
			throws Exception {
		if (args.length < min_args || (max_args >= 0 && args.length > max_args))
			throw new VCallError();
		if (!enabled)
			throw new VCallError("Function was disabled");
		return this.function((Object[])args);
	}

	/**
	 * External method for requesting function object documentation string.
	 *
	 * @return external doc string (or null)
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public Object peer_doc()
			throws VCallError {
		if (!enabled)
			throw new VCallError("Function was disabled");
		return doc;
	}

	/**
	 * Disables calling this function.
	 */
	public void disable() {
		this.enabled = false;
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] code_tags = VFunction.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[code_tags.length+2];
		for (int i = 0; i < code_tags.length; i++)
			tags[i] = code_tags[i];
		tags[code_tags.length] = new VInteger(min_args);
		if (max_args >= 0)
			tags[code_tags.length+1] = new VInteger(max_args);
		else
			tags[code_tags.length+1] = VNone.get();
		VEntity value = this._v_fake_object();
		return new VTagged(value, tags);
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}
}
