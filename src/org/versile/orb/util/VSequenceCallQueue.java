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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import org.versile.common.call.VCall;


/**
 * Call handler resolving peer calls in order.
 *
 * <p>Resolves calls with an associated callID in increasing order. It is
 * typically used to resolve calls generated (remotely) by a
 * {@link VSequenceCaller}. Enforces a limit on maximum parallel calls.</p>
 */
public class VSequenceCallQueue {

	int call_lim;
	long next_id = 0L;
	Map<Long, CallData> calls;

	/**
	 * Set up call queue.
	 *
	 * @param maxCalls maximum allowed calls in queue (negative if no limit)
	 */
	public VSequenceCallQueue(int maxCalls) {
		call_lim = maxCalls;
		calls = new Hashtable<Long, CallData>();
	}

	/**
	 * Queues a call for execution.
	 *
	 * @param callID call ID which determines call sequence
	 * @param localCall call to execute
	 * @return reference to call result
	 * @throws VSequenceCallException max calls limit exceeded or other call ID error
	 */
	public VCall<Object> queue(long callID, LocalCall localCall)
			throws VSequenceCallException {
		VCall<Object> result;
		synchronized(this) {
			if (callID < next_id)
				throw new VSequenceCallException();
			if (callID < next_id || (call_lim >= 0 && callID >= next_id+call_lim) || calls.get(callID) != null)
				throw new VSequenceCallException();
			result = new VCall<Object>();
			CallData data = new CallData();
			data.call = localCall;
			data.result = result;
			calls.put(callID, data);
		}
		this._process_queue();
		return result;
	}

	void _process_queue() {
		LinkedList<CallData> _calls = new LinkedList<CallData>();
		synchronized(this) {
			while (true) {
				CallData data = calls.get(next_id);
				if (data != null) {
					_calls.addLast(data);
					calls.remove(next_id);
					next_id += 1;
				}
				else
					break;
			}
		}
		while (!_calls.isEmpty()) {
			CallData data = _calls.removeFirst();
			try {
				Object result = data.call.execute();
				data.result.silentPushResult(result);
			} catch (Exception e) {
				data.result.silentPushException(e);
			}
		}
	}

	/**
	 * A local call which should be executed in sequence.
	 */
	public static abstract class LocalCall {

		/**
		 * Remote call which can be initiated by a sequence caller.
		 *
		 * @return call result
		 * @throws call exception
		 */
		public abstract Object execute() throws Exception;
	}

	static class CallData {
		public LocalCall call;
		VCall<Object> result;
	}
}
