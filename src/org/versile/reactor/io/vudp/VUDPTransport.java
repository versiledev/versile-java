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

package org.versile.reactor.io.vudp;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.peer.VSocketPeer;
import org.versile.common.util.VByteBuffer;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VHash;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VInteger.NetbytesResult;
import org.versile.reactor.VIOHandler;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteConsumer;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.VByteProducer;
import org.versile.reactor.io.VConsumer;
import org.versile.reactor.io.VIOControl;
import org.versile.reactor.io.VIOMissingControl;


/**
 * Implements Versile UDP Transport for reliable streaming.
 *
 * <p>Performs VUT reliable byte data streaming over UDP, intented to be used when
 * reliable streaming is needed but TCP communication is not possible, such as
 * peer-to-peer communication between hosts on separate internal networks. The
 * UDP based scheme implements flow control and congestion avoidance mechanisms
 * similar to TCP.</p>
 *
 * <p>The UDP based transport is typically used to provide a byte transport for a
 * VOP based link. In order to secure a connection, the peers should use VOP
 * with a secure transport mode.</p>
 *
 * <p>VUT communication is secured by HMAC authentication. Transferred data is
 * unencrypted, however each package is authenticated by a message digest which
 * is protected by two secrets, one for each peer. The secrets should normally
 * be negotiated via a relay service, and (if the relay can be trusted and
 * connection to the relay is secure) is known only by the communicating parties
 * and the relay.</p>
 *
 * <p>Establishing a separate secure connection layer on top of the UDP transport
 * also enables additional authentication to be performed between the peers, in
 * addition to securing the communication channel. This minimizes the role of a
 * relay, and means a relay does not need to be fully trusted. This enables
 * peer-based communication models which rely on central services only for
 * relaying connections or discovering peers.</p>
 */
public class VUDPTransport extends VIOHandler {

	// Package size limit
	static final int _MTU = 576;                        // IPv4 requirement
	static final int _IP4_H = 60;                       // Max IPv4 header
	static final int _UDP_H = 8;                        // UDP header length
	static final int MAX_SEGMENT = _MTU-_IP4_H-_UDP_H;  // Max segment size
	static final int _MAX_DGRAM = 65507;                // Max UDP datagram size

	// Max data to include in a package
	static final int _FLG = 1;
	static final int _SEQ = 8;
	static final int _ACK = 8;
	static final int _WIN = 8;
	static final int _MAC = 20;
	static final int MAX_DATA = MAX_SEGMENT - (_FLG+_SEQ+_ACK+_WIN+_MAC);

	// Protocol handshake message
	static final byte[] _PROTO_HELLO = "VUDPTransport-0.8".getBytes();

	// Package header flags
	static final int FLAG_CLOSE = 0x80;                 // Notifies end-of-stream
	static final int FLAG_ACK_CLOSE = 0x40;             // Acknowledges end-of-stream
	static final int FLAG_FAIL = 0x20;                  // Notifies of general failure
	static final int FLAG_MASK = 0xe0;

	// Flow control parameters
	static final float SSTHRESH = 65535.0f/MAX_DATA;    // Initial threshold for SSHTHRESH
	static final float MIN_RTO = 100f;                  // Minimum RTO (ms)
	static final float MAX_RTO = 60000f;                // Maximum RTO (ms)
	static final float DEFAULT_RTO = MIN_RTO;           // Default RTO (ms) - reduced from earlier default
	//static final float DEFAULT_RTO = 3000f;           // Default retransmission timeout (ms)
	static final int RTO_INVALIDATE_BACKOFF = 5;        // Num back-offs to invalidate SRTT/RTTVAR
	static final int HSHAKE_WIN = 128;                  // Window size during protocol handshake

	// Timer handling
	static final int _MAX_TIMERS = 20;                  // Maximum active timers
	static final float _TIMER_REDUCE_FACTOR = 0.8f;     // Minimum reduction factor for adding timer

	// Other
	static final int _DUP_ACK_RESEND = 3;               // Number of dup ack for initiating fast retransmit

	DatagramChannel channel;                            // Selectable channel associated with 'sock'
	InetSocketAddress address;

	byte[] secret;
	byte[] p_secret;
	byte[] _send_secret;
	byte[] _recv_secret;

	boolean _peer_validated = false;
	boolean _peer_acked_hello = false;
	boolean _validated = false;
	boolean _failed = false;                    // True if connection has failed

	boolean _sock_enabled = true;
	boolean _sock_closed = false;               // If true the socket has been closed

	boolean _in_closed = false;                 // If true transport input is closed
	boolean _in_closed_clean = true;            // If true input was closed cleanly
	boolean _out_closed = false;                // If true transport output is closed
	boolean _out_closed_clean = true;           // If true input was closed cleanly
	boolean _out_sent_close = false;            // If true sent output 'close' to peer

	VByteBuffer _sbuf;
	int _sbuf_len;                              // Max buffered send data w/in-flight
	long _sbuf_pos = 0;                         // Stream pos of send buffer start
	long _send_lim;                             // End of peer's advertised window
	long _send_acked = 0;                       // Last acknowledged send position
	Hashtable<Long, InFlightPackage> _in_fl;    // seq_num -> in_flight_package
	LinkedList<Long> _in_fl_pos;                // Sorted pos index of in-flight data
	int _in_fl_num = 0;                         // Amount of in-flight data
	Date _last_send_t = null;                   // timestamp for last data send

	int _num_dup_ack = 0;                       // Number consecutive duplicate ack

	VByteBuffer _rbuf;                          // Receive data buffer
	int _rbuf_len;                              // Max buffered read data
	long _rbuf_spos = 0;                        // Stream pos of recv buffer start
	Hashtable<Long, byte[]> _recv_queue;        // Stream_pos -> data
	long _recv_win_end = 0;                     // End of advertised window
	int _recv_win_step;                         // Step size of inc adv.win
	long _recv_acked = 0;                       // Last acked position

	boolean _recv_closing = false;              // True if input closing
	long _recv_close_pos = 0;                   // Input end stream position

	boolean _force_ack = false;                 // If true force sending a package
	boolean _force_resend = false;              // If true re-send first in-flight
	boolean _fast_recovery = false;             // If true stream is in fast recovery

	float _srtt = -1.0f;                        // Sample round-trip time in milliseconds (none if negative)
	float _rttvar = -1.0f;                      // Round-trip time variance (none if negative)
	float _rto = DEFAULT_RTO;                   // Re-transmission timeout in milliseconds
	int _rto_num_backoff = 0;                   // Number of RTO back-off
	float _cwnd = 2.0f;                         // Congestion window as number of in-flight segments
	float _ssthresh = SSTHRESH;                 // Slow-start threshold as number of segments

	HashSet<Date> _timers;                      // Currently active timers

	WeakReference<VByteConsumer> _ci = null;
	boolean _ci_eod = false;
	boolean _ci_eod_clean = true;
	VByteProducer _ci_producer = null;
	long _ci_consumed = 0;
	long _ci_lim_sent = 0;
	boolean _ci_aborted = false;

	WeakReference<VByteProducer> _pi = null;
	boolean _pi_closed = false;
	VByteConsumer _pi_consumer = null;
	long _pi_produced = 0;
	long _pi_prod_lim = 0;
	VByteBuffer _pi_buffer;
	boolean _pi_aborted = false;

	VHash _hash_cls;
	int _hmac_len;

	VByteBuffer _tmp_buf;

	// Data structure for reading datagrams
	ByteBuffer _tmp_dgram = ByteBuffer.allocate(_MAX_DGRAM);

	/**
	 * Set up client socket consumer/producer.
	 *
	 * <p>'sock' is a bound UDP socket which is reachable by the peer on a network
	 * address known by the peer.</p>
	 *
	 * <p>'peer' is a network address to an UDP port for the peer side of the
	 * connection, which can be reached from this host (e.g. if the peer is
	 * behind a NAT, this must be the external facing host and port for the
	 * network route to the peer). This typically requires using a relay service
	 * to negotiate the UDP connection, which helps the two peers discover each
	 * others' external facing UDP port addresses using NAT hole-punching
	 * techniques.</p>
	 *
	 * <p>Sets up with a buffer length of 65535.</p>
	 *
	 * @param reactor owning reactor
	 * @param channel bound datagram socket for the connection
	 * @param address peer address
	 * @param secret local HMAC secret
	 * @param pSecret peer HMAC secret
	 */
	public VUDPTransport(VReactor reactor, DatagramChannel channel, InetSocketAddress address,
						 byte[] secret, byte[] pSecret) {
		super(reactor);
		this._construct(channel, address, secret, pSecret, 65535);

	}

	/**
	 * Set up client socket consumer/producer.
	 *
	 * <p>'sock' is a bound UDP socket which is reachable by the peer on a network
	 * address known by the peer.</p>
	 *
	 * <p>'peer' is a network address to an UDP port for the peer side of the
	 * connection, which can be reached from this host (e.g. if the peer is
	 * behind a NAT, this must be the external facing host and port for the
	 * network route to the peer). This typically requires using a relay service
	 * to negotiate the UDP connection, which helps the two peers discover each
	 * others' external facing UDP port addresses using NAT hole-punching
	 * techniques.</p>
	 *
	 * @param reactor owning reactor
	 * @param channel bound datagram socket for the connection
	 * @param address peer address
	 * @param secret local HMAC secret
	 * @param pSecret peer HMAC secret
	 * @param bufLen length of receive/send buffers
	 */
	public VUDPTransport(VReactor reactor, DatagramChannel channel, InetSocketAddress address,
						 byte[] secret, byte[] pSecret, int bufLen) {
		super(reactor);
		this._construct(channel, address, secret, pSecret, bufLen);

	}

	void _construct(DatagramChannel channel, InetSocketAddress address, byte[] secret, byte[] p_secret, int buf_len) {
		// Force channel to non-blocking mode
		try {
			channel.configureBlocking(false);
		} catch (IOException e) {
			// Should never happen
			throw new RuntimeException(e);
		}

		this.channel = channel;
		this.address = address;

		VByteBuffer _tmp = new VByteBuffer();
		_tmp.append(secret);
		this.secret = _tmp.popAll();
		_tmp.append(p_secret);
		this.p_secret = _tmp.popAll();
		_tmp.append(secret);
		_tmp.append(p_secret);
		_send_secret = _tmp.popAll();
		_tmp.append(p_secret);
		_tmp.append(secret);
		_recv_secret = _tmp.popAll();

		_sbuf = new VByteBuffer();
		_sbuf_len = buf_len;
		_send_lim = HSHAKE_WIN;
		_in_fl = new Hashtable<Long, InFlightPackage>();
		_in_fl_pos = new LinkedList<Long>();

		_rbuf = new VByteBuffer();
		_rbuf_len = buf_len;
		_recv_win_step = _rbuf_len/5;
		_recv_queue = new Hashtable<Long, byte[]>();

		_timers = new HashSet<Date>();

		_pi_buffer = new VByteBuffer();

		try {
			_hash_cls = VHash.getHashGenerator("sha1");
		} catch (VCryptoException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
		_hmac_len = _hash_cls.getDigestLength();

		_tmp_buf = new VByteBuffer();

		// Initialize send buffer with peer hello
		_sbuf.append(_PROTO_HELLO);

		// Send packages and start reading (must be done from reactor thread)
		class Job implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				_send_packages();
				startReading();
				return null;
			}
		}
		this.getReactor().schedule(new Job());
	}


	/**
	 * Get consumer interface to the socket.
	 *
	 * @return byte consumer interface
	 */
	public VByteConsumer getConsumer() {
		VByteConsumer result = null;
		if (_ci != null)
			result = _ci.get();
		if (result == null) {
			result = new TransportConsumer(this);
			_ci = new WeakReference<VByteConsumer>(result);
		}
		return result;
	}

	/**
	 * Get producer interface to the socket.
	 *
	 * @return byte producer interface
	 */
	public VByteProducer getProducer() {
		VByteProducer result = null;
		if (_pi != null)
			result = _pi.get();
		if (result == null) {
			result = new TransportProducer(this);
			_pi = new WeakReference<VByteProducer>(result);
		}
		return result;
	}

	/**
	 * Get a byte I/O pair interface to the socket.
	 *
	 * @return byte I/O pair
	 */
	public VByteIOPair getIOPair() {
		return new VByteIOPair(getConsumer(), getProducer());
	}

	@Override
	public void doRead() {
		// Read limited number of datagrams to enable reactor to regain control
		for (int _iter = 0; _iter < _MAX_DGRAM/MAX_DATA; _iter++) {
			if (_sock_closed) {
				this.stopReading();
				break;
			}

			_tmp_dgram.rewind();
			SocketAddress _source = null;
			try {
				_source = channel.receive(_tmp_dgram);
			} catch (IOException e) {
				this._p_abort();
				break;
			}
			if (_source == null) {
				// Asynchronous operation timed out
				break;
			}

			if (!_source.equals(address)) {
				this._handle_invalid_peer();
				continue;
			}

			int _num_read = _tmp_dgram.position();
			byte[] dgram = new byte[_num_read];
			for (int i = 0; i < dgram.length; i++)
				dgram[i] = _tmp_dgram.get(i);

			// Validate datagram HMAC
			if (dgram.length <= _hmac_len) {
				this._handle_invalid_hmac();
				continue;
			}
			_tmp_buf.clear();
			_tmp_buf.append(dgram);
			byte[] payload = _tmp_buf.pop(dgram.length - _hmac_len);
			byte[] hmac = _tmp_buf.popAll();
			byte[] _comp_hmac = this._dgram_hmac(payload, _recv_secret);
			for (int i = 0; i < hmac.length; i++) {
				if (hmac[i] != _comp_hmac[i]) {
					this._handle_invalid_hmac();
					continue;
				}
			}

			// Validate datagram length does not exceed allowed max
			if (dgram.length > MAX_SEGMENT) {
				_fail("Maximum package size exceeded");
				break;
			}

			// Decode datagram payload
			_tmp_buf.append(payload);
			// - flags
			if (_tmp_buf.length() < 1) {
				this._fail("Datagram format error");
				break;
			}
			int flags = _tmp_buf.pop(1)[0];
			// - sequence number
			byte[] _tmp = _tmp_buf.peekAll();
			NetbytesResult _num = VInteger.netbytes_to_posint(_tmp);
			if (!_num.hasValue()) {
				_fail("Datagram format error");
				break;
			}
			long seq_num = _num.getValue().longValue();
			_tmp_buf.pop(_num.getBytesRead());
			// - acknowledge number
			_tmp = _tmp_buf.peekAll();
			_num = VInteger.netbytes_to_posint(_tmp);
			if (!_num.hasValue()) {
				_fail("Datagram format error");
				break;
			}
			long ack_num = _num.getValue().longValue();
			_tmp_buf.pop(_num.getBytesRead());
			// - advertised window
			_tmp = _tmp_buf.peekAll();
			_num = VInteger.netbytes_to_posint(_tmp);
			if (!_num.hasValue()) {
				_fail("Datagram format error");
				break;
			}
			long adv_win = _num.getValue().longValue();
			_tmp_buf.pop(_num.getBytesRead());
			// - data
			byte[] data = _tmp_buf.popAll();

			// Check for failure
			if ((flags & FLAG_FAIL) != 0) {
				_fail("Peer sent failure message");
				return;
			}

			// Process ack data
			if (!_peer_acked_hello && ack_num > 0) {
				_peer_acked_hello = true;
				this.getReactor().log("VUDP: peer acknowledged protocol hello");
				this._validate();
			}
			if (ack_num > _send_acked) {
				boolean _have_aligned_pkg = false;
				InFlightPackage _in_fl_pkg = null;
				long _aligned_pos = 0;
				for (long _pos: _in_fl_pos) {
					_in_fl_pkg = _in_fl.get(_pos);
					if (ack_num == _pos + _in_fl_pkg.getData().length) {
						_aligned_pos = _pos;
						_have_aligned_pkg = true;
						break;
					}
				}
				if (!_have_aligned_pkg) {
					// Acknowledge not aligned to any in-flight package, abort
					this._fail("Acknowledge of unknown position");
					break;
				}
				while (!_in_fl_pos.isEmpty() && _in_fl_pos.getFirst() <= _aligned_pos) {
					long _fpos = _in_fl_pos.removeFirst();
					InFlightPackage _removed = _in_fl.remove(_fpos);
					_in_fl_num -= _removed.getData().length;
				}

				// Update 'ack' point; exit fast recovery mode if any
				_send_acked = ack_num;
				_num_dup_ack = 0;

				// Update congestion window and end fast recovery if any
				if (_fast_recovery) {
					// End fast recovery
					_cwnd = _ssthresh;
					_fast_recovery = false;
				}
				else if (_cwnd < _ssthresh) {
					// Slow start, ref. RFC 2581
					_cwnd += 1.0;
				}
				else {
					// Congestion avoidance, ref. RFC 2581
					_cwnd += (1.0f/_cwnd);
				}

				// If RTT can be measured, update parameters as per RFC 2988
				if (_in_fl_pkg.getRetries() == 0) {
					long _rtt = new Date().getTime() - _in_fl_pkg.getTimeStamp().getTime();
					if (_srtt < 0) {
						_srtt = _rtt;
						_rttvar = _rtt/2.0f;
					}
					else {
						_rttvar *= 0.75f;
						_rttvar += 0.25f * Math.abs(_srtt - _rtt);
						_srtt *= 0.875f;
						_srtt += 0.125f * _rtt;
					}
				}
				float old_rto = _rto;
				_rto = _srtt + 4*_rttvar;
				// Enforce min/max on RTO as per RFC 793
				_rto = Math.max(_rto, MIN_RTO);
				_rto = Math.min(_rto, MAX_RTO);

				// Lazy-set new timer for new RTO if RTO was reduced
				if (_rto < old_rto && !_in_fl.isEmpty()) {
					Date _tstamp = _in_fl.get(_in_fl_pos.getFirst()).getTimeStamp();
					long delay = Math.max(_tstamp.getTime() + (long)_rto - new Date().getTime(), 0L);
					this._set_rto_timer(delay);
				}
			}
			else {
				// Detect duplicate ACK as per http://www.ietf.org/mail-archive/web/tcpm/current/msg01200.html
				boolean same_win = ((ack_num+adv_win) <= _send_lim);
				if (same_win && data.length == 0) {
					if (!_in_fl.isEmpty() && ack_num == _send_acked) {
						_num_dup_ack += 1;
						if (_num_dup_ack == _DUP_ACK_RESEND) {
							// Initiate fast retransmit/recovery, ref. RFC 2581
							_ssthresh = Math.max(_in_fl.size()/2, 2);
							_cwnd = _ssthresh + 3;
							_force_resend = true;
							_fast_recovery = true;
						}
						else if (_num_dup_ack > _DUP_ACK_RESEND) {
							_cwnd += 1.0f;
						}
					}
					else if (!_fast_recovery){
						// Custom adaptation; do not reset dup_ack if already in fast recovery mode
						_num_dup_ack = 0;
					}
				}
			}

			// Process received data
			boolean rbuf_data_added = false;
			if (data.length != 0) {
				if (_in_closed || _recv_closing) {
					// Validate peer does not send out-of-bounds package
					if ((seq_num + data.length) > _recv_close_pos) {
						this._fail("Got data past stream close position");
						break;
					}
				}

				if (seq_num == _recv_win_end) {
					// Allow peer to try to send 1 octet past receive window
					if (data.length > 1) {
						this._fail("Advertised window exceeded");
						break;
					}
					else {
						// Ensure an 'ack' is sent
						_force_ack = true;
					}
				}
				else {
					// Handle regular data transfer

					long adv_end = HSHAKE_WIN;
					if (_validated) {
						adv_end = _rbuf_spos + _rbuf_len;
						adv_end -= adv_end % _recv_win_step;
					}
					if ((seq_num + data.length) > adv_end) {
						// Peer advertised window violation
						this._fail("Advertised window exceeded");
						break;
					}

					long _rbuf_next = _rbuf_spos + _rbuf.length();
					if (seq_num == _rbuf_next) {
						_rbuf.append(data);
						_rbuf_next += data.length;
						rbuf_data_added = true;
						// Process receive queue
						Enumeration<Long> _keys = _recv_queue.keys();
						Vector<Long> _sorted_keys= new Vector<Long>();
						while(_keys.hasMoreElements())
							_sorted_keys.add(_keys.nextElement());
						Collections.sort(_sorted_keys);
						for (long _pos: _sorted_keys) {
							if (_pos > _rbuf_next)
								break;
							else if (_pos == _rbuf_next) {
								byte[] _data = _recv_queue.remove(_pos);
								_rbuf.append(_data);
								_rbuf_next += _data.length;
							}
							else {
								// Protocol violation, segment overlaps
								this._fail("Overlapping segments");
								return;
							}
						}
					}
					else if (seq_num > _rbuf_next) {
						long _spos = seq_num;
						long _epos = seq_num + data.length;
						// Check no overlaps with current queue
						Enumeration<Long> _keys = _recv_queue.keys();
						Vector<Long> _sorted_keys= new Vector<Long>();
						while(_keys.hasMoreElements())
							_sorted_keys.add(_keys.nextElement());
						Collections.sort(_sorted_keys);
						for (long _pos: _sorted_keys) {
							if (_pos >= _epos)
								break;
							byte[] _data = _recv_queue.get(_pos);
							if (_pos == _spos) {
								// Re-send of existing segment
								break;
								// NOTE: consider including a check that data and _data are identical,
								// to verify this is a true re-send
							}
							else if ((_pos+_data.length)> _spos) {
								// Protocol violation, overlapping segments
								this._fail("Overlapping segments");
								return;
							}
						}
						// Add segment to queue
						_recv_queue.put(seq_num, data);
						// Force an immediate ack for out-of-order data, similar to RFC 2581
						_force_ack = true;
					}
					else {
						// Old data package; force an immediate ack for out-of-order data, ref. to RFC 2581
						_force_ack = true;
					}
				}

				if (_force_ack) {
					// Immediately resolve force-ack, in order to send a package which
					// can be identified as a 'duplicate ack' by peer
					this._send_force_ack();
					_force_ack = false;
				}
			}

			if (!_peer_validated && data.length > 0) {
				// The first received data must be a protocol handshake
				byte[] hello = _rbuf.popAll();
				if (hello.length != _PROTO_HELLO.length) {
					this._fail("Invalid peer protocol handshake");
					return;
				}
				for (int i = 0; i < hello.length; i++)
					if (hello[i] != _PROTO_HELLO[i]) {
						this._fail("Invalid peer protocol handshake");
						return;
					}
				_peer_validated = true;
				_rbuf_spos += hello.length;
				this.getReactor().log("VUDP: got valid peer protocol hello");

				// If needed force re-sending hello message to peer
				if (!_peer_acked_hello) {
					if (_in_fl.get(0) != null)
						_force_resend = true;
				}

				this._validate();
			}

			// Handle 'close' flag
			if ((flags & FLAG_CLOSE) != 0) {
				long close_pos = seq_num + data.length;
				if (_recv_closing) {
					if (_recv_close_pos != close_pos) {
						this._fail("inconsistent close flag use by peer");
						return;
					}
				}
				else {
					// Check no conflict with buffered data
					if ((_rbuf_spos+_rbuf.length()) > close_pos) {
						this._fail("close flag conflicts with other data");
						return;
					}
					Enumeration<Long> _keys = _recv_queue.keys();
					while(_keys.hasMoreElements()) {
						Long _pos = _keys.nextElement();
						byte[] _data = _recv_queue.get(_pos);
						if ((_pos+_data.length) > close_pos) {
							this._fail("close flag conflicts with other data");
							return;
						}
					}

					// Set input 'closing' status
					_recv_closing = true;
					_recv_close_pos = close_pos;

					// If no pending data, close input and force an 'ack'
					if (_recv_queue.isEmpty() && ((_rbuf_spos+_rbuf.length()) == close_pos)) {
						this.closeInput(true);
						_force_ack = true;
					}
				}
			}

			// Handle 'ack_close' flag
			if ((flags & FLAG_ACK_CLOSE) != 0 && !_out_closed) {
				if (!_ci_eod || _sbuf.hasData()) {
					// Premature ack_close means peer aborted the output
					this._c_abort();
				}
				else if (_in_fl.isEmpty())
					this.closeOutput(true);
			}

			// Update send limit
			_send_lim = Math.max(_send_lim, ack_num+adv_win);

			// If data was added, perform a production iteration
			if (rbuf_data_added)
				this._do_produce();

			// Perform a package send iteration
			this._send_packages();
		}

		// If validated and have producer, evaluate consume limit
		if (_validated && _ci_producer != null) {
			long _old_lim = _ci_lim_sent;
			long _cur_lim = _ci_consumed + _sbuf_len - _sbuf.length() - _in_fl_num;
			if (_cur_lim > _old_lim) {
				_ci_lim_sent = _cur_lim;
				try {
					_ci_producer.canProduce(_ci_lim_sent);
				} catch (IOException e) {
					this._c_abort();
				}
			}
		}
	}

	/**
	 * Perform regular sending of packages to peer.
	 */
	public void _send_packages() {
		while (true) {
			// Ensure socket is open
			if (_sock_closed || !_sock_enabled || channel == null)
				break;

			// Determine if we can send data (if not max_data will be zero)
			int max_data = 0;
			boolean check_close = false;
			if (_cwnd >= _in_fl.size()+1) {
				if (_sbuf.hasData()) {
					if (_sbuf_pos < _send_lim)
						max_data = (int)(_send_lim - _sbuf_pos);
					else if (_sbuf_pos == _send_lim) {
						// Allow single octet past adv window if RTO expired
						if (_last_send_t == null || (new Date().getTime() - _last_send_t.getTime()) >= _rto)
							max_data = 1;
					}
				}
				else if (_ci_eod && !_out_sent_close)
					check_close = true;
			}

			// Determine new ack number and advertised window size
			long ack_num = _rbuf_spos + _rbuf.length();
			long adv_end = 0;
			if (_validated) {
				adv_end = _rbuf_spos + _rbuf_len;
				adv_end -= adv_end % _recv_win_step;
			}
			else
				adv_end = HSHAKE_WIN;

			// If nothing to send to peer, just return
			if (_force_resend && _in_fl.isEmpty())
				_force_resend = false;
			if (!(max_data > 0 || check_close || ack_num > _recv_acked || adv_end > _recv_win_end
				  || _force_ack || _force_resend))
				break;

			// Compose a package for sending to peer
			long _seq_num;
			byte[] _b_seq_num;
			byte[] data;
			long _force_pos = 0;
			if (_force_resend) {
				_force_pos = _in_fl_pos.getFirst();
				_seq_num = _force_pos;
				_b_seq_num = VInteger.posint_to_netbytes(_seq_num);
				data = _in_fl.get(_force_pos).getData();
			}
			else {
				_seq_num = _sbuf_pos;
				_b_seq_num = VInteger.posint_to_netbytes(_seq_num);
				data = _sbuf.peek(Math.min(max_data, MAX_DATA));
			}
			byte[] _b_ack_num = VInteger.posint_to_netbytes(ack_num);
			byte[] _b_adv_win = VInteger.posint_to_netbytes(adv_end-ack_num);

			if (data.length > 0 && _last_send_t != null) {
				// Initialize "slow start" if it is more than one RTO since
				// the last data transmission, ref. RFC 2581
				if ((new Date().getTime() - _last_send_t.getTime()) > _rto)
					_cwnd = 2.0f;
			}

			int flag = this._gen_flag(_seq_num+data.length);
			byte fbyte = (byte)(flag & 0xff);

			_tmp_buf.clear();
			_tmp_buf.append(fbyte);
			_tmp_buf.append(_b_seq_num);
			_tmp_buf.append(_b_ack_num);
			_tmp_buf.append(_b_adv_win);
			_tmp_buf.append(data);
			_tmp_buf.append(this._dgram_hmac(_tmp_buf.peekAll(), _send_secret));
			byte[] pkg = _tmp_buf.popAll();

			ByteBuffer _send_data = ByteBuffer.wrap(pkg);
			int _num_sent = 0;
			try {
				_num_sent = channel.send(_send_data, address);
			} catch (IOException e) {
				this._c_abort();
				break;
			}
			if (_num_sent == 0) {
				// Socket not ready
				break;
			}
			else if (_num_sent != pkg.length) {
				// Critical error; data not sent as single datagram
				this._c_abort();
				break;
			}

			// Meta-data was sent, no need to force_ack
			_force_ack = false;

			if (data.length > 0) {
				if (_force_resend) {
					// Increase re-send counter and disable force re-send
					InFlightPackage _in_fl_pkg = _in_fl.get(_force_pos);
					_in_fl_pkg.setTimeStamp(new Date());
					_in_fl_pkg.addRetry();
					_force_resend = false;
				}
				else {
					_sbuf.pop(data.length);
					_in_fl.put(_sbuf_pos, new InFlightPackage(data, new Date(), (long)_rto));
					_in_fl_pos.addLast(_sbuf_pos);
					_in_fl_num += data.length;
					_sbuf_pos += data.length;
				}

				// Update time of last data transmission and set RTO timer
				_last_send_t = new Date();
				this._set_rto_timer((long)_rto);
			}

			_recv_acked = ack_num;
			if (_recv_win_end < adv_end)
				_recv_win_end = adv_end;
			if ((flag & FLAG_CLOSE) != 0)
				_out_sent_close = true;
		}
	}

	/**
	 * Re-sends package at position 'pos' in _in_fl
	 *
	 * @param pos in-flight package position
	 */
	public void _resend_package(long pos) {

		// Ensure socket is open
		if (_sock_closed || !_sock_enabled || channel == null)
			return;

		InFlightPackage _in_fl_pkg = _in_fl.get(pos);
		if (_in_fl_pkg == null) {
			// Should never happen
			return;
		}

		// Determine new ack number and advertised window
		long ack_num = _rbuf_spos + _rbuf.length();
		long adv_end;
		if (_validated) {
			adv_end = _rbuf_spos + _rbuf.length();
			adv_end -= adv_end % _recv_win_step;
		}
		else
			adv_end = HSHAKE_WIN;

		// Compose package for sending to peer
		long _seq_num = pos;
		byte[] _b_seq_num = VInteger.posint_to_netbytes(_seq_num);
		byte[] _b_ack_num = VInteger.posint_to_netbytes(ack_num);
		byte[] _b_adv_win = VInteger.posint_to_netbytes(adv_end-ack_num);
		byte[] data = _in_fl_pkg.getData();

		int flag = this._gen_flag(_seq_num+data.length);
		byte fbyte = (byte)(flag & 0xff);

		_tmp_buf.clear();
		_tmp_buf.append(fbyte);
		_tmp_buf.append(_b_seq_num);
		_tmp_buf.append(_b_ack_num);
		_tmp_buf.append(_b_adv_win);
		_tmp_buf.append(data);
		_tmp_buf.append(this._dgram_hmac(_tmp_buf.peekAll(), _send_secret));
		byte[] pkg = _tmp_buf.popAll();

		ByteBuffer _send_data = ByteBuffer.wrap(pkg);
		int _num_sent = 0;
		try {
			_num_sent = channel.send(_send_data, address);
		} catch (IOException e) {
			this._c_abort();
			return;
		}
		if (_num_sent == 0) {
			// Socket not ready
			return;
		}
		else if (_num_sent != pkg.length) {
			// Critical error; data not sent as single datagram
			this._c_abort();
			return;
		}

		_in_fl_pkg.setTimeStamp(new Date());
		// Custom logic for 'backing off package delay timer'
		long _delay = _in_fl_pkg.getDelay();
		_delay = Math.min(2*_delay, (long)_rto);
		_in_fl_pkg.setDelay(_delay);
		_in_fl_pkg.addRetry();
		this._set_rto_timer(_delay);

		_recv_acked = ack_num;
		if (_recv_win_end < adv_end)
			_recv_win_end = adv_end;
		if ((flag & FLAG_CLOSE) != 0)
			_out_sent_close = true;
	}

	/**
	 * Sends an ack package with no new information.
	 */
	public void _send_force_ack() {
		// Ensure socket is open
		if (_sock_closed || !_sock_enabled || channel == null)
			return;

		// Compose package for sending to peer
		long seq_num = _sbuf_pos;
		long ack_num = _rbuf_spos + _rbuf.length();
		long adv_win = _recv_win_end - ack_num;
		byte[] _b_seq_num = VInteger.posint_to_netbytes(seq_num);
		byte[] _b_ack_num = VInteger.posint_to_netbytes(ack_num);
		byte[] _b_adv_win = VInteger.posint_to_netbytes(adv_win);
		byte[] data = new byte[0];

		int flag = this._gen_flag(seq_num);
		byte fbyte = (byte)(flag & 0xff);

		_tmp_buf.clear();
		_tmp_buf.append(fbyte);
		_tmp_buf.append(_b_seq_num);
		_tmp_buf.append(_b_ack_num);
		_tmp_buf.append(_b_adv_win);
		_tmp_buf.append(data);
		_tmp_buf.append(this._dgram_hmac(_tmp_buf.peekAll(), _send_secret));
		byte[] pkg = _tmp_buf.popAll();

		ByteBuffer _send_data = ByteBuffer.wrap(pkg);
		int _num_sent = 0;
		try {
			_num_sent = channel.send(_send_data, address);
		} catch (IOException e) {
			this._c_abort();
			return;
		}
		if (_num_sent == 0) {
			// Socket not ready
			return;
		}
		else if (_num_sent != pkg.length) {
			// Critical error; data not sent as single datagram
			this._c_abort();
			return;
		}
	}

	int _gen_flag(long pos) {
		int flag = 0x00;
		// Handle setting 'close' flag
		if (_ci_eod) {
			if ((pos) == (_sbuf_pos + _sbuf.length())) {
				flag |= FLAG_CLOSE;
			}
		}
		if (_in_closed)
			flag |= FLAG_ACK_CLOSE;
		return flag;
	}

	@Override
	public void doWrite() {
		// Selectable write not used by this handler
		this.stopWriting();
	}

	@Override
	public SelectableChannel getChannel() {
		return channel;
	}

	/**
	 * Sets a timer for the given timeout.
	 *
	 * <p>Timer that is set depends on the current pool of timers. A timer will
	 * only be set if if the current pool is not maxed and sufficient reduction
	 * in time until next set timer is achieved.</p>
	 *
	 * <p>This handling is a trade-off between setting exact timers when RTO
	 * estimates are reduced, vs. the cost of re-normalizing reactor scheduled
	 * tasks when cancelled or delayed.</p>
	 *
	 * @param delay timeout in milliseconds
	 */
	void _set_rto_timer(long delay) {
		// Ensure no negative delays
		delay = Math.max(delay, 0);

		if (_timers.size() >= _MAX_TIMERS)
			return;

		Date cur_time = new Date();
		boolean _do_set;
		if (_timers.isEmpty())
			_do_set = true;
		else {
			long _set_delay = Math.max(Collections.min(_timers).getTime()-cur_time.getTime(), 0);
			// Must test with '<' in order to avoid scheduling multiple zero-delay timers
			_do_set = (delay < _TIMER_REDUCE_FACTOR*_set_delay);
		}

		if (_do_set) {
			Date timeout = new Date(cur_time.getTime() + delay);
			class Job implements VReactorFunction {
				Date timeout;
				public Job(Date timeout) {
					this.timeout = timeout;
				}
				@Override
				public Object execute() throws Exception {
					_handle_rto_timer(timeout);
					return null;
				}

			}
			this.getReactor().schedule(new Job(timeout), delay);
			_timers.add(timeout);
		}
	}

	void _handle_rto_timer(Date timeout) {

		Date cur_time = new Date();

		// Remove all expired timers
		LinkedList<Date> _expired = new LinkedList<Date>();
		for (Date _t_out: _timers)
			if (_t_out.compareTo(cur_time) <= 0)
				_expired.addLast(_t_out);
		for (Date _exp: _expired)
			_timers.remove(_exp);

		if (!_in_fl.isEmpty()) {
			// Resend all expired in-flight packages
			Enumeration<Long> _ipos = _in_fl.keys();
			while (_ipos.hasMoreElements()) {
				long _pos = _ipos.nextElement();
				InFlightPackage _in_fl_pkg = _in_fl.get(_pos);
				long resend_t = _in_fl_pkg.getTimeStamp().getTime();
				resend_t += Math.min(_in_fl_pkg.getDelay(), (long)_rto);
				if (resend_t <= cur_time.getTime()) {
					this._resend_package(_pos);
					// Back off the RTO timer, ref. RFC 2988
					_rto *= 2.0f;
					_rto = Math.min(_rto, MAX_RTO);
					_rto_num_backoff += 1;
					// Reset congestion window and threshold as per RFC 2581
					_ssthresh = Math.max(_in_fl.size()/2, 2);
					_cwnd = 1.0f;
				}
			}

			// Reset the RTO timer
			long _timeout = -1;
			_ipos = _in_fl.keys();
			while (_ipos.hasMoreElements()) {
				long _pos = _ipos.nextElement();
				InFlightPackage _in_fl_pkg = _in_fl.get(_pos);
				long _t_out = _in_fl_pkg.getTimeStamp().getTime() + Math.min(_in_fl_pkg.getDelay(), (long)_rto);
				if (_timeout < 0 || _t_out < _timeout)
					_timeout = _t_out;
			}
			if (_timeout >= 0)
				this._set_rto_timer(_timeout - cur_time.getTime());
		}
		else {
			boolean can_send = false;
			boolean should_force = false;

			// If send buffer saturated with no window, send 1 octet if RTO
			if (!_out_closed) {
				if (_sbuf.hasData() && _sbuf_pos == _send_lim)
					can_send = true;
			}

			// If waiting for ack_close, force ack if RTO timeout
			if (_out_sent_close && !_out_closed) {
				should_force = true;
				can_send = true;
			}

			// If waiting to send, process if RTO or if not set a timeout
			if (can_send) {
				if (_last_send_t == null || (cur_time.getTime()-_last_send_t.getTime()) >= _rto) {
					if (should_force)
						_force_ack = true;
					_send_packages();
					// Fake update to last_send_t to ensure new RTO offset
					_last_send_t = cur_time;
					// Back off the RTO timer
					_rto *= 2.0f;
					_rto = Math.min(_rto, MAX_RTO);
					_rto_num_backoff += 1;
				}
				else
					_set_rto_timer(_last_send_t.getTime() + (long)_rto - cur_time.getTime());
			}

		}

		// Invalidate the RSTT/RTTVAR data if too many RTO back-off, ref. optional approach in RFC 2988
		if (_rto_num_backoff >= RTO_INVALIDATE_BACKOFF) {
			_srtt = -1.0f;
			_rttvar = -1.0f;
		}
	}

	/**
	 * Close socket input.
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeInput(boolean clean) {
		if (_in_closed)
			return;
		else if (_out_closed) {
			this.closeIO(clean);
			return;
		}
		else {
			_in_closed = true;
			this.getReactor().log("VUDP: closed input only");
			_in_closed_clean = true;
			this._input_was_closed(clean);
		}
	}

	/**
	 * Close socket output.
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeOutput(boolean clean) {
		if (_out_closed)
			return;
		else if (_in_closed) {
			this.closeIO(clean);
			return;
		}
		else {
			_out_closed = true;
			this.getReactor().log("VUDP: closed output only");
			_out_closed_clean = clean;
			this._output_was_closed(clean);
		}
	}

	/**
	 * Close socket (input and output).
	 *
	 * @param clean if true output is closed cleanly
	 */
	public void closeIO(boolean clean) {
		if (!(_in_closed && _out_closed)) {
			boolean in_was_closed = _in_closed;
			boolean out_was_closed = _out_closed;
			_in_closed = true;
			_out_closed = true;

			// Send an 'ack' before close, to ensure status flags are sent
			if (!_failed) {
				_force_ack = true;
				this._send_packages();
			}

			try {
				channel.close();
			} catch (IOException e) {
				// SILENT
			}
			_sock_closed = true;
			this.getReactor().log("VUDP: closed");

			if (!in_was_closed) {
				_in_closed_clean = clean;
				this._input_was_closed(clean);
			}
			if (!out_was_closed) {
				_out_closed_clean = clean;
				this._output_was_closed(clean);
			}
			this.stopReading();
		}
	}

	/**
	 * Called internally if datagram received from invalid peer IP.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	protected void _handle_invalid_peer() {
	}

	/**
	 * Called internally if a datagram failed to authenticate.
	 *
	 * <p>Default does nothing, derived classes can override.</p>
	 */
	protected void _handle_invalid_hmac() {
	}

	/**
	 * Computes VUDPTransport HMAC for a payload.
	 *
	 * @param data datagram payload
	 * @param secret HMAC secret
	 * @return message HMAC
	 */
	byte[] _dgram_hmac(byte[] data, byte[] secret) {
		return _hash_cls.hmacDigestOf(secret,  data);
	}

	long _c_consume(VByteBuffer data) throws IOException {
		if (_out_closed || _ci_eod || _ci_producer == null || (_ci_lim_sent >= 0 && _ci_consumed >= _ci_lim_sent))
			throw new IOException("Consume error");
		else if (data.length() == 0)
			throw new IOException("No data");

		int max_cons = _sbuf_len - _sbuf.length();
		max_cons = Math.min(max_cons, (int)(_ci_lim_sent-_ci_consumed));

		byte[] indata = data.pop(max_cons);
		_sbuf.append(indata);
		_ci_consumed += indata.length;

		// Trigger package sending in case more data can be sent
		this._send_packages();

		// Re-evaluate consume limit
		long _lim = _ci_consumed + _sbuf_len - _sbuf.length() - _in_fl_num;
		_ci_lim_sent = Math.max(_ci_lim_sent, _lim);
		return _ci_lim_sent;
	}

	void _c_end_consume(boolean clean) {
		if (_out_closed || _ci_eod)
			return;

		_ci_eod = true;
		_ci_eod_clean = clean;
		this._send_packages();
	}

	void _c_abort() {
		if (!_ci_aborted) {
			_ci_aborted = true;
			_ci_eod = true;
			_ci_consumed = 0L;
			_ci_lim_sent = 0L;
			_sbuf.clear();
			if (!_out_closed)
				this.closeOutput(true);
			if (_ci_producer != null) {
				_ci_producer.abort();
				this._c_detach(true);
			}
		}
	}

	void _c_attach(VByteProducer producer, boolean safe)
		throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteProducer producer;
				public Func(VByteProducer producer) {
					this.producer = producer;
				}
				@Override
				public Object execute() throws Exception {
					_c_attach(producer, true);
					return null;
				}
			}
			reactor.schedule(new Func(producer));
			return;
		}
		if (_ci_producer == producer)
			return;
		else if (_ci_producer != null)
			throw new IOException("Producer already attached");

		_ci_producer = producer;
		_ci_consumed = 0L;
		_ci_lim_sent = 0L;
		producer.attach(this.getConsumer(), true);

		// Send a produce limit if connection has been validated
		if (_validated) {
			_ci_lim_sent = _sbuf_len - _sbuf.length();
			producer.canProduce(_ci_lim_sent);
		}

		// Notify attached chain
		try {
			producer.getControl().notifyConsumerAttached(this.getConsumer());
		} catch (VIOMissingControl e) {
			// SILENT
		}
	}

	void _c_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_c_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}

		if (_ci_producer != null) {
			VByteProducer prod = _ci_producer;
			_ci_producer = null;
			prod.detach();
			_ci_consumed = 0;
			_ci_lim_sent = 0;
		}
	}

	void _output_was_closed(boolean clean) {
		// No more output will be written, abort consumer
		this._c_abort();
	}

	VIOControl _c_get_control() {
		return new VIOControl();
	}

	VByteProducer _c_get_producer() {
		return _ci_producer;
	}

	void _p_can_produce(long limit) throws IOException {
		if (_pi_consumer == null)
			throw new IOException("No connected consumer");

		boolean limit_changed = false;
		if (limit < 0) {
			if (_pi_prod_lim >= 0 && _pi_produced >= _pi_prod_lim)
				limit_changed = true;
			_pi_prod_lim = limit;
		}
		else {
			if (_pi_prod_lim >= 0 && _pi_prod_lim < limit) {
				if (_pi_produced >= _pi_prod_lim)
					limit_changed = true;
				_pi_prod_lim = limit;
			}
		}

		if (limit_changed) {
			// Limits changed, trigger a (possible) produce operation
			class Job implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_do_produce();
					return null;
				}
			}
			this.getReactor().schedule(new Job());
		}
	}

	void _p_abort() {
		if (!_pi_aborted) {
			_pi_aborted = true;
			_pi_produced = 0L;
			_pi_prod_lim = 0L;
			if (!_in_closed)
				this.closeInput(true);
			if (_pi_consumer != null) {
				_pi_consumer.abort();
				this._p_detach(true);
			}
		}
	}

	void _p_attach(VByteConsumer consumer, boolean safe) throws IOException {
		if (!safe) {
			class Func implements VReactorFunction {
				VByteConsumer consumer;
				public Func(VByteConsumer consumer) {
					this.consumer = consumer;;
				}
				@Override
				public Object execute() throws Exception {
					_p_attach(consumer, true);
					return null;
				}
			}
			reactor.schedule(new Func(consumer));
			return;
		}
		if (_pi_consumer == consumer)
			return;
		else if (_pi_consumer != null)
			throw new IOException("Consumer already attached");

		_pi_consumer = consumer;
		_pi_produced = 0L;
		_pi_prod_lim = 0L;
		consumer.attach(this.getProducer(), true);

		// Notify attached chain
		try {
			consumer.getControl().notifyProducerAttached(this.getProducer());
		} catch (VIOMissingControl e) {
			// SILENT
		}

		// If already connected, schedule notification of 'connected' status
		if (_validated) {
			VIOControl control = consumer.getControl();
			class Job implements VReactorFunction {
				VIOControl control;
				public Job(VIOControl control) {
					this.control = control;
				}
				@Override
				public Object execute() throws Exception {
					VSocketPeer _peer = VSocketPeer.fromAddress(address);
					try {
						control.notifyConnected(_peer);
					} catch (VIOMissingControl e) {
						// SILENT
					}
					return null;
				}
			}
			this.getReactor().schedule(new Job(control));
		}
	}

	void _p_detach(boolean safe) {
		if (!safe) {
			class Func implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_p_detach(true);
					return null;
				}
			}
			reactor.schedule(new Func());
			return;
		}
		if (_pi_consumer != null) {
			VByteConsumer cons = _pi_consumer;
			_pi_consumer = null;
			cons.detach();
			_pi_produced = 0L;
			_pi_prod_lim = 0L;
		}
	}

	void _input_was_closed(boolean clean) {
		if (_pi_consumer != null) {
			// Notify consumer about end-of-data
			_pi_consumer.endConsume(clean);
		}
		else {
			this._p_abort();
		}
	}

	VIOControl _p_get_control() {
		class Control extends VIOControl {
			VUDPTransport transport;
			public Control(VUDPTransport transport) {
				this.transport = transport;
			}
			@Override
			public void requestProducerState(VConsumer consumer)
					throws VIOMissingControl {
				// Send notification of socket connect status
				class Job implements VReactorFunction {
					VConsumer consumer;
					public Job(VConsumer consumer) {
						this.consumer = consumer;
					}
					@Override
					public Object execute() throws Exception {
						if (transport._validated) {
							VSocketPeer _peer = VSocketPeer.fromAddress(address);
							try {
								consumer.getControl().notifyConnected(_peer);
							}
							catch (VIOMissingControl e) {
								// SILENT
							}
						}
						return null;
					}
				}
				this.transport.getReactor().schedule(new Job(consumer));
			}
		}
		return new Control(this);
	}

	VByteConsumer _p_get_consumer() {
		return _pi_consumer;
	}

	// Handles validation when peer protocol handshake is not yet completed
	void _validate() {
		if (!_validated && _peer_validated && _peer_acked_hello) {
			_validated = true;
			this.getReactor().log("VUDP: connection validated");

			// Re-initialize congestion window and slow start threshold, so
			// performance is not penalized by handshake timeouts
			_cwnd = Math.max(_cwnd, 2);
			_ssthresh = Math.max(_ssthresh, SSTHRESH);

			// If connected, notify connected consumer
			if (_pi_consumer != null) {
				class Job implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						VSocketPeer _peer = VSocketPeer.fromAddress(address);
						try {
							_pi_consumer.getControl().notifyConnected(_peer);
						}
						catch (VIOMissingControl e) {
							// SILENT
						}
						return null;
					}
				}
				this.getReactor().schedule(new Job());
			}

			// Schedule a send iteration, to advertise new window size
			class Job implements VReactorFunction {
				@Override
				public Object execute() throws Exception {
					_send_packages();
					return null;
				}
			}
			this.getReactor().schedule(new Job());

			// If a producer is connected, send produce limit
			if (_ci_producer != null) {
				_ci_lim_sent = _sbuf_len - _sbuf.length();
				try {
					_ci_producer.canProduce(_ci_lim_sent);
				} catch (IOException e) {
					this._c_abort();
				}
			}
		}
	}

	void _do_produce() {
		if (_pi_consumer == null)
			return;

		if (0 <= _pi_prod_lim && _pi_prod_lim <= _pi_produced)
			return;
		else if (!_validated)
			return;

		if (_rbuf.hasData()) {
			int old_len = _rbuf.length();
			try {
				_pi_prod_lim = _pi_consumer.consume(_rbuf);
			} catch (IOException e) {
				this._p_abort();
				return;
			}
			int _prod = old_len - _rbuf.length();
			_pi_produced += _prod;
			_rbuf_spos += _prod;

			// Perform a send iteration in case peer limits can be updated
			this._send_packages();

			// If more data can be produced, schedule another iteration
			if (_rbuf.hasData() && !(0 <= _pi_prod_lim && _pi_prod_lim <= _pi_produced)) {
				class Job implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						_do_produce();
						return null;
					}
				}
				this.getReactor().schedule(new Job());
			}
		}
	}

	/**
	 * Fails a connection.
	 *
	 * @param msg failure message
	 */
	public void _fail(String msg) {
		if (!_failed) {
			if (msg != null)
				this.getReactor().log("VUDP: connection failed, " + msg);
			else
				this.getReactor().log("VUDP: connection failed");
			this._c_abort();
			this._p_abort();
			this.closeIO(false);
			_failed = true;
		}
	}

	class InFlightPackage {
		byte[] data;
		Date t_stamp;
		long delay;
		int retries = 0;

		public InFlightPackage(byte[] data, Date timeStamp, long delay) {
			this.data = data;
			this.t_stamp = timeStamp;
			this.delay = delay;
		}

		public byte[] getData() {
			return data;
		}

		public long getDelay() {
			return delay;
		}

		public void setDelay(long delay) {
			this.delay = delay;
		}

		public Date getTimeStamp() {
			return t_stamp;
		}

		public void setTimeStamp(Date timeStamp) {
			t_stamp = timeStamp;
		}

		public int getRetries() {
			return retries;
		}

		public void addRetry() {
			retries += 1;
		}
	}

	class TransportConsumer implements VByteConsumer {

		VUDPTransport agent;

		public TransportConsumer(VUDPTransport agent) {
			this.agent = agent;
		}

		@Override
		public long consume(VByteBuffer data) throws IOException {
			return agent._c_consume(data);
		}

		@Override
		public void endConsume(boolean clean) {
			agent._c_end_consume(clean);
		}

		@Override
		public void abort() {
			agent._c_abort();
		}

		@Override
		public void attach(VByteProducer producer) throws IOException {
			this.attach(producer, false);
		}

		@Override
		public void attach(VByteProducer producer, boolean safe)
				throws IOException {
			agent._c_attach(producer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._c_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._c_get_control();
		}

		@Override
		public VByteProducer getProducer() {
			return agent._c_get_producer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}

	class TransportProducer implements VByteProducer {

		VUDPTransport agent;

		public TransportProducer(VUDPTransport agent) {
			this.agent = agent;
		}

		@Override
		public void canProduce(long limit) throws IOException {
			agent._p_can_produce(limit);
		}

		@Override
		public void abort() {
			agent._p_abort();
		}

		@Override
		public void attach(VByteConsumer consumer) throws IOException {
			this.attach(consumer, false);
		}

		@Override
		public void attach(VByteConsumer consumer, boolean safe)
				throws IOException {
			agent._p_attach(consumer, safe);
		}

		@Override
		public void detach() {
			this.detach(false);
		}

		@Override
		public void detach(boolean safe) {
			agent._p_detach(safe);
		}

		@Override
		public VIOControl getControl() {
			return agent._p_get_control();
		}

		@Override
		public VByteConsumer getConsumer() {
			return agent._p_get_consumer();
		}

		@Override
		public VReactor getReactor() {
			return agent.getReactor();
		}
	}

}
