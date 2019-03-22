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
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCall;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.processor.VProcessor;
import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VExceptionProxy;
import org.versile.crypto.VCryptoException;
import org.versile.orb.entity.VCallError;
import org.versile.orb.entity.VException;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.external.Publish;
import org.versile.orb.external.VExternal;
import org.versile.orb.link.VLinkException;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.VByteIOPair;
import org.versile.reactor.io.link.VLinkAgent;
import org.versile.reactor.io.link.VLinkAgentConfig;
import org.versile.reactor.io.vec.VEntityChannel;
import org.versile.reactor.io.vop.VOPBridge;
import org.versile.reactor.io.vop.VOPBridge.Transport;
import org.versile.reactor.io.vop.VOPClientBridge;
import org.versile.reactor.io.vop.VOPServerBridge;
import org.versile.reactor.io.vts.VSecureChannelConfig;
import org.versile.reactor.io.vts.VSecureClient;
import org.versile.reactor.io.vts.VSecureServer;


/**
 * A VUDP Relay client connecter for negotiating a VOP link.
 */
public class VUDPRelayedVOPConnecter extends VExternal {

	boolean _is_client;
	VPrivateCredentials _credentials;
	VObject _gateway;
	VCall<VProxy> _peer_gw;

	String _connect_host = null;       // Host for initial UDP connect
	int _connect_port = 0;             // Port for initial UDP connect
	byte[] _connect_l_token = null;    // Local token for initial UDP connect
	byte[] _connect_r_token = null;    // Remote token for initial UDP connect

	String _external_udp_host = null;  // External UDP address host of local UDP port
	int _external_udp_port = 0;        // External UDP address port of local UDP port

	DatagramChannel _channel = null;   // UDP socket channel for sending/receiving datagrams
	VLinkAgent _link = null;

	float _udp_resend_t = 0.0f;        // Current UDP resend time (seconds)
	float _udp_max_resend = 60.0f;     // Maximum UDP resend time (seconds)
	Date _timeout = null;              // Timeout for handshake
	Date _start_time = null;           // Start time for UDP handshake
	Timer _timer = null;               // Timer for timeouts

	int _num_confirm_calls = 0;        // Number of calls to confirm_udp
	int _num_link_calls = 0;           // Number of calls to link_to_peer

	VUDPRelayedVOPConfig _config;

	/**
	 * Set up connecter.
	 *
	 * <p>Implements the VUDP Relay standard for negotiating a VOP link over a
	 * Versile UDP Transport.</p>
	 *
	 * @param isClient if True set up as a VOP client, otherwise as a server
	 * @param credentials link credentials (or null)
	 * @param gateway link gateway object to send to peer (or null)
	 */
	public VUDPRelayedVOPConnecter(boolean isClient, VPrivateCredentials credentials, VObject gateway) {
		this._construct(isClient, credentials, gateway, null);
	}

	/**
	 * Set up connecter.
	 *
	 * <p>Implements the VUDP Relay standard for negotiating a VOP link over a
	 * Versile UDP Transport.</p>
	 *
	 * @param isClient if True set up as a VOP client, otherwise as a server
	 * @param credentials link credentials (or null)
	 * @param gateway link gateway object to send to peer (or null)
	 * @param config additional configuration (or null)
	 */
	public VUDPRelayedVOPConnecter(boolean isClient, VPrivateCredentials credentials, VObject gateway,
			VUDPRelayedVOPConfig config) {
		this._construct(isClient, credentials, gateway, config);
	}

	void _construct(boolean isClient, VPrivateCredentials credentials, VObject gateway,
			VUDPRelayedVOPConfig config) {
		_is_client = isClient;
		_credentials = credentials;
		_gateway = gateway;
		if (config == null)
			config = new VUDPRelayedVOPConfig();
		_config = config;

		// Asynchronous call result for link gateway
		class Call extends VCall<VProxy> {
			WeakReference<VUDPRelayedVOPConnecter> w_connecter;
			public Call(VUDPRelayedVOPConnecter connecter) {
				w_connecter = new WeakReference<VUDPRelayedVOPConnecter>(connecter);
			}
			@Override
			protected void _cancel() {
				VUDPRelayedVOPConnecter connecter = w_connecter.get();
				if (connecter != null)
					connecter._cancel();
			}
		}
		_peer_gw = new Call(this);
	}

	/**
	 * External method for confirming external UDP address.
	 *
	 * <p>Called during a VUDP Relay handshake. Informs the connecter of its external UDP
	 * address as seen by the relay. Connecter must stop sending UDP packages to the relay
	 * when this method is called.</p>
	 *
	 * <p>Should normally not be called internally, but only be called remotely by a
	 * VUDP Relay which is relaying the connecter.</p>
	 *
	 * @param host external UDP host IP address
	 * @param port external UDP port number
	 * @throws VExceptionProxy call error
	 */
	@Publish(show=true, ctx=false)
	public void confirm_udp(String host, int port)
		throws VExceptionProxy {
		if (_num_confirm_calls != 0)
			throw new VException("May only call confirm_udp once").getProxy();
		_num_confirm_calls += 1;
		if (port < 1024 || port > 65535)
			throw new VException("Illegal port number").getProxy();
		// This will stop UDP handshake re-transmission
		_external_udp_host = host;
		_external_udp_port = port;
	}

	/**
	 * External method for instructing connecter to initiate peer connection.
	 *
	 * <p>Called during a VUDP Relay handshake. Informs the connecter of UDP address
	 * and connection parameters for making a peer Versile UDP Transport connection.</p>
	 *
	 * <p>Should normally not be called internally, but only be called remotely by a
	 * VUDP Relay which is relaying the connecter.</p>
	 *
	 * @param host peer UDP host IP address
	 * @param port peer UDP port number
	 * @param l_sec local side HMAC secret
	 * @param r_sec peer side HMAC secret
	 * @throws VExceptionProxy call error
	 * @throws VCallError error resolving call
	 */
	@Publish(show=true, ctx=false)
	public void link_to_peer(String host, int port, byte[] l_sec, byte[] r_sec)
		throws VExceptionProxy, VCallError {
		if (_num_link_calls != 0)
			throw new VException("May only call link_to_peer once").getProxy();
		_num_link_calls += 1;
		if (host == null || l_sec == null || r_sec == null)
			throw new VException("Illegal argument(s)").getProxy();
		if (port < 1024 || port > 65535)
			throw new VException("Illegal port number").getProxy();
		if (l_sec.length > 32 || r_sec.length > 32)
			throw new VException("Token(s) exceed 32 byte limit").getProxy();

		VProcessor _processor = _config.getProcessor();
		VReactor _reactor = _config.getReactor();
		VLinkAgentConfig _link_conf = _config.getLinkConfig();
		VLinkAgent link;
		try {
			link = new VLinkAgent(_gateway, _processor, _reactor, _link_conf);
		} catch (VLinkException e2) {
			throw new VCallError(e2);
		}

		// Perform filtering on peer UDP address; if denied reject connection
		if (_config.getUdpFilter() != null && !_config.getUdpFilter().allowPeer(host, port)) {
			if (_timer != null)
				_timer.cancel();
			_peer_gw.silentPushException(new IOException("Peer denied by UDP filter"));
			return;
		}

		// Validate legal call sequence and client HMAC token
		if ( _connect_l_token == null || _connect_r_token == null) {
			if (_timer != null)
				_timer.cancel();
			_peer_gw.silentPushException(new IOException("Invalid call sequence"));
		}

		if (!_is_client && (_credentials == null || _credentials.getKeyPair() == null) && (_config.enableVts || _config.enableTls))
			throw new VCallError("Server mode with VTS/TLS requires a key");

		// Set up entity channel interface to link
		VEntityChannel vec = new VEntityChannel(link.getReactor(), link, _config.getVecConfig());
		try {
			vec.getEntityIOPair().attach(link.getIOPair());
		} catch (IOException e1) {
			throw new VCallError("Could not set up link");
		}

		// Set up VOP multiplexer
		VOPBridge vop;
		VOPBridge.TransportFactory vts_factory = null;
		VOPBridge.TransportFactory tls_factory = null; // TLS not yet supported
		if (_is_client) {
			if (_config.isEnableVts()) {
				class VTSTransport extends VOPBridge.TransportFactory {
					VReactor reactor;
					VPrivateCredentials credentials;
					VSecureChannelConfig config;
					public VTSTransport(VReactor reactor, VPrivateCredentials credentials, VSecureChannelConfig config) {
						this.reactor = reactor;
						this.credentials = credentials;
						this.config = config;
					}
					@Override
					public Transport createTransport() {
						VSecureClient vts = null;
						try {
							vts = new VSecureClient(reactor, credentials, config);
						} catch (VCryptoException e) {
							return null;
						}
						return new VOPBridge.Transport(vts.getCiphertextIOPair(), vts.getPlaintextIOPair());
					}
				}
				vts_factory = new VTSTransport(link.getReactor(), _credentials, _config.getVtsConfig());
			}
			try {
				vop = new VOPClientBridge(link.getReactor(), vec.getByteIOPair(), vts_factory, tls_factory, _config.isAllowInsecure());
			} catch (IOException e) {
				throw new VCallError("Could not set up link");
			}
		}
		else {
			if (_config.isEnableVts()) {
				class VTSTransport extends VOPBridge.TransportFactory {
					VReactor reactor;
					VPrivateCredentials credentials;
					VSecureChannelConfig config;
					public VTSTransport(VReactor reactor, VPrivateCredentials credentials, VSecureChannelConfig config) {
						this.reactor = reactor;
						this.credentials = credentials;
						this.config = config;
					}
					@Override
					public Transport createTransport() {
						VSecureServer vts = null;
						try {
							vts = new VSecureServer(reactor, credentials, config);
						} catch (VCryptoException e) {
							return null;
						}
						return new VOPBridge.Transport(vts.getCiphertextIOPair(), vts.getPlaintextIOPair());
					}
				}
				vts_factory = new VTSTransport(link.getReactor(), _credentials, _config.getVtsConfig());
			}
			try {
				vop = new VOPServerBridge(link.getReactor(), vec.getByteIOPair(), vts_factory, tls_factory, _config.isAllowInsecure());
			} catch (IOException e) {
				throw new VCallError("Could not set up link");
			}
		}
		VByteIOPair _link_io = vop.getExternalIOPair();

		InetSocketAddress _addr;
		try {
			_addr = new InetSocketAddress(host, port);
		}
		catch (Exception e) {
			throw new VException("Invalid host:port").getProxy();
		}
		VUDPTransport transport = new VUDPTransport(link.getReactor(), _channel, _addr, l_sec, r_sec);
		try {
			transport.getIOPair().attach(_link_io);
		} catch (IOException e) {
			throw new VCallError("Internal error, could not connect link and transport");
		}

		// Set up callbacks for returning link gateway
		class ResHandler implements VCallResultHandler<VProxy> {
			@Override
			public void callback(VProxy result) {
				_peer_gw.silentPushResult(result);
				if (_timer != null)
					_timer.cancel();
			}
		}
		class ExcHandler implements VCallExceptionHandler {
			@Override
			public void callback(Exception e) {
				_peer_gw.silentPushException(e);
				if (_timer != null)
					_timer.cancel();
			}
		}
		link.nowaitPeerGateway().addHandlerPair(new ResHandler(), new ExcHandler());
		_link = link;
	}

	/**
	 * Initiates a UDP handshake with a relay.
	 *
	 * <p>Sets up with minimim resend time 0.1 seconds, maximum resend time 5.0 seconds, and
	 * timeout 30.0 seconds.</p>
	 *
	 * @see #connectUdp
	 */
	public void connectUdp(String host, int port, byte[] l_token, byte[] r_token)
			throws IOException  {
		this.connectUdp(host, port, l_token, r_token, 0.1f, 5.0f, 30.0f);
	}

	/**
	 * Initiates a UDP handshake with a relay.
	 *
	 * <p>If a UDP filter is registered on the connecter object, the
	 * filter is applied to host/port before connecting, and an
	 * exception is raised if the connection is rejected by the filter.</p>
	 *
	 * @param host UDP handshake hostname
	 * @param port UDP handshake port
	 * @param l_token local UDP handshake token
	 * @param r_token relay UDP handshake token
	 * @param min_resend minimum re-transmission time in seconds
	 * @param max_resend maximum re-transmission time in seconds
	 * @param timeout handshake timeout in seconds (negative if no timeout)
	 * @throws IOException
	 */
	public void connectUdp(String host, int port, byte[] l_token, byte[] r_token, float min_resend,
					   float max_resend, float timeout)
			throws IOException {
		if (_config.getUdpFilter() != null && _config.getUdpFilter().allowRelay(host, port))
			throw new IOException("UDP host/port rejected by connecter UDP filter");

		_connect_host = host;
		_connect_port = port;
		_connect_l_token = l_token;
		_connect_r_token = r_token;
		_udp_resend_t = min_resend;
		_udp_max_resend = max_resend;
		_start_time = new Date();
		if (timeout >= 0)
			_timeout = new Date(_start_time.getTime() + (long)(timeout*1000));
		else
			_timeout = null;

		_start_time = new Date();
		_channel = DatagramChannel.open();
		_channel.configureBlocking(false);
		_channel.socket().bind(null);
		this._send_udp_pkg();

		_timer = new Timer();
		class Task extends TimerTask {
			@Override
			public void run() {
				_tick();
			}
		}
		_timer.schedule(new Task(), (long)(_udp_resend_t*1000));
	}

	/**
	 * Get peer gateway reference.
	 *
	 * @return peer gateway reference
	 */
	public VCall<VProxy> getPeerGateway() {
		return this._peer_gw;
	}

	/**
	 * Handler for re-transmit and timeout handler.
	 */
	void _tick() {
		if (_peer_gw.hasResult())
			return;
		Date cur_time = new Date();
		if (_timeout != null && cur_time.compareTo(_timeout) >= 0) {
			this._cancel();
			return;
		}

		// If needed schedule new timer
		float _tick_t = -1.0f;
		if (_external_udp_host == null) {
			try {
				_send_udp_pkg();
			} catch (IOException e) {
				this._cancel();
				return;
			}
			_udp_resend_t *= 2;
			_udp_resend_t = Math.min(_udp_resend_t, _udp_max_resend);
			_tick_t = _udp_resend_t;
		}
		if (_timeout != null) {
			if (_tick_t < 0)
				_tick_t = (_timeout.getTime() - cur_time.getTime())/1000.0f;
			else
				_tick_t = Math.min(_tick_t, (_timeout.getTime() - cur_time.getTime())/1000.0f);
		}
		if (_tick_t >= 0) {
			class Task extends TimerTask {
				@Override
				public void run() {
					_tick();
				}
			}
			_timer.schedule(new Task(), (long)(1000*_tick_t));
		}
	}

	void _send_udp_pkg()
		throws IOException {
		InetSocketAddress addr = new InetSocketAddress(_connect_host, _connect_port);
		VByteBuffer _tmp = new VByteBuffer();
		_tmp.append(_connect_l_token);
		_tmp.append(_connect_r_token);
		ByteBuffer _buf = ByteBuffer.wrap(_tmp.popAll());
		int num_sent = _channel.send(_buf,  addr);
		if (num_sent > 0 && num_sent != (_connect_l_token.length + _connect_r_token.length))
			throw new IOException();
	}

	/**
	 * Cancels ongoing connect operation.
	 */
	void _cancel() {
		if (_timer != null)
			_timer.cancel();
		if (_link != null)
			_link.shutdown(true);
		if (_channel != null)
			try {
				_channel.close();
			} catch (IOException e) {
				// SILENT
			}
	}
}
