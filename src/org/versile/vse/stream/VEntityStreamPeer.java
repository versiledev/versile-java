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

import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VProxy;
import org.versile.orb.external.Publish;
import org.versile.orb.util.VSequenceCallException;
import org.versile.orb.util.VSequenceCallQueue;


/**
 * Local stream interface to a remote VSE VEntityStreamer.
 *
 * <p>The class implements the mechanisms required to perform stream
 * interaction and is intended primarily for internal use by the VSE
 * framework. Stream users should normally interact with the stream
 * through a {@link VObjectStream}.</p>
 *
 * <p>This stream class tracks stream elements as VEntity objects, without
 * lazy-conversion to native representations. See {@link VObjectStreamPeer}
 * for a variation which performs such lazy-conversion.</p>
 */
public class VEntityStreamPeer extends VStreamPeer<VEntity> {

	/**
	 * Set up stream peer with default configuration parameters.
	 */
	public VEntityStreamPeer() {
		super(new VEntityStreamBuffer(), new VStreamConfig());
	}

	/**
	 * Set up stream peer.
	 *
	 * @param config link configuration parameters
	 */
	public VEntityStreamPeer(VStreamConfig config) {
		super(new VEntityStreamBuffer(), config);
	}

	/**
	 * Pushes read data to a read context.
	 *
	 * It is not allowed to push read data for a read context after eos* was
	 * sent for that context. Also, the caller is responsible for making sure
	 * that all read push limits are followed.
	 *
	 * Should only be called by a connected peer.
	 *
	 * @param msg_id seauence call ID
	 * @param read_ctx call ID of receiving read context
	 * @param data data to push (cannot be empty unless 'eos' is true)
	 * @param eos if true then end-of-stream after 'data'
	 */
	@Publish(show=true, ctx=false)
	public void peer_read_push(long msg_id, long read_ctx, VEntity[] data, boolean eos)
			throws VCallError {

		if (msg_id < 0 || read_ctx < 0 || data == null) {
			this._fail("Invalid peer_read_push arguments");
			throw new VCallError();
		}
		synchronized(this) {
			if (_failed || _done)
				return;

			_r_pending += 1;
			if (_r_pending > _r_num) {
				this._fail("Too many read packages");
				throw new VCallError();
			}
			if (data.length == 0 && !eos) {
				this._fail("Invalid read push package");
				throw new VCallError();
			}
			if (data.length > _r_size) {
				this._fail("Read push package too large");
				throw new VCallError();
			}

			class Call extends VSequenceCallQueue.LocalCall {
				long read_ctx;
				VEntity[] data;
				boolean eos;
				public Call(long read_ctx, VEntity[] data, boolean eos) {
					this.read_ctx = read_ctx;
					this.data = data;
					this.eos = eos;
				}
				@Override
				public Object execute() throws Exception {
					_peer_rpush(read_ctx, data, eos);
					return null;
				}
			}
			try {
				_calls.queue(msg_id, new Call(read_ctx, data, eos));
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Connect the stream to a peer streamer.
	 *
	 * <p>The returned stream proxy should be used for all further interaction with
	 * the stream, and references to this stream peer object can be dropped (as
	 * the stream proxy will hold a reference). This method should only be called
	 * once.</p>
	 *
	 * @param peer peer to connect with
	 * @return proxy to connected stream
	 * @throws VStreamError
	 */
	public synchronized VObjectStream<VEntity> connect(VProxy peer)
		throws VStreamError {
		this.initConnect(peer);
		return new VObjectStream<VEntity>(this);
	}
}
