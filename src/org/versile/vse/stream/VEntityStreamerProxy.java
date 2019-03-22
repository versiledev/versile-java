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

package org.versile.vse.stream;

import java.util.LinkedList;
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
 * Reference to a remote entity streamer.
 *
 * <p>This class is the type decoded by the VSE standard representation
 * of a entity streamer. It is normally not directly instantiated, but is
 * decoded as part of receiving a VSE-encoded representation.</p>
 */
public class VEntityStreamerProxy extends VEntity {

	/**
	 * VSE code for the VEntityStreamer type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"stream", "entitystreamer"},
							              new VInteger[] {new VInteger(0), new VInteger(8)},
							              new VObjectIdentifier(2, 2));

	VProxy streamer;
	VStreamMode mode;

	public VEntityStreamerProxy(VProxy streamer, VStreamMode mode) {
		this.streamer = streamer;
		this.mode = mode.clone();
	}

	/** Initiate a stream connection with the referenced streamer.
	 *
     * <p>This method generates a local-side stream object which
     * connects with the referenced streamer. The method should only
     * be called once.</p>
     *
     * <p>Sets up a connected stream which tracks its local data as
     * VEntity objects.</p>
     *
     * <p>Sets up a default buffer for reading entity data.</p>
	 *
	 * @return local stream connected to referenced streamer
	 */
	public VObjectStream<VEntity> connect()
			throws VStreamError {
		VEntityStreamPeer peer = new VEntityStreamPeer();
		return peer.connect(streamer);
	}

	/** Initiate a stream connection with the referenced streamer.
	 *
     * <p>This method generates a local-side stream object which
     * connects with the referenced streamer. The method should only
     * be called once.</p>
	 *
     * <p>Sets up a connected stream which tracks its local data as
     * VEntity objects.</p>
     *
	 * @return local stream connected to referenced streamer
	 */
	public VObjectStream<VEntity> connect(VStreamConfig config)
			throws VStreamError {
		VEntityStreamPeer peer = new VEntityStreamPeer(config);
		return peer.connect(streamer);
	}

	/** Initiate a stream connection with the referenced streamer.
	 *
     * <p>This method generates a local-side stream object which
     * connects with the referenced streamer. The method should only
     * be called once.</p>
     *
     * <p>Sets up a connected stream which tracks its local data as
     * lazy-converted native representations of VEntity objects.</p>
     *
     * <p>Sets up a default buffer for reading entity data.</p>
	 *
	 * @return local stream connected to referenced streamer
	 */
	public VObjectStream<Object> connectNative()
			throws VStreamError {
		VObjectStreamPeer peer = new VObjectStreamPeer();
		return peer.connect(streamer);
	}

	/** Initiate a stream connection with the referenced streamer.
	 *
     * <p>This method generates a local-side stream object which
     * connects with the referenced streamer. The method should only
     * be called once.</p>
	 *
     * <p>Sets up a connected stream which tracks its local data as
     * lazy-converted native representations of VEntity objects.</p>
     *
	 * @return local stream connected to referenced streamer
	 */
	public VObjectStream<Object> connectNative(VStreamConfig config)
			throws VStreamError {
		VObjectStreamPeer peer = new VObjectStreamPeer(config);
		return peer.connect(streamer);
	}

	/**
	 * Get reference to streamer service object.
	 *
	 * @return proxy reference
	 */
	public VProxy getStreamer() {
		return streamer;
	}

	/**
	 * Get streamer mode flags.
	 *
	 * @return mode flags
	 */
	public VStreamMode getMode() {
		return mode.clone();
	}

	/**
	 * Get a Versile Entity Representation of this object
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VEntityStreamerProxy.VSE_CODE.getTags(ctx);
		LinkedList<VEntity> _tags = new LinkedList<VEntity>();
		for (VEntity tag: tags)
			_tags.addLast(tag);
		_tags.addLast(new VInteger(mode.getFlags()));
		VEntity value = streamer.get();
		return new VTagged(value, _tags.toArray(new VEntity[0]));
	}

	/**
	 * Get VSE decoder for tagged-value data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				if (tags.length != 1)
					throw new VModuleError("Illegal use of residual tags");
				VProxy streamer;
				int mode;
				try {
					streamer = VProxy.valueOf(value);
					mode = VInteger.nativeOf(tags[0]).intValue();
				} catch (VEntityError e) {
					throw new VModuleError("Invalid streamer proxy or mode tag");
				}
				if (mode <= 0)
					throw new VModuleError("Invalid mode tag");
				Vector<Object> objs = new Vector<Object>();
				objs.add(new VEntityStreamerProxy(streamer, new VStreamMode(mode)));
				return new VCombiner.Pair(null, objs);
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
