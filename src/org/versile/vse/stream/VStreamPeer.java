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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VTuple;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;
import org.versile.orb.util.VSequenceCallException;
import org.versile.orb.util.VSequenceCallQueue;
import org.versile.orb.util.VSequenceCaller;
import org.versile.vse.stream.VStream.PosBase;


/**
 * Stream interface interacting with remote VStreamer.
 *
 * <p>A {@link VStreamPeer} is a local access point to a stream which receives
 * and/or sends data by interacting with a remote streamer object. Normally the
 * stream peer object is not used directly, instead stream access
 * is performed via a {@link VStream} proxy object generated by calling
 * {@link VByteStreamerProxy#connect()}, {@link VEntityStreamerProxy#connect()}
 * or {@link VEntityStreamerProxy#connectNative()}.</p>
 *
 * <p>If 'mode' is not None then it is a required mode for the peer streamer, and
 * validation is performed during peer connect handshake that the peer's mode is
 * identical to this mode. If mode information differs then the stream is failed
 * during handshake.</p>
 *
 * <p>'w_pkg' and 'w_size' are locally set limits. If not None, write limits
 * received from a connected stream peer during connect handshake will be
 * truncated so they do not exceed local limits.</p>
 *
 * <p>A default read-ahead limit is set on the class which is the product of
 * 'r_pkg' and 'r_size', ref. {@link VStream#setReadahead(long, long)}. However, read-ahead
 * is enabled by default and needs to be explicitly enabled (see
 * {link {@link VStream#enableReadahead()}). The default read-ahead limit can be
 * overridden by setting another read-ahead limit before enabling read-ahead.</p>
 *
 * @param <T> the data type transferred by the stream
 */
public abstract class VStreamPeer<T> extends VExternal {

	enum ContextMode {NONE, READING, WRITING};

	// Codes for reading and writing
	static int _READING = 1;
	static int _WRITING = 2;

	boolean _connected = false;
	VStreamBuffer<T> _buf;

	boolean _done = false;                      // if true has initiated peer_close()
	boolean _failed = false;                    // if true stream has failure condition
	boolean _closed = false;                    // if true has completed peer_close()

	VStreamMode _mode;                          // configured mode
	VStreamMode _req_mode;	                    // required peer mode; negative number means no requirement

	int _call_lim;
	VSequenceCallQueue _calls;

	int _r_num;
	int _r_size;
	int _r_pending = 0;

	int _w_num = -1;
	int _w_size = -1;
	int _w_pending = 0;

	VProxy _peer = null;
	VSequenceCaller _caller;

	long _ctx = 0L;                             // first msg_id of current context
	ContextMode _ctx_mode = ContextMode.NONE;   // current stream mode
	boolean _ctx_err = false;                   // error condition on current context
	int _ctx_err_code = 0;                      // error code for error condition

	boolean _delayed_seek = false;              // if true delayed seek active
	long _delayed_seek_pos = 0;

	VStream.PosBase _delayed_seek_base;

	long _rel_pos = 0;                          // relative position in current context
	boolean _have_spos = false;                 // true if _spos holds a confirmed start position
	long _spos = 0;                             // start position current context

	boolean _r_ahead;                           // if true perform read-ahead
	long _r_ahead_lim = 0;                      // limit on read-ahead
	long _r_ahead_step = 0;                     // step on read-ahead

	boolean _r_request_eos;                     // if true set EOS for new read contexts

	long _r_rel_lim = 0;                        // current relative position receive limit
	long _r_recv = 0;                           // amount data received in context
	boolean _r_eos = false;                     // if true then EOS was reached

	long _w_rel_lim = 0;
	long _w_sent = 0;

	long _peer_w_num = 0;
	long _peer_w_size = 0;

	int _local_w_num;
	int _local_w_size;

	Set<WeakReference<VStreamObserver<T>>> _observers;

	/**
	 * Set up stream peer.
	 *
	 * @param buffer stream data buffer
	 * @param config link configuration parameters
	 */
	public VStreamPeer(VStreamBuffer<T> buffer, VStreamConfig config) {

		_buf = buffer;
		_req_mode = config.getRequiredMode();

		_r_num = config.getMaxReadPackages();
		_r_size = config.getMaxReadPkgSize();
		_local_w_num = config.getMaxWritePackages();
		_local_w_size = config.getMaxWritePkgSize();
		_r_request_eos = config.isRequestEOS();
		_r_ahead = config.isReadAhead();

		_call_lim = _r_num + config.getMaxCalls();
		_calls = new VSequenceCallQueue(_call_lim);
		_caller = new VSequenceCaller(0);
		_observers = new HashSet<WeakReference<VStreamObserver<T>>>();

		// Read-ahead settings in case read-ahead is (or gets) enabled
		long read_ahead_size = _r_num*_r_size;
		long read_ahead_step = read_ahead_size/2 + read_ahead_size%2;
		this.setReadahead(read_ahead_size, read_ahead_step);
	}

	/**
	 * Notify of streamer's start position of the current context.
	 *
	 * <p>This call is made by a peer in response to starting a new read
	 * context or write context.</p>
	 *
	 * <p>Should only be called by a connected peer, and can only be
	 * called once per context.</p>
	 *
	 * @param msg_id call ID
	 * @param pos absolute start position of context
	 */
	@Publish(show=true, ctx=false)
	public void peer_set_start_pos(long msg_id, long ctx, long pos)
			throws VCallError {
		if (msg_id < 0 || ctx < 0) {
			this._fail("Illegal IDs");
			throw new VCallError();
		}

		synchronized(this) {
			if (_failed || _done)
				return;
			class Call extends VSequenceCallQueue.LocalCall {
				long ctx;
				long pos;
				public Call(long ctx, long pos) {
					this.ctx = ctx;
					this.pos = pos;
				}
				@Override
				public Object execute() throws Exception {
					_peer_set_start_pos(ctx, pos);
					return null;
				}
			}
			try {
				_calls.queue(msg_id, new Call(ctx, pos));
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Sets a new write push limit for the current read context.
	 *
     * <p>'rel_pos' must be larger than any previously sent write push
     * delimiter for this context. Should only be called by a connected peer.</p>
	 *
	 * @param msg_id sequence call ID
	 * @param write_ctx first call ID of target write context
	 * @param rel_pos write push delimiter after write ctx start position
	 */
	@Publish(show=true, ctx=false)
	public void peer_write_lim(long msg_id, long write_ctx, long rel_pos)
			throws VCallError {
		if (msg_id < 0 || write_ctx < 0) {
			this._fail("Invalid peer_write_lim arguments");
			throw new VCallError();
		}
		synchronized(this) {
			if (_failed || _done)
				return;

			class Call extends VSequenceCallQueue.LocalCall {
				long write_ctx;
				long rel_pos;
				public Call(long write_ctx, long rel_pos) {
					this.write_ctx = write_ctx;
					this.rel_pos = rel_pos;
				}
				@Override
				public Object execute() throws Exception {
					_peer_wlim(write_ctx, rel_pos);
					return null;
				}
			}
			try {
				_calls.queue(msg_id, new Call(write_ctx, rel_pos));
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Acknowledges a peer streamer has closed the stream.
	 *
	 * <p>Acknowledges the streamer received and processed a
	 * peer_close call. Should only be called by a connected peer.</p>
	 *
	 * @param msg_id call sequence ID
	 */
	@Publish(show=true, ctx=false)
	public void peer_closed(long msg_id)
			throws VCallError {
		if (msg_id < 0) {
			this._fail("Invalid peer_closed arguments");
			throw new VCallError();
		}
		synchronized(this) {
			if (_failed || _closed)
				return;

			class Call extends VSequenceCallQueue.LocalCall {
				@Override
				public Object execute() throws Exception {
					_peer_closed();
					return null;
				}
			}
			try {
				_calls.queue(msg_id, new Call());
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Informs the stream peer the stream connection has failed.
	 *
	 * <p>Signal from the connected stream peer that a critical failure occured on
	 * the peer end of the stream. The streamer should abort all current
	 * streamer operation and free related resources. Should only be called by
	 * a connected peer.</p>
	 *
	 * @param msg_id call sequence ID
	 * @param msg error message (or null)
	 */
	@Publish(show=true, ctx=false)
	public void peer_fail(long msg_id, String msg)
			throws VCallError {
		if (msg_id < 0) {
			this._fail("Invalid peer_fail arguments");
			throw new VCallError();
		}
		synchronized(this) {
			if (_failed || _closed)
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
				_calls.queue(msg_id, new Call(msg));
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Notifies of an error condition on a context.
	 *
	 * <p>The error condition only applies to the given context, and it is stilled
	 * allowed to try to initiate new contexts. This is different from
	 * {@link #peer_fail} (which sets a general failure condition preventing any
	 * further stream operation).</p>
	 *
	 * @param msg_id sequence call ID
	 * @param ctx context ID
	 * @param error_code error code
	 */
	@Publish(show=true, ctx=false)
	public void peer_error(long msg_id, long ctx, int error_code)
			throws VCallError {
		if (msg_id < 0 || ctx < 0) {
			this._fail("Invalid peer_error arguments");
			throw new VCallError();
		}
		synchronized(this) {
			if (_failed || _done)
				return;

			class Call extends VSequenceCallQueue.LocalCall {
				long ctx;
				int error_code;
				public Call(long ctx, int error_code) {
					this.ctx = ctx;
					this.error_code = error_code;
				}
				@Override
				public Object execute() throws Exception {
					_peer_error(ctx, error_code);
					return null;
				}
			}
			try {
				_calls.queue(msg_id, new Call(ctx, error_code));
			} catch (VSequenceCallException e) {
				this._fail("Call sequencing error");
				throw new VCallError();
			}
		}
	}

	/**
	 * Initiate connection to a streamer.
	 *
	 * @param peer proxy reference to streamer
	 * @throws VStreamError
	 */
	protected synchronized void initConnect(VProxy peer)
			throws VStreamError {
		if (_peer != null)
			throw new VStreamError("Peer already connected");
		else if (_failed || _done)
			throw new VStreamError("Stream already done or failed");
		this._peer = peer;
		VCall<Object> call = _peer.nowait("peer_connect", this, _call_lim, _r_num, _r_size);
		class ResultHandler implements VCallResultHandler<Object> {
			@Override
			public void callback(Object result) {
				_connect_callback(result);
			}
		}
		class ExceptionHandler implements VCallExceptionHandler {
			@Override
			public void callback(Exception e) {
				_connect_failback(e);
			}
		}
		call.addHandlerPair(new ResultHandler(), new ExceptionHandler());
	}

	// See VStream.waitStatus
	synchronized void waitStatus(boolean connected, boolean active, boolean done,
									       boolean failed, boolean closed, long timeout,
									       int ntimeout)
			throws VStreamTimeout {
		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if ((connected && _connected) || (active && isActive()) || (done && _done)
				&& (failed && _failed) || (closed && _closed))
				return;
			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}
	}

	/**
	 * Checks whether stream is active.
	 *
	 * <p>The stream is "active" if it is connected and is not finished and has
	 * no error condition.</p>
	 *
	 * @return true if active
	 */
	public boolean isActive() {
        return _connected && !(_done || _closed || _failed);
	}

	void setReadahead(long num, long step) {
		if (step < 0)
			step = num/2 + num%2;
		synchronized(this) {
			_r_ahead_lim = num;
			_r_ahead_step = step;
			if (_r_ahead)
				this._update_rlim();
		}
	}

	synchronized void enableReadahead() {
		_r_ahead = true;
		this._update_rlim();
	}

	synchronized void disableReadahead() {
		_r_ahead = false;
	}

	synchronized T[] recv(int maxNum, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (_failed)
			throw new VStreamFailure();
		if (_done)
			throw new VStreamError("Stream was closed");

		if (_ctx_mode != ContextMode.READING)
			this._start_read_ctx(_delayed_seek, _delayed_seek_pos, _delayed_seek_base, _r_request_eos);
		else if (_ctx_err)
			throw VStreamError.createFromCode(_ctx_err_code, "Error condition on read context");

		// If read buffer is empty, lazy-send new read limits to peer
		if (_buf.getMaxRead() == 0) {
			if (_r_ahead)
				this._update_rlim();
			else if (_r_rel_lim - _rel_pos < maxNum) {
				_r_rel_lim = _rel_pos + maxNum;
				class Call extends VSequenceCaller.RemoteCall {
					long lim;
					public Call(long lim) {
						this.lim = lim;
					}
					@Override
					public VCall<Object> execute(long callID) {
						return _peer.nowait("peer_read_lim", callID, lim);
					}
				}
				class ExceptionHandler extends VSequenceCaller.Failback {
					@Override
					public void callback(long callID, Exception e) {
						_fail("Could not perform remote call");
					}
				}
				_caller.call(new Call(_r_rel_lim), null, new ExceptionHandler());
			}
		}

		// Wait for read buffer data
		long r_ctx = _ctx;

		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (_ctx != r_ctx)
				throw new VStreamError("Local context changed");
			else if (_failed)
				throw new VStreamFailure();
			else if (_done)
				throw new VStreamError("Stream was closed");
			else if (_ctx_err)
				throw VStreamError.createFromCode(_ctx_err_code);
			T[] data = _buf.read(maxNum);
			if (data.length > 0) {
				_rel_pos += data.length;
				this._update_rlim();
				return data;
			}
			else if (_r_eos)
				return _buf.emptyData();
			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}
	}

	synchronized int send(T[] data, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (_failed)
			throw new VStreamFailure();
		else if (_done)
			throw new VStreamError("Stream was closed");

		if (_ctx_mode != ContextMode.WRITING)
			this._start_write_ctx(_delayed_seek, _delayed_seek_pos, _delayed_seek_base);
		else if (_ctx_err)
			throw VStreamError.createFromCode(_ctx_err_code, "Error condition on read context");

		// If 'data' is empty, just return
		if (data.length == 0)
			return 0;

		// Enter wait loop for sending data
		long w_ctx = _ctx;

		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (_ctx != w_ctx)
				throw new VStreamError("Local context changed");
			else if (_failed)
				throw new VStreamFailure();
			else if (_done)
				throw new VStreamError("Stream was closed");
			else if (_ctx_err)
				throw VStreamError.createFromCode(_ctx_err_code);

			long max_send = _w_rel_lim - _w_sent;
			if (max_send < 0)
				max_send = 0;
			if (_w_pending < _w_num && max_send > 0 && _have_spos) {
				if (data.length > max_send) {
					T[] _data = _buf.createArray((int)max_send);
					for (int i = 0; i < _data.length; i++)
						_data[i] = data[i];
					data = _data;
				}
				class Call extends VSequenceCaller.RemoteCall {
					long ctx;
					T[] data;
					public Call(long ctx, T[] data) {
						this.ctx = ctx;
						this.data = data;
					}
					@Override
					public VCall<Object> execute(long callID) {
						return _peer.nowait("peer_write_push", callID, ctx, data);
					}
				}
				class Callback extends VSequenceCaller.Callback {
					VStreamPeer<T> stream_peer;
					public Callback(VStreamPeer<T> stream_peer) {
						this.stream_peer = stream_peer;
					}
					@Override
					public void callback(long callID, Object result) {
						synchronized(stream_peer) {
							if (_failed || _done)
								return;
							boolean was_peak = (_w_pending == _w_num);
							_w_pending -= 1;
							if (_ctx_mode == ContextMode.WRITING && was_peak)
								stream_peer.notifyAll();
						}
					}
				}
				class Failback extends VSequenceCaller.Failback {
					@Override
					public void callback(long callID, Exception e) {
						_fail("Could not perform remote call");
					}
				}
				_caller.call(new Call(_ctx, data), new Callback(this), new Failback());

				_buf.write(data, true);
				_w_pending += 1;
				int num_sent = data.length;
				_rel_pos += num_sent;
				_w_sent += num_sent;
				return num_sent;
			}

			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}
	}

	synchronized void _close() {
		_done = true;
		notifyAll();
		class Call extends VSequenceCaller.RemoteCall {
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_close", callID);
			}
		}
		class Failback extends VSequenceCaller.Failback {
			@Override
			public void callback(long callID, Exception e) {
				_fail("Could not perform remote call");
			}
		}
		_caller.call(new Call(), null, new Failback());
	}

	synchronized void rseek(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (posRef == VStream.PosBase.CURRENT) {
			pos += this._pos(timeout, ntimeout);
			posRef = VStream.PosBase.ABS;
		}
		this._seek_to(pos, posRef, timeout, ntimeout);
		this._start_read_ctx(true, pos, posRef, _r_request_eos);
	}

	synchronized void wseek(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (posRef == VStream.PosBase.CURRENT) {
			pos += this._pos(timeout, ntimeout);
			posRef = VStream.PosBase.ABS;
		}
		this._seek_to(pos, posRef, timeout, ntimeout);
		this._start_write_ctx(true, pos, posRef);
	}

	synchronized void seek(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (!(_mode.seekRew() || _mode.seekFwd()))
			throw new VStreamError("Stream does not allow seek operations");
		else if (_ctx_mode == ContextMode.READING)
			this.rseek(pos, posRef, timeout, ntimeout);
		else if (_ctx_mode == ContextMode.WRITING)
			this.wseek(pos, posRef, timeout, ntimeout);
		else {
			if (posRef == PosBase.CURRENT) {
				pos += this._pos(timeout, ntimeout);
				posRef = VStream.PosBase.ABS;
			}
			_delayed_seek = true;
			_delayed_seek_pos = pos;
			_delayed_seek_base = posRef;
		}
	}

	// See VStream.tell()
	synchronized long _pos(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (_failed)
				throw new VStreamFailure();
			else if (_done)
				throw new VStreamError("Stream was closed");
			else if (_ctx_err)
				throw VStreamError.createFromCode(_ctx_err_code);

			if (_have_spos)
				return _spos + _rel_pos;

			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}
	}

	synchronized void truncBefore(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (!(_mode.canMoveStart() && _mode.startCanInc()))
			throw new VStreamError("Stream does not allow start truncation");
		else if (_ctx_mode != ContextMode.WRITING)
			throw new VStreamError("Must be in write-mode when truncating");
		else if (_failed)
			throw new VStreamFailure();
		else if (_done)
			throw new VStreamError("Stream was closed");
		else if (_ctx_err)
			throw VStreamError.createFromCode(_ctx_err_code, "Error condition on context");

		long w_ctx = _ctx;
		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (_ctx != w_ctx)
				throw new VStreamError("Context was changed");
			if (_failed)
				throw new VStreamFailure();
			else if (_done)
				throw new VStreamError("Stream was closed");
			else if (_ctx_err)
				throw VStreamError.createFromCode(_ctx_err_code);

			if (_have_spos)
				break;

			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}

		// We have a start position so allowed to truncate
		class Call extends VSequenceCaller.RemoteCall {
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_trunc_before", callID);
			}
		}
		class Failback extends VSequenceCaller.Failback {
			@Override
			public void callback(long callID, Exception e) {
				_fail("Could not perform remote call");
			}
		}

		long msg_id = _caller.call(new Call(), null, new Failback());

		// Truncation updates context ID
		_ctx = msg_id;
    }

	synchronized void truncAfter(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (!(_mode.canMoveEnd() && _mode.endCanDec()))
			throw new VStreamError("Stream does not allow start truncation");
		else if (_ctx_mode != ContextMode.WRITING)
			throw new VStreamError("Must be in write-mode when truncating");
		else if (_failed)
			throw new VStreamFailure();
		else if (_done)
			throw new VStreamError("Stream was closed");
		else if (_ctx_err)
			throw VStreamError.createFromCode(_ctx_err_code, "Error condition on context");

		long w_ctx = _ctx;
		long start_time = 0L;
		long end_time = 0L;
		if (timeout >= 0) {
			start_time = System.nanoTime();
			end_time = start_time + 1000000L*timeout + ntimeout;
		}
		while (true) {
			if (_ctx != w_ctx)
				throw new VStreamError("Context was changed");
			if (_failed)
				throw new VStreamFailure();
			else if (_done)
				throw new VStreamError("Stream was closed");
			else if (_ctx_err)
				throw VStreamError.createFromCode(_ctx_err_code);

			if (_have_spos)
				break;

			try {
				if (timeout < 0)
					wait();
				else {
					long time_left = end_time - System.nanoTime();
					if (time_left <= 0)
						throw new VStreamTimeout();
					long msec = time_left / 1000000L;
					wait(msec, (int)(time_left-msec*1000000));
				}
			} catch (InterruptedException e) {
				// Ignore interrupt, treat it just as wait completion
			}
		}

		// We have a start position so allowed to truncate
		class Call extends VSequenceCaller.RemoteCall {
			@Override
			public VCall<Object> execute(long callID) {
				return _peer.nowait("peer_trunc_after", callID);
			}
		}
		class Failback extends VSequenceCaller.Failback {
			@Override
			public void callback(long callID, Exception e) {
				_fail("Could not perform remote call");
			}
		}

		long msg_id = _caller.call(new Call(), null, new Failback());

		// Truncation updates context ID
		_ctx = msg_id;

		// Truncation invalidates any previous write limit
		_w_rel_lim = _w_sent;
    }

	synchronized void setEosPolicy(boolean requestEos) {
		_r_request_eos = requestEos;
	}

	void addObserver(VStreamObserver<T> observer) {
		synchronized(this) {
			Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
			while(iter.hasNext()) {
				WeakReference<VStreamObserver<T>> w_obs = iter.next();
				if (w_obs.get() == observer)
					return;
			}
			_observers.add(new WeakReference<VStreamObserver<T>>(observer));
		}
	}

	void removeObserver(VStreamObserver<T> observer) {
		synchronized(this) {
			Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
			while(iter.hasNext()) {
				WeakReference<VStreamObserver<T>> w_obs = iter.next();
				if (w_obs.get() == observer)
					iter.remove();
			}
		}
	}

	synchronized void _peer_set_start_pos(long ctx, long pos) {
		if (_failed || _done || _ctx_err)
			return;
		if (_have_spos) {
			this._fail("Too many position notifications");
			return;
		}
		if (_ctx == ctx) {
			_have_spos = true;
			_spos = pos;
			boolean can_cache = _mode.fixedData() || _mode.dataLock();
			_buf.newContext(pos, can_cache);
			notifyAll();
		}
	}

	synchronized void _peer_rpush(long read_ctx, T[] data, boolean eos)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;

		_r_pending -= 1;

		// Validate context
		if (read_ctx != _ctx)
			return;
		if (_ctx_mode != ContextMode.READING) {
			this._fail("Read push without read mode");
			throw new VCallError();
		}

		// rpush only allowed after start position was specified
		if (!_have_spos) {
			this._fail("Read push without position");
			throw new VCallError();
		}

		// Cannot send data or eos after eos already sent
		if (_r_eos) {
			this._fail("Read push after end-of-stream");
			throw new VCallError();
		}

		// Check data or overflow
		if (_r_recv + data.length > _r_rel_lim) {
			this._fail("Read push limit exceeded");
			return;
		}

		// Push data to buffer and send I/O notification
		boolean had_data = (_buf.getMaxRead() > 0);
		_buf.write(data);
		_r_recv += data.length;
		if (eos)
			_r_eos = true;
		notifyAll();

		// Notify any stream observers
		if (!_observers.isEmpty() && data.length > 0 && !had_data) {
			Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
			while(iter.hasNext()) {
				WeakReference<VStreamObserver<T>> w_obs = iter.next();
				VStreamObserver<T> obs = w_obs.get();
				if (obs != null)
					obs.canReceive();
				else
					iter.remove();
			}
		}
	}

	synchronized void _peer_wlim(long write_ctx, long rel_pos)
			throws VCallError {
		if (_failed || _done || _ctx_err)
			return;

		// Validate context
		if (write_ctx != _ctx)
			return;
		else if (_ctx_mode != ContextMode.WRITING) {
			this._fail("Writel imit requires write mode");
			throw new VCallError();
		}
		else if (_ctx_err)
			throw new VCallError();

		// Write lim ony allowed after start position was specified
		if (!_have_spos){
			this._fail("Write limit without position");
			throw new VCallError();
		}

		if (rel_pos <= _w_rel_lim) {
			this._fail("Illegal write limit");
			throw new VCallError();
		}

		boolean could_write = (_w_rel_lim > _w_sent);
		_w_rel_lim = rel_pos;
		notifyAll();

		if (!_observers.isEmpty() && !could_write && _w_rel_lim > _w_sent) {
			Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
			while(iter.hasNext()) {
				WeakReference<VStreamObserver<T>> w_obs = iter.next();
				VStreamObserver<T> obs = w_obs.get();
				if (obs != null)
					obs.canSend();
				else
					iter.remove();
			}
		}
	}

	synchronized void _peer_closed() {
		if (_failed || _closed || _ctx_err)
			return;
		_closed = true;
		notifyAll();

		// Notify any stream observers
		Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
		while(iter.hasNext()) {
			WeakReference<VStreamObserver<T>> w_obs = iter.next();
			VStreamObserver<T> obs = w_obs.get();
			if (obs != null)
				obs.closed();
			else
				iter.remove();
		}

		this._cleanup();
	}

	synchronized void _peer_fail(String msg) {
		if (_failed)
			return;
		_failed = true;

		// Notify any stream observers
		Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
		while(iter.hasNext()) {
			WeakReference<VStreamObserver<T>> w_obs = iter.next();
			VStreamObserver<T> obs = w_obs.get();
			if (obs != null)
				obs.failed();
			else
				iter.remove();
		}

		this._cleanup();
	}

	synchronized void _peer_error(long ctx, int error_code) {
		if (_failed || _done || _ctx_err)
			return;

		if (ctx == _ctx) {
			_ctx_err = true;
			_ctx_err_code = error_code;
			notifyAll();

			// Notify any stream observers
			Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
			while(iter.hasNext()) {
				WeakReference<VStreamObserver<T>> w_obs = iter.next();
				VStreamObserver<T> obs = w_obs.get();
				if (obs != null)
					obs.contextError();
				else
					iter.remove();
			}
		}
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

				// Notify any stream observers
				Iterator<WeakReference<VStreamObserver<T>>> iter = _observers.iterator();
				while(iter.hasNext()) {
					WeakReference<VStreamObserver<T>> w_obs = iter.next();
					VStreamObserver<T> obs = w_obs.get();
					if (obs != null)
						obs.failed();
					else
						iter.remove();
				}

				this._cleanup();
			}
		}
	}

	// Set up a read context. Position must be alread translated ('current' not allowed)
	synchronized void _start_read_ctx(boolean havePos, long pos,
									  VStream.PosBase pos_base, boolean eos) {
		this._clear_ctx_data();
		_ctx_mode = ContextMode.READING;
		class Call extends VSequenceCaller.RemoteCall {
			VEntity pos;
			VEntity pos_base;
			VBoolean eos;
			public Call(VEntity pos, VEntity pos_base, VBoolean eos) {
				this.pos = pos;
				this.pos_base = pos_base;
				this.eos = eos;
			}
			@Override
			public VCall<Object> execute(long callID) {
				_have_spos = false;
				return _peer.nowait("peer_read_start", callID, pos, pos_base, eos);
			}
		}
		class Failback extends VSequenceCaller.Failback {
			public void callback(long callID, Exception e) {
				_fail("Could not perform remote call");
			}
		}
		Call call;
		if (havePos) {
			int _pos_base = VStream.posRefToInt(pos_base);
			call = new Call(new VInteger(pos), new VInteger(_pos_base), new VBoolean(eos));
		}
		else
			call = new Call(VNone.get(), VNone.get(), new VBoolean(eos));
		_ctx = _caller.call(call, null, new Failback());
		this._update_rlim();
	}

	// Set up a write context. Position must already be translated (CURRENT not allowed).
	synchronized void _start_write_ctx(boolean havePos, long pos, VStream.PosBase pos_base) {
		this._clear_ctx_data();
		_ctx_mode = ContextMode.WRITING;

		class Call extends VSequenceCaller.RemoteCall {
			VEntity pos;
			VEntity pos_base;
			public Call(VEntity pos, VEntity pos_base) {
				this.pos = pos;
				this.pos_base = pos_base;
			}
			@Override
			public VCall<Object> execute(long callID) {
				_have_spos = false;
				return _peer.nowait("peer_write_start", callID, pos, pos_base);
			}
		}
		class Failback extends VSequenceCaller.Failback {
			public void callback(long callID, Exception e) {
				_fail("Could not perform remote call");
			}
		}
		Call call;
		if (havePos) {
			int _pos_base = VStream.posRefToInt(pos_base);
			call = new Call(new VInteger(pos), new VInteger(_pos_base));
		}
		else
			call = new Call(VNone.get(), VNone.get());
		_ctx = _caller.call(call, null, new Failback());
	}

	// Caller must hold a lock on this, and position must be translated (CURRENT not allowed)
	void _seek_to(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		if (_failed)
			throw new VStreamFailure();
		if (_done)
			throw new VStreamError("Stream was closed");
		if (!(_mode.seekRew() || _mode.seekFwd()))
			throw new VStreamError("Stream does not allow seek operations");
		if (posRef == VStream.PosBase.ABS && _have_spos) {
			long cur_pos = _spos + _rel_pos;
			if (pos == cur_pos)
				return; // Already at pos
			if (pos > cur_pos && !_mode.seekFwd())
				throw new VStreamError("Forward seek not allowed");
			if (pos < cur_pos && !_mode.seekRew())
				throw new VStreamError("Backward seek not allowed");
		}
	}

	/**
	 * Lazy-performs read limit update based on read-ahead.
	 */
	synchronized void _update_rlim() {
		if (_ctx_mode != ContextMode.READING || !_r_ahead)
			return;
		long lim = _rel_pos + _r_ahead_lim;
		lim -= lim % _r_ahead_step;
		if (lim > _r_rel_lim) {
			_r_rel_lim = lim;
			class Call extends VSequenceCaller.RemoteCall {
				long lim;
				public Call(long lim) {
					this.lim = lim;
				}
				@Override
				public VCall<Object> execute(long callID) {
					return _peer.nowait("peer_read_lim", callID, lim);
				}
			}
			class Failback extends VSequenceCaller.Failback {
				public void callback(long callID, Exception e) {
					_fail("Could not perform remote call");
				}
			}
			_caller.call(new Call(_r_rel_lim), null, new Failback());
		}
	}

	synchronized void _connect_callback(Object result) {
		Object[] _result;
		if (result.getClass().isArray())
			_result = (Object[])result;
		else
			try {
				_result = VTuple.valueOf(result).toArray();
			} catch (VEntityError e) {
				_fail("Invalid connect callback data");
				return;
			}
		if (_result.length != 5) {
			_fail("Invalid connect callback data");
			return;
		}

		int mode;
		long pos;
		int clim;
		int w_num;
		int w_size;
		try {
			mode = VInteger.nativeOf(_result[0]).intValue();
			pos = VInteger.nativeOf(_result[1]).longValue();
			clim = VInteger.nativeOf(_result[2]).intValue();
			w_num = VInteger.nativeOf(_result[3]).intValue();
			w_size = VInteger.nativeOf(_result[4]).intValue();
		} catch (VEntityError e) {
			_fail("Invalid connect callback data");
			return;
		}
		if (mode < 0 || clim < 1 || w_num < 1 || w_size < 1) {
			_fail("Invalid connect callback data");
			return;
		}

		_mode = new VStreamMode(mode);
		if (_req_mode != null && !_mode.contains(_req_mode)) {
			_fail("Streamer peer mode mismatch");
			return;
		}
		if (!VStreamMode.validateMode(mode)) {
			_fail("Invalid stream mode");
			return;
		}

		_have_spos = true;
		_spos = pos;

		_peer_w_num = w_num;
		_w_num = w_num;
		if (_local_w_num >= 0 && _w_num > _local_w_num)
			_w_num = _local_w_num;

		_peer_w_size = w_size;
		_w_size = w_size;
		if (_local_w_size >= 0 && _w_size > _local_w_size)
			_w_size = _local_w_size;

		_connected = true;
		_caller.setLimit(clim);
		notifyAll();
	}

	synchronized void _connect_failback(Exception e) {
		this._fail("Connect failure");
	}

	// Clears all current context settings.
	void _clear_ctx_data() {
		_ctx_mode = ContextMode.NONE;
		_ctx = 0;
		_ctx_err = false;
		_ctx_err_code = 0;
		_spos = 0;
		_have_spos = false;
		_rel_pos = 0;
		_r_rel_lim = 0;
		_r_recv = 0;
		_r_eos = false;
		_w_rel_lim = 0;
		_w_sent = 0;
		_buf.endContext();
	}

	void _cleanup() {
		this._clear_ctx_data();
		_peer = null;
		_calls = null;
		_observers.clear();
	}

	/**
	 * Create a stream object which can be connected to this peer.
	 *
	 * <p>Default creates a {@link VObjectStream} of the appropriate type.
	 * Derived classes can override.</p>
	 *
	 * @return stream object
	 */
	protected VStream<T> createStream(VStreamPeer<T> peer) {
		return new VObjectStream<T>(peer);
	}
}