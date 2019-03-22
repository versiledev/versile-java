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

import org.versile.common.call.VCall;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;
import org.versile.orb.util.VSequenceCallException;
import org.versile.orb.util.VSequenceCallQueue;
import org.versile.orb.util.VSequenceCaller;


/**
 * Streaming interface to a streamer data source.
 *
 * <p>A {@link VStreamer} publishes remotely accessible methods for interacting
 * with stream data. A remote {@link VStreamPeer} object can connect to the
 * streamer by calling {@link #peer_connect} to perform a streamer-connection
 * handshake.</p>
 *
 * @param <T> data type of streamed data
 */
public abstract class VStreamer<T> extends VExternal {

	enum ContextMode {NONE, READING, WRITING};

	VStreamerData<T> _streamdata;

	VStreamBuffer<T> _buf;

	boolean _done = false;                      // if true has initiated peer_close()
	boolean _failed = false;                    // if true stream has failure condition

	VStreamMode _mode;                          // configured mode

	int _call_lim;
	VSequenceCallQueue _calls;

	int _r_num = -1;
	int _r_size = -1;
	int _r_pending = 0;

	int _w_num;
	int _w_size;
	int _w_pending = 0;

	long _w_buf_lim = 0;                        // Relative limit for write data elements from peer
	long _w_buf_step = 0;                       // Step size for increasing write limit

	VProxy _peer = null;
	VSequenceCaller _caller;

	long _ctx = 0L;                             // first msg_id of current context
	ContextMode _ctx_mode = ContextMode.NONE;   // current stream mode
	boolean _ctx_err = false;                   // error condition on current context
	boolean _ctx_sent_err = false;              // if true error was sent to peer on current context
	int _ctx_err_code = 0;                      // error code for error condition
	long _ctx_spos;                             // start position current context
	long _ctx_rpos = 0;                         // relative position in current context

	long _r_rel_lim = 0;                        // current relative position receive limit
	boolean _r_eos = false;                     // if true then EOS mode is set on stream
	boolean _r_sent_eos = false;                // if true then EOS was sent to peer

	long _w_rel_lim = 0;
	long _w_recv = 0;

	long _peer_r_num = 0;
	long _peer_r_size = 0;

	int _local_r_num;
	int _local_r_size;

	VStreamerData.Endpoint _start;              // streamer data start position
	VStreamerData.Endpoint _end;                // streamer data end position

	/**
	 * Sets up the streamer.
	 *
	 * <p>If write-mode is enabled on the stream then 'writeBuffer' must be set.</p>
	 *
	 * <p>The config object limits for maximum read packages and maximum read package size
     * are locally set limits. Read limits received from a connected stream peer during
     * connect handshake will be truncated so they do not exceed local limits.</p>
	 *
	 * @param data streamer data source/sink
	 * @param buffer buffer for stream data operations
	 * @param mode streamer mode
	 * @param config stream configuration data
	 * @throws VStreamError error setting up streamer
	 */
	public VStreamer(VStreamerData<T> data, VStreamBuffer<T> buffer,
				     VStreamMode mode, VStreamerConfig config)
			throws VStreamError {

		VStreamMode[] _data_mode = data.getRequiredMode();
		VStreamMode req_mode = _data_mode[0];
		VStreamMode req_mask = _data_mode[1];
		if (!req_mode.equals(mode.andWith(req_mask)))
			throw new VStreamError("Mismatch with required streamer data mode settings");
		VStreamMode max_mode = req_mask.orWith(data.getOptionalMode());
		if (!mode.subtract(max_mode).isEmpty())
			throw new VStreamError("All mode bits not supported by streamer data");

		if (!mode.validate())
			throw new VStreamError("Invalid mode combination");
		if (mode.writable() && buffer == null)
			throw new VStreamError("Writable stream requires a write buffer");

		_streamdata = data;
		_mode = mode;
		_buf = buffer;

		_w_num = config.getMaxWritePackages();
		_w_size = config.getMaxWritePkgSize();

		_call_lim = _w_num + config.getMaxCalls();
		_calls = new VSequenceCallQueue(_call_lim);
		_caller = new VSequenceCaller(0);

		_ctx_spos = _streamdata.getPosition();

		_local_r_num = config.getMaxReadPackages();
		_local_r_size = config.getMaxReadPkgSize();

		// Initializes _w_buf_lim and _w_buf_step
		this.setWriteBuffering(config.getWriteLim(), config.getWriteStep());

		// Start tracking streamdata endpoints
		VStreamerData.Endpoint[] _endpoints = _streamdata.getEndpoints();
		_start = _endpoints[0];
		_end = _endpoints[1];
		_streamdata.setStreamer(this);
		_streamdata.enableNotifications(VStreamerData.NOTIFY_ENDPOINTS);
	}

	/**
	 * Connect a peer stream object with the streamer.
	 *
	 * <p>Peer must implement the {@link VStreamPeer} published methods.</p>
	 *
	 * <p>The returned 'mode' parameter is a bitwise or of the mode bits
	 * for the stream. 'pos' is the current absolute stream position.
	 * 'callLim' is the max pending calls accepted by the streamer (in addition
	 * to write push calls). 'maxWritePkg' is the max pending write push calls
	 * accepted by the streamer. 'maxWriteSize' is the max number of elements
	 * per write push call.</p>
	 *
	 * <p>Should only be called by a connecting peer, and should only be
	 * called once.</p>
	 *
	 * @param peer connecting peer
	 * @param maxCalls max pending calls (in addition to read packages)
	 * @param maxReadPkg maximum pending read push calls
	 * @param maxReadSize maximum elements per read push call
	 * @return VSE handshake tuple (mode, pos, callLim, maxWritePkg, int maxWriteSize)
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized VTuple peer_connect(VProxy peer, int maxCalls, int maxReadPkg, int maxReadSize)
			throws VCallError {
		if (maxCalls < 1 || maxReadPkg < 1 || maxReadSize < 1) {
			this._fail("Invalid peer_connect arguments");
			throw new VCallError();
		}

		if (_peer != null || _done)
			throw new VCallError("Can only connect once");
		_peer = peer;
		_caller.setLimit(maxCalls);

		_peer_r_num = maxReadPkg;
		_r_num = maxReadPkg;
		if (_local_r_num >= 0 && _local_r_num < _r_num)
			_r_num = _local_r_num;

		_peer_r_size = maxReadSize;
		_r_size = maxReadSize;
		if (_local_r_size >= 0 && _local_r_size < _r_size)
			_r_size = _local_r_size;

		long pos = _ctx_spos + _ctx_rpos;
		return new VTuple(new VEntity[] {new VInteger(_mode.getFlags()), new VInteger(pos),
		                                 new VInteger(_call_lim), new VInteger(_w_num),
		                                 new VInteger(_w_size)});
	}

	/**
	 * Initiate a read context.
	 *
	 * <p>If 'pos' is VNone then the current position is used if the previous
	 * context was a write context. Initiating a new read context with 'pos' set
	 * to VNone is not allowed.</p>
	 *
	 * <p>If 'pos' is not None, 'pos_ref' must be the VSE code for a
	 * position base reference.</p>
	 *
	 * <p>If 'endEOS' is true then reaching the end of the stream should trigger
	 * sending end-of-stream to peer.</p>
	 *
	 * <p>The streamer acknowledges the new read context by calling 'peer_notify_push'
	 * on the connected peer.</p>
	 *
	 * <p>Should only be called by a connected peer.</p>
	 *
	 * @param callID message call ID
	 * @param pos target read position relative to position reference (or null/VNone)
	 * @param posRef VSE code for position reference (or null/VNone)
	 * @param endEOS if true endpoint should trigger end-of-stream on context
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_read_start(long callID, VEntity pos,
											 VEntity posRef, boolean endEOS)
			throws VCallError {

		boolean have_pos;
		long _pos = 0;
		VStream.PosBase _pos_ref = null;

		if (pos == null || pos instanceof VNone)
			have_pos = false;
		else
			try {
				have_pos = true;
				_pos = VInteger.nativeOf(pos).longValue();
				int _ref_code = VInteger.nativeOf(posRef).intValue();
				_pos_ref = VStream.intToPosRef(_ref_code);
			} catch (VEntityError e) {
				this._fail("Invalid position arguments");
				throw new VCallError();
			} catch (VStreamException e) {
				this._fail("Invalid position arguments");
				throw new VCallError();
			}
		if (callID < 0) {
			this._fail("Invalid call argument");
			throw new VCallError();
		}

		if (_failed || _done)
			throw new VCallError();

		if (!_mode.readable()) {
			this._fail("Stream not readable");
			throw new VCallError();
		}

		class Call extends VSequenceCallQueue.LocalCall {
			long msg_id;
			boolean have_pos;
			long pos;
			VStream.PosBase pos_ref;
			boolean eos;
			public Call(long msg_id, boolean have_pos, long pos, VStream.PosBase pos_ref,
					    boolean eos) {
				this.msg_id = msg_id;
				this.have_pos = have_pos;
				this.pos = pos;
				this.pos_ref = pos_ref;
				this.eos = eos;
			}
			@Override
			public Object execute() throws Exception {
				_peer_rstart(msg_id, have_pos, pos, pos_ref, eos);
				return null;
			}
		}
		try {
			_calls.queue(callID, new Call(callID, have_pos, _pos, _pos_ref, endEOS));
		} catch (VSequenceCallException e) {
			this._fail("Call error");
			throw new VCallError();
		}
	}

	/**
	 * Initiate a write context.
	 *
	 * <p>Parameters are similar to {@link #peer_read_start} except it does
	 * not take an end-of-stream parameters, as write-mode does not trigger
	 * end-of-stream.</p>
	 *
	 * <p>If 'pos' is null or VNone then the current position is used if the
	 * previous context was a read context. Initiating a write context with
	 * 'pos' null or VNone when the previous context was a write context is
	 * not allowed.</p>
	 *
	 * <p>Initiating a write context with 'pos' set to null or VNone or
	 * relative to the current position could cause unwanted side-effects
	 * if read-ahead is in effect as read-ahead will normally cause the
	 * streamer position to be different from the current position tracked
	 * by a stream peer.</p>
	 *
	 * <p>The streamer acknowledges the new read context by calling
	 * peer_notify_push on the connected peer.</p>
	 *
	 * @param callID call ID
	 * @param pos position relative to reference (or null/VNone)
	 * @param posRef position reference code
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_write_start(long callID, VEntity pos, VEntity posRef)
			throws VCallError {
		boolean have_pos;
		long _pos = 0;
		VStream.PosBase _pos_ref = null;
		if (pos == null || pos instanceof VNone) {
			have_pos = false;
		}
		else {
			have_pos = true;
			try {
				_pos = VInteger.nativeOf(pos).longValue();
				int _pos_code = VInteger.nativeOf(posRef).intValue();
				_pos_ref = VStream.intToPosRef(_pos_code);
			} catch (VEntityError e) {
				this._fail("Invalid position arguments");
				throw new VCallError();
			} catch (VStreamException e) {
				this._fail("Invalid position arguments");
				throw new VCallError();
			}
		}
		if (callID < 0) {
			this._fail("Invalid call ID value");
			throw new VCallError();
		}

		if (_failed || _done)
			throw new VCallError();
		if (!_mode.writable()) {
			this._fail("Stream not writable");
			throw new VCallError();
		}
		if (!have_pos) {
			if (!(_pos_ref == VStream.PosBase.ABS || _pos_ref == VStream.PosBase.START
				  || _pos_ref == VStream.PosBase.END)) {
				this._fail("Stream not writable");
				throw new VCallError();
			}
		}

		class Call extends VSequenceCallQueue.LocalCall {
			long msg_id;
			boolean have_pos;
			long pos;
			VStream.PosBase pos_ref;
			public Call(long msg_id, boolean have_pos, long pos, VStream.PosBase pos_ref) {
				this.msg_id = msg_id;
				this.have_pos = have_pos;
				this.pos = pos;
				this.pos_ref = pos_ref;
			}
			@Override
			public Object execute() throws Exception {
				_peer_wstart(msg_id, have_pos, pos, pos_ref);
				return null;
			}
		}
		try {
			this._calls.queue(callID, new Call(callID, have_pos, _pos, _pos_ref));
		} catch (VSequenceCallException e) {
			this._fail("Internal error handling peer call");
			throw new VCallError();
		}
	}

	/**
	 * Sets a new read push limit for the current read context.
	 *
	 * <p>'relPos' must be larger than any previously sent read push delimiter.
	 * Calling this method is only allowed when in a read context that was
	 * initiated with {@link #peer_read_start}.</p>
	 *
	 * <p>Should only be called by a connected peer.</p>
	 *
	 * @param callID call ID
	 * @param relPos read push delimiter relative to read context start position
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_read_lim(long callID, long relPos)
			throws VCallError {
		if (callID < 0 || relPos < 1) {
			this._fail("Invalid peer argument(s)");
			throw new VCallError();
		}
		if (_failed || _done)
			return;
		class Call extends VSequenceCallQueue.LocalCall {
			long rel_pos;
			public Call(long rel_pos) {
				this.rel_pos = rel_pos;
			}
			@Override
			public Object execute() throws Exception {
				_peer_rlim(rel_pos);
				return null;
			}
		}
		try {
			this._calls.queue(callID, new Call(relPos));
		} catch (VSequenceCallException e) {
			this._fail("Internal error processing peer call");
			throw new VCallError();
		}
	}

	/**
	 * Truncates all stream data before current position.
	 *
	 * <p>The stream must be in write mode and the streamer mode must allow start
	 * position increases and moving the start position.</p>
	 *
	 * <p>Calling this method resets the context ID to 'callID'. The stream peer
	 * may only call this method after it has received peer_set_start_pos
	 * notification for the previous write context.</p>
	 *
	 * <p>The method should only be called by a streamer peer.</p>
	 *
	 * @param callID
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_trunc_before(long callID)
			throws VCallError {
		if (callID < 0) {
			this._fail("Invalid call ID");
			throw new VCallError();
		}

		if (_failed || _done)
			return;
		if (_mode.canMoveStart() && _mode.startCanInc()) {
			this._fail("Illegal truncate operation");
			throw new VCallError();
		}
		class Call extends VSequenceCallQueue.LocalCall {
			long callID;
			public Call(long callID) {
				this.callID = callID;
			}
			@Override
			public Object execute() throws Exception {
				_peer_trunc_before(callID);
				return null;
			}
		}
		try {
			_calls.queue(callID, new Call(callID));
		} catch (VSequenceCallException e) {
			this._fail("Internal error handling peer_trunc_before call");
			throw new VCallError();
		}
	}

	/**
	 * Truncates all stream data after current position.
	 *
	 * <p>The stream must be in write mode and the streamer mode must allow end
	 * position decreases and moving the end position.</p>
	 *
	 * <p>Calling this method resets the context ID to 'callID'. The stream peer
	 * may only call this method after it has received peer_set_start_pos
	 * notification for the previous write context.</p>
	 *
	 * <p>The method should only be called by a streamer peer.</p>
	 *
	 * @param callID
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_trunc_after(long callID)
			throws VCallError {
		if (callID < 0) {
			this._fail("Invalid call ID");
			throw new VCallError();
		}

		if (_failed || _done)
			return;
		if (_mode.canMoveEnd() && _mode.endCanDec()) {
			this._fail("Illegal truncate operation");
			throw new VCallError();
		}
		class Call extends VSequenceCallQueue.LocalCall {
			long callID;
			public Call(long callID) {
				this.callID = callID;
			}
			@Override
			public Object execute() throws Exception {
				_peer_trunc_after(callID);
				return null;
			}
		}
		try {
			_calls.queue(callID, new Call(callID));
		} catch (VSequenceCallException e) {
			this._fail("Internal error handling peer_trunc_before call");
			throw new VCallError();
		}
	}

	/**
	 * Closes the stream connection.
	 *
	 * <p>Causes the streamer to close its side of the stream and
	 * acknowledge closing by calling peer_closed on the connected
	 * peer before freeing up stream resources.</p>
	 *
	 * <p>Should only be called by a connected peer.</p>
	 *
	 * @param callID
	 * @throws VCallError
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_close(long callID)
			throws VCallError {
		if (callID < 0) {
			this._fail("Invalid peer_close arguments");
			throw new VCallError();
		}
		if (_failed || _done)
			return;
		class Call extends VSequenceCallQueue.LocalCall {
			@Override
			public Object execute() throws Exception {
				_peer_close();
				return null;
			}
		}
		try {
			_calls.queue(callID, new Call());
		} catch (VSequenceCallException e) {
			this._fail("Internal error handling peer_close");
			throw new VCallError();
		}
	}

	/**
	 * Informs the streamer the stream connection has failed.
	 *
	 * <p>Signal from the connected stream peer that a critical failure
	 * occurred on the peer end of the stream. The streamer should abort
	 * all current streamer operation and free related resources.</p>
	 *
	 * <p>Should only be called by a connected peer.</p>
	 *
	 * @param callID call ID
	 * @param msg error message (or null/VNone)
	 */
	@Publish(show=true, ctx=false)
	public synchronized void peer_fail(long callID, VEntity msg)
			throws VCallError {
		if (callID < 0) {
			this._fail("Invalid peer_close arguments");
			throw new VCallError();
		}
		String _msg = null;
		if (msg != null && !(msg instanceof VNone)) {
			try {
				_msg = VString.nativeOf(msg);
			} catch (VEntityError e) {
				this._fail("Invalid peer_close arguments");
				throw new VCallError();
			}
		}
		if (_failed || _done)
			return;
		class Call extends VSequenceCallQueue.LocalCall {
			String msg;
			public Call(String msg) {
				this.msg = msg;
			}
			@Override
			public Object execute() throws Exception {
				_peer_fail(msg);
				return null;
			}
		}
		try {
			_calls.queue(callID, new Call(_msg));
		} catch (VSequenceCallException e) {
			this._fail("Internal error handling peer_close");
			throw new VCallError();
		}
	}

	/**
	 * Set write buffering limits for received write push data.
	 *
	 * <p>The 'num' limit is the maximum write push limit (relative to
	 * peer position) that can be sent to peer. If 'step' is negative
	 * then it is set equal to hald of 'num' (rounded up).</p>
	 *
	 * @param num max write push data that can be received
	 * @param step increment step for updating write push limits (or negative)
	 */
	public synchronized void setWriteBuffering(long num, long step) {
		if (step < 0)
			step = num/2 + num%2;
		_w_buf_lim = num;
		_w_buf_step = step;
		if (_ctx_mode == ContextMode.WRITING)
			this._update_wlim();
	}

	/**
	 * Notification from streamer data that its data range changed.
	 *
	 * <p>This method should only be called by a streamer data object which has had
	 * this streamer registered with its {@link VStreamerData#setStreamer(VStreamer)}.</p>
	 *
	 * <p>Warning: this method should not be called from inside any streamer data methods
	 * which are being called by its associated streamer, as executing this
	 * method interferes with the streamer's internal state.</p>
	 */
	public synchronized void notifyEndpoints() {
		if (_streamdata != null && (_failed || _done || _ctx_err))
			_streamdata.disableNotifications(VStreamerData.NOTIFY_ENDPOINTS);
		else {
			try {
				this._poll_endpoints();
				if (!_ctx_err && _ctx_mode == ContextMode.READING)
					this._perform_read();
			} catch (VCallError e) {
				// SILENT
			}
		}
	}

	void _peer_rstart(long msg_id, boolean have_pos, long pos,
			          VStream.PosBase pos_ref, boolean eos)
			throws VCallError {
		if (_failed || _done)
			return;
		if (_ctx_mode == ContextMode.READING && !have_pos) {
			this._fail("Invalid read context position");
			throw new VCallError();
		}
		if (_ctx_mode == ContextMode.WRITING)
			_buf.endContext();

		this._clear_ctx_data();

		long old_pos = _ctx_spos + _ctx_rpos;
		boolean have_new_pos = false;
		long new_pos = 0;

		// Pre-validation of seek operation
		if (have_pos) {
			if (!(_mode.seekRew() || _mode.seekFwd())) {
				this._fail("Stream does not support seek");
				throw new VCallError();
			}
			if (pos_ref == VStream.PosBase.ABS) {
				have_new_pos = true;
				new_pos = pos;
				if (new_pos > old_pos && !_mode.seekFwd()) {
					this._fail("Forward seek not allowed");
					throw new VCallError();
				}
				if (new_pos < old_pos && !_mode.seekRew()) {
					this._fail("Reverse seek not allowed");
					throw new VCallError();
				}
			}
		}

		if (!have_pos) {
			have_new_pos = true;
			new_pos = _ctx_spos + _ctx_rpos;
		}
		else if (!have_new_pos || new_pos != old_pos) {
			// Seek to new position and post-validate result
			try {
				new_pos = _streamdata.seek(pos, pos_ref);
			} catch (VStreamFailure e) {
				this._fail("Seek related failure");
				throw new VCallError();
			} catch (VStreamError e) {
				throw new VCallError("Could not initiate reading");
			}
			have_new_pos = true;
			if (new_pos > old_pos && !_mode.seekFwd()) {
				this._fail("Forward seek not allowed");
				throw new VCallError();
			}
			if (new_pos < old_pos && !_mode.seekRew()) {
				this._fail("Reverse seek not allowed");
				throw new VCallError();
			}
		}

		// Initialize resulting read mode parameters
		_ctx_mode = ContextMode.READING;
		_ctx = msg_id;
		_ctx_err = false;
		_ctx_err_code = 0;
		_ctx_sent_err = false;
		_r_eos = eos;
		_ctx_spos = new_pos;
		_ctx_rpos = 0;

		// Notify peer of resulting position
		class Call extends VSequenceCaller.RemoteCall {
			long ctx;
			long ctx_spos;
			public Call(long ctx, long ctx_spos) {
				this.ctx = ctx;
				this.ctx_spos = ctx_spos;
			}
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_set_start_pos", callID, ctx, ctx_spos);
			}

		}
		this._caller.call(new Call(_ctx, _ctx_spos), null, null);

		// Update end-point information
		this._poll_endpoints();
	}

	void _peer_wstart(long msg_id, boolean have_pos, long pos, VStream.PosBase pos_ref)
			throws VCallError {
		if (_failed || _done)
			return;
		if (_ctx_mode == ContextMode.WRITING && !have_pos) {
			this._fail("Invalid write start position");
			throw new VCallError();
		}

		this._clear_ctx_data();

		long old_pos = _ctx_spos + _ctx_rpos;
		boolean have_new_pos = false;
		long new_pos = 0;

		// Pre-validation of seek operation
		if (have_pos) {
			if (!(_mode.seekRew() || _mode.seekFwd())) {
				this._fail("Stream does not support seek");
				throw new VCallError();
			}
			if (pos_ref == VStream.PosBase.ABS) {
				have_new_pos = true;
				new_pos = pos;
				if (new_pos > old_pos && !_mode.seekFwd()) {
					this._fail("Forward seek not allowed");
					throw new VCallError();
				}
				if (new_pos < old_pos && !_mode.seekRew()) {
					this._fail("Reverse seek not allowed");
					throw new VCallError();
				}
			}
		}

		if (!have_pos) {
			have_new_pos = true;
			new_pos = _ctx_spos + _ctx_rpos;
		}
		else if (!have_new_pos || new_pos != old_pos) {
			// Seek to new position and post-validate result
			try {
				new_pos = _streamdata.seek(pos, pos_ref);
			} catch (VStreamFailure e) {
				this._fail("Seek related failure");
				throw new VCallError();
			} catch (VStreamError e) {
				throw new VCallError("Could not initiate reading");
			}
			have_new_pos = true;
			if (new_pos > old_pos && !_mode.seekFwd()) {
				this._fail("Forward seek not allowed");
				throw new VCallError();
			}
			if (new_pos < old_pos && !_mode.seekRew()) {
				this._fail("Reverse seek not allowed");
				throw new VCallError();
			}
		}

		// Initialize write mode parameters
		_ctx_mode = ContextMode.WRITING;
		_ctx = msg_id;
		_ctx_err = false;
		_ctx_err_code = 0;
		_ctx_sent_err = false;
		_ctx_spos = new_pos;
		_ctx_rpos = 0;
		_w_rel_lim = 0;
		_w_recv = 0;
		_buf.newContext(_ctx_spos, false);

		// Notify peer of the resulting position
		class Call extends VSequenceCaller.RemoteCall {
			long ctx;
			long ctx_spos;
			public Call (long ctx, long ctx_spos) {
				this.ctx = ctx;
				this.ctx_spos = ctx_spos;
			}
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_set_start_pos", callID, ctx, ctx_spos);
			}
		}
		this._caller.call(new Call(_ctx, _ctx_spos), null, null);
		this._update_wlim();

		// Update end-point information
		this._poll_endpoints();
	}

	synchronized void _peer_rlim(long rel_pos)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			throw new VCallError();
		if (!_mode.readable()) {
			this._fail("Stream not readable");
			throw new VCallError();
		}
		if (rel_pos <= _r_rel_lim) {
			this._fail("Illegal read limit");
			throw new VCallError();
		}
		_r_rel_lim = rel_pos;
		this._perform_read();
	}

	/**
	 * Internal handler for peer_wpush call from peer.
	 *
	 * @param write_ctx write context
	 * @param data pushed data
	 * @throws VCallError
	 */
	protected synchronized void _peer_wpush(long write_ctx, T[] data)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;

		_w_pending -= 1;

		// Validate context
		if (write_ctx != _ctx)
			return;
		if (_ctx_mode != ContextMode.WRITING) {
			this._fail("Write push without write mode");
			throw new VCallError();
		}

		// Push data onto write buffer unless data overflow
		if (_w_recv + data.length > _w_rel_lim) {
			this._fail("Write limit exceeded");
			throw new VCallError();
		}

		try {
			_streamdata.write(data);
		} catch (VStreamFailure e) {
			this._fail("Streamer write fail");
			throw new VCallError();
		} catch (VStreamError e) {
			this._fail("Streamer write fail");
			throw new VCallError();
		}
		_buf.write(data);
		_w_recv += data.length;
		_ctx_rpos += data.length;

		// Update endpoint informoation
		this._poll_endpoints();

		// Update write limit
		this._update_wlim();
	}

	synchronized void _peer_trunc_before(long msg_id)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;
		if (_ctx_mode != ContextMode.WRITING) {
			this._fail("Truncate requires write context");
			throw new VCallError();
		}
		try {
			_streamdata.truncateBefore();
		} catch (VStreamFailure e) {
			this._fail("Truncate operation general stream failure");
			throw new VCallError();
		} catch (VStreamError e) {
			this._set_error(VStream.ERR_GENERAL_ERROR);
			throw new VCallError();
		}

		// Truncation updates context ID
		_ctx = msg_id;

		// Update endpoint information
		this._poll_endpoints();
	}

	synchronized void _peer_trunc_after(long msg_id)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;
		if (_ctx_mode != ContextMode.WRITING) {
			this._fail("Truncate requires write context");
			throw new VCallError();
		}
		try {
			_streamdata.truncateAfter();
		} catch (VStreamFailure e) {
			this._fail("Truncate operation general stream failure");
			throw new VCallError();
		} catch (VStreamError e) {
			this._set_error(VStream.ERR_GENERAL_ERROR);
			throw new VCallError();
		}

		// Truncation updates context ID
		_ctx = msg_id;

		// Update endpoint information
		this._poll_endpoints();

		// Truncation resets write limit, reset and send new limit
		_w_rel_lim = _w_recv;
		this._update_wlim();
	}

	synchronized void _peer_close()
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;
		_done = true;
		if (_streamdata != null)
			_streamdata.close();
		class Call extends VSequenceCaller.RemoteCall {
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_closed", callID);
			}
		}
		_caller.call(new Call(), null, null);
		this._cleanup();
	}

	synchronized void _peer_fail(String msg)
			throws VCallError {
		if (!_failed) {
			_failed = true;
			this._cleanup();
		}
	}

	/**
	 * Internal call to perform a read operation.
	 */
	synchronized void _perform_read()
			throws VCallError {
		if (_failed || _done || _ctx_err)
			throw new VCallError();
		else if (!_mode.readable())
			throw new VCallError();
		else if (_r_sent_eos)
			return;

		boolean end_of_data = false;
		while (_r_pending < _r_num && !_ctx_err && !end_of_data) {
			long max_push = _r_rel_lim - _ctx_rpos;
			if (max_push < 0)
				max_push = 0;
			if (max_push > _r_size)
				max_push = _r_size;
			if (max_push <= 0)
				break;

			// Read data for sending to peer
			T[] data;
			try {
				data = _streamdata.read((int)max_push);
			} catch (VStreamFailure e) {
				this._fail("Read operation failure");
				throw new VCallError();
			} catch (VStreamError e) {
				this._set_error(VStream.ERR_GENERAL_ERROR);
				break;
			}
			if (data.length < max_push)
				end_of_data = true;

			boolean eos = (end_of_data && _r_eos);
			if (data.length > 0 || eos) {
				class Call extends VSequenceCaller.RemoteCall {
					long ctx;
					T[] data;
					boolean eos;
					public Call(long ctx, T[] data, boolean eos) {
						this.ctx = ctx;
						this.data = data;
						this.eos = eos;
					}
					@Override
					public VCall<Object> execute(long callID) {
						return _peer.nowait("peer_read_push", callID, ctx, data, eos);
					}
				}
				class Callback extends VSequenceCaller.Callback {
					@Override
					public void callback(long callID, Object result) {
						_rpush_callback(result);
					}
				}
				class Failback extends VSequenceCaller.Failback{
					@Override
					public void callback(long callID, Exception e) {
						_fail("Could not perform remote call");
					}
				}
				this._caller.call(new Call(_ctx, data, eos), new Callback(), new Failback());

				_ctx_rpos += data.length;
				_r_pending += 1;
				if (eos)
					_r_sent_eos = true;
			}
			this._poll_endpoints();
		}
	}

	synchronized void _rpush_callback(Object result) {
		if (_failed || _done)
			return;
		_r_pending -= 1;
		if (_ctx_mode == ContextMode.READING)
			try {
				this._perform_read();
			} catch (VCallError e) {
				// SILENT
			}
	}

	// Sets error condition and reports to peer (max once per context)
	void _set_error(int code) {
		if (!_ctx_sent_err) {
			_ctx_err = true;
			_ctx_err_code = code;
			_ctx_sent_err = true;
			class Call extends VSequenceCaller.RemoteCall {
				int code;
				public Call(int code) {
					this.code = code;
				}
				@Override
				public VCall<Object> execute(long callID) {
					return _peer.nowait("peer_error", callID, _ctx, code);
				}
			}
			this._caller.call(new Call(code), null, null);
		}
	}

	// Tests whether to update write limit based on write-ahead.
	synchronized void _update_wlim() {
		if (_failed || _done)
			return;
		if (_ctx_mode != ContextMode.WRITING)
			return;
		long lim = _ctx_rpos + _w_buf_lim;
		lim -= lim % _w_buf_step;
		if (!((_mode.endCanInc() && _mode.canMoveEnd())) && _end.isBounded()) {
			// If not END_CAN_INC, ensure no writing past endpoints
			long _max_to_end = _end.getPosition() - _ctx_spos;
			if (_max_to_end < 0)
				_max_to_end = 0;
			if (lim > _max_to_end)
				lim = _max_to_end;
		}
		if (lim > _w_rel_lim) {
			_w_rel_lim = lim;
			class Call extends VSequenceCaller.RemoteCall {
				long ctx;
				long lim;
				public Call (long ctx, long lim) {
					this.ctx = ctx;
					this.lim = lim;
				}
				@Override
				public VCall<Object> execute(long callID) {
					return _peer.nowait("peer_write_lim", callID, ctx, lim);
				}
			}
			this._caller.call(new Call(_ctx, _w_rel_lim), null, null);
		}
	}

	/**
	 * Updates streamer end-points by polling streamer data.
	 *
	 * <p>If the current position is no longer inside the range of
	 * endpoints, an invalid position error condition is set on the context.</p>
	 *
	 * @return true if endpoints were changed
	 */
	synchronized boolean _poll_endpoints()
			throws VCallError {
		VStreamerData.Endpoint old_start = _start;
		VStreamerData.Endpoint old_end = _end;
		VStreamerData.Endpoint[] _endpoints;
		try {
			_endpoints = _streamdata.getEndpoints();
		} catch (VStreamFailure e) {
			this._fail("Streamer data failed");
			throw new VCallError();
		} catch (VStreamError e) {
			this._fail("Streamer data failed");
			throw new VCallError();
		}
		_start = _endpoints[0];
		_end = _endpoints[1];
		long pos = _ctx_spos + _ctx_rpos;
		boolean valid_pos = true;
		if (_start.isBounded() && _start.getPosition() > pos)
			valid_pos = false;
		if (_end.isBounded() && _end.getPosition() < pos)
			valid_pos = false;
		if (!valid_pos)
			this._set_error(VStream.ERR_INVALID_POS);
		return !(old_start.equals(_start) && old_end.equals(_end));
	}

	// Clears all current context settings.
	void _clear_ctx_data() {
		_ctx_mode = ContextMode.NONE;
		_ctx = 0;
		_ctx_err = false;
		_ctx_err_code = 0;
		_ctx_sent_err = false;
		_r_rel_lim = 0;
		_r_eos = false;
		_r_sent_eos = false;
		_w_rel_lim = 0;
		_w_recv = 0;
	}

	void _cleanup() {
		this._clear_ctx_data();
		if (_streamdata != null)
			_streamdata.close();
		_peer = null;
		_calls = null;
	}

	// Sets a general failure condition and schedules peer notification.
	void _fail(String msg) {
		synchronized(this) {
			if (!_failed || _done) {
				_failed = true;
				this.notifyAll();

				if (_peer != null) {
					class Call extends VSequenceCaller.RemoteCall {
						String msg;
						public Call(String msg) {
							this.msg = msg;
						}
						@Override
						public VCall<Object> execute(long callID) {
							return _peer.nowait("peer_fail", callID, msg);
						}
					}
					_caller.call(new Call(msg), null, null);
				}
			}
		}
	}
}
