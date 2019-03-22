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

package org.versile.orb.link;


/**
 * Keep-alive settings for a link.
 *
 * <p>Time periods are in milliseconds. If 'reqTime' is negative then no keep-alive
 * is requested from peer. Default value is -1 (not requested).
 * </p>
 *
 * <p>'expireFactor' is a factor applied to negotiated time for requested
 * keep-alive time, such that if elapsed time since a link protocol message was
 * received exceeds the negotiated keep-alive time multiplied by this factor,
 * the link is terminated. Default value is 1.5.</p>
 *
 * <p>'spamFactor' is applied to negotiated receive keep-alive. If the median time
 * between keep-alive packages is less than this factor applied to negotiated
 * keep-alive time, the peer is considered to be illegally spamming keep-alive
 * packages, and the link should be terminated. Default value is 0.5.</p>
 *
 * <p>'minSend' is the minimum allowed period for sending keep-alive packages to
 * peer. Default value is 300000ms (5 mins).</p>
 */
public class VLinkKeepAlive {

	// When adding fields remember to update copyTo()
	long reqTime = -1;
	float expireFactor = 1.5f;
	float spamFactor = 0.5f;
	long minSend = 300000;

	@Override
	public VLinkKeepAlive clone() {
		VLinkKeepAlive conf = new VLinkKeepAlive();
		this.copyTo(conf);
		return conf;
	}

	public long getReqTime() {
		return reqTime;
	}

	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}

	public float getExpireFactor() {
		return expireFactor;
	}

	public void setExpireFactor(float expireFactor) {
		this.expireFactor = expireFactor;
	}

	public float getSpamFactor() {
		return spamFactor;
	}

	public void setSpamFactor(float spamFactor) {
		this.spamFactor = spamFactor;
	}

	public long getMinSend() {
		return minSend;
	}

	public void setMinSend(long minSend) {
		this.minSend = minSend;
	}

	protected void copyTo(VLinkKeepAlive config) {
		config.reqTime = reqTime;
		config.expireFactor = expireFactor;
		config.spamFactor = spamFactor;
		config.minSend = minSend;
	}
}
