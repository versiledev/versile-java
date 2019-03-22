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

package org.versile.reactor.io.vop;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.versile.common.util.VByteBuffer;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;


/**
 * Reactor interface for Versile Object Protocol.
 *
 * <p>Handles VOP handshake and setup of a VOL link, negotiating a
 * byte transport for the connection.</p>
 */
public abstract class VOPBridge {

	VReactor reactor;
	VByteIOPair vec = null;

	/**
	 * If set allow VTS secure transport (and use as factory).
	 */
	protected TransportFactory vtsFactory;
	/**
	 * If set allow TLS secure transport (and use as factory).
	 */
	protected TransportFactory tlsFactory;
	/**
	 * If true allow insecure transports.
	 */
	protected boolean allowInsecure;

	boolean _handshaking = true;
	boolean _handshake_error = false;
	int _handshake_consumed = 0;
	int _handshake_produced = 0;

	VByteProducer _tc_producer = null;
	VByteConsumer _tp_consumer = null;
	VByteProducer _ec_producer = null;
	VByteConsumer _ep_consumer = null;

	long _tc_cons_lim = 0;
	long _tp_prod_lim = 0;
	long _ec_cons_lim = 0;
	long _ep_prod_lim = 0;

	WeakReference<TransportConsumer> tc_iface = null;
	WeakReference<TransportProducer> tp_iface = null;
	WeakReference<ExternalConsumer> ec_iface = null;
	WeakReference<ExternalProducer> ep_iface = null;

	/**
	 * Set up secure VOP bridge.
	 *
	 * <p>Sets up allowing only secure connections.</p>
	 *
	 * @param reactor bridge reactor
	 * @param vec byte I/O pair of link serialized entity data
	 * @param vtsFactory VTS transport factory (null if VTS not enabled)
	 * @param tlsFactory TLS transport factory (null if TLS not enabled)
	 * @throws IOException no transports enabled
	 */
	public VOPBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory)
			throws IOException {
		this.construct(reactor, vec, vtsFactory, tlsFactory, false);
	}

	/**
	 * Set up VOP bridge.
	 *
	 * @param reactor bridge reactor
	 * @param vec byte I/O pair of serialized link entity data
	 * @param vtsFactory VTS transport factory (null if VTS not enabled)
	 * @param tlsFactory TLS transport factory (null if TLS not enabled)
	 * @param allowInsecure if true allow insecure (plaintext) transport
	 * @throws IOException no transports enabled
	 */
	public VOPBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory, boolean allowInsecure)
		throws IOException {
		this.construct(reactor, vec, vtsFactory, tlsFactory, allowInsecure);
	}

	void construct(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory, boolean allowInsecure)
		throws IOException {
		if (vtsFactory == null && tlsFactory == null && !allowInsecure)
			throw new IOException("VOP requires at least one enabled byte transport");
		this.reactor = reactor;
		this.vec = vec;
		this.vtsFactory = vtsFactory;
		this.tlsFactory = tlsFactory;
		this.allowInsecure = allowInsecure;
	}

	/**
	 * Get an internal interface to the transport's external consumer.
	 *
	 * @return transport's external consumer
	 */
	protected VByteConsumer getTransportConsumer() {
		TransportConsumer result = null;
		if (tc_iface != null)
			result = tc_iface.get();
		if (result == null) {
			result = new TransportConsumer(this);
			tc_iface = new WeakReference<TransportConsumer>(result);
		}
		return result;
	}

	/**
	 * Get an internal interface to the transport's external producer.
	 *
	 * @return transport's external producer
	 */
	protected VByteProducer getTransportProducer() {
		TransportProducer result = null;
		if (tp_iface != null)
			result = tp_iface.get();
		if (result == null) {
			result = new TransportProducer(this);
			tp_iface = new WeakReference<TransportProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to transport data.
	 *
	 * @return byte I/O pair interface
	 */
	protected VByteIOPair getTransportIOPair() {
		return new VByteIOPair(getTransportConsumer(), getTransportProducer());
	}

	/**
	 * Get an external interface to a VOP protocol consumer.
	 *
	 * @return external consumer interface to VOP protocol
	 */
	public VByteConsumer getExternalConsumer() {
		ExternalConsumer result = null;
		if (ec_iface != null)
			result = ec_iface.get();
		if (result == null) {
			result = new ExternalConsumer(this);
			ec_iface = new WeakReference<ExternalConsumer>(result);
		}
		return result;
	}

	/**
	 * Get an external interface to a VOP protocol producer.
	 *
	 * @return external producer interface to VOP protocol
	 */
	public VByteProducer getExternalProducer() {
		ExternalProducer result = null;
		if (ep_iface != null)
			result = ep_iface.get();
		if (result == null) {
			result = new ExternalProducer(this);
			ep_iface = new WeakReference<ExternalProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to external data.
	 *
	 * @return byte I/O pair interface
	 */
	public VByteIOPair getExternalIOPair() {
		return new VByteIOPair(getExternalConsumer(), getExternalProducer());
	}

	/**
	 * Get the owning reactor.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	long _tc_consume(VByteBuffer data) throws IOException {
		if (_tc_producer == null)
			throw new IOException("No connected producer");
		else if (data.isEmpty())
			throw new IOException("No data to consume");
		else if (_handshake_error)
			throw new IOException("Error during handshake");
		else if (_handshaking)
			throw new IOException("Handshake not completed");

		if (_ep_consumer != null) {
			long _lim = _ep_consumer.consume(data);
			_ep_prod_lim = _lim;
			if (_lim >= 0)
				_lim -= _handshake_produced;
				if (_lim < 0)
					_lim = 0;
				_tc_cons_lim = _lim;
			}
		return _tc_cons_lim;
	}

	void _tc_end_consume(boolean clean) {
		if (_handshake_error)
			return;

		if (_handshaking)
			this.handshakeAbort();
		else {
			if (_ep_consumer != null)
				_ep_consumer.endConsume(clean);
			else
				this._tc_abort();
		}
	}

	void _tc_abort() {
		if (_handshaking && !_handshake_error)
			this.handshakeAbort();
		else {
			if (_ep_consumer != null) {
				_ep_consumer.abort();
				this._ep_detach(true);
			}
			if (_tc_producer != null) {
				_tc_producer.abort();
				this._tc_detach(true);
			}
		}
	}

	void _tc_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_tc_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}

		if (_handshake_error)
			throw new IOException("Earlier error during handshake");
		else if (_tc_producer == producer)
			return;
		else if (_tc_producer != null)
			throw new IOException("Producer already connected");

		_tc_producer = producer;
		_tc_cons_lim = 0;
		producer.attach(this.getTransportConsumer(), true);

		if (!_handshaking) {
			long _lim = _ec_cons_lim;
			if (_lim >= 0)
				_lim -= _handshake_consumed;
			producer.canProduce(_lim);
		}

		try {
			producer.getControl().notifyConsumerAttached(this.getTransportConsumer());
		} catch (VIOMissingControl e) {
			// Silent
		}
	}

	void _tc_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_tc_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}

		if (_tc_producer != null) {
			VByteProducer prod = _tc_producer;
			_tc_producer = null;
			_tc_cons_lim = 0;
			prod.detach();
		}
	}

	VIOControl _tc_get_control() {
		if (_ep_consumer != null)
			return _ep_consumer.getControl();
		else
			return new VIOControl();
	}

	VByteProducer _tc_get_producer() {
		return _tc_producer;
	}

	void _tp_can_produce(long limit) throws IOException {
		if (_handshake_error)
			throw new IOException("Earlier error during handshake");
		else if (_tp_consumer == null)
			throw new IOException("No attached consumer");

		_tp_prod_lim = limit;
		if (!_handshaking && _ec_producer != null) {
			if (limit >= 0)
				limit += _handshake_consumed;
			_ec_producer.canProduce(limit);
		}
	}

	void _tp_abort() {
		this._ec_abort();
	}

	void _tp_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_tp_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}

		if (_handshake_error)
			throw new IOException("Earlier error during handshake");
		else if (_tp_consumer == consumer)
			return;
		else if (_tp_consumer != null)
			throw new IOException("Consumer already attached");

		_tp_consumer = consumer;
		_tp_prod_lim = 0;
		consumer.attach(this.getTransportProducer(), true);

		try {
			consumer.getControl().notifyProducerAttached(this.getTransportProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _tp_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_tp_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}

		if (_tp_consumer != null) {
			VByteConsumer cons = _tp_consumer;
			_tp_consumer = null;
			cons.detach();
			_tp_prod_lim = 0;
		}
	}

	VIOControl _tp_get_control() {
		if (_ec_producer != null)
			return _ec_producer.getControl();
		else
			return new VIOControl();
	}

	VByteConsumer _tp_get_consumer() {
		return _tp_consumer;
	}

	long _ec_consume(VByteBuffer data) throws IOException {
		if (_ec_producer == null)
			throw new IOException("No connected external producer");
		else if (data.isEmpty())
			throw new IOException("No data to consume");
		else if (_handshake_error)
			throw new IOException("Earlier handshake error");

		// Handle handshake
		if (_handshaking)
			this.handshakeConsume(data);

		// Handle post-handshake pass-through to transport
		if (!_handshaking && _tp_consumer != null && data.hasData()) {
			long _lim = _tp_consumer.consume(data);
			if (_lim >= 0)
				_lim += _handshake_consumed;
			_ec_cons_lim = _lim;
		}

		return _ec_cons_lim;
	}

	void _ec_end_consume(boolean clean) {
		if (_handshake_error)
			return;

		if (_handshaking)
			this.handshakeAbort();
		else
			if (_tp_consumer != null)
				_tp_consumer.endConsume(clean);
	}

	void _ec_abort() {
		if (_handshaking && !_handshake_error)
			this.handshakeAbort();
		else {
			if (_tp_consumer != null) {
				_tp_consumer.abort();
				this._tp_detach(true);
			}
			if (_ec_producer != null) {
				_ec_producer.abort();
				this._ec_detach(true);
			}
		}
	}

	void _ec_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
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

		if (_handshake_error)
			throw new IOException("Earlier error during handshake");
		else if (_ec_producer == producer)
			return;
		else if (_ec_producer != null)
			throw new IOException("Producer already connected");

		_ec_producer = producer;
		_ec_cons_lim = 0;
		producer.attach(this.getExternalConsumer(), true);

		try {
			producer.getControl().notifyConsumerAttached(this.getExternalConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}

		// Trigger any handshake actions
		this.handshakeProducerAttached();
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
			VByteProducer prod = _ec_producer;
			_ec_producer = null;
			prod.detach();
			_ec_cons_lim = 0;
		}
	}

	VIOControl _ec_get_control() {
		if (_tp_consumer != null)
			return _tp_consumer.getControl();
		else
			return new VIOControl();
	}

	VByteProducer _ec_get_producer() {
		return _ec_producer;
	}

	void _ep_can_produce(long limit) throws IOException {
		if (_handshake_error)
			throw new IOException("Earlier error during handshake");
		else if (_ep_consumer == null)
			throw new IOException("No attached consumer");

		_ep_prod_lim = limit;

		if (_handshaking)
			this.handshakeCanProduce();
		else
			if (_tc_producer != null) {
				if (limit >= 0) {
					limit -= _handshake_produced;
					if (limit < 0)
						limit = 0;
				}
				_tc_cons_lim = limit;
				_tc_producer.canProduce(limit);
			}
	}

	void _ep_abort() {
		this._tc_abort();
	}

	void _ep_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
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

		if (_handshake_error)
			throw new IOException("Earlier error during handshaking");
		else if (_ep_consumer == consumer)
			return;
		else if (_ep_consumer != null)
			throw new IOException("Consumer already attached");

		_ep_consumer = consumer;
		_ep_prod_lim = 0;
		consumer.attach(this.getExternalProducer(), true);

		try {
			consumer.getControl().notifyProducerAttached(this.getExternalProducer());
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
			VByteConsumer cons = _ep_consumer;
			_ep_consumer = null;
			cons.detach(true);
			_ep_prod_lim = 0;
		}
	}

	VIOControl _ep_get_control() {
		if (_tc_producer != null)
			return _tc_producer.getControl();
		else
			return new VIOControl();
	}

	VByteConsumer _ep_get_consumer() {
		return _ep_consumer;
	}

	/**
	 * Abort handshake and abort connected consumers/producers.
	 */
	protected void handshakeAbort() {
		if (!_handshake_error) {
			reactor.log("VOP: Aborting handshake");
			_handshake_error = true;
			this._ec_abort();
			this._ep_abort();

			// Abort VEC chain
			if (vec != null) {
				VByteConsumer _vec_cons = vec.getConsumer();
				if (_vec_cons != null)
					_vec_cons.abort();
				VByteProducer _vec_prod = vec.getProducer();
				if (_vec_prod != null)
					_vec_prod.abort();
			}

			// Free up held references
			vec = null;
			vtsFactory = null;
			tlsFactory = null;
		}
	}

	/**
	 * Checks whether bridge takes a VOP server or client role.
	 *
	 * @return true if server role
	 */
	protected abstract boolean isServer();

	/**
	 * Notification producer was attached during handshake.
	 */
	protected abstract void handshakeProducerAttached();

	/**
	 * Consume handshake data.
	 *
	 * @param data input buffer to consume from
	 */
	protected abstract void handshakeConsume(VByteBuffer data);

	/**
	 * Notification handshake output can be produced.
	 */
	protected abstract void handshakeCanProduce();

	/**
	 * Finalizes handshake after sending/receiving VOP hello messages.
	 *
	 * @param factory factory for negotiated VOP transport (plaintext if null)
	 */
	protected void handshakeComplete(TransportFactory factory) {
		_handshaking = false;

		// Initiate VOP transport
		if (factory == null) {
			// Plaintext transport
			try {
				this._tc_attach(vec.getProducer(), true);
				this._tp_attach(vec.getConsumer(), true);
			} catch (IOException e) {
				this._tc_abort();
				this._tp_abort();
			}
		}
		else {
			// Secure transport
			Transport trans = factory.createTransport();
			try {
				this._tc_attach(trans.getExternalIOPair().getProducer(), true);
				this._tp_attach(trans.getExternalIOPair().getConsumer(), true);
				trans.getInternalIOPair().attach(vec);
			} catch (IOException e) {
				this._tc_abort();
				this._tp_abort();
			}
		}

		// Dereference resources that were held for the handshake
		vtsFactory = null;
		tlsFactory = null;
		vec = null;

		reactor.log("VOP: Completed handshake");
	}


	class TransportConsumer implements VByteConsumer {

		VOPBridge channel;

		public TransportConsumer(VOPBridge channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return channel._tc_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			channel._tc_end_consume(clean);
		}

		@Override
		public void abort() {
			channel._tc_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			channel._tc_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._tc_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._tc_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return channel._tc_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class TransportProducer implements VByteProducer {

		VOPBridge channel;

		public TransportProducer(VOPBridge channel) {
			this.channel = channel;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			channel._tp_can_produce(limit);
		}

		@Override
		public void abort() {
			channel._tp_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			channel._tp_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._tp_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._tp_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return channel._tp_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class ExternalConsumer implements VByteConsumer {

		VOPBridge channel;

		public ExternalConsumer(VOPBridge channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return channel._ec_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			channel._ec_end_consume(clean);
		}

		@Override
		public void abort() {
			channel._ec_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			channel._ec_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._ec_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._ec_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return channel._ec_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class ExternalProducer implements VByteProducer {

		VOPBridge channel;

		public ExternalProducer(VOPBridge channel) {
			this.channel = channel;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			channel._ep_can_produce(limit);
		}

		@Override
		public void abort() {
			channel._ep_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			channel._ep_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._ep_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._ep_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return channel._ep_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	/**
	 * Factory for a VOP transport.
	 */
	public abstract static class TransportFactory {
		/**
		 * Create a VOP transport.
		 *
		 * @return VOP transport
		 */
		public abstract Transport createTransport();
	}

	/**
	 * Producers and consumers associated with a VOP transport.
	 */
	public static class Transport {

		VByteIOPair internal;
		VByteIOPair external;

		/**
		 * Set up transport with consumer/producer endpoints.
		 *
		 * @param external external facing I/O pair
		 * @param internal internal facing I/O pair (interface with VEC serializer)
		 */
		public Transport(VByteIOPair external, VByteIOPair internal) {
			this.external = external;
			this.internal = internal;
		}

		/**
		 * Get external (ciphertext) I/O pair for the transport.
		 *
		 * @return external I/O pair
		 */
		public VByteIOPair getExternalIOPair() {
			return external;
		}

		/**
		 * Get internal (plaintext) I/O pair for the transport.
		 *
		 * @return internal I/O pair
		 */
		public VByteIOPair getInternalIOPair() {
			return internal;
		}
	}
}
