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

package org.versile.orb.util;

import java.util.LinkedList;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;


/**
 * Call queue which limits number of pending calls to a receiving peer.
 *
 * <p>Queues remote calls for execution and submits to peer in sequence, while keeping
 * the number of pending remote calls within the set maximum calls limit.</p>
 *
 * <p>Calls are provided with a call ID number which can be passed to the peer so that the receiving peer
 * can use the call ID argument to resolve calls in sequence. The call ID of the first call is 0, and for each
 * following call the ID is increased by one.</p>
 */
public class VSequenceCaller {

	LinkedList<CallData> call_queue;
	int call_lim;
	int num_pending = 0;
	long next_msg_id = 0L;

	/**
	 * Set up call queue.
	 *
	 * @param maxCalls maximum pending calls (unlimited if negative)
	 */
	public VSequenceCaller(int maxCalls) {
		call_queue = new LinkedList<CallData>();
		call_lim = maxCalls;
	}

	/**
	 * Queues a remote call for execution.
	 *
	 * @param remoteCall call to execute
	 * @param callback callback for result (or null)
	 * @param failback callback for exception (or null)
	 * @return call ID which will be passed to job
	 */
	public long call(RemoteCall remoteCall, Callback callback, Failback failback) {
		long msg_id;
		synchronized(this) {
			msg_id = next_msg_id;
			next_msg_id += 1;

			CallData data = new CallData();
			data.msg_id = msg_id;
			data.call = remoteCall;
			data.callback = callback;
			data.failback = failback;
			call_queue.addLast(data);
		}
		this._process_queue();
		return msg_id;
	}

	/**
	 * Sets a new call limit for the maximum number of pending calls.
	 *
	 * @param maxCalls max pending calls
	 */
	public void setLimit(int maxCalls) {
		synchronized(this) {
			call_lim = maxCalls;
		}
		this._process_queue();
	}

	// Internal call execution handler
	void _call(long msg_id, RemoteCall callable, Callback callback, Failback failback) {
		class ResultHandler implements VCallResultHandler<Object> {
			VSequenceCaller caller;
			long msg_id;
			Callback handler;
			public ResultHandler(VSequenceCaller caller, long msg_id, Callback handler) {
				this.caller = caller;
				this.msg_id = msg_id;
				this.handler = handler;
			}
			@Override
			public void callback(Object result) {
				synchronized(caller) {
					caller.num_pending -= 1;
				}
				if (handler != null)
					handler.callback(msg_id, result);
				caller._process_queue();
			}
		}
		class ExceptionHandler implements VCallExceptionHandler {
			VSequenceCaller caller;
			long msg_id;
			Failback handler;
			public ExceptionHandler(VSequenceCaller caller, long msg_id, Failback handler) {
				this.caller = caller;
				this.msg_id = msg_id;
				this.handler = handler;
			}
			@Override
			public void callback(Exception e) {
				synchronized(caller) {
					caller.num_pending -= 1;
				}
				if (handler != null)
					handler.callback(msg_id, e);
				caller._process_queue();
			}
		}
		VCall<Object> call = callable.execute(msg_id);
		VCallResultHandler<Object> r_handler = new ResultHandler(this, msg_id, callback);
		VCallExceptionHandler e_handler = new ExceptionHandler(this, msg_id, failback);
		call.addHandlerPair(r_handler, e_handler);
	}

	// Execute all calls which can be activated
	void _process_queue() {
		LinkedList<CallData> calls = new LinkedList<CallData>();
		synchronized(this) {
			while ((call_lim < 0 || num_pending < call_lim) && !call_queue.isEmpty()) {
				calls.addLast(call_queue.removeFirst());
				num_pending += 1;
			}
		}

		while (!calls.isEmpty()) {
			CallData data = calls.removeFirst();
			this._call(data.msg_id, data.call, data.callback, data.failback);
		}
	}

	/**
	 * A remote call which can be executed by a sequence caller.
	 */
	public static abstract class RemoteCall {

		/**
		 * Remote call which can be initiated by a sequence caller.
		 *
		 * @param callID call ID to apply for call
		 * @return reference to remote call result
		 */
		public abstract VCall<Object> execute(long callID);
	}

	/**
	 * Callback for a sequenced call's result.
	 */
	public abstract static class Callback {
		/**
		 * Callback with call result.
		 *
		 * @param callID call ID of executed call
		 * @param result call result
		 */
		public abstract void callback(long callID, Object result);
	}

	/**
	 * Callback for a sequenced call's raised exception.
	 */
	public abstract static class Failback {
		/**
		 * Callback with call's raised exception.
		 *
		 * @param callID call ID of executed call
		 * @param e exception raised by call
		 */
		public abstract void callback(long callID, Exception e);
	}

	static class CallData {
		public RemoteCall call;
		public long msg_id;
		Callback callback;
		Failback failback;
	}
}
