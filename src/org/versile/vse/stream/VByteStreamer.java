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

import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VTagged;
import org.versile.orb.external.Publish;
import org.versile.orb.util.VSequenceCallException;
import org.versile.orb.util.VSequenceCallQueue;


/**
 * Streamer for byte data.
 *
 * <p>Operates on byte data.</p>
 */
public class VByteStreamer extends VStreamer<Byte> {

	/**
	 * Set up streamer with default mode and configuration parameters.
	 *
	 * <p>Enables all stream modes required and allowed by the streamer data on
	 * the streamer. Parameters are similar to {@link VStreamer}.</p>
	 */
	public VByteStreamer(VStreamerData<Byte> data)
			throws VStreamError {
		super(data, new VByteStreamBuffer(),
			  data.getRequiredMode()[0].orWith(data.getOptionalMode()),
			  new VByteStreamerConfig());
	}

	/**
	 * Set up streamer with default configuration parameters.
	 *
	 * <p>Parameters are similar to {@link VStreamer}.</p>
	 */
	public VByteStreamer(VStreamerData<Byte> data, VStreamMode mode)
			throws VStreamError {
		super(data, new VByteStreamBuffer(), mode, new VByteStreamerConfig());
	}

	/**
	 * Set up streamer.
	 *
	 * <p>Parameters are similar to {@link VStreamer}.</p>
	 */
	public VByteStreamer(VStreamerData<Byte> data, VStreamMode mode, VByteStreamerConfig config)
			throws VStreamError {
		super(data, new VByteStreamBuffer(), mode, config);
	}

	/**
	 * Get a VSE streamer proxy reference to the streamer.
	 *
	 * @return streamer proxy reference
	 */
	public VByteStreamerProxy getProxy() {
		return new VByteStreamerProxy(this._v_proxy(), _mode);
	}

	/**
	 * Pushes write data to an active write context.
	 *
	 * <p>Pushing write data is only allowed when in a write context which was
	 * initiated with peer_write_start. It is subject to write push
	 * limits that were negotiated during stream connect handshake.</p>
	 *
	 * <p>Should only be called by a connected peer.</p>
	 *
	 * @param callID call ID
	 * @param writeCtx target write context
	 * @param data data to write
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_write_push(long callID, long writeCtx, byte[] data)
			throws VCallError {
		if (callID < 0 || writeCtx < 0 || data == null) {
			this._fail("Invalid peer_write_push arguments");
			throw new VCallError();
		}
		if (_failed || _done)
			return;

		_w_pending += 1;
		if (_w_pending > _w_num) {
			this._fail("Too many pending write push");
			throw new VCallError();
		}
		if (data.length == 0) {
			this._fail("Write push without data");
			throw new VCallError();
		}
		if (data.length > _w_size) {
			this._fail("Write package too big");
			throw new VCallError();
		}
		Byte[] _data = new Byte[data.length];
		for (int i = 0; i < _data.length; i++)
			_data[i] = data[i];

		class Call extends VSequenceCallQueue.LocalCall {
			long write_ctx;
			Byte[] data;
			public Call(long write_ctx, Byte[] data) {
				this.write_ctx = write_ctx;
				this.data = data;
			}
			@Override
			public Object execute() throws Exception {
				_peer_wpush(write_ctx, data);
				return null;
			}
		}
		try {
			this._calls.queue(callID, new Call(writeCtx, _data));
		} catch (VSequenceCallException e) {
			this._fail("Internal peer_write_push processing error");
			throw new VCallError();
		}
	}

	/**
	 * Creates a fixed-data streamer.
	 *
	 * <p>Creates a streamer connected to a read-only fixed memory-cached data set.
	 * This is a convenience method for creating a fixed streamer data source,
	 * and creating and returning an associated streamer.</p>
	 *
	 * @param data fixed data set to stream
	 * @param allowRew if true allow stream rewind
	 * @param allowFwd if true allow stream forwarding
	 * @param config streamer configuration parameters
	 * @return streamer
	 * @throws VStreamError error creating streamer
	 */
	public static VStreamer<Byte> createFixedDataStreamer(Byte[] data, boolean allowRew,
														  boolean allowFwd,
														  VByteStreamerConfig config)
			throws VStreamError {
		VStreamerData<Byte> s_data = new VByteFixedStreamerData(data);
		VStreamMode mode = s_data.getRequiredMode()[0];
		if (allowRew)
			mode.setSeekRew(true);
		if (allowFwd)
			mode.setSeekFwd(true);
		return new VByteStreamer(s_data, mode, config);
	}

	/**
	 * Get a Versile Entity Representation of this object.
	 *
	 * @param ctx I/O context
	 * @return VER representation
	 */
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] tags = VByteStreamerProxy.VSE_CODE.getTags(ctx);
		LinkedList<VEntity> _tags = new LinkedList<VEntity>();
		for (VEntity tag: tags)
			_tags.addLast(tag);
		_tags.addLast(new VInteger(_mode.getFlags()));
		VEntity value = this._v_fake_object();
		return new VTagged(value, _tags.toArray(new VEntity[0]));
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}
}
