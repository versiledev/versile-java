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

import java.util.logging.Logger;


/**
 * Link monitor configuration parameters.
 *
 * <p>'holdGw' is True if link monitor should hold a reference to the peer gateway,
 * otherwise False. Default is False.</p>
 *
 * <p>'linkTimeout' is the timout in milliseconds for connecting and handshaking a
 * new link. Default value is 30000ms (30s).</p>
 *
 * <p>'minRetryTime' is the minimum time in milliseconds to wait before a new link attempt after a
 * failed link attempt. Default value is 5000ms (5s).</p>
 *
 * <p>'maxRetryTime' is the maximum time in milliseconds to wait before a new link attempt after a
 * failed link attempt. Default value is 600000ms (10min).</p>
 *
 * <p>'backOffFactor' is a multiplier of retry time for each new retry attempt before a successful
 * link connection is made. Default value is 2.0f.</p>
 *
 * <p>'logger' is a logger for log events from the monitor.</p>
 */
public class VLinkMonitorConfig {

	// When adding fields remember to update copyTo()
	boolean holdGw = false;
	long linkTimeout = 30000;
	long minRetryTime = 5000;
	long maxRetryTime = 600000;
	float backOffFactor = 2.0f;
	Logger logger = null;

	@Override
	public VLinkMonitorConfig clone() {
		VLinkMonitorConfig result = new VLinkMonitorConfig();
		this.copyTo(result);
		return result;
	}

	public boolean isHoldGw() {
		return holdGw;
	}

	public void setHoldGw(boolean holdGw) {
		this.holdGw = holdGw;
	}

	public long getLinkTimeout() {
		return linkTimeout;
	}

	public void setLinkTimeout(long linkTimeout) {
		this.linkTimeout = linkTimeout;
	}

	public long getMinRetryTime() {
		return minRetryTime;
	}

	public void setMinRetryTime(long minRetryTime) {
		this.minRetryTime = minRetryTime;
	}

	public long getMaxRetryTime() {
		return maxRetryTime;
	}

	public void setMaxRetryTime(long maxRetryTime) {
		this.maxRetryTime = maxRetryTime;
	}

	public float getBackOffFactor() {
		return backOffFactor;
	}

	public void setBackOffFactor(float backOffFactor) {
		this.backOffFactor = backOffFactor;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	protected void copyTo(VLinkMonitorConfig config) {
		config.holdGw = holdGw;
		config.linkTimeout = linkTimeout;
		config.minRetryTime = minRetryTime;
		config.maxRetryTime = maxRetryTime;
		config.backOffFactor = backOffFactor;
		config.logger = logger;
	}
}
