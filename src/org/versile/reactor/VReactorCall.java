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

package org.versile.reactor;

import org.versile.common.call.VCall;


/**
 * References a call scheduled for execution by the reactor.
 */
public class VReactorCall extends VCall<Object> implements Comparable<VReactorCall>{

	/**
	 * Call number to assign to next reactor call.
	 */
	protected static Long nextCallNumber = 0L;

	/**
	 * Reactor which schedules/executes the call.
	 */
	protected VReactor reactor;
	/**
	 * Function to execute when executing call.
	 */
	protected VReactorFunction function;
	/**
	 * Scheduled (earliest) execution time of the call.
	 */
	protected long scheduledTime;
	/**
	 * Reactor call sequence number.
	 */
	protected long callNumber;

	/**
	 * Registers a call with a scheduled time.
	 *
	 * @param reactor owning reactor
	 * @param function function to execute which produces a call result
	 * @param scheduled_time scheduled time (vs. System.nanoTime())
	 */
	protected VReactorCall(VReactor reactor, VReactorFunction function, long scheduled_time) {
		synchronized(nextCallNumber) {
			callNumber = nextCallNumber;
			nextCallNumber += 1;
		}
		this.reactor = reactor;
		this.function = function;
		this.scheduledTime = scheduled_time;
	}

	/**
	 * Compares to another call based on call scheduled execution time.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(VReactorCall call) {
		if (scheduledTime < call.scheduledTime)
			return -1;
		else if (scheduledTime > call.scheduledTime)
			return 1;
		else if (callNumber < call.callNumber)
			return -1;
		else if (callNumber > call.callNumber)
			return 1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VReactorCall))
			return false;
		VReactorCall o_call = (VReactorCall) other;
		return (callNumber == o_call.callNumber);
	}

	/**
	 * Get the call's scheduled time.
	 *
	 * @return scheduled time (relative to System.nanoTime())
	 */
	public long getScheduledTime() {
		return scheduledTime;
	}

	@Override
	protected void _pushCleanup() {
		reactor = null;
		function = null;
	}

	@Override
	protected void _cancel() {
		if (reactor != null) {
			reactor.unschedule(this);
			this._pushCleanup();
		}
	}
}
