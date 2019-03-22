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

import java.lang.ref.WeakReference;

import org.versile.common.call.VCall;
import org.versile.common.call.VCallCancelled;
import org.versile.common.call.VCallHaveResult;
import org.versile.common.processor.VProcessorException;
import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VException;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VReference;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;


/**
 * Implements a standard Versile Orb Link handshake.
 *
 * <p>Intended primarily for internal use by the Versile Java platform to handle
 * a standard VOL protocol handshake.</p>
 */
public class VLinkHandshake extends VExternal {

	WeakReference<VLink> wlink;
	VProxy peer = null;

	boolean got_keep_alive = false;

	boolean allow_finish = false;
	VCall<Object> pending_finish = null;
	boolean sent_gw = false;
	boolean got_gw = false;

	/**
	 * Set up handshake for the provided link
	 *
	 * @param link target link
	 */
	public VLinkHandshake(VLink link) {
		wlink = new WeakReference<VLink>(link);
	}

	/**
	 * Terminates link if link handshake was not completed.
	 *
	 * @see java.lang.Object#finalize()
	 */
	@Override
	public void finalize() {
		if (!sent_gw || !got_gw) {
			this.abort();
		}
	}

	/**
	 * Request from peer this side of link should send keep-alive.
	 *
	 * <p>Accepted keep-alive period is the maximum of the requested period
	 * and the minimum allowed by this side of the link.</p>
	 *
	 * @param reqKeepAlive requested keep-alive period in milliseconds
	 * @return accepted keep-alive period
	 */
	@Publish(show=true, ctx=false)
	public synchronized long keep_alive(long reqKeepAlive)
		throws VCallError {
		// Validate method is called only once, and input argument
		if (got_keep_alive || reqKeepAlive <= 0) {
			this.abort();
			throw new VCallError();
		}
		got_keep_alive = true;

		VLink link = wlink.get();
		if (link == null)
			throw new VCallError();
		long min_send_t = link.config.getKeepAlive().getMinSend();
		long send_t = Math.max(min_send_t, reqKeepAlive);
		link.keepAliveSend = send_t;
		return send_t;
	}

	/**
	 * Implements the VOL handshake call to the "finish" remote method.
	 *
	 * @return link local gateway
	 * @throws VExceptionProxy link handshake error
	 */
	@Publish(show=true, ctx=false)
	public synchronized Object finish()
		throws VExceptionProxy {
		if (sent_gw) {
			// Can only call finish() once
			this.abort();
			throw new VException().getProxy();
		}
		if (allow_finish) {
			sent_gw = true;
			VLink link = wlink.get();
			if (link == null)
				throw new VException().getProxy();
			if (got_gw) {
				peer = null;
				link.handshakeCompleted();
			}
			VObject local_gw;
			try {
				local_gw = link.localGateway();
			} catch (VLinkException e) {
				throw new VException().getProxy();
			}
			return local_gw;
		}
		else {
			pending_finish = new VCall<Object>();
			return pending_finish;
		}
	}

	/**
	 * Called internally by link during link handshake when peer handshake was object received.
	 *
	 * @param peer peer handshake object
	 * @throws VLinkException handshake error
	 */
	protected synchronized void recvPeer(VReference peer)
		throws VLinkException {
		this.peer = peer._v_proxy();
		VLink link = wlink.get();
		if (link == null)
			throw new VLinkException("Internal error");
		class Task implements Runnable {
			@Override
			public void run() {
				try {
					handshake();
				} catch (VLinkException e) {
					// SILENT
				}
			}
		}
		try {
			link.getProcessor().submit(new Task());
		} catch (VProcessorException e) {
			throw new VLinkException("Internal error");
		}
	}

	/**
	 * Called internally by the owning link to initiate link handshake.
	 *
	 * @throws VLinkException
	 */
	protected void handshake()
		throws VLinkException {

		VLink link = wlink.get();
		if (link == null)
			throw new VLinkException();

		// If defined on link, request keep-alive
		long req_t = link.config.getKeepAlive().getReqTime();
		if (req_t > 0) {
			long granted_t;
			try {
				granted_t = VInteger.nativeOf(peer.call("keep_alive", req_t)).longValue();
			} catch (Exception e) {
				this.abort();
				throw new VLinkException(e);
			}
			if (granted_t < req_t) {
				this.abort();
				throw new VLinkException("Illegal negotiated recv keep-alive");
			}
			link.keepAliveRecv = granted_t;
		}

		// Finish handshake
		this.canFinish();
		Object result = null;
		try {
			result = peer.call("finish");
		} catch (Exception e) {
			this.abort();
			throw new VLinkException(e);
		}

		VReference peer_gw = null;
		if (result instanceof VProxy)
			result = ((VProxy)result).get();
		try {
			peer_gw = (VReference)result;
		} catch (Exception e) {
			link.shutdown(true, 0, true);
			throw new VLinkException();
		}
		got_gw = true;

		if (sent_gw) {
			peer = null;
			link.handshakeCompleted();
		}
		link.submitPeerGateway(peer_gw);
	}

	/**
	 * Called internally when local handshake is ready to complete handshake.
	 *
	 * @throws VLinkException link error
	 */
	protected synchronized void canFinish()
		throws VLinkException {
		if (pending_finish != null) {
			VLink link = wlink.get();
			if (link == null)
				throw new VLinkException();
			VObject local_gw = link.localGateway();
			try {
				pending_finish.pushResult(local_gw);
			} catch (VCallHaveResult e) {
				throw new VLinkException();
			} catch (VCallCancelled e) {
				throw new VLinkException();
			}
			pending_finish = null;
			sent_gw = true;
			if (got_gw)
				peer = null;
		}
		else
			allow_finish = true;
	}

	void abort() {
		VLink link = wlink.get();
		if (link != null)
			link.shutdown(true, 0, true);
		link = null;
	}
}
