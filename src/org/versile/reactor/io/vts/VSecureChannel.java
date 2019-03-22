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

package org.versile.reactor.io.vts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import org.versile.common.auth.VAuth;
import org.versile.common.auth.VCredentials;
import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VByteGenerator;
import org.versile.crypto.VBlockCipher;
import org.versile.crypto.VBlockTransform;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VHash;
import org.versile.crypto.VMessageDecrypter;
import org.versile.crypto.VMessageEncrypter;
import org.versile.crypto.VRSACipher;
import org.versile.crypto.VRSAKeyPair;
import org.versile.crypto.rand.VPseudoRandomHMAC;
import org.versile.crypto.rand.VSecureRandom;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityReader;
import org.versile.orb.entity.VEntityReaderException;
import org.versile.orb.entity.VEntityWriter;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.VConsumer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;


/**
 * Versile Transport Security channel.
 *
 * <p>Implements a bridge between VTS plaintext and ciphertext data. A channel
 * end-point should be set up as either a {@link VSecureClient} or
 * {@link VSecureServer} depending on which role the end-point takes in the
 * VTS connection.</p>
 */
public abstract class VSecureChannel {

	/**
	 * VTS channel role.
	 */
	protected enum Role {
		/**
		 * VTS client.
		 */
		CLIENT,
		/**
		 * VTS server.
		 */
		SERVER
		};
	/**
	 * VTS role of this channel end-point.
	 */
	protected Role role;

	/**
	 * Reactor driving channel I/O.
	 */
	protected VReactor reactor;
	/**
	 * VTS end-point RSA keypair (possibly null).
	 */
	protected VRSAKeyPair keypair;
	/**
	 * VTS end-point claimed identity (or null).
	 */
	protected X500Principal identity;
	/**
	 * VTS end-point certificates (or null).
	 */
	protected CertPath certs;
	/**
	 * Random-data generator.
	 */
	protected VByteGenerator rand;
	/**
	 * Authorizer for VTS communication peer.
	 */
	protected VAuth peerAuthorizer;
	/**
	 * VTS channel configuration parameters.
	 */
	protected VSecureChannelConfig config;

	/**
	 * Public key received from peer (possibly null).
	 */
	protected RSAPublicKey peerPublicKey = null;
	/**
	 * Certificates received from peer (or null).
	 */
	protected CertPath peerCertificates = null;
	/**
	 * Claimed identity received from peer (or null).
	 */
	protected X500Principal peerIdentity = null;

	// Negotiated parameters
	/**
	 * Name of negotiated hash method for pseudo-random data.
	 */
	protected String negPrfHash = null;
	/**
	 * Name of negotiated cipher.
	 */
	protected String negCipherName = null;
	/**
	 * Mode name of negotiated cipher.
	 */
	protected String negCipherMode = null;
	/**
	 * VCA name of hash method for MAC.
	 */
	protected String negMacHash = null;

	VMessageEncrypter encrypter = null;
	VMessageDecrypter decrypter = null;

	String[] prf_hash_methods;
	Vector<CipherAndModes> ciphers;
	String[] mac_hash_methods;

	WeakReference<PlaintextConsumer> pc_iface = null;
	WeakReference<PlaintextProducer> pp_iface = null;
	WeakReference<CiphertextConsumer> cc_iface = null;
	WeakReference<CiphertextProducer> cp_iface = null;

	/**
	 * Maximum peer RSA key length (number of key bits, -1 if no limit).
	 */
	protected int maxKeyLen;
	/**
	 * Peer's maximum RSA key length (number of key bits, -1 if no limit).
	 */
	protected int peerMaxKeylen = 0; // set to -1 if no limit

	boolean _have_protocol = false;
	static final int PROTO_MAXLEN = 32;
	VByteBuffer _proto_data;
	int _proto_len = 0;
	VByteBuffer _proto_send;
	boolean _can_send_proto = false;

	boolean _handshaking = false;
	boolean _end_handshaking = false;
	/**
	 * Maximum length of handshake message.
	 */
	protected int hshakeLim;
	/**
	 * Peer's maximum length of handshake message.
	 */
	protected int peerHshakeLim = 0; // -1 if no limit
	int _hshake_num_read = 0;
	VEntityReader _handshake_reader = null;
	VEntityWriter _handshake_writer = null;
	Runnable _handshake_handler = null;

	int buf_len;

	VByteProducer _pc_producer = null;
	boolean _pc_eod = false;
	boolean _pc_eod_clean = false;
	long _pc_consumed = 0L;
	long _pc_lim_sent = 0L;
	VByteBuffer _pc_rbuf;
	boolean _pc_aborted = false;

	VByteConsumer _pp_consumer = null;
	boolean _pp_closed = false;
	long _pp_produced = 0L;
	long _pp_prod_lim = 0L;
	VByteBuffer _pp_wbuf;
	boolean _pp_aborted = false;
	boolean _pp_sent_eod = false;

	VByteProducer _cc_producer = null;
	boolean _cc_eod = false;
	boolean _cc_eod_clean = false;
	long _cc_consumed = 0L;
	long _cc_lim_sent = 0L;
	VByteBuffer _cc_rbuf;
	boolean _cc_aborted = false;

	VByteConsumer _cp_consumer = null;
	boolean _cp_closed = false;
	long _cp_produced = 0L;
	long _cp_prod_lim = 0L;
	VByteBuffer _cp_wbuf;
	boolean _cp_aborted = false;
	boolean _cp_sent_eod = false;


	/**
	 * Set up secure channel.
	 *
	 * @param reactor owning reactor
	 * @param credentials credentials for authentication (or null)
	 * @param config channel config settings (defaults if null)
	 * @throws VCryptoException
	 */
	public VSecureChannel(VReactor reactor, VPrivateCredentials credentials, VSecureChannelConfig config,
						  Role role)
		throws VCryptoException {

		this.role = role;

		if (credentials == null)
			credentials = new VPrivateCredentials();

		if (config == null)
			config = new VSecureChannelConfig();
		this.config = config;

		maxKeyLen = config.getMaxRSAKeylen()/8;
		hshakeLim = config.getHandshakeLimit();

		this.reactor = reactor;
		this.keypair = credentials.getKeyPair();
		this.identity = credentials.getIdentity();
		this.certs = credentials.getCertificates();
		VByteGenerator rand = config.getSecureRandom();
		if (rand == null)
			rand = new VSecureRandom();
		this.rand = rand;
		this.peerAuthorizer = config.getAuthorizer();

		buf_len = config.getBufferLength();

		// Generate PRF hash methods
		String[] _config_prf_methods = config.getPRFHashMethods();
		if (_config_prf_methods == null)
			prf_hash_methods = VHash.listHashMethods();
		else {
			if (_config_prf_methods.length == 0)
				throw new VCryptoException("No hash method set for VTS pseudo-random function");
			LinkedList<String> _prf_methods = new LinkedList<String>();
			for (String method: _config_prf_methods) {
				boolean added = false;
				for (String method2: VHash.listHashMethods())
					if (method.equals(method2)) {
						_prf_methods.addLast(method);
						added = true;
						break;
					}
				if (!added)
					throw new VCryptoException("Illegal hash method name: " + method);
			}
			prf_hash_methods = _prf_methods.toArray(new String[0]);
		}

		// Generate cipher names and modes
		ciphers = new Vector<CipherAndModes>();
		Iterable<CipherAndModes> _config_ciphers = config.getCiphers();
		if (_config_ciphers == null) {
			// Build a default cipher list (aes256, blowfish, blowfish128) including
			// only those ciphers which are available from the block cipher provider
			Map<String, Set<String>> _ciphers = VBlockCipher.listCiphers();
			for (String _cname: new String[] {"aes256", "blowfish", "blowfish128"}) {
				Set<String> _modes = _ciphers.get(_cname);
				LinkedList<String> _cmod = new LinkedList<String>();
				if (_modes != null) {
					for (String _mname: new String[] {"cbc", "ofb"}) {
						if (_modes.contains(_mname))
							_cmod.addLast(_mname);
					}
					if (!_cmod.isEmpty())
						ciphers.add(new CipherAndModes(_cname,  _cmod));
				}
			}
		}
		else {
			Map<String, Set<String>> _vca_ciphers = VBlockCipher.listCiphers();
			for (CipherAndModes _cm: _config_ciphers) {
				String name = _cm.getCipher();
				if (_vca_ciphers.get(name) == null)
					throw new VCryptoException("Illegal cipher name");
				LinkedList<String> modes = new LinkedList<String>();
				for (String mode: _cm.getModes()) {
					if (!_vca_ciphers.get(name).contains(mode))
						throw new VCryptoException("Illegal cipher mode");
					modes.addLast(mode);
				}
				if (modes.isEmpty())
					throw new VCryptoException("Cipher without modes");
				ciphers.add(new CipherAndModes(name, modes));
			}
			if (ciphers.isEmpty())
				throw new VCryptoException("No VTS ciphers have been set");
		}

		// Generate MAC hash methods
		String[] _config_mac_methods = config.getMACHashMethods();
		if (_config_mac_methods == null)
			mac_hash_methods = VHash.listHashMethods();
		else {
			if (_config_mac_methods.length == 0)
				throw new VCryptoException("No hash method set for VTS MAC");
			LinkedList<String> _mac_methods = new LinkedList<String>();
			for (String method: _config_mac_methods) {
				boolean added = false;
				for (String method2: VHash.listHashMethods())
					if (method.equals(method2)) {
						_mac_methods.addLast(method);
						added = true;
						break;
					}
				if (!added)
					throw new VCryptoException("Illegal hash method name: " + method);
			}
			mac_hash_methods = _mac_methods.toArray(new String[0]);
		}

		_proto_data = new VByteBuffer();
		try {
			_proto_send = new VByteBuffer("VTS_DRAFT-0.8\n".getBytes("ASCII"));
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException(e);
		}

		_pc_rbuf = new VByteBuffer();
		_pp_wbuf = new VByteBuffer();
		_cc_rbuf = new VByteBuffer();
		_cp_wbuf = new VByteBuffer();
	}

	/**
	 * Get consumer for plaintext data.
	 *
	 * @return plaintext consumer
	 */
	public VByteConsumer getPlaintextConsumer() {
		PlaintextConsumer result = null;
		if (pc_iface != null)
			result = pc_iface.get();
		if (result == null) {
			result = new PlaintextConsumer(this);
			pc_iface = new WeakReference<PlaintextConsumer>(result);
		}
		return result;
	}

	/**
	 * Get producer for plaintext data.
	 *
	 * @return plaintext producer
	 */
	public VByteProducer getPlaintextProducer() {
		PlaintextProducer result = null;
		if (pp_iface != null)
			result = pp_iface.get();
		if (result == null) {
			result = new PlaintextProducer(this);
			pp_iface = new WeakReference<PlaintextProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to plaintext data.
	 *
	 * @return byte I/O pair interface
	 */
	public VByteIOPair getPlaintextIOPair() {
		return new VByteIOPair(getPlaintextConsumer(), getPlaintextProducer());
	}

	/**
	 * Get consumer for ciphertext data.
	 *
	 * @return ciphertext consumer
	 */
	public VByteConsumer getCiphertextConsumer() {
		CiphertextConsumer result = null;
		if (cc_iface != null)
			result = cc_iface.get();
		if (result == null) {
			result = new CiphertextConsumer(this);
			cc_iface = new WeakReference<CiphertextConsumer>(result);
		}
		return result;
	}

	/**
	 * Get producer for ciphertext data.
	 *
	 * @return ciphertext producer
	 */
	public VByteProducer getCiphertextProducer() {
		CiphertextProducer result = null;
		if (cp_iface != null)
			result = cp_iface.get();
		if (result == null) {
			result = new CiphertextProducer(this);
			cp_iface = new WeakReference<CiphertextProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to ciphertext data.
	 *
	 * @return byte I/O pair interface
	 */
	public VByteIOPair getCiphertextIOPair() {
		return new VByteIOPair(getCiphertextConsumer(), getCiphertextProducer());
	}

	/**
	 * Get the owning reactor.
	 *
	 * @return reactor
	 */
	public VReactor getReactor() {
		return reactor;
	}

	long _pc_consume(VByteBuffer data) throws IOException {
		if (_pc_eod || (_pc_producer == null) || (_pc_lim_sent >= 0 && _pc_consumed >= _pc_lim_sent))
			throw new IOException("Consume error");
		else if (data.length() == 0)
			throw new IOException("No data");

		int max_cons = buf_len - _pc_rbuf.length();
		if (_pc_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_pc_lim_sent-_pc_consumed));
		byte[] indata = data.pop(max_cons);
		_pc_rbuf.append(indata);
		_pc_consumed += indata.length;

		this.__cp_produce(false);

		if (_pc_lim_sent >= 0)
			_pc_lim_sent = _pc_consumed + buf_len - _pc_rbuf.length();
		return _pc_lim_sent;
	}

	void _pc_end_consume(boolean clean) {
		if (_pc_eod)
			return;
		_pc_eod = true;
		_pc_eod_clean = clean;

		if (_cp_consumer != null) {
			try {
				this.__cp_produce(false);
			} catch (IOException e) {
				this._pc_abort();
			}
		}
		else
			this._pc_abort();
	}

	void _pc_abort() {
		if (!_pc_aborted) {
			_pc_aborted = true;
			_pc_eod = true;
			_pc_rbuf.clear();
			encrypter = null;
			_cp_wbuf.clear();
			if (_cp_consumer != null) {
				_cp_consumer.abort();
				this._cp_detach(true);
			}
			if (_pc_producer != null) {
				_pc_producer.abort();
				this._pc_detach(true);
			}
		}
	}

	void _pc_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_pc_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_pc_producer == producer)
			return;
		else if (_pc_eod)
			throw new IOException("Consumer already received end-of-data");
		else if (_pc_producer != null)
			throw new IOException("Producer already attached");
		_pc_producer = producer;
		_pc_consumed = 0L;
		_pc_lim_sent = 0L;
		producer.attach(this.getPlaintextConsumer(), true);

		if (_handshaking && _have_protocol)
			this.enablePlaintext();

		try {
			producer.getControl().notifyConsumerAttached(this.getPlaintextConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _pc_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_pc_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_pc_producer != null) {
			VByteProducer prod = _pc_producer;
			_pc_producer = null;
			_pc_consumed = 0L;
			_pc_lim_sent = 0L;
			prod.detach(true);
		}
	}

	VIOControl _pc_get_control() {
		if (_cp_consumer != null)
			return _cp_consumer.getControl();
		else
			return new VIOControl();
	}

	VByteProducer _pc_get_producer() {
		return _pc_producer;
	}

	void _pp_can_produce(long limit) throws IOException {
		if (_pp_consumer == null)
			throw new IOException("No connected consumer");

		boolean _produce = false;
		if (limit < 0) {
			if (_pp_prod_lim >= 0 && _pp_produced >= _pp_prod_lim)
				_produce = true;
			_pp_prod_lim = limit;
		}
		else {
			if (_pp_prod_lim >= 0 && _pp_prod_lim < limit) {
				if (_pp_produced >= _pp_prod_lim)
					_produce = true;
				_pp_prod_lim = limit;
			}
		}
		if (_produce) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__pp_produce(true);
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	void _pp_abort() {
		this._cc_abort();
	}

	void _pp_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_pp_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_pp_consumer == consumer)
			return;
		else if (this._pp_eod())
			throw new IOException("Producer already reached end-of-data");
		else if (_pp_consumer != null)
			throw new IOException("Consumer already attached");
		_pp_consumer = consumer;
		_pp_produced = 0L;
		_pp_prod_lim = 0L;
		consumer.attach(this.getPlaintextProducer(), true);

		if (_cc_producer != null && _cc_lim_sent == 0) {
			_cc_lim_sent = buf_len;
			class Func implements VReactorFunction {
				long limit;
				public Func(long limit) {
					this.limit = limit;
				}
				@Override
				public Object execute() throws Exception {
					_cc_producer.canProduce(limit);
					return null;
				}
			}
			reactor.schedule(new Func(_cc_lim_sent));
		}

		try {
			consumer.getControl().notifyProducerAttached(this.getPlaintextProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _pp_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_pp_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_pp_consumer != null) {
			VByteConsumer cons = _pp_consumer;
			_pp_consumer = null;
			cons.detach();
			_pp_produced = 0L;
			_pp_prod_lim = 0L;
		}
	}

	VIOControl _pp_get_control() {
		class Control extends VIOControl {
			VSecureChannel channel;
			public Control(VSecureChannel channel) {
				this.channel = channel;
			}
			@Override
			public void requestProducerState(VConsumer consumer)
					throws VIOMissingControl {
				// Pass notification to the rest of the chain
				VByteProducer prod = channel.getCiphertextConsumer().getProducer();
				if (prod != null) {
					try {
						prod.getControl().requestProducerState(consumer);
					} catch (VIOMissingControl e) {
						// SILENT
					}
				}
				// Provide authorization information
				if (channel.peerPublicKey != null) {
					class Func implements VReactorFunction {
						@Override
						public Object execute() throws Exception {
							VByteConsumer cons = channel.getPlaintextProducer().getConsumer();
							if (cons != null) {
								try {
									RSAPublicKey key = channel.peerPublicKey;
									CertPath certs = channel.peerCertificates;
									X500Principal identity = channel.peerIdentity;
									VIOControl ctrl = cons.getControl();
									VCredentials _cred = new VCredentials(key, identity, certs);
									boolean allowed = ctrl.authorize(_cred, "VTS");
									if (!allowed) {
										// Connection not authorized, abort
										class Func2 implements VReactorFunction {
											@Override
											public Object execute()
													throws Exception {
												channel._pc_abort();
												channel._pp_abort();
												return null;
											}
										}
										reactor.schedule(new Func2());
									}
								} catch (VIOMissingControl e) {
									// SILENT
								}
							}
							return null;
						}
					}
					reactor.schedule(new Func());

				}
			}
		}
		return new Control(this);
	}

	VByteConsumer _pp_get_consumer() {
		return _pp_consumer;
	}

	long _cc_consume(VByteBuffer data)
					throws IOException {
		if (_cc_eod || (_cc_producer == null) || (_cc_lim_sent >= 0 && _cc_consumed >= _cc_lim_sent))
			throw new IOException("Consume error");
		else if (data.length() == 0)
			throw new IOException("No data");

		int max_cons = buf_len - _cc_rbuf.length();
		if (_cc_lim_sent >= 0)
			max_cons = Math.min(max_cons, (int)(_cc_lim_sent-_cc_consumed));
		byte[] indata = data.pop(max_cons);
		_cc_rbuf.append(indata);
		_cc_consumed += indata.length;

		// If set to true a produce cycle is run before method returns
		boolean c_produce = false;
		boolean p_produce = false;

		if (!_have_protocol) {
			c_produce = true;
			this._consume_protocol();
		}

		if (_handshaking && _have_protocol) {
			c_produce = true;
			if (_cc_rbuf.hasData() && _handshake_reader != null) {
				int num_read = 0;
				try {
					num_read = _handshake_reader.read(_cc_rbuf);
				} catch (VEntityReaderException e) {
					throw new IOException("Invalid VEntity encoding in protocol handshake");
				}
				_hshake_num_read += num_read;
				if (hshakeLim >= 0 && hshakeLim < _hshake_num_read)
					throw new IOException("Handshake message limit exceeded");
				try {
					if (_handshake_reader.done()) {
						VEntity result = _handshake_reader.getResult();
						_handshake_reader = null;
						class Function implements VReactorFunction {
							VEntity indata;
							public Function(VEntity indata) {
								this.indata = indata;
							}
							@Override
							public Object execute() throws Exception {
								handshakeHandler(indata);
								return null;
							}
						}
						reactor.schedule(new Function(result));
					}
				} catch (VEntityReaderException e) {
					throw new IOException("Invalid VEntity encoding in protocol handshake");
				}
			}
		}

		if (!_handshaking && _have_protocol) {
			p_produce = true;
			while (_cc_rbuf.hasData()) {
				byte[] cdata = _cc_rbuf.popAll();
				try {
					decrypter.decrypt(cdata);
				} catch (VCryptoException e) {
					// Critical error, encrypted data did not validate, abort
					class Function implements VReactorFunction {
						@Override
						public Object execute() throws Exception {
							_cc_abort();
							return null;
						}
					}
					reactor.schedule(new Function());
					throw new IOException("Encrypted data did not validate");
				}
				if (decrypter.hasDecrypted())
					_pp_wbuf.append(decrypter.getDecrypted());
			}
		}

		// Run produce/update cycle
		if (c_produce) {
			class Function implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__cp_produce(true);
					return null;
				}
			}
			reactor.schedule(new Function());
		}
		if (p_produce) {
			class Function implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__pp_produce(true);
					return null;
				}
			}
			reactor.schedule(new Function());
		}

		this._cc_update_lim();
		return _cc_lim_sent;
	}

	void _cc_end_consume(boolean clean) {
		if (_cc_eod)
			return;
		_cc_eod = true;
		_cc_eod_clean = clean;

		if (_pp_consumer != null) {
			try {
				this.__pp_produce(false);
			} catch (IOException e) {
				this._cc_abort();
			}
		}
		else
			this._cc_abort();
	}

	void _cc_abort() {
		if (!_cc_aborted) {
			_cc_aborted = true;
			_cc_eod = true;
			_pp_wbuf.clear();
			decrypter = null;
			_cc_rbuf.clear();
			if (_pp_consumer != null) {
				_pp_consumer.abort();
				this._pp_detach(true);
			}
			if (_cc_producer != null) {
				_cc_producer.abort();
				this._cc_detach(true);
			}
		}
	}

	void _cc_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_cc_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_cc_producer == producer)
			return;
		else if (_cc_eod)
			throw new IOException("Consumer already received end-of-data");
		else if (_cc_producer != null)
			throw new IOException("Producer already attached");
		_cc_producer = producer;
		_cc_consumed = 0L;
		_cc_lim_sent = 0L;
		producer.attach(this.getCiphertextConsumer(), true);

		_cc_lim_sent = buf_len;
		producer.canProduce(_cc_lim_sent);

		try {
			producer.getControl().notifyConsumerAttached(this.getCiphertextConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _cc_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_cc_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_cc_producer != null) {
			VByteProducer prod = _cc_producer;
			_cc_producer = null;
			_cc_consumed = 0L;
			_cc_lim_sent = 0L;
			prod.detach(true);
		}
	}

	VIOControl _cc_get_control() {
		if (_pp_consumer != null)
			return _pp_consumer.getControl();
		else
			return new VIOControl();
	}

	VByteProducer _cc_get_producer() {
		return _cc_producer;
	}

	void _cp_can_produce(long limit) throws IOException {
		if (_cp_consumer == null)
			throw new IOException("No connected consumer");

		boolean _produce = false;
		if (limit < 0) {
			if (_cp_prod_lim >= 0 && _cp_produced >= _cp_prod_lim)
				_produce = true;
			_cp_prod_lim = limit;
		}
		else {
			if (_cp_prod_lim >= 0 && _cp_prod_lim < limit) {
				if (_cp_produced >= _cp_prod_lim)
					_produce = true;
				_cp_prod_lim = limit;
			}
		}
		if (_produce) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					__cp_produce(true);
					return null;
				}
			}
			reactor.schedule(new Func());
		}
	}

	void _cp_abort() {
		this._pc_abort();
	}

	void _cp_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_cp_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_cp_consumer == consumer)
			return;
		else if (_cp_eod())
			throw new IOException("Producer already reached end-of-data");
		else if (_cp_consumer != null)
			throw new IOException("Consumer already attached");
		_cp_consumer = consumer;
		_cp_produced = 0L;
		_cp_prod_lim = 0L;
		consumer.attach(this.getCiphertextProducer(), true);

		if (_pc_producer != null && !_handshaking && _have_protocol)
			this.enablePlaintext();

		try {
			consumer.getControl().notifyProducerAttached(this.getCiphertextProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _cp_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_cp_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_cp_consumer != null) {
			VByteConsumer cons = _cp_consumer;
			_cp_consumer = null;
			cons.detach();
			_cp_produced = 0L;
			_cp_prod_lim = 0L;
		}
	}

	VIOControl _cp_get_control() {
		if (_pc_producer != null)
			return _pc_producer.getControl();
		else
			return new VIOControl();
	}

	VByteConsumer _cp_get_consumer() {
		return _cp_consumer;
	}


	void __pp_produce(boolean cc_update)
			throws IOException {
		if (_pp_consumer == null)
			return;

		if (this._pp_eod()) {
			if (_pp_consumer != null && !_pp_sent_eod) {
				_pp_consumer.endConsume(_cc_eod_clean);
				_pp_sent_eod = true;
			}
			return;
		}

		if (_pp_wbuf.hasData()) {
			if (_pp_prod_lim >= 0 && _pp_produced >= _pp_prod_lim)
				return;

			long old_lim = _pp_prod_lim;
			if (_pp_prod_lim != 0) {
				int b_len = _pp_wbuf.length();
				_pp_prod_lim = _pp_consumer.consume(_pp_wbuf);
				_pp_produced += b_len - _pp_wbuf.length();
			}

			// If produce limit was updated, schedule another 'produce' batch
			if (_pp_prod_lim != old_lim) {
				class Func implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						__pp_produce(true);
						return null;
					}
				}
				reactor.schedule(new Func());
			}

			// Plaintext produce may have enabled consuming more ciphertext
			if (cc_update && _cc_producer != null && !_cc_eod) {
				old_lim = _cc_lim_sent;
				this._cc_update_lim();
				if (old_lim != _cc_lim_sent) {
					class Func implements VReactorFunction {
						long limit;
						public Func(long limit) {
							this.limit = limit;
						}
						@Override
						public Object execute() throws Exception {
							if (_cc_producer != null)
								_cc_producer.canProduce(limit);
							return null;
						}
					}
					reactor.schedule(new Func(_cc_lim_sent));
				}
			}
		}
	}

	void __cp_produce(boolean pc_update)
			throws IOException {
		if (_cp_consumer == null)
			return;

		if (this._cp_eod()) {
			if (_cp_consumer != null && !_cp_sent_eod) {
				_cp_consumer.endConsume(_pc_eod_clean);
				_cp_sent_eod = true;
			}
			return;
		}

		if (_cp_prod_lim >= 0 && _cp_produced >= _cp_prod_lim)
			return;

		if (_can_send_proto && _proto_send != null && !_proto_send.isEmpty()) {
			if (_cp_prod_lim != 0) {
				int b_len = _proto_send.length();
				_cp_prod_lim = _cp_consumer.consume(_proto_send);
				_cp_produced += b_len - _proto_send.length();
				if (_proto_send.isEmpty()) {
					_proto_send = null;
					reactor.log("VTS: sent protocol hello");
				}
			}
			return;
		}

		if (_handshaking && _have_protocol && _handshake_writer != null) {
			int max_write = buf_len - _cp_wbuf.length();
			if (_cp_prod_lim >= 0)
				max_write = Math.min(max_write, (int)(_cp_prod_lim-_cp_produced));
			if(max_write > 0) {
				byte[] send_data = _handshake_writer.write(max_write);
				_cp_wbuf.append(send_data);
				_cp_prod_lim = _cp_consumer.consume(_cp_wbuf);
				if (!_cp_wbuf.isEmpty())
					throw new IOException("Consume limit violation");
				_cp_produced += send_data.length;
			}
			if (_handshake_writer.isDone()) {
				_handshake_writer = null;
				if (_end_handshaking) {
					// Handshake complete, enable plaintext
					_handshaking = false;
					this.enablePlaintext();
					if (role == Role.CLIENT)
						reactor.log("VTS: client handshake completed");
					else
						reactor.log("VTS: server handshake completed");
				}
			}
		}

		if (!_handshaking && _have_protocol) {
			while (_cp_wbuf.hasData() || _pc_rbuf.hasData()) {
				if (_cp_prod_lim >= 0 && _cp_produced >= _cp_prod_lim)
					break;
				if (_cp_wbuf.hasData() && _cp_prod_lim != 0) {
					int b_len = _cp_wbuf.length();
					_cp_prod_lim = _cp_consumer.consume(_cp_wbuf);
					_cp_produced += b_len - _cp_wbuf.length();
				}
				else {
					// Create new ciphertext from plaintext buffer
					byte[] data = _pc_rbuf.popAll();
					byte[] encoded;
					try {
						encoded = encrypter.encrypt(data);
					} catch (VCryptoException e) {
						throw new IOException("Crypto subsystem error");
					}
					_cp_wbuf.append(encoded);
				}
			}

			// Plaintext consume limits may have changed
			if (pc_update && _pc_producer != null && !_pc_eod) {
				long old_pc_lim = _pc_lim_sent;
				if (_pc_lim_sent >= 0)
					_pc_lim_sent = _pc_consumed + buf_len - _pc_rbuf.length();
				if (old_pc_lim != _pc_lim_sent) {
					class Func implements VReactorFunction {
						long limit;
						public Func(long limit) {
							this.limit = limit;
						}
						@Override
						public Object execute() throws Exception {
							if (_pc_producer != null) {
								_pc_producer.canProduce(limit);
							}
							return null;
						}
					}
					reactor.schedule(new Func(_pc_lim_sent));
				}
			}
		}
	}

	/**
	 * Enables plaintext producer/consumer to start sending data.
	 */
	protected void enablePlaintext() {
		class Func implements VReactorFunction {
			long limit;
			public Func(long limit) {
				this.limit = limit;
			}
			@Override
			public Object execute() throws Exception {
				_pc_producer.canProduce(limit);
				return null;
			}
		}
		if (_pc_producer != null && !_pc_eod) {
			long old_lim = _pc_lim_sent;
			int max_add = buf_len - _pc_rbuf.length();
			_pc_lim_sent = _pc_consumed + max_add;
			if (old_lim != _pc_lim_sent)
				reactor.schedule(new Func(_pc_lim_sent));
		}
	}


	boolean _pp_eod() {
		return _cc_eod && _cc_rbuf.isEmpty() && _pp_wbuf.isEmpty() &&
				(decrypter == null || !decrypter.hasDecrypted());
	}

	boolean _cp_eod() {
		return _cc_eod && _pc_rbuf.isEmpty() && _cp_wbuf.isEmpty();
	}

	// Updates _cc_lim_sent (caller is responsible for actually sending)
	void _cc_update_lim() {
		if (_cc_producer != null && !_cc_eod) {
			// Do not update ciphertext if plaintext buffer holds more than buffer limit
			if (_have_protocol && !_handshaking)
				if (_pp_prod_lim >= 0 && _pp_wbuf.length() > buf_len)
					return;
			if (_cc_lim_sent >= 0)
				_cc_lim_sent = _cc_consumed + buf_len - _cc_rbuf.length();
		}
	}

	void _consume_protocol()
			throws IOException {
		boolean got_end = false;
		while (_cc_rbuf.hasData() && _proto_len < PROTO_MAXLEN) {
			byte[] bdata = _cc_rbuf.pop(1);
			_proto_data.append(bdata);
			_proto_len += 1;
			if (bdata[0] == (byte)0x0a) {
				// Byte is "\n" termination character
				got_end = true;
				break;
			}
		}
		if (!got_end && _proto_len >= PROTO_MAXLEN)
			throw new IOException("Handshake protocol exceeds set byte limit");

		if (_proto_data.hasData()) {
			byte[] hdata = _proto_data.peekAll();
			if (hdata[hdata.length-1] != (byte)0x0a)
				return;
			String header = new String(hdata, "ASCII");
			header = header.substring(0, header.length()-1);
			String[] s_parts = header.split("-");
			if (s_parts.length != 2)
				throw new IOException("Malformed protocol header");
			String p_name = s_parts[0];
			String p_version = s_parts[1];
			if (!p_name.equals("VTS_DRAFT"))
				throw new IOException("Requires VTS protocol");
			if (!p_version.equals("0.8"))
				throw new IOException("Illegal VTS protocol version");

			// Hello message validated, handshake can proceed
			_have_protocol = true;
			_can_send_proto = true;
			_handshaking = true;
			reactor.log("VTS: received valid protocol handshake");
			initHandshake();
		}
	}

	/**
	 * Initialize a handshake after peer hello message was validated.
	 *
	 * @throws IOException
	 */
	protected abstract void initHandshake()
			throws IOException;

	/**
	 * Handle protocol handshake.
	 *
	 * @param indata handshake message received from peer
	 * @throws IOException
	 */
	protected abstract void handshakeHandler(VEntity indata)
			throws IOException;


	/**
	 * Creates a block cipher encrypted handshake message.
	 *
	 * @param entity handshake message
	 * @param keyseed key seed
	 * @return encrypted message
	 * @throws VCryptoException
	 */
	protected byte[] blockcipherEncEntity(VEntity entity, byte[] keyseed)
			throws VCryptoException {
		byte[] data = entity._v_write(new VIOContext());
		VBlockCipher cipher = VBlockCipher.getCipher(negCipherName, negCipherMode);
		VHash hmac_hash = VHash.getHashGenerator(negPrfHash);
		VByteGenerator prf = new VPseudoRandomHMAC(hmac_hash, new byte[0], keyseed);
		SecretKey key = cipher.importKey(prf);
		byte[] key_iv = prf.getBytes(cipher.getBlockSize(key));
		VBlockTransform _enc = cipher.getEncrypter(key, key_iv);
		VHash hash = VHash.getHashGenerator(negMacHash);
		VByteGenerator padder = config.getPadding();
		if (padder == null)
			padder = new VSecureRandom();
		VMessageEncrypter enc = new VMessageEncrypter(_enc, hash, padder, new byte[0]);
		return enc.encrypt(data);
	}

	/**
	 * Decodes a block cipher encrypted handshake message.
	 *
	 * @param data data to decode
	 * @param keyseed key seed
	 * @return decoded handshake message
	 * @throws VCryptoException
	 */
	protected VEntity blockcipherDecEntity(byte[] data, byte[] keyseed)
			throws VCryptoException {
		VBlockCipher cipher = VBlockCipher.getCipher(negCipherName, negCipherMode);
		VHash hmac_hash = VHash.getHashGenerator(negPrfHash);
		VByteGenerator prf = new VPseudoRandomHMAC(hmac_hash, new byte[0], keyseed);
		SecretKey key = cipher.importKey(prf);
		byte[] key_iv = prf.getBytes(cipher.getBlockSize(key));
		VBlockTransform _dec = cipher.getDecrypter(key, key_iv);
		VHash hash = VHash.getHashGenerator(negMacHash);
		VMessageDecrypter dec = new VMessageDecrypter(_dec, hash, new byte[0]);
		int num_left = dec.decrypt(data);
		if (num_left != 0 || !dec.hasDecrypted())
			throw new VCryptoException("Ciphertext did not resolve as a single set of plaintext");
		byte[] plain = dec.getDecrypted();
		VEntityReader reader = VEntity._v_reader(new VIOContext());
		VByteBuffer rbuf = new VByteBuffer(plain);
		try {
			reader.read(rbuf);
			if (rbuf.hasData() || !reader.done())
				throw new VCryptoException("Decrypted plaintext did not resolve as a VEntity");
			return reader.getResult();
		} catch (VEntityReaderException e) {
			throw new VCryptoException("Decrypted plaintext did not resolve as a VEntity");
		}
	}

	/**
	 * Create an RSA VCA block cipher encrypted message.
	 *
	 * @param entity message to encrypt
	 * @param pubkey public key of receiving peer
	 * @return encrypted message
	 * @throws VCryptoException
	 */
	protected byte[] rsaEncEntity(VEntity entity, RSAPublicKey pubkey)
			throws VCryptoException {
		byte[] data = entity._v_write(new VIOContext());
		VRSACipher rsa = new VRSACipher(pubkey, null);
		VSecureRandom rand = new VSecureRandom();
		VBlockTransform _enc = rsa.vcaBlockEncoder(rand, VRSACipher.CipherMode.CBC);
		VHash hash = VHash.getHashGenerator(negMacHash);
		VByteGenerator padder = config.getPadding();
		if (padder == null)
			padder = new VSecureRandom();
		VMessageEncrypter enc = new VMessageEncrypter(_enc, hash, padder, new byte[0]);
		return enc.encrypt(data);
	}

	/**
	 * Decode an RSA VCA block cipher encrypted message.
	 *
	 * @param data data to decode
	 * @param privkey private key for deciphering
	 * @return decoded message
	 * @throws VCryptoException
	 */
	protected VEntity rsaDecEntity(byte[] data, RSAPrivateKey privkey)
			throws VCryptoException {
		VRSACipher rsa = new VRSACipher(null, privkey);
		VHash hash = VHash.getHashGenerator(negMacHash);
		VBlockTransform _dec = rsa.vcaBlockDecoder(VRSACipher.CipherMode.CBC);
		VMessageDecrypter dec = new VMessageDecrypter(_dec, hash, new byte[0]);
		int num_left = dec.decrypt(data);
		if (num_left != 0 || !dec.hasDecrypted())
			throw new VCryptoException("Data did not resolve as a set of encrypted plaintext");
		byte[] plain = dec.getDecrypted();
		VEntityReader reader = VEntity._v_reader(new VIOContext());
		VByteBuffer rbuf = new VByteBuffer(plain);
		try {
			reader.read(rbuf);
			if (rbuf.hasData() || !reader.done())
				throw new VCryptoException("Decrypted plaintext did not resolve as a VEntity");
			return reader.getResult();
		} catch (VEntityReaderException e) {
			throw new VCryptoException("Decrypted plaintext did not resolve as a VEntity");
		}
	}

	/**
	 * Generate keys from handshake seed data.
	 *
	 * @param s_seed server seed
	 * @param c_seed client seed
	 * @return (c_key, c_iv, c_mac, s_key, s_iv, s_mac)
	 * @throws VCryptoException
	 */
	protected byte[][] generateKeys(byte[] s_seed, byte[] c_seed)
			throws VCryptoException {
		byte[][] result = new byte[6][];
		try {
			byte[] keyseed = VBytes.concat("vts key expansion".getBytes("ASCII"), s_seed, c_seed);
			VHash hmac = VHash.getHashGenerator(negPrfHash);
			VBlockCipher cipher = VBlockCipher.getCipher(negCipherName, negCipherMode);
			VByteGenerator prf = new VPseudoRandomHMAC(hmac, new byte[0], keyseed);
			SecretKey c_key = cipher.importKey(prf);
			SecretKey s_key = cipher.importKey(prf);
			result[0] = c_key.getEncoded();
			result[3] = s_key.getEncoded();
			result[1] = prf.getBytes(cipher.getBlockSize(c_key));
			result[4] = prf.getBytes(cipher.getBlockSize(s_key));
			result[2] = prf.getBytes(cipher.getBlockSize(c_key));
			result[5] = prf.getBytes(cipher.getBlockSize(s_key));
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new VCryptoException("Internal error");
		}
	}

	/**
	 * Generate message encrypter for channel.
	 *
	 * @param key cipher key
	 * @param key_iv cipher Initialization Vector
	 * @param key_mac key MAC
	 * @return encrypter
	 * @throws VCryptoException
	 */
	protected VMessageEncrypter genMsgEnc(byte[] key, byte[] key_iv, byte[] key_mac)
			throws VCryptoException {
		VBlockCipher cipher = VBlockCipher.getCipher(negCipherName, negCipherMode);
		VHash hash = VHash.getHashGenerator(negMacHash);
		SecretKey _key = cipher.importKey(key);
		VBlockTransform _enc = cipher.getEncrypter(_key, key_iv);
		VByteGenerator padder = config.getPadding();
		if (padder == null)
			padder = new VSecureRandom();
		return new VMessageEncrypter(_enc, hash, padder, key_mac);
	}

	/**
	 * Generate message decrypter for channel.
	 *
	 * @param key cipher key
	 * @param key_iv cipher Initialization Vector
	 * @param key_mac key MAC
	 * @return decrypter
	 * @throws VCryptoException
	 */
	protected VMessageDecrypter genMsgDec(byte[] key, byte[] key_iv, byte[] key_mac)
			throws VCryptoException {
		VBlockCipher cipher = VBlockCipher.getCipher(negCipherName, negCipherMode);
		VHash hash = VHash.getHashGenerator(negMacHash);
		SecretKey _key = cipher.importKey(key);
		VBlockTransform _dec = cipher.getDecrypter(_key, key_iv);
		return new VMessageDecrypter(_dec, hash, key_mac);
	}

	/**
	 * Reconstruct RSA key from key data received from peer during handshake.
	 *
	 * @param keydata key data to parse
	 * @return reconstructed RSA key
	 * @throws IOException
	 */
	protected RSAPublicKey parseRsaPubkeyData(VEntity keydata)
			throws IOException {
		String c_pub_name = null;
		VTuple c_pub_data = null;
		try {
			VTuple c_pubdata = VTuple.valueOf(keydata);
			if (c_pubdata.length() != 2)
				throw new IOException("Invalid server public key data");
			c_pub_name = VString.nativeOf(c_pubdata.get(0));
			c_pub_data = VTuple.valueOf(c_pubdata.get(1));
		} catch (VEntityError e) {
			throw new IOException("Invalid server public key data");
		}
		if (!c_pub_name.equals("rsa"))
			throw new IOException("Unsupported server public key type (must be type 'rsa')");
		if (c_pub_data.length() != 5)
			throw new IOException("Invalid server public key data format");
		BigInteger rsa_modulus = null;
		BigInteger rsa_exponent = null;
		try {
			rsa_modulus = VInteger.valueOf(c_pub_data.get(0)).getBigIntegerValue();
			rsa_exponent = VInteger.valueOf(c_pub_data.get(1)).getBigIntegerValue();
		} catch (VEntityError e) {
			throw new IOException("Invalid server public key data format");
		}
		if (maxKeyLen >= 0 && VInteger.posint_to_bytes(rsa_modulus).length > maxKeyLen)
			throw new IOException("Server public key exceeds maximum allowed length");
		if (rsa_modulus.compareTo(BigInteger.ONE) <= 0)
			throw new IOException("Invalid RSA modulus");
		if (rsa_exponent.compareTo(BigInteger.ZERO) < 0)
			throw new IOException("Invalid RSA exponent");
		if (rsa_exponent.compareTo(rsa_modulus) >= 0)
			throw new IOException("Invalid RSA exponent");
		KeyFactory factory = null;
		try {
			factory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1) {
			throw new IOException("Internal RSA key generation error");
		}
		RSAPublicKey result = null;
		try {
			RSAPublicKeySpec _spec = new RSAPublicKeySpec(rsa_modulus, rsa_exponent);
			result = (RSAPublicKey) factory.generatePublic(_spec);
		} catch (Exception e1) {
			throw new IOException("Internal RSA key generation error");
		}
		return result;
	}

	/**
	 * Parse certificate path data received from peer during handshake.
	 *
	 * @param data certificate path data as a tuple of encoded certificates
	 * @return parsed certificate
	 * @throws IOException
	 */
	protected CertPath parseCertPath(VEntity data)
			throws IOException {
		CertPath result = null;

		// Parse X.509 certificate path
		try {
			VTuple t_cert = VTuple.valueOf(data);
		    CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    LinkedList<Certificate> certs = new LinkedList<Certificate>();
		    for (VEntity item: t_cert) {
		    	byte[] cert_data = VBytes.nativeOf(item);
		    	InputStream data_as_stream = new ByteArrayInputStream(cert_data);
		    	certs.addLast(cf.generateCertificate(data_as_stream));
		    }
		    result = cf.generateCertPath(certs);
		} catch (VEntityError e) {
			throw new IOException("Invalid credentials certificate path");
    	} catch (CertificateException e) {
    		throw new IOException("Invalid protocol certificate data");
    	}

		// Validate certificate path
		List<? extends Certificate> c_list = result.getCertificates();
		for (int i = 0; i < c_list.size(); i++) {
			X509Certificate _cert = (X509Certificate)(c_list.get(i));
			try {
				_cert.checkValidity();
			} catch (CertificateExpiredException e) {
				throw new IOException("Certificate chain includes expired certificate");
			} catch (CertificateNotYetValidException e) {
				throw new IOException("Certificate chain includes not yet valid certificate");
			}
			if (i < c_list.size()-1) {
				X509Certificate _next = (X509Certificate)(c_list.get(i+1));
				try {
					_cert.verify(_next.getPublicKey());
				} catch (InvalidKeyException e) {
					throw new IOException("Certificate chain is not a valid signed chain");
				} catch (CertificateException e) {
					throw new IOException("Certificate chain validation error");
				} catch (NoSuchAlgorithmException e) {
					throw new IOException("Certificate chain validation error");
				} catch (NoSuchProviderException e) {
					throw new IOException("Certificate chain validation error");
				} catch (SignatureException e) {
					throw new IOException("Certificate chain validation error");
				}
			}
		}
		return result;
	}

	/**
	 * Check authorization of peer identity or certificates.
	 *
	 * @param identity peer's claimed identity
	 * @param certificates peer's certificates
	 * @throws IOException
	 */
	protected void authorizeCredentials(X500Principal identity, CertPath certificates)
			throws IOException {
		if (peerAuthorizer != null) {
			if (peerAuthorizer.requiresCertificate() && certificates == null)
				throw new IOException("Authorization requires a certificate chain");
			if (peerAuthorizer.requireRootSignature()) {
				if (certificates == null)
					throw new IOException("Authorization requires root-signed certificate");
				List<? extends Certificate> certs = certificates.getCertificates();
				boolean have_root = false;
				try {
					byte[] cert_ca_der = certs.get(certs.size()-1).getEncoded();
					for (Certificate root: peerAuthorizer.getRootCertificates()) {
						byte[] root_der = root.getEncoded();
						if (cert_ca_der.length != root_der.length)
							continue;
						for (int i = 0; i < root_der.length; i++)
							if (root_der[i] != cert_ca_der[i])
								break;
						have_root = true;
						break;
					}
				} catch (CertificateEncodingException e) {
					throw new IOException("Internal certificate handling error");
				}
				if (!have_root)
					throw new IOException("Certificate chain does not have a recognized root CA");
			}
			VCredentials _cred = new VCredentials(peerPublicKey, identity, certificates);
			if (!peerAuthorizer.acceptCredentials(_cred))
				throw new IOException("Credentials rejected by peer authorizer");
		}

		if (_pp_consumer != null) {
			try {
				VCredentials _cred = new VCredentials(peerPublicKey, identity, certificates);
				if (!_pp_consumer.getControl().authorize(_cred, "VTS"))
					throw new IOException("Credentials rejected by producer/consumer chain authorized");
			} catch (VIOMissingControl e) {
				// SILENT
			}
		}
	}


	class PlaintextConsumer implements VByteConsumer {

		VSecureChannel channel;

		public PlaintextConsumer(VSecureChannel channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return channel._pc_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			channel._pc_end_consume(clean);
		}

		@Override
		public void abort() {
			channel._pc_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			channel._pc_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._pc_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._pc_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return channel._pc_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class PlaintextProducer implements VByteProducer {

		VSecureChannel channel;

		public PlaintextProducer(VSecureChannel channel) {
			this.channel = channel;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			channel._pp_can_produce(limit);
		}

		@Override
		public void abort() {
			channel._pp_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			channel._pp_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._pp_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._pp_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return channel._pp_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class CiphertextConsumer implements VByteConsumer {

		VSecureChannel channel;

		public CiphertextConsumer(VSecureChannel channel) {
			this.channel = channel;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return channel._cc_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			channel._cc_end_consume(clean);
		}

		@Override
		public void abort() {
			channel._cc_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			channel._cc_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._cc_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._cc_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return channel._cc_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	class CiphertextProducer implements VByteProducer {

		VSecureChannel channel;

		public CiphertextProducer(VSecureChannel channel) {
			this.channel = channel;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			channel._cp_can_produce(limit);
		}

		@Override
		public void abort() {
			channel._cp_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			channel._cp_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			channel._cp_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return channel._cp_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return channel._cp_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return channel.getReactor();
		}
	}

	/**
	 * Cipher name and mode names for a VTS handshake.
	 */
	public static class CipherAndModes {
		String cipher;
		Vector<String> modes;

		public CipherAndModes(String name, List<String> modes) {
			this.cipher = name;
			this.modes = new Vector<String>();
			for (String mname: modes)
				this.modes.add(mname);
		}

		/**
		 * Get cipher name.
		 *
		 * @return cipher name
		 */
		public String getCipher() {
			return cipher;
		}

		/**
		 * Get mode names.
		 *
		 * @return mode names
		 */
		public List<String> getModes() {
			return modes;
		}
	}
}
