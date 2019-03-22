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

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallCancelled;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.util.VSimpleBusListener;
import org.versile.orb.link.VLink;
import org.versile.orb.link.VLink.Status;
import org.versile.orb.url.VUrlResolver.ResolveResult;


/**
 * Link monitor which tries to keep a link in operation.
 *
 * <p>When the monitor is active it will try to set up and maintain a link which is
 * set up by {@link #createLink}. If setting up the link times out or an
 * existing link fails, the link is terminated and the monitor tries to set up a
 * new link to replace the timed out or failed link.</p>
 *
 * <p>When an attempt is made to set up a link and the attempt fails or times out,
 * a new attempt is made after a delay, which for the first attempt is
 * a minimum retry time set on the monitor. For each new attempt until a link is
 * successfully set up, the retry time is multiplied by a back-off factor until it
 * reaches a set maximum valu.</p>
 *
 * <p>Control mechanisms such as {@link VLinkMonitor} are useful for operating
 * decentral services which are dispatched through a central resource, when
 * availability of the connecting network or the peer service is not guaranteed.</p>
 */
public abstract class VLinkMonitor {

	/**
	 * Configuration settings.
	 */
	protected VLinkMonitorConfig config;

	long _retry_t;

	Object _gw = null;                                  // Current link's gateway (if holding gw)
	VLink _link = null;                                 // Current link
	VCall<ResolveResult> _link_call = null;             // Current link resolve call
	VSimpleBusListener<VLink.Status> _listener = null;  // Status listener

	boolean _active = false;                            // True if link monitor is active

	Timer _timer = null;

	/**
	 * Set up monitor with default configuration.
	 */
	public VLinkMonitor() {
		this.construct(null);
	}

	/**
	 * Set up monitor.
	 *
	 * @param config monitor configuration
	 */
	public VLinkMonitor(VLinkMonitorConfig config) {
		this.construct(config);
	}

	void construct(VLinkMonitorConfig config) {
		if (config == null)
			config = new VLinkMonitorConfig();
		this.config = config;

		_retry_t = config.getMinRetryTime();
	}

	/**
	 * Starts the monitor and initializes link control.
	 *
	 * <p>If not already running, starting the monitor will cause the
	 * monitor to initialize a link and start link control.</p>
	 */
	public void start() {
		synchronized(this) {
			if (!_active) {
				this.log("Starting monitor");
				_active = true;
				this.newLink();
			}
		}
	}

	/**
	 * Stops the monitor and terminates any current link.
	 */
	public void stop() {
		synchronized(this) {
			if (_active) {
				this.log("Stopping monitor");
				_active = false;
				this.log("Terminating any active link");
				this.terminateLink();
			}
		}
	}


	/**
	 * Creates a new link.
	 *
	 * <p>The asynchronous call result should throw an exception if link setup fails,
	 * and when successful it should return a gateway and link as a result. Return values
	 * are similar to {@link org.versile.orb.url.VUrlResolver#nowaitResolveWithLink()}.</p>
	 *
	 * <p>The returned call handler must be cancellable, i.e. calling cancel() on the call
	 * result will abort the link connection process and free up any associated resources.</p>
	 *
	 * @return asynchronous call result to a gateway and link.
	 */
	public abstract VCall<ResolveResult> createLink();

	/**
	 * Get the current link, or null if no link.
	 *
	 * @return link or null
	 */
	public VLink getLink() {
		return _link;
	}


	/**
	 * Return gateway of current link.
	 *
	 * <p>Returns null if there is no active link, or if the monitor is configured not
	 * to hold a link peer gateway.</p>
	 *
	 * @return gateway of current link, or null
	 */
	public Object getPeerGateway() {
		return _gw;
	}

	/**
	 * Get the logger set up for the monitor.
	 *
	 * @return logger, or null
	 */
	public Logger getLogger() {
		return config.getLogger();
	}

	void newLink() {
		synchronized(this) {
			this.log("Initiating link");
			// Terminate any old link
			this.terminateLink();

			// Initialize new link
			_link_call = this.createLink();
			class ResHandler implements VCallResultHandler<ResolveResult> {
				@Override
				public void callback(ResolveResult result) {
					linkResult(result);
				}
			}
			class ExcHandler implements VCallExceptionHandler {
				@Override
				public void callback(Exception e) {
					linkException(e);
				}
			}
			_link_call.addHandlerPair(new ResHandler(), new ExcHandler());

			// Set a timer for timing out the link
			Timer _old_timer = _timer;
			if (_old_timer != null)
				_old_timer.cancel();
			class Task extends TimerTask {
				@Override
				public void run() {
					linkTimeout();
				}
			}
			_timer = new Timer();
			_timer.schedule(new Task(), config.getLinkTimeout());
		}
	}

	/**
	 * Called when a link is successfully connected.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 *
	 * @param link connected link
	 * @param gw peer gw of connected link
	 */
	protected void linkConnected(VLink link, Object gw) {
	}

	/**
	 * Called when an active link is lost.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	protected void linkLost() {
	}

	void terminateLink() {
		synchronized(this) {
			_listener = null;
			VLink _lnk = _link;
			_link = null;
			if (_lnk != null)
				_lnk.shutdown(true);
			VCall<ResolveResult> _lnk_call = _link_call;
			_link_call = null;
			if (_lnk_call != null)
				_lnk_call.cancel();
			Timer _tim = _timer;
			_timer = null;
			if (_tim != null)
				_tim.cancel();
		}
	}

	// Callback when link is running
	void running(LinkMonitorListener listener) {
		// For now do nothing
	}


	// Callback when link is closing
	void closing(LinkMonitorListener listener) {
		// For now do nothing
	}

	// Callback when link is closed, perform monitor/control actions
	void closed(LinkMonitorListener listener) {
		synchronized(this) {
			if (listener == _listener) {
				this.log("Lost current link");
				this.terminateLink();
				this.linkLost();
				if (_active)
					this.newLink();
			}
		}
	}

	void linkResult(ResolveResult result) {
		synchronized(this) {
			_link_call = null;
			if (!_active) {
				this.terminateLink();
				return;
			}
			_link = result.getLink();
			Object _link_gw = result.getResource();
			if (config.isHoldGw())
				_gw = _link_gw;
			else
				_gw = null;
			_listener = new LinkMonitorListener(this);
			Timer _old_timer = _timer;
			if (_old_timer != null)
				_old_timer.cancel();
			_timer = null;
			_retry_t = config.getMinRetryTime();

			this.linkConnected(_link, _link_gw);
		}

		_link.registerStatusListener(_listener);
		this.log("Link connected");
	}

	void linkException(Exception exc) {
		synchronized(this) {
			if (exc instanceof VCallCancelled)
				// Call cancelled internally, handled elsewhere
				return;
			this.log("Setting up link failed");
			_link_call = null;
			Timer _old_timer = _timer;
			_timer = null;
			if (_old_timer != null)
				_old_timer.cancel();
			if (_active && _link == null) {
				scheduleRetry();
			}
		}
	}

	void linkTimeout() {
		synchronized(this) {
			this.log("Link timeout");
			VCall<ResolveResult> _old_call = _link_call;
			_link_call = null;
			if (_old_call != null)
				_old_call.cancel();
			if (_active && _link == null) {
				scheduleRetry();
			}
		}
	}

	void scheduleRetry() {
		class Task extends TimerTask {
			@Override
			public void run() {
				if (_active && _link == null)
					newLink();
			}
		}
		if (_active) {
			Timer _old_timer = _timer;
			if (_old_timer != null)
				_old_timer.cancel();
			long _ret_t = _retry_t;
			_retry_t = (long) (_retry_t*config.getBackOffFactor());
			_retry_t = Math.min(_retry_t,  config.getMaxRetryTime());
			_timer = new Timer();
			_timer.schedule(new Task(), _ret_t);
			this.log("Scheduled retry in " + (_ret_t/1000.0f) + " s");
		}
	}

	void log(String msg) {
		if (config.getLogger() != null)
			config.getLogger().log(Level.FINEST, "VLinkMonitor: " + msg);
	}

	/**
	 * Listener for the VLinkMonitor class.
	 */
    class LinkMonitorListener implements VSimpleBusListener<VLink.Status> {
		VLinkMonitor monitor;
		public LinkMonitorListener(VLinkMonitor monitor) {
			this.monitor = monitor;
		}
		@Override
		public void busPush(Status obj) {
			if (obj == VLink.Status.RUNNING)
				this.monitor.running(this);
			else if (obj == VLink.Status.CLOSING)
				this.monitor.closing(this);
			else if (obj == VLink.Status.CLOSED)
				this.monitor.closed(this);
		}
	}

}
