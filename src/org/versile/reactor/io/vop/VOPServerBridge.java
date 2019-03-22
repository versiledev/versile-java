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
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.versile.common.util.VByteBuffer;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;
import org.versile.reactor.io.VByteIOPair;


/**
 * Server-side channel interface for the VOP protocol.
 *
 * <p>Implements the server side of a VOP channel.</p>
 */
public class VOPServerBridge extends VOPBridge {

	int _HSHAKE_MAXLEN = 64;
	boolean _have_client_hello = false;
	VByteBuffer _buffer;
	TransportFactory _negotiated_factory = null;

	/**
	 * Set up channel VOP bridge.
	 *
	 * <p>See {@link VOPBridge} for information about constructor arguments.</p>
	 */
	public VOPServerBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory)
					throws IOException {
		super(reactor, vec, vtsFactory, tlsFactory, false);
		this.construct();
	}

	/**
	 * Set up channel VOP bridge.
	 *
	 * <p>See {@link VOPBridge} for information about constructor arguments.</p>
	 */
	public VOPServerBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory, boolean allowInsecure)
					throws IOException {
		super(reactor, vec, vtsFactory, tlsFactory, allowInsecure);
		this.construct();
	}

	void construct() {
		_buffer = new VByteBuffer();
	}

	@Override
	protected boolean isServer() {
		return true;
	}

	@Override
	protected void handshakeProducerAttached() {
		// Start listening for handshake message from client
		_ec_cons_lim = _HSHAKE_MAXLEN;
		try {
			_ec_producer.canProduce(_ec_cons_lim);
		} catch (IOException e) {
			this.handshakeAbort();
		}
	}

	@Override
	protected void handshakeConsume(VByteBuffer data) {
		if (_have_client_hello || !_handshaking || _handshake_error)
			return;

		while (data.hasData() && _buffer.length() < _HSHAKE_MAXLEN) {
			byte _data = data.pop(1)[0];
			_buffer.append(_data);
			_handshake_consumed += 1;
			if (_data == (byte)0x0a) {
				_have_client_hello = true;
				break;
			}
		}
		if (_buffer.length() >= _HSHAKE_MAXLEN && !_have_client_hello)
			this.handshakeAbort();

		if (_have_client_hello) {
			// Parse the received hello message
			byte[] hello = _buffer.pop(_buffer.length()-1);
			_buffer.popAll(); // Discard the final 0x0a byte

			String hello_str;
			try {
				hello_str = new String(hello, "ASCII");
			} catch (UnsupportedEncodingException e) {
				this.handshakeAbort();
				return;
			}
			if (!hello_str.startsWith("VOP_DRAFT-0.8 TRANSPORTS:")) {
				this.handshakeAbort();
				return;
			}
			String[] _protos = hello_str.substring(25).split(":");
			Set<String> protos = new HashSet<String>();
			for (String p: _protos) {
				if (p.equals("VTS") || p.equals("TLS") || p.equals("PLAIN"))
					if (!protos.contains(p)) {
						protos.add(p);
						continue;
					}
				this.handshakeAbort();
				return;
			}

			String proto;
			if (protos.contains("VTS") && vtsFactory != null) {
				proto = "VTS";
				_negotiated_factory = vtsFactory;
				this.getReactor().log("VOP: Negotiated VTS transport");
			}
			else if (protos.contains("TLS") && tlsFactory != null) {
				proto = "TLS";
				_negotiated_factory = tlsFactory;
				this.getReactor().log("VOP: Negotiated TLS transport");
			}
			else if (protos.contains("PLAIN") && allowInsecure) {
				proto = "PLAIN";
				_negotiated_factory = null;
				this.getReactor().log("VOP: Negotiated insecure (plaintext) transport");
			}
			else {
				this.handshakeAbort();
				return;
			}

			// Prepare protocol return message
			try {
				_buffer.append(("VOP_DRAFT-0.8 USE_TRANSPORT:" + proto).getBytes("ASCII"));
			} catch (UnsupportedEncodingException e) {
				// Should never happen
				throw new RuntimeException(e);
			}
			_buffer.append((byte)0x0a);

			// Initiate production
			if (_ep_prod_lim != 0) {
				class Job implements VReactorFunction {
					@Override
					public Object execute() throws Exception {
						handshakeCanProduce();
						return null;
					}
				}
				this.getReactor().schedule(new Job());
			}
		}

	}

	@Override
	protected void handshakeCanProduce() {
		if (!_handshaking || _handshake_error || !_have_client_hello)
			return;

		// Send handshake message
		if (_ep_consumer != null && (_ep_prod_lim < 0 || _ep_prod_lim > _handshake_produced)) {
			int old_len = _buffer.length();
			try {
				_ep_prod_lim = _ep_consumer.consume(_buffer);
			} catch (IOException e) {
				this.handshakeAbort();
				return;
			}
			_handshake_produced += old_len - _buffer.length();
			if (_buffer.isEmpty())
				this.handshakeComplete(_negotiated_factory);
		}
	}

}
