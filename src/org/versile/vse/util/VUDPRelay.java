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

import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.vse.VSECode;


/**
 * Relay for making a Versile UDP Transport connection.
 *
 * <p>Implements the standard VUDP Relay procedure for negotiating a
 * Versile UDP Transport peer connection.</p>
 */
public class VUDPRelay extends VEntity {

	/**
	 * VSE code for the VUDPRelay type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"util", "udprelay"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(4, 2));

	VProxy _handler;

	/**
	 * Set up a relay.
	 *
	 * @param handler handler to object implementing relay service
	 */
	public VUDPRelay(VProxy handler) {
		this._handler = handler;
	}

	/**
	 * Get reference to object implementing VUDPRelay handler.
	 *
	 * @return relay handler
	 */
	public VProxy getHandler() {
		return _handler;
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
		throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VUDPRelay.VSE_CODE.getTags(ctx);
		VEntity value = _handler.get();
		return new VTagged(value, tags);
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
				comb_items.add(new VUDPRelay(handler));
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}
}
