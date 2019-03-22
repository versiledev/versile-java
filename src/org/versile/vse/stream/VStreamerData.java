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


/**
 * Data which can be accessed via a streamer.
 *
 * <p>Stream data may support reading and/or writing, and other operations such as
 * seek operations.</p>
 *
 * <p>A connected {@link VStreamer} should be set up with mode settings which are
 * appropriate for the features available on a connected {@link VStreamerData}
 * object.</p>
 *
 * @param <T> stream data type
 */
public abstract class VStreamerData<T> {

	/**
	 * Notification flag for changes to stream data endpoints.
	 */
	public static int NOTIFY_ENDPOINTS = 0x01;

	/**
	 * Streamer owning access to streamer data.
	 */
	protected WeakReference<VStreamer<T>> streamer = null;

	/**
	 * Bitwise OR of flags for enabled notification events.
	 */
	protected int notify_flags = 0;

	/**
	 * Reads data from current position.
	 *
	 * <p>Returns empty data set if end-of-stream was reached for the current data
	 * set boundary. If less than 'maxNum' elements are read then this implies
	 * the (current) end-of-stream delimiter was reached after the returned
	 * data.</p>
	 *
	 * <p>The method should only be called by the object's controlling streamer.</p>
	 *
	 * <p>The method may update streamer data endpoints. If a controlling streamer
	 * tracks streamer data endpoints, it should poll {@link #getEndpoints()}
	 * after calling this method to get new endpoint information.</p>
	 *
	 * @param maxNum max elements to read
	 * @return data read
	 * @throws VStreamError operation error
	 * @throws VStreamFailure general streamer data failure
	 */
	public abstract T[] read(int maxNum)
			throws VStreamError, VStreamFailure;

	/**
	 * Writes data at current position.
	 *
	 * <p>Writing data will overwrite the corresponding data elements in the data
	 * set after the current position. The method must accept and 'write' all
	 * received data, otherwise it should raise an exception if all data could
	 * not be written.</p>
	 *
	 * <p>The method should only be called by the object's controlling streamer.</p>
	 *
	 * <p>The method may update streamer data endpoints. If a controlling streamer
	 * tracks streamer data endpoints, it should poll updated {@link #getEndpoints()}
	 * after calling this method to get new endpoint information.</p>
	 *
	 * @param data data to write
	 * @throws VStreamError operation error
	 * @throws VStreamFailure general streamer data failure
	 */
	public abstract void write(T[] data)
			throws VStreamError, VStreamFailure;

	/**
	 * Repositions the data set to a new position.
	 *
	 * <p>The method should only be called by the object's controlling
	 * {@link VStreamer}.</p>
	 *
	 * <p>The method may update streamer data endpoints. If a controlling
	 * streamer tracks streamer data endpoints, it should poll updated
	 * endpoints after calling this method to get new endpoint
	 * information.</p>
	 *
	 * @param pos position relative to position reference
	 * @param posRef position reference
	 * @return resulting absolute stream position
	 * @throws VStreamError seek operation error
	 * @throws VStreamFailure permanent stream data failure
	 */
	public abstract long seek(long pos, VStream.PosBase posRef)
			throws VStreamError, VStreamFailure;

	/**
	 * Truncate streamer data before current position.
	 *
	 * <p>When successfully called, the current position becomes the
	 * start position of the streamer data.</p>
	 *
	 * <p>The method must be implemented on derived classes which support
	 * the mode flags CAN_MOVE_START and START_CAN_INC.</p>
	 *
	 * <p>The method should only be called by the object's controlling streamer.</p>
	 *
	 * <p>The method may update streamer data endpoints. If a controlling streamer
	 * tracks streamer data endpoints, it should poll updated {@link #getEndpoints()}
	 * after calling this method to get new endpoint information.</p>
	 *
	 * @throws VStreamError seek operation error
	 * @throws VStreamFailure permanent stream data failure
	 */
	public abstract void truncateBefore()
			throws VStreamError, VStreamFailure;

	/**
	 * Truncate streamer data after current position.
	 *
	 * <p>When successfully called, the current position becomes the
	 * last position of the streamer data.</p>
	 *
	 * <p>The method must be implemented on derived classes which support
	 * the mode flags CAN_MOVE_END and END_CAN_INC.</p>
	 *
	 * <p>The method should only be called by the object's controlling streamer.</p>
	 *
	 * <p>The method may update streamer data endpoints. If a controlling streamer
	 * tracks streamer data endpoints, it should poll updated {@link #getEndpoints()}
	 * after calling this method to get new endpoint information.</p>
	 *
	 * @throws VStreamError seek operation error
	 * @throws VStreamFailure permanent stream data failure
	 */
	public abstract void truncateAfter()
			throws VStreamError, VStreamFailure;

	/**
	 * Closes access to streamer data.
	 *
	 * <p>This enables streamer data resources to be freed up. Closing streamer
	 * data will close any current context, and opening new contexts after
	 * closing is not allowed.</p>
	 *
	 * <p>The method should only be called by the object's controlling
	 * {@link VStreamer}.</p>
	 */
	public abstract void close();

	/**
	 * Sets streamer for callbacks.
	 *
	 * <p>Will only hold a weak reference to connected streamer.</p>
	 *
	 * <p>Notifications can be enabled with {@link #enableNotifications(int)}.</p>
	 *
	 * @param streamer connected streamer
	 * @throws VStreamError streamer already set
	 */
	public synchronized void setStreamer(VStreamer<T> streamer)
			throws VStreamError {
		if (this.streamer != null)
			throw new VStreamError("Stream already registered");
		this.streamer = new WeakReference<VStreamer<T>>(streamer);
	}

	/**
	 * Enables notifications to a registered streamer.
	 *
	 * @param flags bitwise or of flags for notifications to enable
	 */
	public synchronized void enableNotifications(int flags) {
		notify_flags |= flags;
	}

	/**
	 * Disables notifications to a registered streamer.
	 *
	 * @param flags bitwise or of flags for notifications to disable
	 */
	public synchronized void disableNotifications(int flags) {
		notify_flags |= flags;
		notify_flags ^= flags;
	}

	/**
	 * Get the current (absolute) stream position.
	 *
	 * @return current stream position
	 */
	public abstract long getPosition()
			throws VStreamError, VStreamFailure;

	/**
	 * Get data endpoints.
	 *
	 * @return endpoints as 2-tuple (start_pos, end_pos)
	 * @throws VStreamError no endpoint due to stream error
	 * @throws VStreamFailure no endpoint due to general stream failure
	 */
	public abstract Endpoint[] getEndpoints()
			throws VStreamError, VStreamFailure;

	/**
	 * Get required mode for the streamer data.
	 *
	 * <p>Returns a 2-tuple of [supported_modes, required_mode_mask]. The supported bits are the
	 * stream mode bits which may be enabled that are supported by the streamer data. The mask is a
	 * set of bytes which must be set according to how they occur in supported bits.</p>
	 *
	 * @return array of 2 modes (supported_modes, required_mode_mask)
	 */
	public abstract VStreamMode[] getRequiredMode();

	/**
	 * Optional mode flags for the streamer data.
	 *
	 * @return optional mode flags
	 */
	public abstract VStreamMode getOptionalMode();

	/**
	 * Internal call to notify streamer of endpoint change.
	 *
	 * <p>If a peer streamer has been registered with endpoint notification
	 * enabled, a notification is passed to the streamer.</p>
	 *
	 * <p>The notification should only be triggered whenever one of the end-point
	 * positions of the stream data changes, and it should not be called by the
	 * streamer data's controlling streamer (which keeps track of the
	 * end-point effects of operations it performs itself).</p>
	 */
	protected void notifyEndpoints() {
		VStreamer<?> streamer;

		synchronized(this) {
			if ((notify_flags & NOTIFY_ENDPOINTS) == 0 || this.streamer == null)
				return;
			streamer = this.streamer.get();
			if (streamer == null) {
				this.streamer = null;
				return;
			}
		}
		streamer.notifyEndpoints();
	}

	/**
	 * Holds end-point information.
	 *
	 * <p>If 'bounded' is true then 'position' holds a position reference for
	 * the end-point, otherwise the endpoint is unbounded (and 'position'
	 * holds no meaning)</p>
	 */
	public static class Endpoint {
		boolean bounded;
		long position;
		/**
		 * Set position data.
		 *
		 * @param bounded true if position is bounded
		 * @param position position (only has meaning if bounded)
		 */
		public Endpoint(boolean bounded, long position) {
			this.bounded = bounded;
			this.position = position;
		}
		/**
		 * Check if position is bounded.
		 *
		 * @return true if bounded
		 */
		public boolean isBounded() {
			return bounded;
		}
		/**
		 * Get end-point position
		 *
		 * @return position (only has meaning if bounded)
		 */
		public long getPosition() {
			return position;
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Endpoint))
				return false;
			Endpoint other = (Endpoint) obj;
			if (bounded ^ other.bounded)
				return false;
			if (bounded && position != other.position)
				return false;
			return true;
		}
	}
}
