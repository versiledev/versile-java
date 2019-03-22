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

package org.versile.reactor.io.vec;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityReader;
import org.versile.orb.entity.VEntityReaderException;
import org.versile.orb.entity.VEntityWriter;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VString;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.VEntityConsumer;
import org.versile.reactor.io.VEntityIOPair;
import org.versile.reactor.io.VEntityProducer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;


/**
 * Versile Entity Channel for serialized VEntity data.
 *
 * <p>Implements the VEC standard for exchanging
 * {@link org.versile.orb.entity.VEntity} data over a byte channel. The class acts
 * as a bridge between two consumer/producer pairs, one for byte data and another
 * for VEntity data.</p>
 *
 * <p>The byte-side interfaces can be retrieved from {@link #getByteProducer} and
 * {@link #getByteConsumer}.The VEntity-side interfaces are available from
 * {@link #getEntityProducer} and {@link #getEntityConsumer}.</p>
 */
public class VEntityChannel {

	VReactor reactor;
	VIOContext ctx;
	VEntityChannelConfig config;
	int buf_len;
	int queue_len;

	WeakReference<ByteProducer> bc_iface = null;
	WeakReference<ByteConsumer> bp_iface = null;
	WeakReference<EntityProducer> ec_iface = null;
	WeakReference<EntityConsumer> ep_iface = null;

	VByteProducer _bc_producer = null;
	boolean _bc_eod = false;
	boolean _bc_eod_clean = false;
	long _bc_consumed = 0L;
	long _bc_lim_sent = 0L;
	VByteBuffer _bc_buffer;
	boolean _bc_aborted = false;
	VEntityReader _bc_reader = null;

	VByteConsumer _bp_consumer = null;
	boolean _bp_closed = false;
	long _bp_produced = 0L;
	long _bp_prod_lim = 0L;
	VByteBuffer _bp_buffer;
	VEntityWriter _bp_writer = null;
	boolean _bp_aborted = false;
	boolean _bp_sent_eod = false;

	VEntityProducer _ec_producer = null;
	boolean _ec_eod = false;
	boolean _ec_eod_clean = false;
	long _ec_consumed = 0L;
	long _ec_lim_sent = 0L;
	LinkedList<VEntity> _ec_queue;
	boolean _ec_aborted = false;

	VEntityConsumer _ep_consumer = null;
	boolean _ep_closed = false;
	long _ep_produced = 0L;
	long _ep_prod_lim = 0L;
	LinkedList<VEntity> _ep_queue;
	boolean _ep_aborted = false;
	boolean _ep_sent_eod = false;

	boolean _handshaking = true;
	VByteBuffer _handshake_send;
	VByteBuffer _handshake_data;
	int _handshake_len = 0;
	static final int _HANDSHAKE_MAX_LEN = 32;


	/**
	 * Set up the VEC bridge.
	 *
	 * @param reactor reactor driving bridge I/O
	 * @param ctx VEntity serialization context
	 * @param config entity channel configuration parameters (or null)
	 */
	public VEntityChannel(VReactor reactor, VIOContext ctx, VEntityChannelConfig config) {
		this.reactor = reactor;
		this.ctx = ctx;
		if (config == null)
			config = new VEntityChannelConfig();
		this.config = config;

		if (config.getStringEncoding() != null)
			ctx.setStrEncoding(config.getStringEncoding());

		this.buf_len = config.getBufferLength();
		this.queue_len = config.getQueueLength();

		_bc_buffer = new VByteBuffer();
		_bp_buffer = new VByteBuffer();
		_ec_queue = new LinkedList<VEntity>();
		_ep_queue = new LinkedList<VEntity>();
		_handshake_data = new VByteBuffer();

		byte[] _hdata = null;
		try {
			if (ctx.getStrEncoding() != null)
				_hdata = ("VEC_DRAFT-0.8-" + ctx.getStrEncoding() + "\n").getBytes("ASCII");
			else
				_hdata = ("VEC_DRAFT-0.8\n").getBytes("ASCII");
		}  catch (UnsupportedEncodingException e) {
			// This should never happen
			throw new RuntimeException();
		}
		_handshake_send = new VByteBuffer(_hdata);
	}

	/**
	 * Get consumer for serialized VEntity data.
	 *
	 * @return byte-side consumer
	 */
	public VByteConsumer getByteConsumer() {
		ByteConsumer result = null;
		if (bp_iface != null)
			result = bp_iface.get();
		if (result == null) {
			result = new ByteConsumer(this);
			bp_iface = new WeakReference<ByteConsumer>(result);
		}
		return result;
	}

	/**
	 * Get producer for serialized VEntity data.
	 *
	 * @return byte-side producer
	 */
	public VByteProducer getByteProducer() {
		ByteProducer result = null;
		if (bc_iface != null)
			result = bc_iface.get();
		if (result == null) {
			result = new ByteProducer(this);
			bc_iface = new WeakReference<ByteProducer>(result);
		}
		return result;
	}

	/**
	 * Get an I/O pair interface to byte data.
	 *
	 * @return byte I/O pair interface
	 */
	public VByteIOPair getByteIOPair() {
		return new VByteIOPair(getByteConsumer(), getByteProducer());
	}

	/**
	 * Get consumer of VEntity data.
	 *
	 * @return entity-side consumer
	 */
	public VEntityConsumer getEntityConsumer() {
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
	 * Get producer of VEntity data.
	 *
	 * @return entity-side producer
	 */
	public VEntityProducer getEntityProducer() {
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
	 * Get an I/O pair interface to entity data.
	 *
	 * @return entity I/O pair interface
	 */
	public VEntityIOPair getEntityIOPair() {
		return new VEntityIOPair(getEntityConsumer(), getEntityProducer());
	}

	/**
	 * Get the owning reactor.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	long _bc_consume(VByteBuffer data) throws IOException {
		if (_bc_eod || (_bc_producer == null) || (_bc_lim_sent >= 0 && _bc_consumed >= _bc_lim_sent))
			throw new IOException("Consume error");
		else if (data.length() == 0)
			throw new IOException("No data");

		int max_cons = buf_len - _bc_buffer.length();
		if (_bc_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_bc_lim_sent-_bc_consumed));
		byte[] indata = data.pop(max_cons);
		_bc_buffer.append(indata);
		_bc_consumed += indata.length;

		if (_handshaking) {
			try {
				this.__handshake();
			} catch (IOException e) {
				this._bc_abort();
				throw e;
			}
		}
		if (!_handshaking) {
			boolean ep_had_data = (_ep_queue.size() > 0);
			while (_bc_buffer.length() > 0) {
				if (_bc_reader == null)
					_bc_reader = VEntity._v_reader(ctx);
				try {
					_bc_reader.read(_bc_buffer);
				} catch (VEntityReaderException e) {
					this._bc_abort();
					throw new IOException("VEntity decoding error");
				}

				try {
					if (_bc_reader.done()) {
						_ep_queue.addLast(_bc_reader.getResult());
						_bc_reader = null;
					}
				} catch (VEntityReaderException e) {
					// Should never happen
					throw new RuntimeException();
				}
			}
			if (!ep_had_data && _ep_queue.size() > 0) {
				this.__ep_produce(false);
			}
		}

		if (_bc_lim_sent >= 0)
			_bc_lim_sent = _bc_consumed + buf_len;
		return _bc_lim_sent;

	}

	void _bc_end_consume(boolean clean) {
		if (_bc_eod)
			return;
		_bc_eod = true;
		_bc_eod_clean = clean;

		if (_ep_consumer != null) {
			if (_bc_reader != null) {
				_bc_eod_clean = false;
				_bc_reader = null;
			}
			this.__ep_produce(false);
		}
		else
			this._bc_abort();
	}

	void _bc_abort() {
		if (!_bc_aborted) {
			_bc_aborted = true;
			_bc_eod = true;
			_bc_buffer.clear();
			_ep_queue.clear();
			if (_ep_consumer != null) {
				_ep_consumer.abort();
				this._ep_detach(true);
			}
			if (_bc_producer != null) {
				_bc_producer.abort();
				this._bc_detach(true);
			}
		}
	}

	void _bc_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_bc_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_bc_producer == producer)
			return;
		else if (_bc_producer != null)
			throw new IOException("Producer already attached");
		_bc_producer = producer;
		_bc_consumed = 0L;
		_bc_lim_sent = 0L;
		producer.attach(this.getByteConsumer(), true);
		_bc_lim_sent = buf_len;
		producer.canProduce(_bc_lim_sent);

		try {
			producer.getControl().notifyConsumerAttached(this.getByteConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _bc_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_bc_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_bc_producer != null) {
			VByteProducer prod = _bc_producer;
			_bc_producer = null;
			prod.detach(true);
		}
	}

	VIOControl _bc_get_control() {
		if (_ep_consumer != null)
			return _ep_consumer.getControl();
		else
			return new VIOControl();
	}

	VByteProducer _bc_get_producer() {
		return _bc_producer;
	}

	void _bp_can_produce(long limit) throws IOException {
		if (_bp_consumer == null)
			throw new IOException("No connected consumer");

		boolean _produce = false;
		if (limit < 0) {
			if (_bp_prod_lim >= 0 && _bp_produced >= _bp_prod_lim)
				_produce = true;
			_bp_prod_lim = limit;
		}
		else {
			if (_bp_prod_lim >= 0 && _bp_prod_lim < limit) {
				if (_bp_produced >= _bp_prod_lim)
					_produce = true;
				_bp_prod_lim = limit;
			}
		}
		if (_produce) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__bp_produce(true);
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	void _bp_abort() {
		this._ec_abort();
	}

	void _bp_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_bp_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_bp_consumer == consumer)
			return;
		else if (_bp_consumer != null)
			throw new IOException("Consumer already attached");
		_bp_consumer = consumer;
		_bp_produced = 0L;
		_bp_prod_lim = 0L;
		consumer.attach(this.getByteProducer(), true);

		// Trigger check of entity-side data which can be serialized
		if (_ec_producer != null && _ec_lim_sent == 0) {
			_ec_lim_sent = queue_len;
			class Func implements VReactorFunction {
				long limit;
				public Func(long limit) {
					this.limit = limit;
				}
				@Override
				public Object execute() throws Exception {
					_ec_producer.canProduce(limit);
					return null;
				}
			}
			reactor.schedule(new Func(_ec_lim_sent));
		}

		try {
			consumer.getControl().notifyProducerAttached(this.getByteProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _bp_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_bp_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_bp_consumer != null) {
			VByteConsumer cons = _bp_consumer;
			_bp_consumer = null;
			cons.detach();
			_bp_produced = 0L;
			_bp_prod_lim = 0L;
		}
	}

	VIOControl _bp_get_control() {
		if (_ec_producer != null)
			return _ec_producer.getControl();
		else
			return new VIOControl();
	}

	VByteConsumer _bp_get_consumer() {
		return _bp_consumer;
	}

	long _ec_consume(VEntity[] data) throws IOException {
		if (_ec_eod || _ec_producer == null || (_ec_lim_sent >= 0 && _ec_consumed >= _ec_lim_sent))
			throw new IOException("Consume error");
		else if (data.length == 0)
			throw new IOException("No data");

		int max_cons = queue_len - _ec_queue.size();
		if (_ec_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_ec_lim_sent-_ec_consumed));
		if (data.length > max_cons)
			throw new IOException("Provided data exceeds consume limit");

		boolean was_empty = (_ec_queue.size() == 0);
		for (VEntity item: data)
			_ec_queue.addLast(item);
		_ec_consumed += data.length;
		if (was_empty)
			this.__bp_produce(false);

		if (_ec_lim_sent >= 0)
			_ec_lim_sent = _ec_consumed + queue_len - _ec_queue.size();
		return _ec_lim_sent;
	}

	void _ec_end_consume(boolean clean) {
		if (_ec_eod)
			return;
		_ec_eod = true;
		_ec_eod_clean = clean;
		if (_bp_consumer == null)
			this._ec_abort();
		else
			this.__bp_produce(true);
	}

	void _ec_abort() {
		if (!_ec_aborted) {
			_ec_aborted = true;
			_ec_eod = true;
			_bp_buffer.clear();
			_ec_queue.clear();
			if (_bp_consumer != null) {
				_bp_consumer.abort();
				this._bp_detach(true);
			}
			if (_ec_producer != null) {
				_ec_producer.abort();
				this._ec_detach(true);
			}
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
		producer.attach(this.getEntityConsumer(), true);
		_ec_lim_sent = queue_len;
		producer.canProduce(_ec_lim_sent);

		try {
			producer.getControl().notifyConsumerAttached(this.getEntityConsumer());
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
		if (_bp_consumer != null)
			return _bp_consumer.getControl();
		else
			return new VIOControl();
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
					__ep_produce(true);
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	void _ep_abort() {
		this._bc_abort();
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
		consumer.attach(this.getEntityProducer(), true);

		try {
			consumer.getControl().notifyProducerAttached(this.getEntityProducer());
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
		if (_bc_producer != null)
			return _bc_producer.getControl();
		else
			return new VIOControl();
	}

	VEntityConsumer _ep_get_consumer() {
		return _ep_consumer;
	}

	void __ep_produce(boolean bc_update) {
		if (_ep_consumer != null && !(_bc_eod && _ep_queue.isEmpty())) {
			int max_prod = _ep_queue.size();
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
							__ep_produce(true);
							return null;
						}
					}
					reactor.schedule(new Func());
				}
			}

			if (_bc_producer != null && bc_update && (_bc_lim_sent >= 0)) {
				long old_lim = _bc_lim_sent;
				_bc_lim_sent = _bc_consumed + buf_len - _bc_buffer.length();
				if (_bc_lim_sent != old_lim) {
					class Func implements VReactorFunction {
						long limit;
						public Func (long limit) {
							this.limit = limit;
						}
						@Override
						public Object execute() throws Exception {
							if (_bc_producer != null)
								_bc_producer.canProduce(limit);
							return null;
						}
					}
					reactor.schedule(new Func(_bc_lim_sent));
				}
			}

			if (_bc_eod && _ep_queue.size() == 0 && _ep_consumer != null)
				if (!_ep_sent_eod) {
					_ep_consumer.endConsume(_bc_eod_clean);
					_ep_sent_eod = true;
				}
		}
	}

	void __bp_produce(boolean ec_update) {
		if (_bp_consumer == null)
			return;

		if (_ec_eod && _ec_queue.isEmpty() && _bp_writer == null && _bp_buffer.isEmpty()) {
			if (_bp_consumer != null && !_bp_sent_eod) {
				_bp_consumer.endConsume(_ec_eod_clean);
				_bp_sent_eod = true;
			}
			return;
		}

		if (_bp_prod_lim >= 0 && _bp_prod_lim < _bp_produced)
			return;

		int max_write = buf_len - _bp_buffer.length();
		if (_bp_prod_lim >= 0)
			max_write = Math.min(max_write, (int)(_bp_prod_lim-_bp_produced));

		if (_handshake_send != null && max_write > 0) {
			try {
				int old_len = _handshake_send.length();
				_bp_prod_lim = _bp_consumer.consume(_handshake_send);
				_bp_produced += old_len - _handshake_send.length();
				if (_handshake_send.isEmpty())
					_handshake_send = null;
			} catch (IOException e) {
				this._bp_abort();
			}
		}
		if (_handshaking)
			return;

		if (_bp_writer == null && _ec_queue.isEmpty() && _bp_buffer.isEmpty())
			return;

		while (max_write > 0 && (_bp_writer != null || !_ec_queue.isEmpty())) {
			if (_bp_writer == null) {
				VEntity entity = _ec_queue.removeFirst();
				_bp_writer = entity._v_writer(ctx, true);
			}
			byte[] data = _bp_writer.write(max_write);
			_bp_buffer.append(data);
			max_write -= data.length;
			if (_bp_writer.isDone())
				_bp_writer = null;
		}

		if (_bp_prod_lim < 0 || _bp_produced < _bp_prod_lim) {
			long old_len = _bp_buffer.length();
			long old_lim = _bp_prod_lim;
			try {
				_bp_prod_lim = _bp_consumer.consume(_bp_buffer);
			} catch (IOException e) {
				this._bp_abort();
			}
			_bp_produced += old_len - _bp_buffer.length();
			if (_bp_prod_lim != old_lim) {
				class Func implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						__bp_produce(true);
						return null;
					}
				}
				reactor.schedule(new Func());
			}
		}

		if (ec_update && _ec_producer != null) {
			long old_ec_lim = _ec_lim_sent;
			if (_ec_lim_sent >= 0)
				_ec_lim_sent = _ec_consumed + queue_len - _ec_queue.size();
			if (old_ec_lim != _ec_lim_sent) {
				class Func implements VReactorFunction {
					long limit;
					public Func(long limit) {
						this.limit = limit;
					}
					@Override
					public Object execute() throws Exception {
						if (_ec_producer != null)
							_ec_producer.canProduce(limit);
						return null;
					}
				}
				reactor.schedule(new Func(_ec_lim_sent));
			}
		}
	}

	void __handshake() throws IOException {
		while (!_bc_buffer.isEmpty() && _handshake_len < _HANDSHAKE_MAX_LEN) {
			byte b = _bc_buffer.pop(1)[0];
			_handshake_data.append(new byte[]{b});
			_handshake_len += 1;
			if (b == (byte)0x0a)
				break;
			if (_handshake_len >= _HANDSHAKE_MAX_LEN)
				throw new IOException("Handshake limit exceeded");
		}

		byte[] hdata = _handshake_data.peekAll();
		if (hdata.length > 0 && hdata[hdata.length-1] == (byte)0x0a) {
			String hello = null;
			try {
				hello = new String(hdata, "ASCII");
			} catch (UnsupportedEncodingException e) {
				throw new IOException("Could not parse hello string");
			}
			hello = hello.substring(0, hello.length()-1);
			String[] sub = hello.split("-");
			if (sub.length < 2 || sub.length > 3)
				throw new IOException("Invalid header");
			if (!sub[0].equals("VEC_DRAFT"))
				throw new IOException("Invalid protocol name");
			if (!sub[1].equals("0.8"))
				throw new IOException("Unsupported protocol version");
			if (sub.length == 3) {
				boolean allowed = false;
				for (String enc: VString._v_codecs())
					if (sub[2].equals(enc)) {
						allowed = true;
						break;
					}
				if (!allowed)
					throw new IOException("Illegal string encoding scheme");
				ctx.setStrDecoding(sub[2]);
				reactor.log("VEC: peer VString codec set to " + sub[2]);
			}

			_handshaking = false;
            _handshake_data = null;
            class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__bp_produce(true);
					return null;
				}
            }
            reactor.schedule(new Func());
		}
	}

	class ByteConsumer implements VByteConsumer {

		VEntityChannel channel;

		public ByteConsumer(VEntityChannel channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return channel._bc_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			channel._bc_end_consume(clean);
		}

		@Override
		public void abort() {
			channel._bc_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			channel._bc_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._bc_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._bc_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return channel._bc_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class ByteProducer implements VByteProducer {

		VEntityChannel channel;

		public ByteProducer(VEntityChannel channel) {
			this.channel = channel;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			channel._bp_can_produce(limit);
		}

		@Override
		public void abort() {
			channel._bp_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			channel._bp_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._bp_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._bp_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return channel._bp_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class EntityConsumer implements VEntityConsumer {

		VEntityChannel channel;

		public EntityConsumer(VEntityChannel channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VEntity[] data) throws IOException {
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
		public void attach(VEntityProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VEntityProducer producer, boolean safe)
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
		public VEntityProducer getProducer() {
			return channel._ec_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class EntityProducer implements VEntityProducer {

		VEntityChannel channel;

		public EntityProducer(VEntityChannel channel) {
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
		public void attach(VEntityConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VEntityConsumer consumer, boolean safe)
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
		public VEntityConsumer getConsumer() {
			return channel._ep_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

}
