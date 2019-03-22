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

package org.versile.orb.service;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.Versile;
import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCallTimeout;
import org.versile.common.processor.VProcessor;
import org.versile.common.util.VSimpleBusListener;
import org.versile.orb.link.VLink;
import org.versile.orb.link.VLink.Status;
import org.versile.orb.link.VLinkAuth;
import org.versile.orb.link.VLinkException;


/**
 * Listening service for instantiating links on inbound connections.
 *
 * <p>Must be implemented by derived classes to handle services for a particular
 * {@link org.versile.orb.link.VLink} link subsystem.</p>
 *
 * <p>Global license configuration must be set before a service object can be constructed.
 * See {@link org.versile.Versile} for details.<p>

 */
public abstract class VService {
	/**
	 * Gateway factory for service.
	 */
	protected VGatewayFactory gwFactory;
	/**
	 * Service-side credentials for negotiating links and secure transports.
	 */
	protected VPrivateCredentials credentials;
	/**
	 * Service listening socket.
	 */
	protected ServerSocketChannel channel;
	/**
	 * Link authorizer for approving new links during link handshake.
	 */
	protected VLinkAuth linkAuth;
	/**
	 * Shared processor for instantiated links.
	 */
	protected VProcessor processor;
	/**
	 * Service configuration parameters.
	 */
	protected VServiceConfig config;

	/**
	 * If true service owns a processor shared by instantiated links.
	 */
	protected boolean ownsProcessor = false;
	/**
	 * Active links instantiated by the service.
	 */
	protected Set<VLink> links;
	/**
	 * Monitors for the current active links.
	 */
	protected Hashtable<VLink, VSimpleBusListener<VLink.Status>> linkMonitors;
	/**
	 * Object for synchronizing access to service state changes.
	 */
	protected Lock statusLock;
	/**
	 * If true service has been started.
	 */
	protected boolean started = false;
	/**
	 * If true service is currently active.
	 */
	protected boolean active = false;

	/**
	 * Set up listening service.
	 *
	 * <p>Credentials cannot be provided for VOP service with only insecure transport.</p>
	 *
	 * <p>If link authorizer is not null then it will override (replace) the authorizer
	 * set on the config object's link configuration attribute. If the link is set up without
	 * an authorizer then all communication peers are authorized.</p>
	 *
	 * <p>If listening channel is null then the constructor attempts to bind a new channel
	 * to the default port number for the service protocol.</p>
	 *
	 * <p>If processor is null then a processor is lazy-created for the service. A lazy-created
	 * processor is owned by the service and is shut down by the service when service is stopped.</p>
	 *
	 * @param gwFactory factory for gateway objects for instantiated links
	 * @param credentials server's credentials for secure VTS/TLS transport (or null)
	 * @param linkAuthorizer authorizer for an instantiated link (or null)
	 * @param channel bound listening channel (or null)
	 * @param processor processor to use for instantiated links (or null)
	 * @param config service configuration (or null)
	 * @throws IOException could not set up listening service
	 * @throws VLinkException global org.versile.Versile copyleft info not configured
	 */
	public VService(VGatewayFactory gwFactory, VPrivateCredentials credentials, VLinkAuth linkAuthorizer,
			        ServerSocketChannel channel, VProcessor processor, VServiceConfig config)
			throws IOException, VLinkException {
		if (Versile.copyleft().getCopyleft() == null)
			throw new VLinkException("Global org.versile.Versile copyleft information not configured.");

		this.gwFactory = gwFactory;
		this.credentials = credentials;
		linkAuth = linkAuthorizer;
		this.channel = channel;
		if (config == null)
			config = new VServiceConfig();
		this.config = config;

		if (processor == null) {
			ownsProcessor = true;
			int workers = config.getLazyProcessorWorkers();
			processor = new VProcessor(workers, workers);
		}
		this.processor = processor;
		links = new HashSet<VLink>();
		linkMonitors = new Hashtable<VLink, VSimpleBusListener<VLink.Status>>();
		statusLock = new ReentrantLock();
	}

	/**
	 * Starts the service.
	 *
	 * @throws IOException error starting service
	 */
	public abstract void start()
			throws IOException;

	/**
	 * Stops the service.
	 *
	 * <p>Should only be called after the service has previously been started. It can be
	 * called multiple times with different arguments (e.g. going from a "soft" service
	 * stop to a "hard" service stop).</p>
	 *
	 * @param stopLinks if true stop currently active links
	 * @param force if true use force shutdown when stopping active links
	 */
	public synchronized void stop(boolean stopLinks, boolean force) {
		this.stop(stopLinks, force, false);
	}

	/**
	 * Internal call to handle stopping the service.
	 *
	 * @param stopLinks if true stop currently active links
	 * @param force if true use force shutdown when stopping active links
	 * @param safe false unless known to have link thread separation
	 */
	protected void stop(boolean stopLinks, boolean force, boolean safe) {
		if (!safe) {
			class Job implements Runnable {
				boolean stopLinks;
				boolean force;
				public Job(boolean stopLinks, boolean force) {
					this.stopLinks = stopLinks;
					this.force = force;
				}
				public void run() {
					stop(stopLinks, force, true);
				}
			}
			this.schedule(new Job(stopLinks, force));
			return;
		}
		LinkedList<VLink> shutdown_links = new LinkedList<VLink>();

		synchronized(this) {
			if (active) {
				this.stopListener();
				if (links.isEmpty())
					this.stopThreads();
			}
			if (stopLinks) {
				for (VLink link: links)
					shutdown_links.addLast(link);
			}
		}
		for (VLink link: shutdown_links)
			link.shutdown(force);
	}

	/**
	 * Waits for one of the specified states to occur.
	 *
	 * @param started if true return if state becomes 'started'
	 * @param active if true return if state becomes 'active'
	 * @param stopped if true return if state becomes 'stopped'
	 * @param timeout timeout in milliseconds (or negative if no timeout)
	 * @throws VCallTimeout call timeout
	 * @throws IllegalArgumentException no arguments are true
	 */
	void waitStates(boolean started, boolean active, boolean stopped, long timeout)
			throws VCallTimeout {
		if (!(started || active || stopped))
			throw new IllegalArgumentException("At least one argument must be true");
		synchronized(statusLock) {
			if ((started && this.started) || (active && this.active) || (stopped && started && !active))
				return;
			long start_time = 0L;
			long end_time = 0L;
			if (timeout >= 0) {
				start_time = System.nanoTime();
				end_time = start_time + 1000000L*timeout;
			}
			while (true) {
				if ((started && this.started) || (active && this.active) || (stopped && started && !active))
					return;
				try {
					if (timeout < 0)
						statusLock.wait();
					else {
						long time_left = end_time - System.nanoTime();
						if (time_left <= 0)
							throw new VCallTimeout();
						long msec = time_left / 1000000L;
						statusLock.wait(msec, (int)(time_left-msec*1000000));
					}
				} catch (InterruptedException e) {
					// Ignore interrupt, treat it just as wait completion
				}
			}
		}
	}

	/**
	 * Check if service has been started.
	 *
	 * <p>Should return true even if service was later stopped.</p>
	 *
	 * @return true if service was started
	 */
	public boolean wasStarted() {
		synchronized(statusLock) {
			return started;
		}
	}

	/**
	 * Check if service is currently active.
	 *
	 * @return true if active
	 */
	public boolean isActive() {
		synchronized(statusLock) {
			return active;
		}
	}

	/**
	 * Check if service was started and later stopped.
	 *
	 * @return true if service was stopped
	 */
	public boolean wasStopped() {
		synchronized(statusLock) {
			return (started && !active);
		}
	}

	/**
	 * Should be called internally when a new link is added.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 *
	 * @param link new link
	 */
	protected void linkAdded(VLink link) {
	}

	/**
	 * Should be called internally when a link is closed.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 *
	 * @param link closed link
	 */
	protected void linkClosed(VLink link) {

	}

	/**
	 * Internal call to register a new link with the service object.
	 *
	 * @param link new link
	 */
	protected void addLink(VLink link) {
		this.addLink(link, false);
	}

	/**
	 * Internal call to register a new link with the service object.
	 *
	 * @param link new link
	 * @param safe false unless known to have link thread separation
	 */
	protected void addLink(VLink link, boolean safe) {
		if (!safe) {
			class Job implements Runnable {
				VLink link;
				public Job(VLink link) {
					this.link = link;
				}
				public void run() {
					addLink(link, true);
				}
			}
			this.schedule(new Job(link));
			return;
		}
		synchronized(this) {
			links.add(link);
			class LinkMonitor implements VSimpleBusListener<VLink.Status> {
				VLink link;
				public LinkMonitor(VLink link) {
					this.link = link;
				}
				@Override
				public void busPush(Status obj) {
					checkIfLinkInactive(link);
				}
			}
			linkMonitors.put(link, new LinkMonitor(link));
			this.linkAdded(link);
		}
	}

	/**
	 * Internal call to monitor link status changes and process stopped links.
	 *
	 * @param link link to check
	 */
	protected void checkIfLinkInactive(VLink link) {
		this.checkIfLinkInactive(link, false);
	}

	/**
	 * Internal call to monitor link status changes and process stopped links.
	 *
	 * @param link link to check
	 * @param safe false unless known to have link thread separation
	 */
	protected void checkIfLinkInactive(VLink link, boolean safe) {
		if (!safe) {
			class Job implements Runnable {
				VLink link;
				public Job(VLink link) {
					this.link = link;
				}
				public void run() {
					checkIfLinkInactive(link, true);
				}
			}
			this.schedule(new Job(link));
			return;
		}

		// Must be done outside synchronized block to avoid possible deadlocks
		boolean link_closed = link.isClosed();

		synchronized(this) {
			if (links.contains(link) && link_closed) {
				links.remove(link);
				linkMonitors.remove(link);
				this.linkClosed(link);
				if (!active && links.isEmpty())
					this.stopThreads();
			}
		}
	}

	/**
	 * Must be called internally when service is activated.
	 */
	protected void activate() {
		synchronized(statusLock) {
			started = true;
			active = true;
			statusLock.notifyAll();
		}
	}

	/**
	 * Schedule a job with a service job processing subsystem.
	 *
	 * @param job job to schedule
	 */
	protected abstract void schedule(Runnable job);

	/**
	 * Stop service from listening on new connections.
	 */
	protected abstract void stopListener();

	/**
	 * Stop all running service threads.
	 */
	protected abstract void stopThreads();

}
