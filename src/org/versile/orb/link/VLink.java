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

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.versile.Versile;
import org.versile.common.call.VCall;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.call.VCallTimeout;
import org.versile.common.processor.VProcessor;
import org.versile.common.processor.VProcessorException;
import org.versile.common.util.VExceptionProxy;
import org.versile.common.util.VLinearIDProvider;
import org.versile.common.util.VSimpleBus;
import org.versile.common.util.VSimpleBusListener;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VObjectIOContext;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VReference;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTaggedParser;
import org.versile.orb.entity.VTuple;


/**
 * Versile Object Link which provides remote object interaction capability.
 *
 * <p>Global license configuration must be set before a link can be constructed.
 * See {@link org.versile.Versile} for details.<p>
 */
public abstract class VLink extends VObjectIOContext {

	/**
	 * Link status.
	 */
	public enum Status {HANDSHAKING, RUNNING, CLOSING, CLOSED};

	// Length of list of receive keep-alive time intervals (must be odd)
	static int KAP_DEQUE_LEN = 5;

	/**
	 * Link's processor for handling remote calls on link.
	 */
	protected VProcessor processor;
	/**
	 * If true lazy-create processor if none provided during link creation.
	 */
	protected boolean lazyProcessor;

	/**
	 * Link configuration parameters.
	 */
	protected VLinkConfig config;

	/**
	 * Remote calls submitted to link peer.
	 */
	protected Map<Number, WeakReference<VCall<Object>>> remoteCalls;

	/**
	 * Object for synchronizing access to remote calls.
	 */
	protected Lock remoteCallsLock;
	VLinearIDProvider msg_id_provider;
	long next_peer_msg_id = 1L;

	boolean _entity_lazy = true;
	boolean _native_lazy = true;
	VTaggedParser parser;

	VObject _local_gw;
	Lock _local_gw_lock;
	WeakReference<VObject> _weak_local_gw;

	// Lock access to _peer_gw by locking _status_lock
	VReference _peer_gw = null;
	WeakReference<VReference> _weak_peer_gw = null;
	protected LinkedList<VCall<VProxy>> _peer_gw_calls;

	boolean performedAuthorize = false;
	boolean _was_authorized = false;

	/**
	 * Lock for synchronizing access to link state-related fields.
	 */
	protected Lock statusLock;

	/**
	 * Bus for passing link status updates.
	 */
	protected VSimpleBus<Status> statusBus;

	/**
	 * True unless link closed or aborted.
	 */
	protected boolean active = true;
	/**
	 * True if waiting to initiate post-hello VOL protocol handshake.
	 */
	protected boolean pendingProtocolHandshake = true;
	/**
	 * True if received VOL hello message from peer.
	 */
	protected boolean gotPeerHello = false;
	/**
	 * True if link is in the process of closing.
	 */
	protected boolean closing = false;
	/**
	 * Handshake object received from peer.
	 */
	protected VLinkHandshake handshakeObj = null;
	/**
	 * True if link handshake was completed.
	 */
	protected boolean handshakeDone = false;
	/**
	 * Timer for forced timeout for closing link (milliseconds).
	 */
	protected long forceTimeout = -1;     // HARDCODED
	/**
	 * If true purge link calls when closing.
	 */
	protected boolean purgeCalls = false; // HARDCODED

	/**
	 * Number of ongoing (unresolved) calls submitted by peer.
	 */
	protected long ongoingCalls = 0L;
	/**
	 * Lock object for synchronization of oncoing calls counter.
	 */
	protected Lock ongoingCallsLock;

	/**
	 * Call context for the link.
	 */
	protected VLinkCallContext context;

	/**
	 * Keep-alive send period (msec), -1 if none set.
	 */
	protected long keepAliveSend = -1;

	/**
	 * Last time a keep-alive message was sent
	 */
	Date keepAliveSendTime = null;

	/**
	 * Negotiated keep-alive receive period (msec), -1 if none set.
	 */
	protected long keepAliveRecv = -1;

	/**
	 * Keep-alive expiry period.
	 */
	protected long keepAliveExpire = -1;

	/**
	 * Keep-alive spam period.
	 */
	protected long keepAliveSpam = -1;

	/**
	 * Last time a VOL protocol message was received
	 */
	protected Date keepAliveRecvTime = null;

	/**
	 * Last time a VOL keep-alive protocol message was received.
	 */
	protected Date keepAliveKaRecvTime = null;

	/**
	 * The last time intervals between received keep-alive protocol messages.
	 */
	protected LinkedList<Long> keepAliveRecvIntervals;

	/**
	 * Copyleft information for local side of link.
	 *
	 * <p>Set from global {@link org.versile.Versile} configuration.</p>
	 */
	protected Versile.CopyleftInfo copyleft;

	/**
	 * Copyleft information for peer side of link.
	 *
	 * <p>Set during link handshake; initially null.</p>
	 */
	protected Versile.CopyleftInfo peerCopyleft;

	/**
	 * Set up with a dummy local gateway, lazy link processor creation and no parser.
	 *
	 * @throws VLinkException global copyleft information not configured
	 */
	public VLink() throws VLinkException {
		this.construct(null, null, null);
	}

	/**
	 * Set up with a dummy local gateway and lazy link processor creation.
	 *
	 * @param config link configuration data (or null)
	 * @throws VLinkException global copyleft information not configured
	 */
	public VLink(VLinkConfig config) throws VLinkException {
		this.construct(null, null, config);
	}

	/**
	 * Set up with provided parameters.
	 *
	 * @param gateway local gateway object for the link
	 * @param processor link task processor (if null lazy-create processor)
	 * @param config link configuration data (or null)
	 * @throws VLinkException global copyleft information not configured
	 */
	public VLink(VObject gateway, VProcessor processor, VLinkConfig config)
		throws VLinkException {
		this.construct(gateway, processor, config);
	}

	private void construct(VObject gateway, VProcessor processor, VLinkConfig config)
		throws VLinkException {
		copyleft = Versile.copyleft();
		if (copyleft.getCopyleft() == null)
			throw new VLinkException("Global copyleft data on org.versile.Versile not configured.");
		peerCopyleft = null;

		if (gateway == null)
			gateway = new VObject();

		lazyProcessor = false;
		if (processor == null) {
			this.processor = new VProcessor();
			lazyProcessor = true;
		}
		else
			this.processor = processor;

		if (config == null)
			config = new VLinkConfig();
		this.config = config;

		remoteCalls = new Hashtable<Number, WeakReference<VCall<Object>>>();
		remoteCallsLock = new ReentrantLock();
		msg_id_provider = new VLinearIDProvider();

		this.parser = config.getParser();

		_local_gw = gateway;
		_local_gw_lock = new ReentrantLock();
		_weak_local_gw = new WeakReference<VObject>(gateway);
		_peer_gw_calls = new LinkedList<VCall<VProxy>>();
		statusLock = new ReentrantLock();
		statusBus = new VSimpleBus<Status>();
		ongoingCallsLock = new ReentrantLock();
		context = new VLinkCallContext(this);

		keepAliveRecvIntervals = new LinkedList<Long>();
	}

	/**
	 * Get a reference to the remote gateway received from peer (blocking).
	 *
	 * <p>The link will only hold a weak reference to the gateway after the first
	 * call to this method, or if the link was configured not to hold a full
	 * reference. If a peer gateway was received but the {@link VLink} no longer has
	 * a reference, null is returned.</p>
	 *
	 * @return peer gateway (or null)
	 * @throws VLinkException link error condition
	 */
	public VProxy peerGateway()
		throws VLinkException {
		try {
			return this.peerGateway(-1, 0);
		} catch (VCallTimeout e) {
			// Should never happen
			throw new RuntimeException();
		}
	}

	/**
	 * Get a reference to the remote gateway received from peer.
	 *
	 * Similar to {@link #peerGateway()} however throws a timeout if a remote gateway
	 * cannot be resolved before the provided timeout.
	 *
	 * @param timeout timeout in milliseconds
	 * @return peer gateway (or null)
	 * @throws VLinkException link error condition
	 * @throws VCallTimeout timeout exceeded
	 */
	public VProxy peerGateway(long timeout)
		throws VLinkException, VCallTimeout {
		return this.peerGateway(timeout, 0);
	}

	/**
	 * Get a reference to the remote gateway received from peer.
	 *
	 * Similar to {@link #peerGateway()} however throws a timeout if a remote gateway
	 * cannot be resolved before the provided timeout.
	 *
	 * @param timeout timeout in milliseconds
	 * @param ntimeout additional timeout on nanoseconds
	 * @return peer gateway (or null)
	 * @throws VLinkException link error condition
	 * @throws VCallTimeout timeout exceeded
	 */
	public VProxy peerGateway(long timeout, int ntimeout)
		throws VLinkException, VCallTimeout {
		if (!active)
			throw new VLinkException("Link is inactive");
		VReference gw = null;
		synchronized(statusLock) {
			gw = _peer_gw;
			if (gw == null && _weak_peer_gw != null)
				gw = _weak_peer_gw.get();
			if (gw == null && timeout != 0)  {
				long start_time = 0L;
				long end_time = 0L;
				if (timeout >= 0) {
					start_time = System.nanoTime();
					end_time = start_time + 1000000L*timeout + ntimeout;
				}
				while (true) {
					if (!active)
						throw new VLinkException("Link was closed");
					gw = _peer_gw;
					if (gw == null && _weak_peer_gw != null)
						gw = _weak_peer_gw.get();
					if (gw != null)
						break;
					try {
						if (timeout < 0) {
							statusLock.wait();
						}
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
		if (gw == null)
			throw new VLinkException("Peer gateway no longer referenced");
		// Make sure no non-weak reference retained, the return
		_peer_gw = null;
		return gw._v_proxy();
	}

	/**
	 * Returns gateway object received from peer (asynchronous).
	 *
	 * Similar to {@link #peerGateway()} but provides the gateway as an asynchronous
	 * call result.
	 *
	 * @return reference to call result
	 */
	public VCall<VProxy> nowaitPeerGateway() {
		VCall<VProxy> result = new VCall<VProxy>();
		synchronized(statusLock) {
			if (!active || closing) {
				result.silentPushException(new VLinkException("Link is inactive or closing"));
			}
			else if (_weak_peer_gw != null) {
				VReference gw = _weak_peer_gw.get();
				if (gw != null)
					result.silentPushResult(gw._v_proxy());
				else
					result.silentPushException(new VLinkException("Gateway no longer referenced"));
			}
			else {
				_peer_gw_calls.addLast(result);
			}
		}
		return result;
	}

	/**
	 * Shut down the link.
	 *
	 * <p>If the link's processor was lazy-created, it is shut down when the link
	 * is terminated.</p>
	 *
	 * @param force if true force link shutdown
	 */
	public void shutdown(boolean force) {
		this.shutdown(force, forceTimeout, purgeCalls);
	}

	/**
	 * Shut down the link.
	 *
	 * <p>If the link's processor was lazy-created, it is shut down when the link
	 * is terminated.</p>
	 *
	 * @param force if true force link shutdown
	 * @param timeout timeout in milliseconds before a force-shutdown
	 */
	public void shutdown(boolean force, long timeout) {
		this.shutdown(force, timeout, purgeCalls);
	}

	/**
	 * Shut down the link.
	 *
	 * <p>A normal shutdown implies that the link (a) no longer accepts inbound messages from
	 * link peer, (b) waits for pending processor tasks to complete, (c) waits for all outbound
	 * messages to link peer to be sent, and (d) shuts down when no tasks or output remain.</p>
     *
     * <p>If 'force' is true, then the link is immediately shut down, including
     * stopping all communication with link peer and cancelling all pending
     * calls registered on the link's processor.</p>
     *
	 * <p>If the link's processor was lazy-created, it is shut down when the link
	 * is terminated.</p>
	 *
	 * @param force if true perform a force shutdown
	 * @param timeout milliseconds before force-shutdown (or negative if no timeout)
	 * @param purge if true purge pending method calls
	 */
	public abstract void shutdown(boolean force, long timeout, boolean purge);

	/**
	 * Get the link's processor.
	 *
	 * @return link processor
	 */
	public VProcessor getProcessor() {
		return processor;
	}

	/**
	 * Checks if link was closed.
	 *
	 * @return true if closed
	 */
	public boolean isClosed() {
		synchronized(statusLock) {
			return !active;
		}
	}

	/**
	 * Get current link status.
	 *
	 * @return link status
	 */
	public Status getStatus() {
		synchronized(statusLock) {
			if (active) {
				if (closing)
					return Status.CLOSING;
				else if (handshakeDone)
					return Status.RUNNING;
				else
					return Status.HANDSHAKING;
			}
			else
				return Status.CLOSED;
		}
	}

	/**
	 * Get link handshake status.
	 *
	 * @return true if link handshake is completed
	 */
	public boolean isHandshakeDone() {
		return handshakeDone;
	}

	/**
	 * Get status bus for link status updates.
	 *
	 * @return status bus
	 */
	public VSimpleBus<Status> getStatusBus() {
		return statusBus;
	}

	/**
	 * Registers a listener with the status bus.
	 *
	 * <p>Registers listener and triggers sending current status to the status bus.</p>
	 *
	 * @param listener listener
	 * @return listener ID on the status bus
	 */
	public long registerStatusListener(VSimpleBusListener<Status> listener) {
		return this.registerStatusListener(listener, true);
	}

	/**
	 * Registers a listener with the status bus.
	 *
	 * @param listener listener
	 * @param push if True push current status onto the bus
	 * @return listener ID on the status bus
	 */
	public long registerStatusListener(VSimpleBusListener<Status> listener, boolean push) {
		synchronized(statusLock) {
			long _id = statusBus.register(listener);
			Status _status = this.getStatus();
			if (push) {
				statusBus.push(_status);
			}
			return _id;
		}
	}

	/**
	 * Get the link's context object.
	 *
	 * @return link context object
	 */
	public VLinkCallContext getContext() {
		return context;
	}

	/**
	 * Log a low-level message to the link's logger.
	 *
	 * <p>Logs with level "FINEST". Ignored if no logger is set up
	 * on the link.</p>
	 *
	 * @param msg log message
	 */
	public void log(String msg) {
		this.log(Level.FINEST, msg);
	}

	/**
	 * Log to a logger for the link.
	 *
	 * <p>Ignored if no logger is set up on the link.</p>
	 *
	 * @param level logging level
	 * @param msg log message
	 */
	public abstract void log(Level level, String msg);

	/**
	 * Get local side copyleft information associated with the link.
	 *
	 * @return copyleft information
	 */
	public Versile.CopyleftInfo getCopyleftInfo() {
		return copyleft;
	}

	/**
	 * Get peer side copyleft information associated with the link.
	 *
	 * @return copyleft information, or null if not yet set during handshake.
	 */
	public Versile.CopyleftInfo getPeerCopyleftInfo() {
		return peerCopyleft;
	}

	/**
	 * Get the owning link of a proxied {@link VLinkReference}.
	 *
	 * @param proxy proxy for the reference
	 * @return link for the namespace of the reference
	 * @throws VLinkException proxy does not hold a {@link VLinkReference}
	 */
	public static VLink forProxy(VProxy proxy)
		throws VLinkException {
		try {
			return ((VLinkReference)proxy.get())._v_link();
		} catch (Exception e) {
			throw new VLinkException("Not a proxy for a VLinkReference");
		}
	}

	/**
	 * Shuts down owning link of a proxied {@link VLinkReference}.
	 *
	 * <p>If proxy holds a proxy to a link object reference with an associated
	 * link, {@link #shutdown(boolean)} is called on the link and true is
	 * returned; otherwise no link could be identified and false is returned.</p>
	 *
	 * @param proxy proxy whose associated link to shut down
	 * @return true if shutdown was sent, otherwise false
	 */
	public static boolean shutdownForProxy(VProxy proxy) {
		return VLink.shutdownForProxy(proxy, false);
	}

	/**
	 * Shuts down owning link of a proxied {@link VLinkReference}.
	 *
	 * <p>If proxy holds a proxy to a link object reference with an associated
	 * link, {@link #shutdown(boolean)} is called on the link and true is
	 * returned; otherwise no link could be identified and false is returned.</p>
	 *
	 * @param proxy proxy whose associated link to shut down
	 * @param force if true force-shutdown link
	 * @return true if shutdown was sent, otherwise false
	 */
	public static boolean shutdownForProxy(VProxy proxy, boolean force) {
		VLink link;
		try {
			link = VLink.forProxy(proxy);
		} catch (VLinkException e) {
			return false;
		}
		if (link != null) {
			link.shutdown(force);
			return true;
		}
		else
			return false;
	}

	/**
	 * Send a Versile ORB Link protocol message which does not include a message ID.
	 *
	 * @param msg_code message code
	 * @param message message payload
	 * @throws VLinkException link error condition
	 */
	protected void sendMessage(int msg_code, VEntity message)
			throws VLinkException {
		this.sendMessage(msg_code, message, -1);
	}

	/**
	 * Send a Versile ORB Link protocol message.
	 *
	 * <p>Callers providing a message ID should synchronize on msg_id_provider for the duration between
	 * generating a call ID and calling this method.</p>
	 *
	 * @param msg_code message code
	 * @param message message payload
	 * @param message_id (if negative no message ID applies)
	 * @throws VLinkException link error condition
	 */
	protected Number sendMessage(int msg_code, VEntity message, Number message_id)
			throws VLinkException {
		synchronized(msg_id_provider) {
			if (message_id instanceof Integer && (Integer)message_id < 0)
				message_id = msg_id_provider.getID();
			else if (message_id instanceof Long && (Long)message_id < 0L)
				message_id = msg_id_provider.getID();
			else if (message_id instanceof BigInteger && ((BigInteger)message_id).compareTo(BigInteger.ZERO) < 0)
				message_id = msg_id_provider.getID();
			this.sendEntity(new VTuple(new VEntity[] {new VInteger(message_id), new VInteger(msg_code), message}));
		}

		// If send keep-alive is enabled, must update timestamp
		if (keepAliveSend > 0)
			this.keepAliveSendTime = new Date();

		return message_id;
	}

	/**
	 * Send a Versile ORB Link protocol handshake message.
	 *
	 * @param message handshake message
	 * @throws VLinkException link error condtition
	 */
	protected void sendHandshakeMessage(VEntity message)
			throws VLinkException {
		this.sendEntity(message);
	}

	/**
	 * Sends a Versile ORB Link protocol message to link peer.
	 *
	 * <p>Abstract method which must be implemented by derived classes for the particular
	 * {@link org.versile.orb.entity.VEntity} passing I/O subsystem for a link implementation.</p>
	 *
	 * @param entity entity to send
	 * @throws VLinkException link error condition
	 */
	protected abstract void sendEntity(VEntity entity)
		throws VLinkException;

	/**
	 * Initiates a remote method call to the link peer.
	 *
	 * @param obj reference to target remote object
	 * @param args remote method call arguments
	 * @param type method call type
	 * @return reference to method call (or null if call type is oneway)
	 */
	synchronized VCall<Object> sendRemoteCall(VLinkReference obj, List<Object> args, VObject._v_CallType type)
		throws VLinkException {

		if (obj._v_link() != this)
			throw new VLinkException("Not a remote object in this link context");

		// Resolve message code
		int code = 0;
		boolean send_msg_id = true;
		if (type == VObject._v_CallType.NORMAL) {
			code = 0x01;
		}
		else if (type == VObject._v_CallType.NORESULT) {
			code = 0x02;
		}
		else {
			code = 0x03;
			send_msg_id = false;
		}

		// Package message data
		LinkedList<VEntity> msg_data = new LinkedList<VEntity>();
		msg_data.addLast(obj);
		LinkedList<VEntity> msg_args = new LinkedList<VEntity>();
		for (Object arg: args) {
			try {
			if (_entity_lazy)
				msg_args.addLast(lazyEntity(arg));
			else
				msg_args.addLast((VEntity)arg);
			} catch (Exception e) {
				throw new VLinkException("Cannot send arguments as VEntity types");
			}
		}
		msg_data.add(new VTuple(msg_args));
		VTuple message = new VTuple(msg_data);

		if (send_msg_id) {
			VCall<Object> call;
			synchronized(msg_id_provider) {
				long msg_id = msg_id_provider.getID();
				call = this.registerRemoteCall(msg_id);
				this.sendMessage(code, message, msg_id);
			}
			return call;
		}
		else {
			this.sendMessage(code,  message);
			return null;
		}
	}

	@Override
	public synchronized void referenceDeref(Number peer_id) {
		PeerObject _peer = peer_obj.get(peer_id);
		if (_peer == null)
			return;

		try {
			LinkedList<VEntity> msg_data = new LinkedList<VEntity>();
			msg_data.addLast(new VInteger(peer_id));
			msg_data.addLast(new VInteger(_peer.recv_count));
			this.sendMessage(0x07, new VTuple(msg_data));
		}
		catch (VLinkException e) {
			this.shutdown(true, 0, true);
		}
	}

	/**
	 * Handler for VOL protocol handshake message.
	 *
	 * @param entity handshake message
	 * @throws VLinkException link error
	 */
	protected void recvHandshake(VEntity entity) throws VLinkException {
		if (!gotPeerHello) {
			if (!(entity instanceof VTuple))
				throw new VLinkException("Invalid protocol hello message format");
			VTuple tuple = (VTuple)entity;
			if (tuple.length() != 3)
				throw new VLinkException("Invalid protocol hello message format");
			VBytes protocol = null;
			VTuple _copy = null;
			VTuple version = null;
			try {
				protocol = (VBytes)(tuple.getValue()[0]);
				_copy = (VTuple)(tuple.getValue()[1]);
				version = (VTuple)(tuple.getValue()[2]);
			} catch (Exception e) {
				throw new VLinkException("Invalid hello message");
			}

			// Parse protocol name data
			try {
				byte[] msg = "VOL_DRAFT".getBytes("ASCII");
				if (!Arrays.equals(protocol.getValue(), msg))
					throw new VLinkException("Invalid protocol name");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException();
			}

			// Parse copyleft information
			if (_copy.length() != 3)
				throw new VLinkException("Invalid protocol hello message copyleft structure");
			Boolean _copyleft;
			String _copyleftLic;
			String _copyleftUrl;
			try {
				_copyleft = ((VBoolean)(_copy.getValue()[0]))._v_native();
				if (_copyleft) {
					_copyleftLic = ((VString)(_copy.getValue()[1]))._v_native();
					_copyleftUrl = ((VString)(_copy.getValue()[2]))._v_native();
				}
				else {
					if (!(_copy.getValue()[1] instanceof VNone && _copy.getValue()[2] instanceof VNone))
						throw new VLinkException("Invalid protocol hello message copyleft structure");
					_copyleftLic = null;
					_copyleftUrl = null;
				}
			} catch (Exception e) {
				throw new VLinkException("Invalid protocol hello message copyleft structure");
			}
			if(_copyleft) {
				String _clow = _copyleftUrl.toLowerCase();
				if (!(_clow.startsWith("http://") || _clow.startsWith("https://") || _clow.startsWith("vop://")))
					throw new VLinkException("Invalid protocol hello message copyleft URL");
			}
			peerCopyleft = new Versile.CopyleftInfo(_copyleft, _copyleftLic, _copyleftUrl);

			// Parse version number data
			if (version.length() != 2 || !(version.getValue()[0] instanceof VInteger)
					|| !(version.getValue()[1] instanceof VInteger))
				throw new VLinkException("Invalid version number");
			Number num = ((VInteger)version.getValue()[0]).getValue();
			if ((num instanceof Integer && (Integer)num != 0)
					|| (num instanceof Long && (Long)num != 0L)
					|| (num instanceof BigInteger && ((BigInteger)num).compareTo(BigInteger.ZERO) != 0))
				throw new VLinkException("Invalid version number");
			num = ((VInteger)version.getValue()[1]).getValue();
			if ((num instanceof Integer && (Integer)num != 8)
					|| (num instanceof Long && (Long)num != 8L)
					|| (num instanceof BigInteger && ((BigInteger)num).compareTo(BigInteger.valueOf(8)) != 0))
				throw new VLinkException("Invalid version number");

			gotPeerHello = true;

			// Authorize link before proceeding
			if (!this.authorizeLink()) {
				this.shutdown(true, 0, true);
				return;
			}

			handshakeObj = this.createHandshakeObject();
			this.sendHandshakeMessage(handshakeObj);
		}
		else {
			VReference peer_handshake_obj = null;
			try {
				peer_handshake_obj = (VReference)entity;
			} catch (Exception e) {
				throw new VLinkException("Invalid peer handshake message");
			}

			synchronized(statusLock) {
				pendingProtocolHandshake = false;
				statusBus.push(this.getStatus());
			}

			VLinkHandshake handshake_obj = handshakeObj;
			handshakeObj = null;
			handshake_obj.recvPeer(peer_handshake_obj);
		}
	}

	/**
	 * Handler for VOL protocol messages after completing initial hello handshake.
	 *
	 * @param message protocol message
	 * @throws VLinkException link error
	 */
	protected void recvMessage(VEntity message) throws VLinkException {
		if (!active) {
			this.shutdown(false);
			throw new VLinkException("Link is inactive");
		}

		VTuple msg = null;
		try {
			msg = (VTuple)message;
		} catch (Exception e) {
			throw new VLinkException("Invalid message format");
		}
		if (msg.length() != 3)
			throw new VLinkException("Invalid message format");

		VInteger _msg_id = null;
		Number msg_id = null;
		int msg_code = 0;
		VEntity msg_data = null;
		try {
			_msg_id = (VInteger)msg.getValue()[0];
			msg_id = _msg_id.getValue();
			msg_code = ((VInteger)msg.getValue()[1]).getValue().intValue();
			msg_data = msg.getValue()[2];
		} catch (Exception e) {
			throw new VLinkException("Invalid message format");
		}

		// Validate correct message ID sent by peer
		if (!_msg_id.equals(next_peer_msg_id))
			throw new VLinkException("Invalid message code");
		next_peer_msg_id += 1;

		// If keep-alive is enabled, register receive time of message
		if (keepAliveRecv > 0) {
			keepAliveRecvTime = new Date();
		}

		if (msg_code == 0x01) {
			this.handleMethodCall(msg_id, msg_data);
		}
		else if (msg_code == 0x02) {
			this.handleMethodCallVoidResult(msg_id, msg_data);
		}
		else if (msg_code == 0x03) {
			this.handleMethodCallNoReturn(msg_id, msg_data);
		}
		else if (msg_code == 0x04) {
			this.handleCallResult(msg_id, msg_data);
		}
		else if (msg_code == 0x05) {
			this.handleCallException(msg_id, msg_data);
		}
		else if (msg_code == 0x06) {
			this.handleCallError(msg_id, msg_data);
		}
		else if (msg_code == 0x07) {
			this.handleNotifyDeref(msg_id, msg_data);
		}
		else if (msg_code == 0x08) {
			this.handleConfirmDeref(msg_id, msg_data);
		}
		else if (msg_code == 0x09) {
			this.handleKeepAlive(msg_id, msg_data);
		}
		else
			throw new VLinkException("Invalid message code");
	}

	void handleMethodCall(Number msg_id, VEntity msg_data)
			throws VLinkException {
		this._handleMethodCall(msg_id, msg_data, false, false);
	}

	void handleMethodCallVoidResult(Number msg_id, VEntity msg_data)
		throws VLinkException {
		this._handleMethodCall(msg_id, msg_data, true, false);
	}

	void handleMethodCallNoReturn(Number msg_id, VEntity msg_data)
		throws VLinkException {
		this._handleMethodCall(msg_id, msg_data, true, true);
	}

	void handleCallResult(Number msg_id, VEntity msg_data)
		throws VLinkException {
		VTuple msg = null;
		try {
			msg = (VTuple)msg_data;
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}
		if (msg.length() != 2)
			throw new VLinkException("Invalid call result message");
		Number call_id = null;
		Object result = null;
		try {
			call_id = ((VInteger)(msg.getValue()[0])).getValue();
			result = msg.getValue()[1];
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}

		if (_native_lazy)
			result = lazyNative(result);

		WeakReference<VCall<Object>> w_call = null;
		synchronized(remoteCallsLock) {
			call_id = VInteger.normalize(call_id);
			w_call = remoteCalls.get(call_id);
		}
		if (w_call != null) {
			VCall<Object> call = w_call.get();
			if (call != null)
				call.silentPushResult(result);
		}
	}

	void handleCallException(Number msg_id, VEntity msg_data)
			throws VLinkException {
		VTuple msg = null;
		try {
			msg = (VTuple)msg_data;
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}
		if (msg.length() != 2)
			throw new VLinkException("Invalid call result message");
		Number call_id = null;
		VEntity result = null;
		try {
			call_id = ((VInteger)(msg.getValue()[0])).getValue();
			result = msg.getValue()[1];
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}

		Object conv = result;
		if (_native_lazy)
			conv = lazyNative(conv);

		Exception exc = null;
		if (conv instanceof Exception)
			exc = (Exception)conv;
		else
			exc = new VExceptionProxy(conv);

		WeakReference<VCall<Object>> w_call = null;
		synchronized(remoteCallsLock) {
			call_id = VInteger.normalize(call_id);
			w_call = remoteCalls.get(call_id);
		}
		if (w_call != null) {
			VCall<Object> call = w_call.get();
			if (call != null)
				call.silentPushException(exc);
		}
	}

	void handleCallError(Number msg_id, VEntity msg_data)
			throws VLinkException {
		Number call_id = null;
		try {
			call_id = ((VInteger)msg_data).getValue();
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}

		WeakReference<VCall<Object>> w_call = null;
		synchronized(remoteCallsLock) {
			call_id = VInteger.normalize(call_id);
			w_call = remoteCalls.get(call_id);
		}
		if (w_call != null) {
			VCall<Object> call = w_call.get();
			if (call != null)
				call.silentPushException(new VCallError());
		}
	}

	void handleNotifyDeref(Number msg_id, VEntity msg_data)
		throws VLinkException {
		VTuple msg = null;
		try {
			msg = (VTuple)msg_data;
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}
		if (msg.length() != 2)
			throw new VLinkException("Invalid call result message");
		Number peer_id = null;
		Number peer_recv_count = null;
		try {
			peer_id = ((VInteger)msg.getValue()[0]).getValue();
			peer_recv_count = ((VInteger)msg.getValue()[1]).getValue();
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}

		boolean performed_deref = false;
		boolean no_ref_left = false;
		peer_id = VInteger.normalize(peer_id);
		synchronized(local_lock) {
			LocalObject local = local_obj.get(peer_id);
			if (local != null) {
				VObject obj = local.obj;
				long send_count = local.send_count;
				if (send_count == peer_recv_count.longValue()) {
					performed_deref = true;
					local_obj.remove(peer_id);
					local_p_ids.remove(obj);
					if (local_obj.isEmpty() && peer_obj.isEmpty())
						no_ref_left = true;
				}
			}
		}

		if (performed_deref) {
			try {
				this.sendMessage(0x08, new VInteger(peer_id));
			} catch (VLinkException e) {
				this.shutdown(true, 0, true);
			}
		if (no_ref_left)
			this.shutdown(false);
		}
	}

	void handleConfirmDeref(Number msg_id, VEntity msg_data)
		throws VLinkException {
		Number peer_id = null;
		try {
			peer_id = ((VInteger)msg_data).getValue();
		} catch (Exception e) {
			throw new VLinkException("Invalid call result message");
		}

		peer_id = VInteger.normalize(peer_id);
		boolean no_ref_left = false;
		synchronized(peer_lock) {
			peer_obj.remove(peer_id);
			no_ref_left = (local_obj.isEmpty() && peer_obj.isEmpty());
		}
		if (no_ref_left)
			this.shutdown(false);
	}

	void handleKeepAlive(Number msg_id, VEntity msg_data)
			throws VLinkException {
		if (msg_data != null && !(msg_data instanceof VNone))
			throw new VLinkException("Malformed protocol keep-alive message");

		Date lastKaMsg = keepAliveKaRecvTime;
		keepAliveKaRecvTime = new Date();
		if (lastKaMsg != null) {
			long elapsed = keepAliveKaRecvTime.getTime() - lastKaMsg.getTime();
			keepAliveRecvIntervals.addLast(elapsed);
			if (keepAliveRecvIntervals.size() > KAP_DEQUE_LEN)
				keepAliveRecvIntervals.removeFirst();
		}

		if (keepAliveRecvIntervals.size() == KAP_DEQUE_LEN) {
			Long[] _intervals = keepAliveRecvIntervals.toArray(new Long[0]);
			Arrays.sort(_intervals);
			long _median_elapsed = _intervals[_intervals.length/2];
			if (_median_elapsed < this.keepAliveSpam) {
				this.log("VLink: keep-alive spam detected, terminating");
				this.shutdown(true);
			}
		}
	}

	void _handleMethodCall(Number msg_id, VEntity msg_data, boolean nores, boolean noreturn)
		throws VLinkException {
		VTuple msg = null;
		try {
			msg = (VTuple)msg_data;
		} catch (Exception e) {
			throw new VLinkException("Invalid method call message");
		}
		if (msg.length() != 2)
			throw new VLinkException("Invalid method call message");
		VObject obj = null;
		VTuple t_args = null;
		try {
			obj = (VObject)(msg.getValue()[0]);
			t_args = (VTuple)(msg.getValue()[1]);
		} catch (Exception e) {
			throw new VLinkException("Invalid method call message");
		}

		if ((obj instanceof VReference) && ((VReference)obj)._v_context() == this)
			throw new VLinkException("Cannot remotely call method on peer's local object");

		Object[] args = null;
		if (_native_lazy) {
			args = (Object[])(lazyNative(t_args));
		}
		else {
			args = t_args.getValue();
		}

		class Func implements Runnable {
			Number call_id;
			VObject obj;
			Object[] args;
			boolean nores;
			boolean noreturn;
			public Func(Number call_id, VObject obj, Object[] args, boolean nores, boolean noreturn) {
				this.call_id = call_id;
				this.obj = obj;
				this.args = args;
				this.nores = nores;
				this.noreturn = noreturn;
			}
			@Override
			public void run() {
				registerCallStart();
				try {
					Object result = null;
					try {
						VObject._v_CallType call_type;
						if (nores)
							call_type = VObject._v_CallType.NORESULT;
						else
							call_type = VObject._v_CallType.NORMAL;
						LinkedList<Object> arg_l = new LinkedList<Object>();
						for (Object arg: args)
							arg_l.addLast(arg);
						result = obj._v_call(arg_l, call_type, context);
					} catch (Exception e) {
						if (!noreturn) {
							try {
								if (e instanceof VCallError) {
									// Perform dbg-level logging of generated VCallError
									String msg = "Sending VCallError [due to] " + e;
									Object[] trace = e.getStackTrace();
									if (trace.length > 0)
										msg += " [triggered by] " + trace[0];
									log(msg);

									_callError(call_id);
								}
								else
									_callException(call_id, e);
							} catch (VLinkException e2) {
								shutdown(true, 0, true);
							}
						}
						else if (e instanceof VCallError) {
							// Perform dbg-level logging of generated VCallError
							String msg = "Sending VCallError [due to] " + e;
							Object[] trace = e.getStackTrace();
							if (trace.length > 0)
								msg += " [triggered by] " + trace[0];
							log(msg);
						}
						return;
					}

					if (result instanceof VCall<?>) {
						class ResultHandler implements VCallResultHandler<Object> {
							Number call_id;
							boolean nores;
							boolean noreturn;
							public ResultHandler(Number call_id, boolean nores, boolean noreturn) {
								this.call_id = call_id;
								this.nores = nores;
								this.noreturn = noreturn;
							}
							@Override
							public void callback(Object result) {
								if (nores)
									result = null;
								if (!noreturn) {
									try {
										_callResult(call_id, result);
									} catch (VLinkException e) {
										shutdown(true, 0, true);
									}
								}
							}
						}
						class ExceptionHandler implements VCallExceptionHandler {
							Number call_id;
							boolean noreturn;
							public ExceptionHandler(Number call_id, boolean noreturn) {
								this.call_id = call_id;
								this.noreturn = noreturn;
							}
							@Override
							public void callback(Exception e) {
								if (!noreturn) {
									try {
										if (e instanceof VCallError)
											_callError(call_id);
										else
											_callException(call_id, e);
									} catch (VLinkException e2) {
										shutdown(true, 0, true);
									}
								}
							}
						}
						@SuppressWarnings("unchecked")
						VCall<Object> a_result = (VCall<Object>)result;
						a_result.addHandlerPair(new ResultHandler(call_id, nores, noreturn),
								new ExceptionHandler(call_id, noreturn));
					}
					else if (noreturn)
						return;
					else {
						if (nores)
							result = null;
						try {
							_callResult(call_id, result);
						} catch (VLinkException e) {
							shutdown(true, 0, true);
						}
					}
				} finally {
					registerCallEnd();
				}
			}
		}
		Func func = new Func(msg_id, obj, args, nores, noreturn);
		try {
			this.getProcessor().submit(func, this);
		} catch (VProcessorException e) {
			throw new VLinkException("Could not submit job to link processor");
		}
	}

	void registerCallStart() {
		synchronized(ongoingCallsLock) {
			ongoingCalls += 1;
		}
	}

	void registerCallEnd() {
		boolean exit = false;
		synchronized(ongoingCallsLock) {
			ongoingCalls -= 1;
			exit = (ongoingCalls == 0 && active && closing);
		}
		if (exit) {
			if (!processor.hasGroupCalls(this)) {
				class Job implements Runnable {
					@Override
					public void run() {
						shutdownCallsCompleted();
					}
				}
				try {
					processor.submit(new Job());
				} catch (VProcessorException e) {
					// SILENT
				}
			}
		}
	}

	/**
	 * Internal callback for pending shutdowns when all pending remote calls are completed.
	 */
	protected abstract void shutdownCallsCompleted();

	void _callResult(Number call_id, Object result)
			throws VLinkException {

		VEntity entity = null;
		if (result == null)
			entity = VNone.get();
		else if (result instanceof VEntity) {
			entity = (VEntity)result;
		}
		else if (_entity_lazy){
			try {
				entity = lazyEntity(result);
			} catch (VEntityError e) {
				throw new VLinkException();
			}
		}

		if (entity != null) {
			VTuple msg = VTuple.fromElements(new VInteger(call_id), entity);
			try {
				this.sendMessage(0x04, msg);
			} catch (VLinkException e2) {
				this.shutdown(true, 0, true);
				throw e2;
			}
		}
		else {
			// Cannot convert send a call error instead
			this._callError(call_id);
		}
	}

	void _callException(Number call_id, Exception e)
			throws VLinkException {
		Object value = e;
		if (e instanceof VExceptionProxy)
			value = ((VExceptionProxy)e).getValue();

		VEntity exception = null;
		if (value instanceof VEntity)
			exception = (VEntity)value;
		else if (_entity_lazy) {
			try {
				value = lazyEntity(value);
			} catch (VEntityError e2) {

				// Perform dbg-level logging of generated VCallError
				String msg = "Sending VCallError [due to] " + e;
				Object[] trace = e.getStackTrace();
				if (trace.length > 0)
					msg += " [triggered by] " + trace[0];
				log(msg);

				exception = null;
			}
		}

		if (exception != null) {
			VTuple msg = VTuple.fromElements(new VInteger(call_id), exception);
			try {
				this.sendMessage(0x05, msg);
			} catch (VLinkException e2) {
				this.shutdown(true, 0, true);
				throw e2;
			}
		}
		else {
			// If cannot pass as an exception, report it as a call error
			this._callError(call_id);
		}
	}

	void _callError(Number call_id)
			throws VLinkException {
		try {
			this.sendMessage(0x06, new VInteger(call_id));
		} catch (VLinkException e) {
			this.shutdown(true, 0, true);
			throw e;
		}
	}

	/**
	 * Called internally to initiate link handshake.
	 */
	protected void initiateHandshake() {
		try {
			this.sendHandshakeMessage(this.createHelloMessage());
		} catch (VLinkException e) {
			// Critical error, shut down
			this.shutdown(true);
		}
	}

	/**
	 * Called internally to create a link protocol hello message for initial handshake.
	 *
	 * @return protocol message
	 */
	protected VEntity createHelloMessage() {
		LinkedList<VEntity> items = new LinkedList<VEntity>();
		try {
			// Add protocol name
			items.addLast(new VBytes("VOL_DRAFT".getBytes("ASCII")));

			// Add copyleft information
			LinkedList<VEntity> _copy = new LinkedList<VEntity>();
			Boolean _copyleft = copyleft.getCopyleft();
			String _lic = copyleft.getLicenseType();
			String _url = copyleft.getCopyleftUrl();
			_copy.addLast(new VBoolean(_copyleft));
			if (_copyleft) {
				_copy.addLast(new VString(_lic));
				_copy.addLast(new VString(_url));
			}
			else {
				_copy.addLast(VNone.get());
				_copy.addLast(VNone.get());
			}
			items.addLast(new VTuple(_copy.toArray(new VEntity[] {})));

			// Add protocol version
			items.addLast(new VTuple(new VEntity[] {new VInteger(0), new VInteger(8)}));
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException();
		}
		return new VTuple(items);
	}

	/**
	 * Called internally to create a handshake object to send to peer during link handshake.
	 *
	 * @return handshake object
	 */
	protected VLinkHandshake createHandshakeObject() {
		return new VLinkHandshake(this);
	}

	/**
	 * Authorize link connection.
	 *
	 * <p>Called internally to check whether authorized to set up link. Called when
	 * peer credentials have been provided or when handshake is about to complete,
	 * whichever comes first.</p>
	 *
	 * <p>Authorization is done by executing the authorizer registered on the link's
	 * config object. If there is no such authorizer, the default behavior is to accept
	 * the connection and return true.</p>
	 *
	 * <p>Authorization is resolved only once, and consecutive calls will always return
	 * the same result as the return value of the first call.</p>
	 *
	 * @return true if link is authorized
	 */
	protected boolean authorizeLink() {
		if (!performedAuthorize) {
			VLinkAuth authorizer = config.getAuthorizer();
			if (authorizer == null)
				_was_authorized = true;
			else
				_was_authorized = authorizer.authorize(this);
			performedAuthorize = true;
		}
		return _was_authorized;
	}

	void handshakeCompleted() {
		handshakeDone = true;

		// Perform handshake callback handling
		VLinkCallback callback = config.getInitCallback();
		if (callback != null)
			try {
				callback.callback(this, true);
			} catch (VLinkException e) {
				this.log("VLink: post-handshake callback error, shutting down link");
				this.shutdown(true, 0, true);
			}
			finally {
				config.setInitCallback(null);
			}
		this.log("VLink: handshake completed");

		// Initiate keep-alive handling
		if (keepAliveSend > 0)
			if (keepAliveSendTime == null)
				keepAliveSendTime = new Date();
			this.scheduleKeepAliveSend(keepAliveSend);
		if (keepAliveRecv > 0) {
			if (keepAliveRecvTime == null)
				keepAliveRecvTime = new Date();
			keepAliveExpire = (long)(keepAliveRecv*config.getKeepAlive().getExpireFactor());
			keepAliveSpam = (long)(keepAliveSpam*config.getKeepAlive().getSpamFactor());
			this.scheduleKeepAliveRecv(keepAliveExpire);
		}

	}

	VObject localGateway()
		throws VLinkException {
		VObject gateway = null;
		synchronized(_local_gw_lock) {
			gateway = _local_gw;
			if (gateway == null && _weak_local_gw != null)
				gateway = _weak_local_gw.get();
		}
		if (gateway == null)
			throw new VLinkException("Local gateway extraction error");
		return gateway;
	}

	void submitPeerGateway(VReference gateway) {
		LinkedList<VCall<VProxy>> pending = new LinkedList<VCall<VProxy>>();
		synchronized(statusLock) {
			_weak_peer_gw = new WeakReference<VReference>(gateway);
			_peer_gw = gateway;
			statusLock.notifyAll();
			while (!_peer_gw_calls.isEmpty())
				pending.addLast(_peer_gw_calls.removeFirst());
		}
		// Notify any pending calls waiting for a peer gateway
		if (!pending.isEmpty()) {
			class Job implements Runnable {
				VProxy gateway;
				LinkedList<VCall<VProxy>> calls;
				public Job(VProxy gateway, LinkedList<VCall<VProxy>> calls) {
					this.gateway = gateway;
					this.calls = calls;
				}
				@Override
				public void run() {
					for (VCall<VProxy> call: calls) {
						call.silentPushResult(gateway);
					}
				}
			}
			try {
				this.getProcessor().submit(new Job(_peer_gw._v_proxy(), pending));
			} catch (VProcessorException e) {
				// SILENT
			}
			// We lose the solid _peer_gw reference if it had a pending requesting job
			_peer_gw = null;
		}

		// If holding link peer is disabled, immediately lose the solid peer gateway reference
		if (!config.getHoldPeer())
			synchronized(statusLock) {
				_peer_gw = null;
		}
	}

	private VCall<Object> registerRemoteCall(Number call_id)
			throws VLinkException {
		call_id = VInteger.normalize(call_id);
		VCall<Object> call = null;
		synchronized(remoteCallsLock) {
			if (remoteCalls.get(call_id) != null)
				throw new VLinkException("Call ID already in use for another remote call");
			call = new VLinkReferenceCall(this, call_id);
			remoteCalls.put(call_id,  new WeakReference<VCall<Object>>(call));
		}
		return call;
	}

	void unregisterRemoteCall(Number call_id) {
		call_id = VInteger.normalize(call_id);
		synchronized(remoteCallsLock) {
			remoteCalls.remove(call_id);
		}
	}

	@Override
	protected VReference createPeerReference(Number peer_id) {
		return new VLinkReference(this, peer_id);
	}

	/**
	 * Called internally when shutdown is completing.
	 */
	protected void finalizeShutdown() {
		// Change status
		synchronized(statusLock) {
			active = false;
			closing = false;
			statusBus.push(this.getStatus());

			// Interrupt anyone waiting for a peer gateway
			statusLock.notifyAll();
		}

		// If link handshake callback is set, notify callback failed
		VLinkCallback callback = config.getInitCallback();
		if (callback != null) {
			try {
				callback.callback(this, false);
			} catch (VLinkException e) {
				// SILENT
			}
			finally {
				config.setInitCallback(null);
			}
		}

		// If the link owns a lazy-created processor, stop the processor
		if (lazyProcessor) {
			processor.removeGroup(this);
			processor.shutdownNow();
		}
	}

	/**
	 * Schedules a keep-alive send check.
	 *
	 * <p>Should schedule keep-alive check using the link implementation's
	 * scheduling subsystem. When triggered should call
	 * {@link #handleKeepAliveSend}.</p>
	 *
	 * @param delay delay in milliseconds
	 */
	protected abstract void scheduleKeepAliveSend(long delay);

	/**
	 * Handles a scheduled keep-alive send check.
	 */
	protected void handleKeepAliveSend() {
		if (!active || closing)
			return;

		Date cur_time = new Date();
		long elapsed = cur_time.getTime() - keepAliveSendTime.getTime();
		long send_delay = keepAliveSend - elapsed;
		if (send_delay <= 0) {
			// Send keep-alive message to peer
			try {
				this.sendMessage(0x09, VNone.get());
			} catch (VLinkException e) {
				// Shut down if sending message fails
				this.log("VLink: _send_msg failed");
				this.shutdown(true);
				return;
			}
			// Schedule new send keep-alive handler
			this.scheduleKeepAliveSend(keepAliveSend);
		}
		else
			this.scheduleKeepAliveSend(send_delay);
	}

	/**
	 * Schedules a keep-alive recv check.
	 *
	 * <p>Should schedule keep-alive check using the link implementation's
	 * scheduling subsystem. When triggered should call
	 * {@link #handleKeepAliveRecv}.</p>
	 *
	 * @param delay delay in milliseconds
	 */
	protected abstract void scheduleKeepAliveRecv(long delay);

	/**
	 * Handles a scheduled keep-alive recv check.
	 */
	protected void handleKeepAliveRecv() {
		if (!active || closing)
			return;

		Date cur_time = new Date();
		long elapsed = cur_time.getTime() - keepAliveRecvTime.getTime();
		long recv_delay = keepAliveExpire - elapsed;
		if (recv_delay <= 0) {
			this.log("VLink: keep-alive check expired, terminating link");
			this.shutdown(true, 0, true);
		}
		else
			this.scheduleKeepAliveRecv(recv_delay);
	}

	VEntity lazyEntity(Object obj)
		throws VEntityError {
		VTaggedParser _parser = null;
		if (this._weak_peer_gw != null)
			_parser = parser;
		return VEntity._v_lazy(obj, _parser);
	}

	Object lazyNative(Object obj) {
		VTaggedParser _parser = null;
		if (this._weak_peer_gw != null)
			_parser = parser;
		return VEntity._v_lazy_native(obj, _parser);
	}
}
