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

import org.versile.common.call.VCallTimeout;


/**
 * Stream proxy for accessing a connected {@link VStreamPeer}.
 *
 * <p>See {@link VStreamPeer} for details about e.g. read-ahead.</p>
 *
 * @param <T> the data type transferred by the stream
 */
public abstract class VStream<T> {

	/**
	 * VSE relative stream reference.
	 */
	public enum PosBase {
		/**
		 * Absolute position.
		 */
		ABS,
		/**
		 * Relative to stream start position.
		 */
		START,
		/**
		 * Relative to stream end position.
		 */
		END,
		/**
		 * Relative to current stream position.
		 */
		CURRENT};

    /**
     * VSE stream error code for general error.
     */
    public static int ERR_GENERAL_ERROR = 1;

    /**
     * VSE stream error code for invalid position.
     */
    public static int ERR_INVALID_POS = 2;

	/**
	 * Proxied stream peer.
	 */
	protected VStreamPeer<T> _stream;

    /**
     * Set up stream proxy.
     *
     * @param stream stream peer to proxy
     */
    public VStream(VStreamPeer<T> stream) {
    	_stream = stream;
    }

    @Override
    public void finalize() {
    	synchronized(_stream) {
    		if (!_stream._connected)
    			_stream._fail("Stream no longer referenced");
    		else if (_stream.isActive())
    			_stream._close();
    	}
    }

	/**
	 * Waits for one of the set states to occur.
	 *
	 * @param connected if true return when connected
	 * @param active if true return when active
	 * @param done if true return when done
	 * @param failed if true return if failed
	 * @param closed if true return when closed
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VCallTimeout timeout before condition occurred
	 */
	public synchronized void waitStatus(boolean connected, boolean active, boolean done,
			                        	boolean failed, boolean closed, long timeout, int ntimeout)
			throws VStreamTimeout {
		_stream.waitStatus(connected, active, done, failed, closed, timeout, ntimeout);
	}

	/**
	 * Waits for the 'connected' state to occur (blocking).
	 *
	 * @throws VStreamTimeout
	 */
	public void waitConnected()
			throws VStreamTimeout {
		this.waitConnected(-1);
	}

	/**
	 * Waits for the 'connected' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @throws VStreamTimeout
	 */
	public void waitConnected(long timeout)
			throws VStreamTimeout {
		this.waitConnected(timeout, 0);
	}

	/**
	 * Waits for the 'connected' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void waitConnected(long timeout, int ntimeout)
			throws VStreamTimeout {
		this.waitStatus(true, false, false, false, false, timeout, ntimeout);
	}

	/**
	 * Waits for the 'active' state to occur (blocking).
	 *
	 * @throws VStreamTimeout
	 */
	public void waitActive()
			throws VStreamTimeout {
		this.waitActive(-1);
	}

	/**
	 * Waits for the 'active' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @throws VStreamTimeout
	 */
	public void waitActive(long timeout)
			throws VStreamTimeout {
		this.waitActive(timeout, 0);
	}

	/**
	 * Waits for the 'active' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void waitActive(long timeout, int ntimeout)
			throws VStreamTimeout {
		this.waitStatus(false, true, false, false, false, timeout, ntimeout);
	}

	/**
	 * Waits for the 'done' state to occur (blocking).
	 *
	 * @throws VStreamTimeout
	 */
	public void waitDone()
			throws VStreamTimeout {
		this.waitDone(-1);
	}

	/**
	 * Waits for the 'done' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @throws VStreamTimeout
	 */
	public void waitDone(long timeout)
			throws VStreamTimeout {
		this.waitDone(timeout, 0);
	}

	/**
	 * Waits for the 'done' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void waitDone(long timeout, int ntimeout)
			throws VStreamTimeout {
		this.waitStatus(false, false, true, false, false, timeout, ntimeout);
	}

	/**
	 * Waits for the 'failed' state to occur (blocking).
	 *
	 * @throws VStreamTimeout
	 */
	public void waitFailed()
			throws VStreamTimeout {
		this.waitFailed(-1);
	}

	/**
	 * Waits for the 'failed' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @throws VStreamTimeout
	 */
	public void waitFailed(long timeout)
			throws VStreamTimeout {
		this.waitFailed(timeout, 0);
	}

	/**
	 * Waits for the 'failed' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void waitFailed(long timeout, int ntimeout)
			throws VStreamTimeout {
		this.waitStatus(false, false, false, true, false, timeout, ntimeout);
	}

	/**
	 * Waits for the 'closed' state to occur (blocking).
	 *
	 * @throws VStreamTimeout
	 */
	public void waitClosed()
			throws VStreamTimeout {
		this.waitClosed(-1);
	}

	/**
	 * Waits for the 'closed' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @throws VStreamTimeout
	 */
	public void waitClosed(long timeout)
			throws VStreamTimeout {
		this.waitClosed(timeout, 0);
	}

	/**
	 * Waits for the 'closed' state to occur.
	 *
	 * @param timeout timeout in milliseconds (or <0 if no timeout)
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void waitClosed(long timeout, int ntimeout)
			throws VStreamTimeout {
		this.waitStatus(false, false, false, false, true, timeout, ntimeout);
	}

	/**
	 * Sets a read-ahead limit for receiving data.
	 *
	 * <p>If 'step' is negative then the default is num/2 (rounded up).</p>
	 *
	 * <p>Read-ahead makes the stream request read data from the streamer peer
	 * which is buffered locally for reading. This has a number of effects:</p>
	 *
	 * <p>Read data is transferred before requested locally</p>
	 *
	 * <p>Can dramatically
	 * improve stream bandwidth and latency performance (especially when tuned
	 * in combination with stream settings for maximum pending read push calls
	 * and max read push package size), compensating for round-trip latency
	 * effects of performing individual remote calls Steals bandwidth of
	 * buffering data which is never read locally</p>
	 *
	 * <p>De-couples local stream
	 * position and peer streamer position as peer streamer advances faster than
	 * local stream, which means the local stream must be careful about
	 * performing seek operations relative to the 'current' position.</p>
	 *
	 * <p>Setting read-ahead parameters does not activate read-ahead, this requires
	 * calling {@link #enableReadahead()}.</p>
	 *
	 * @param num maximum data elements to read-ahead
	 * @param step step increment to read-ahead (or negative)
	 *
	 */
	public void setReadahead(long num, long step) {
		_stream.setReadahead(num, step);
	}

	/**
	 * Enables read-ahead on read contexts.
	 *
	 */
	public void enableReadahead() {
		_stream.enableReadahead();
	}

	/**
	 * Disables read-ahead on read contexts.
	 *
	 */
	public void disableReadahead() {
		_stream.disableReadahead();
	}

	/**
	 * Seeks to a new stream position and starts a new read context (blocking).
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 */
	public void rseek(long pos, VStream.PosBase posRef)
			throws VStreamError {
		try {
			this.rseek(pos, posRef, -1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seeks to a new stream position and starts a new read context.
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 * @param timeout timout in milliseconds (blocking if negative)
	 */
	public void rseek(long pos, VStream.PosBase posRef, long timeout)
			throws VStreamTimeout, VStreamError {
		this.rseek(pos, posRef, timeout, 0);
	}

	/**
	 * Seeks to a new stream position and starts a new read context.
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 * @param timeout timout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	public void rseek(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		_stream.rseek(pos, posRef, timeout, ntimeout);
	}

	/**
	 * Seeks to a new stream position and starts a new write context (blocking).
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 */
	public void wseek(long pos, VStream.PosBase posRef)
			throws VStreamError {
		try {
			this.wseek(pos,  posRef, -1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seeks to a new stream position and starts a new write context.
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 * @param timeout timout in milliseconds (blocking if negative)
	 */
	public void wseek(long pos, VStream.PosBase posRef, long timeout)
			throws VStreamTimeout, VStreamError {
		this.wseek(pos, posRef, timeout, 0);
	}

	/**
	 * Seeks to a new stream position and starts a new write context.
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 * @param timeout timout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	public void wseek(long pos, VStream.PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		_stream.wseek(pos, posRef, timeout, ntimeout);
	}

	/**
	 * Seeks to a new stream position (blocking).
	 *
	 * <p>Starts a new context of same type as any current active context, e.g. if
	 * the stream has a current active read context, then a new read context is
	 * initiated.</p>
	 *
	 * <p>If there is no current active context, the position is logged as the
	 * position and position reference to be used if a new context is
	 * lazy-started without position information.</p>
	 *
	 * @param pos relative position
	 * @param posRef position reference
	 */
	public void seek(long pos, PosBase posRef)
			throws VStreamError {
		try {
			this.seek(pos, posRef, -1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seeks to a new stream position.
	 *
	 * <p>See {@link #seek(long, PosBase)}.</p>
	 *
	 * @param timeout timout in milliseconds (blocking if negative)
	 */
	public void seek(long pos, PosBase posRef, long timeout)
			throws VStreamTimeout, VStreamError {
		_stream.seek(pos, posRef, timeout, 0);
	}

	/**
	 * Seeks to a new stream position.
	 *
	 * <p>See {@link #seek(long, PosBase)}.</p>
	 *
	 * @param timeout timout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	public void seek(long pos, PosBase posRef, long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		_stream.seek(pos, posRef, timeout, ntimeout);
	}

	/**
	 * Get the current (absolute) stream position (blocking).
	 *
	 * <p>Returned stream position is the current local stream position, which is
	 * at the beginning of any locally buffered data. A position may not always
	 * be available due to synchronization effects with the peer streamer.</p>
	 *
	 * @return absolute stream position
	 */
	long getPos()
			throws VStreamError {
		try {
			return this.getPos(-1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the current (absolute) stream position.
	 *
	 * <p>See {@link #getPos()}.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 */
	long getPos(long timeout)
			throws VStreamTimeout, VStreamError {
		return this.getPos(timeout, 0);
	}

	/**
	 * Get the current (absolute) stream position.
	 *
	 * <p>See {@link #getPos()}.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @param ntimeout additional timeout in nanoseconds
	 */
	long getPos(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		return _stream._pos(timeout, ntimeout);
	}

	/**
	 * Truncates stream data before current position (blocking).
	 *
	 * <p>The stream must be in write-mode when this method is called.</p>
	 *
	 * @throws VStreamError
	 */
	public void truncBefore()
			throws VStreamError {
		try {
			this.truncBefore(-1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Truncates stream data before current position.
	 *
	 * <p>See {@link #truncBefore()}. If truncation cannot be initiated before timeout expires,
	 * {@link VStreamTimeout} is raised. This should typically be because the
	 * peer has not yet acknowledged the current context by sending a start
	 * position notification.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @throws VStreamTimeout
	 */
	public void truncBefore(long timeout)
			throws VStreamTimeout, VStreamError {
		this.truncBefore(timeout, 0);
	}

	/**
	 * Truncates stream data before current position.
	 *
	 * <p>See {@link #truncBefore()}. If truncation cannot be initiated before timeout expires,
	 * {@link VStreamTimeout} is raised. This should typically be because the
	 * peer has not yet acknowledged the current context by sending a start
	 * position notification.</p>
	 *
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void truncBefore(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		_stream.truncBefore(timeout, ntimeout);
	}

	/**
	 * Truncates stream data after current position (blocking).
	 *
	 * <p>The stream must be in write-mode when this method is called.</p>
	 *
	 * @throws VStreamError
	 */
	public void truncAfter()
			throws VStreamError {
		try {
			this.truncAfter(-1);
		} catch (VStreamTimeout e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Truncates stream data after current position.
	 *
	 * <p>See {@link #truncAfter()}. If truncation cannot be initiated before timeout expires,
	 * {@link VStreamTimeout} is raised. This should typically be because the
	 * peer has not yet acknowledged the current context by sending a start
	 * position notification.</p>
	 *
	 * <p>The stream must be in write-mode when this method is called.</p>
	 *
	 * @param timeout timeout in milliseconds (blocking if negative)
	 * @throws VStreamTimeout
	 */
	public void truncAfter(long timeout)
			throws VStreamTimeout, VStreamError {
		this.truncAfter(timeout, 0);
	}

	/**
	 * Truncates stream data after current position.
	 *
	 * <p>See {@link #truncAfter()}. If truncation cannot be initiated before timeout expires,
	 * {@link VStreamTimeout} is raised. This should typically be because the
	 * peer has not yet acknowledged the current context by sending a start
	 * position notification.</p>
	 *
	 * <p>The stream must be in write-mode when this method is called.</p>
	 *
	 * @param ntimeout additional timeout in nanoseconds
	 * @throws VStreamTimeout
	 */
	public void truncAfter(long timeout, int ntimeout)
			throws VStreamTimeout, VStreamError {
		_stream.truncAfter(timeout, ntimeout);
	}

	/**
	 * Closes the stream.
	 *
	 * <p>Initiates stream close with the peer streamer. No other stream operations
	 * should be performed on the stream object after calling this method.</p>
	 */
	public void close() {
		this._stream._close();
	}

	/**
	 * Sets end-of-stream policy for new read contexts.
	 *
     * <p>The policy is only applied to new read contexts, and does not apply
     * to any current active read context.</p>
	 *
	 * @param requestEos if true request end-of-stream on new read contexts
	 */
	synchronized void setEosPolicy(boolean requestEos) {
		_stream.setEosPolicy(requestEos);
	}

	/**
	 * Check if stream was (sometime) connected to a peer.
	 *
	 * @return true if stream is or was connected
	 */
	public boolean isConnected() {
		synchronized(_stream) {
			return _stream._connected;
		}
	}

	/**
	 * Check if stream is active.
	 *
	 * @return true if active
	 */
	public boolean isActive() {
		return _stream.isActive();
	}

	/**
	 * Check if stream has been locally closed.
	 *
	 * @return true if locally closed
	 */
	public boolean isDone() {
		synchronized(_stream) {
			return _stream._done;
		}
	}

	/**
	 * Check if stream was closed and peer acknowledged closing.
	 *
	 * @return true if closed
	 */
	public boolean isClosed() {
		synchronized(_stream) {
			return _stream._closed;
		}
	}

	/**
	 * Check if stream has failed.
	 *
	 * @return true if failed
	 */
	public boolean isFailed() {
		synchronized(_stream) {
			return _stream._failed;
		}
	}

	/**
	 * Adds an observer for stream event notification.
	 *
	 * <p>Will only hold weak references to added observers.</p>
	 *
	 * @param observer observer to add
	 */
	public void addObserver(VStreamObserver<T> observer) {
		_stream.addObserver(observer);
	}

	/**
	 * Removes an observer from stream event observer list.
	 *
	 * @param observer observer to remove
	 */
	public void removeObserver(VStreamObserver<T> observer) {
		_stream.removeObserver(observer);
	}

	/**
	 * Get the associated VSE stream code for a position reference type.
	 *
	 * @param ref position reference
	 * @return VSE stream code
	 */
	public static int posRefToInt(PosBase ref) {
		if (ref == PosBase.ABS)
			return 1;
		else if (ref == PosBase.START)
			return 2;
		else if (ref == PosBase.END)
			return 3;
		else if (ref == PosBase.CURRENT)
			return 4;
		else
			throw new RuntimeException();
	}

	/**
	 * Get the associated reference type for a VSE stream code.
	 *
	 * @param code VSE stream code
	 * @return associated position reference
	 * @throws VStreamException illegal code
	 */
	public static PosBase intToPosRef(int code)
			throws VStreamException {
		if (code == 1)
			return PosBase.ABS;
		else if (code == 2)
			return PosBase.START;
		else if (code == 3)
			return PosBase.END;
		else if (code == 4)
			return PosBase.CURRENT;
		else
			throw new VStreamException("Invalid code");
	}
}
