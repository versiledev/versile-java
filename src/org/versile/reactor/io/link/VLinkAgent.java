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

package org.versile.reactor.io.link;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Level;

import org.versile.common.auth.VCredentials;
import org.versile.common.call.VCall;
import org.versile.common.peer.VLocalPeer;
import org.versile.common.peer.VPeer;
import org.versile.common.processor.VProcessor;
import org.versile.orb.entity.VCallContext;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.link.VLink;
import org.versile.orb.link.VLinkException;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VEntityConsumer;
import org.versile.reactor.io.VEntityIOPair;
import org.versile.reactor.io.VEntityProducer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;
import org.versile.reactor.io.VProducer;
import org.versile.reactor.io.vec.VEntityChannel;


/**
 * Versile ORB Link for reactor-based I/O.
 *
 * <p>Implements {@link org.versile.orb.link.VLink} for reactor-based link
 * communication. The link exposes VEntity consumer/producer interfaces
 * from {@link #getConsumer} and {@link #getProducer} which drive the
 * VEntity-based VOL protocol.</p>
 */
public class VLinkAgent extends VLink {

	VReactor reactor;
	boolean lazy_reactor;
	int max_objects;

	WeakReference<EntityProducer> ec_iface = null;
	WeakReference<EntityConsumer> ep_iface = null;

	VEntityProducer _ec_producer = null;
	boolean _ec_eod = false;
	boolean _ec_eod_clean = false;
	long _ec_consumed = 0L;
	long _ec_lim_sent = 0L;
	boolean _ec_aborted = false;

	VEntityConsumer _ep_consumer = null;
	boolean _ep_closed = false;
	long _ep_produced = 0L;
	long _ep_prod_lim = 0L;
	LinkedList<VEntity> _ep_queue;
	boolean _ep_aborted = false;
	boolean _ep_eod = false;
	boolean _ep_eod_clean = false;
	boolean _ep_sent_eod = false;

	boolean _closing_input = false;
	boolean _closing_output = false;
	boolean _got_connect = false;

	/**
	 * Set up link.
	 *
	 * <p>If the 'reactor' argument is null then the link lazy-creates and starts a reactor. The
	 * reactor is owned by the link and is terminated by the link when the link is shutdown. For
	 * other arguments see {@link org.versile.orb.link.VLink}.</p>
	 *
	 * @param gateway local gateway object
	 * @param processor task processor (lazy-created if null)
	 * @param reactor reactor (lazy-created if null)
	 * @param config link configuration data (or null)
	 * @throws VLinkException global copyleft information not configured
	 */
	public VLinkAgent(VObject gateway, VProcessor processor, VReactor reactor,
			          VLinkAgentConfig config) throws VLinkException {
		super(gateway, processor, config);

		// If config is null, override parent configuration setting
		if (config == null) {
			config = new VLinkAgentConfig();
			this.config = config;
		}

		lazy_reactor = false;
		if (reactor == null) {
			lazy_reactor = true;
			reactor = new VReactor(config.getReactorLogger());
			reactor.start();
		}
		this.reactor = reactor;

		this.max_objects = config.getQueueLength();

		_ep_queue = new LinkedList<VEntity>();
	}

	/**
	 * Get VEntity consumer interface for the VOL protocol.
	 *
	 * @return consumer interface
	 */
	public VEntityConsumer getConsumer() {
		EntityConsumer result = null;
		if (ep_iface != null)
			result = ep_iface.get();
		if (result == null) {
			result = new EntityConsumer(this);
			ep_iface = new WeakReference<EntityConsumer>(result);
		}
		return result;
	}

	/**
	 * Get VEntity producer interface for the VOL protocol.
	 *
	 * @return producer interface
	 */
	public VEntityProducer getProducer() {
		EntityProducer result = null;
		if (ec_iface != null)
			result = ec_iface.get();
		if (result == null) {
			result = new EntityProducer(this);
			ec_iface = new WeakReference<EntityProducer>(result);
		}
		return result;
	}

	/**
	 * Get an I/O pair interface to VOL protocol data.
	 *
	 * @return entity I/O pair interface
	 */
	public VEntityIOPair getIOPair() {
		return new VEntityIOPair(getConsumer(), getProducer());
	}

	@Override
	public void log(Level level, String msg) {
		reactor.log(level, msg);
	}

	@Override
	public void shutdown(boolean force, long timeout, boolean purge) {
		shutdown(force, timeout, purge, false);
	}

	void shutdown(boolean force, long timeout, boolean purge, boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				boolean force;
				long timeout;
				boolean purge;
				public Func(boolean force, long timeout, boolean purge) {
					this.force = force;
					this.timeout = timeout;
					this.purge = purge;
				}
				@Override
				public Object execute() throws Exception {
					shutdown(force, timeout, purge, true);
					return null;
				}
			}
			reactor.schedule(new Func(force, timeout, purge));
			return;
		}

		// For use with sending exceptions to any pending results waiting for a gateway
		LinkedList<VCall<VProxy>> _pending_gw = new LinkedList<VCall<VProxy>>();

		synchronized(statusLock) {
			if (!active)
				return;
			if (!handshakeDone) {
				force = true;
				purge = true;
			}

			boolean was_closing = closing;
			closing = true;
			if (!was_closing) {
				statusBus.push(this.getStatus());
				this._shutdown_input();
			}
			if (!was_closing || force)
				this._shutdown_output(force, purge);
			if (active && !force && timeout >= 0) {
				class Func implements VReactorFunction {
					boolean purge;
					public Func(boolean purge) {
						this.purge = purge;
					}
					@Override
					public Object execute() throws Exception {
						shutdown(true, 0L, purge, true);
						return null;
					}
				}
				reactor.schedule(new Func(purge), timeout);
			}

			while(!_peer_gw_calls.isEmpty()) {
				_pending_gw.addLast(_peer_gw_calls.removeFirst());
			}
		}

		// Pass exception to any calls waiting on a gateway
		for (VCall<VProxy> _call : _pending_gw)
			_call.silentPushException(new VLinkException("Link was terminated"));
	}

	@Override
	protected void finalizeShutdown() {
		super.finalizeShutdown();
		if (lazy_reactor) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					reactor.stopReactor();
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	@ Override
	protected void shutdownCallsCompleted() {
		class Func implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				_shutdown_writer(false);
				return null;
			}
		}
		reactor.schedule(new Func());
	}

	/**
	 * Creates two locally paired links.
	 *
	 * <p>The generated links are connected directly via a shared reactor.</p>
	 *
	 * @param clientGateway gateway provided by the client side of the link (or null)
	 * @param serverGateway gateway provided by the server side of the link (or null)
	 * @return (client_link, server_link) tuple of connected links
	 * @throws VLinkException unable to set up connected links
	 */
	public static VLink[] createLinkPair(VObject clientGateway, VObject serverGateway)
			throws VLinkException {
		return VLinkAgent.createLinkPair(clientGateway, serverGateway, null, null, null);
	}

	/**
	 * Creates two locally paired links.
	 *
	 * <p>The generated links are connected directly via a shared reactor.</p>
	 *
	 * @param clientGateway gateway provided by the client side of the link (or null)
	 * @param serverGateway gateway provided by the server side of the link (or null)
	 * @param reactor reactor for links (or null)
	 * @param processor processor for links (or null)
	 * @param config link configuration (or null)
	 * @return (client_link, server_link) tuple of connected links
	 * @throws VLinkException unable to set up connected links
	 */
	public static VLink[] createLinkPair(VObject clientGateway, VObject serverGateway, VProcessor processor,
									     VReactor reactor, VLinkAgentConfig config)
			throws VLinkException {
		VLinkAgentConfig client_config;
		VLinkAgentConfig server_config;
		if (config != null) {
			client_config = config.clone();
			server_config = config.clone();
		}
		else {
			client_config = new VLinkAgentConfig();
			server_config = new VLinkAgentConfig();
		}

		if (clientGateway == null) {
			clientGateway = new VObject();
			server_config.setHoldPeer(false);
		}
		if (serverGateway == null) {
			serverGateway = new VObject();
			client_config.setHoldPeer(false);
		}

		try {
			VLinkAgent server_link = new VLinkAgent(serverGateway, processor, reactor, server_config);
			if (reactor == null)
				reactor = server_link.getReactor();
			VEntityChannel vec_server = new VEntityChannel(reactor, server_link, null);
			vec_server.getEntityIOPair().attach(server_link.getIOPair());

			VLinkAgent client_link = new VLinkAgent(clientGateway, processor, reactor, client_config);
			VEntityChannel vec_client = new VEntityChannel(reactor, client_link, null);
			vec_client.getEntityIOPair().attach(client_link.getIOPair());

			// Attach chain and then fake a "connect" between the two sides. This is done in the reactor thread
			// in order to ensure the attach operation is performed before the connect notification.
			class Job implements VReactorFunction {
				VEntityChannel vec_server;
				VEntityChannel vec_client;
				VLinkAgent server_link;
				VLinkAgent client_link;
				public Job(VEntityChannel vec_server, VEntityChannel vec_client, VLinkAgent server_link, VLinkAgent client_link) {
					this.vec_server = vec_server;
					this.vec_client = vec_client;
					this.server_link = server_link;
					this.client_link = client_link;
				}
				@Override
				public Object execute() throws Exception {
					vec_server.getByteIOPair().attach(vec_client.getByteIOPair(), true);
					server_link.getConsumer().getControl().notifyConnected(new VLocalPeer());
					client_link.getConsumer().getControl().notifyConnected(new VLocalPeer());
					return null;
				}
			}
			reactor.schedule(new Job(vec_server, vec_client, server_link, client_link));

			VLink[] result = new VLink[2];
			result[0] = client_link;
			result[1] = server_link;
			return result;
		} catch (Exception e) {
			throw new VLinkException(e);
		}
	}

	/**
	 * Creates a link to a virtual server.
	 *
	 * <p>Creates two locally paired links, returning the client side of the connection. The other (server) side
	 * of the link is initialized with 'serverGateway' as its gateway object. The generated links are connected
	 * directly via a shared reactor.</p>
	 *
	 * <p>The method is primarily intended as a convenient mechanism for setting up a link for testing
	 * and demonstration purposes.</p>
	 *
	 * @param serverGateway gateway provided by the server side of the link (or null)
	 * @return client link connected to lazy-created server link
	 * @throws VLinkException unable to set up connected links
	 */
	public static VLink createVirtualServerLink(VObject serverGateway)
			throws VLinkException {
		return VLinkAgent.createVirtualServerLink(serverGateway, null, null, null);
	}

	/**
	 * Creates a link to a virtual server.
	 *
	 * <p>Creates two locally paired links, returning the client side of the connection. The other (server) side
	 * of the link is initialized with 'serverGateway' as its gateway object. The generated links are connected
	 * directly via a shared reactor.</p>
	 *
	 * <p>The method is primarily intended as a convenient mechanism for setting up a link for testing
	 * and demonstration purposes.</p>
	 *
	 * @param serverGateway gateway provided by the server side of the link (or null)
	 * @param reactor reactor for links (or null)
	 * @param processor processor for links (or null)
	 * @param config link configuration (or null)
	 * @return client link connected to lazy-created server link
	 * @throws VLinkException unable to set up connected links
	 */
	public static VLink createVirtualServerLink(VObject serverGateway, VProcessor processor, VReactor reactor, VLinkAgentConfig config)
			throws VLinkException {
		return VLinkAgent.createLinkPair(null, serverGateway, processor, reactor, config)[0];
	}

	/**
	 * Get owning reactor.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	@Override
	protected void sendEntity(VEntity entity)
		throws VLinkException {
		this.sendEntity(entity, false);
	}

	void sendEntity(VEntity entity, boolean safe)
			throws VLinkException {
		if (!safe) {
			class Func implements VReactorFunction {
				VEntity entity;
				public Func(VEntity entity) {
					this.entity = entity;
				}
				@Override
				public Object execute() throws Exception {
					sendEntity(entity, true);
					return null;
				}
			}

			// If link is closed, cannot send - throw an exception
			if (!(active || closing))
				throw new VLinkException("Link is closed");

			reactor.schedule(new Func(entity));
			return;
		}
		if (_ep_sent_eod && _ep_aborted)
			throw new VLinkException("Output closed or aborted");
		else if (_ep_eod)
			throw new VLinkException("End-of-data was set on producer, cannot accept new data");
		boolean was_empty = _ep_queue.isEmpty();
		_ep_queue.addLast(entity);
		if (was_empty)
			this.__ep_produce();
	}


	@Override
	protected void scheduleKeepAliveSend(long delay) {
		class Job implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				handleKeepAliveSend();
				return null;
			}
		}
		if (active && !closing)
			reactor.schedule(new Job(), delay);
	}

	@Override
	protected void scheduleKeepAliveRecv(long delay) {
		class Job implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				handleKeepAliveRecv();
				return null;
			}
		}
		if (active && !closing)
			reactor.schedule(new Job(), delay);
	}

	void _shutdown_input() {
		synchronized(statusLock) {
			_closing_input = true;
			VEntityProducer prod = this.getConsumer().getProducer();
			if (prod != null) {
				class Func implements VReactorFunction {
					VEntityProducer producer;
					public Func(VEntityProducer producer) {
						this.producer = producer;
					}
					@Override
					public Object execute() throws Exception {
						producer.abort();
						return null;
					}

				}
				reactor.schedule(new Func(prod));
			}

			// Push VCallError to all local calls waiting for a result
			LinkedList<VCall<Object>> calls = new LinkedList<VCall<Object>>();
			synchronized(remoteCallsLock) {
				for (WeakReference<VCall<Object>> _w_call: remoteCalls.values()) {
					VCall<Object> call = _w_call.get();
					if (call != null)
						calls.addLast(call);
				}
				remoteCalls = new Hashtable<Number, WeakReference<VCall<Object>>>();
			}
			for (VCall<Object> call: calls) {
				call.silentPushException(new VCallError());
			}
		}
	}

	void _shutdown_output(boolean force, boolean purge) {
		synchronized (statusLock) {
			_closing_output = true;

			if (!force) {
				if (purge) {
					// Purge link's associated calls
					processor.removeGroupCalls(this);
                    }

				if (processor.hasGroupCalls(this))
					return;

				synchronized(ongoingCallsLock) {
					if (ongoingCalls > 0)
						return;
				}

				// No queued or running calls, proceed with closing output
				this._shutdown_writer(false);
			}
			else {
				this._shutdown_writer(true);
			}
		}
	}

	void _shutdown_writer(boolean force) {
		synchronized (statusLock) {
			if (!active)
				return;
		}

		if (!force) {
			this.__ep_end_produce(true);
		}
		else {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_ep_abort();
					finalizeShutdown();
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	long _ec_consume(VEntity[] data) throws IOException {
		if (_ec_eod || _ec_producer == null || (_ec_lim_sent >= 0 && _ec_consumed >= _ec_lim_sent))
			throw new IOException("Consume error");
		else if (data.length == 0)
			throw new IOException("No data");

		int max_cons = max_objects;
		if (_ec_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_ec_lim_sent-_ec_consumed));
		if (data.length > max_cons)
			throw new IOException("Provided data exceeds consume limit");

		if (!active) {
			this._shutdown_input();
			throw new IOException("Link not active");
		}

		for (VEntity entity: data) {
			_ec_consumed += 1;
			if (pendingProtocolHandshake) {
				try {
					this.recvHandshake(entity);
				} catch (VLinkException e) {
					throw new IOException("Error processing handshake entity");
				}
			}
			else {
				try {
					this.recvMessage(entity);
				} catch (VLinkException e) {
					throw new IOException("Error processing message", e);
				}
			}
		}

		if (_ec_lim_sent >= 0)
			_ec_lim_sent = _ec_consumed + max_objects;
		return _ec_lim_sent;
	}

	void _ec_end_consume(boolean clean) {
		if (_ec_eod)
			return;
		_ec_eod = true;
		_ec_eod_clean = clean;
		this.shutdown(false);
	}

	void _ec_abort() {
		if (!_ec_aborted) {
			_ec_aborted = true;
			_ec_eod = true;
			if (_ec_producer != null) {
				_ec_producer.abort();
				this._ec_detach(true);
			}
			this.shutdown(false);
		}
	}

	void _ec_attach(VEntityProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VEntityProducer producer;
				public Func(VEntityProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_ec_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_ec_producer == producer)
			return;
		else if (_ec_producer != null)
			throw new IOException("Producer already attached");
		_ec_producer = producer;
		_ec_consumed = 0L;
		_ec_lim_sent = 0L;
		producer.attach(this.getConsumer(), true);
		try {
			producer.getControl().notifyConsumerAttached(this.getConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}

		// We do not set a consume limit here, instead this is done when
		// receiving a "connected" control message
		try {
			producer.getControl().requestProducerState(this.getConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _ec_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_ec_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_ec_producer != null) {
			VEntityProducer prod = _ec_producer;
			_ec_producer = null;
			prod.detach(true);
			_ec_consumed = 0L;
			_ec_lim_sent = 0L;
		}
	}

	VIOControl _ec_get_control() {
		class Control extends VIOControl {
			@Override
			public void notifyConnected(VPeer peer) throws VIOMissingControl {
				if (!_got_connect){
					_got_connect = true;
					log("VLink: connected to " + peer);
					if (_ec_lim_sent >= 0) {
						_ec_lim_sent = _ec_consumed + max_objects;
						try {
							_ec_producer.canProduce(_ec_lim_sent);
						} catch (IOException e) {
							_ec_abort();
						}
					}
					context.setPeer(peer);
					initiateHandshake();
				}
			}

			@Override
			public void notifyProducerAttached(VProducer producer) throws VIOMissingControl {
				class Func implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						try {
							VEntityProducer prod = getConsumer().getProducer();
							if (prod != null)
								prod.getControl().requestProducerState(getConsumer());
						} catch (VIOMissingControl e) {
							// SILENT
						}
						return null;
					}
				}
				reactor.schedule(new Func());
			}

			@Override
			public boolean authorize(VCredentials credentials, String protocol)
					throws VIOMissingControl {
				if (credentials != null) {
					// Register credentials on link call context
					VCallContext ctx = context;
					ctx.setPublicKey(credentials.getPublicKey());
					ctx.setClaimedIdentity(credentials.getIdentity());
					ctx.setCertificates(credentials.getCertificates());
					if (protocol.equals("vts"))
						ctx.setSecureTransport(VCallContext.SecureTransport.VTS);
					else if (protocol.equals("tls"))
						ctx.setSecureTransport(VCallContext.SecureTransport.TLS);
				}
				return authorizeLink();
			}
		}
		return new Control();
	}

	VEntityProducer _ec_get_producer() {
		return _ec_producer;
	}

	void _ep_can_produce(long limit) throws IOException {
		if (_ep_consumer == null)
			throw new IOException("No connected consumer");

		boolean do_produce = false;
		if (limit < 0) {
			if (_ep_prod_lim >= 0 && _ep_produced >= _ep_prod_lim)
				do_produce = true;
			_ep_prod_lim = limit;
		}
		else {
			if (_ep_prod_lim >= 0 && _ep_prod_lim < limit) {
				if (_ep_produced >= _ep_prod_lim)
					do_produce = true;
				_ep_prod_lim = limit;
			}
		}
		if (do_produce) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__ep_produce();
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	void _ep_abort() {
		if (!_ep_aborted) {
			_ep_aborted = true;
			if (_ep_consumer != null) {
				_ep_consumer.abort();
				this._ep_detach(true);
			}
			this.shutdown(true);
		}
	}

	void _ep_attach(VEntityConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VEntityConsumer consumer;
				public Func(VEntityConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_ep_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_ep_consumer == consumer)
			return;
		else if (_ep_consumer != null)
			throw new IOException("Consumer already attached");
		_ep_consumer = consumer;
		_ep_produced = 0L;
		_ep_prod_lim = 0L;
		consumer.attach(this.getProducer(), true);

		try {
			consumer.getControl().notifyProducerAttached(this.getProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _ep_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_ep_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_ep_consumer != null) {
			VEntityConsumer cons = _ep_consumer;
			_ep_consumer = null;
			cons.detach();
			_ep_produced = 0L;
			_ep_prod_lim = 0L;
		}
	}

	VIOControl _ep_get_control() {
		return new VIOControl();
	}

	VEntityConsumer _ep_get_consumer() {
		return _ep_consumer;
	}

	void __ep_produce() {
		if (_ep_consumer != null && !_ep_aborted && !_ep_sent_eod && !_ep_queue.isEmpty()) {
			int max_prod = Math.min(_ep_queue.size(), max_objects);
			if (_ep_prod_lim >= 0)
				max_prod = Math.min(max_prod, (int)(_ep_prod_lim-_ep_produced));
			if (max_prod > 0) {
				LinkedList<VEntity> _prod = new LinkedList<VEntity>();
				for (int i = 0; i < max_prod; i++)
					_prod.addLast(_ep_queue.removeFirst());
				VEntity[] prod = _prod.toArray(new VEntity[0]);

				_ep_produced += prod.length;
				long old_lim = _ep_prod_lim;
				try {
					_ep_prod_lim = _ep_consumer.consume(prod);
				} catch (IOException e) {
					this._ep_abort();
				}
				if (_ep_prod_lim != old_lim && _ep_queue.size() > 0) {
					class Func implements VReactorFunction {
						@Override
						public Object execute() throws Exception {
							__ep_produce();
							return null;
						}
					}
					reactor.schedule(new Func());
				}
			}
		}
		if (_ep_eod && !_ep_sent_eod && _ep_consumer != null) {
			_ep_sent_eod = true;
			_ep_consumer.endConsume(_ep_eod_clean);
		}
	}

	void __ep_end_produce(boolean clean) {
		if (!_ep_eod) {
			_ep_eod = true;
			_ep_eod_clean = clean;
			class Func implements VReactorFunction {
				public Object execute() throws Exception {
					__ep_produce();
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	class EntityConsumer implements VEntityConsumer {

		VLinkAgent agent;

		public EntityConsumer(VLinkAgent agent) {
			this.agent = agent;
		}

		@Override
		public long consume(VEntity[] data) throws IOException {
			return agent._ec_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			agent._ec_end_consume(clean);
		}

		@Override
		public void abort() {
			agent._ec_abort();
		}

		@Override
		public void attach(VEntityProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VEntityProducer producer, boolean safe)
				throws IOException {
			agent._ec_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._ec_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._ec_get_control();
		}

		@Override
		public VEntityProducer getProducer() {
			return agent._ec_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}

	class EntityProducer implements VEntityProducer {

		VLinkAgent agent;

		public EntityProducer(VLinkAgent agent) {
			this.agent = agent;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			agent._ep_can_produce(limit);
		}

		@Override
		public void abort() {
			agent._ep_abort();
		}

		@Override
		public void attach(VEntityConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VEntityConsumer consumer, boolean safe)
				throws IOException {
			agent._ep_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._ep_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._ep_get_control();
		}

		@Override
		public VEntityConsumer getConsumer() {
			return agent._ep_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}
}
