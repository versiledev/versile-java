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

import org.versile.common.util.VByteBuffer;
import org.versile.reactor.VReactor;
import org.versile.reactor.io.VByteIOPair;


/**
 * Client-side channel interface for the VOP protocol.
 *
 * <p>Implements the client side of a VOP channel.</p>
 */
public class VOPClientBridge extends VOPBridge {

	int _HSHAKE_MAXLEN = 64;
	boolean _sent_client_hello = false;
	boolean _have_server_response = false;
	VByteBuffer _buffer;

	/**
	 * Set up channel VOP bridge.
	 *
	 * <p>See {@link VOPBridge} for information about constructor arguments.</p>
	 */
	public VOPClientBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory)
					throws IOException {
		super(reactor, vec, vtsFactory, tlsFactory, false);
		this.construct();
	}

	/**
	 * Set up channel VOP bridge.
	 *
	 * <p>See {@link VOPBridge} for information about constructor arguments.</p>
	 */
	public VOPClientBridge(VReactor reactor, VByteIOPair vec, TransportFactory vtsFactory, TransportFactory tlsFactory, boolean allowInsecure)
					throws IOException {
		super(reactor, vec, vtsFactory, tlsFactory, allowInsecure);
		this.construct();
	}

	void construct() {
		// Set up client hello message based on enabled protocols
		try {
			_buffer = new VByteBuffer();
			_buffer.append("VOP_DRAFT-0.8 TRANSPORTS".getBytes("ASCII"));
			if (vtsFactory != null)
				_buffer.append(":VTS".getBytes("ASCII"));
			if (tlsFactory != null)
				_buffer.append(":TLS".getBytes("ASCII"));
			if (allowInsecure)
				_buffer.append(":PLAIN".getBytes("ASCII"));
			_buffer.append((byte)0x0a);
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean isServer() {
		return false;
	}

	@Override
	protected void handshakeProducerAttached() {
		if (_sent_client_hello) {
			_ec_cons_lim = _HSHAKE_MAXLEN;
			try {
				_ec_producer.canProduce(_ec_cons_lim);
			} catch (IOException e) {
				this.handshakeAbort();
			}
		}
	}

	@Override
	protected void handshakeConsume(VByteBuffer data) {
		if (!_sent_client_hello || !_handshaking || _handshake_error)
			return;

		while (data.hasData() && _buffer.length() < _HSHAKE_MAXLEN) {
			byte _data = data.pop(1)[0];
			_buffer.append(_data);
			_handshake_consumed += 1;
			if (_data == (byte)0x0a) {
				_have_server_response = true;
				break;
			}
		}
		if (_buffer.length() >= _HSHAKE_MAXLEN && !_have_server_response)
			this.handshakeAbort();

		if (_have_server_response) {
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
			if (!hello_str.startsWith("VOP_DRAFT-0.8 USE_TRANSPORT:")) {
				this.handshakeAbort();
				return;
			}
			String proto = hello_str.substring(28);
			if (proto.equals("VTS")) {
				if (vtsFactory != null)  {
					this.getReactor().log("VOP: Negotiated VTS transport");
					this.handshakeComplete(vtsFactory);
				}
				else
					this.handshakeAbort();
			}
			else if (proto.equals("VTS")) {
				if (tlsFactory != null)  {
					this.getReactor().log("VOP: Negotiated TLS transport");
					this.handshakeComplete(tlsFactory);
				}
				else
					this.handshakeAbort();
			}
			else if (proto.equals("PLAIN")) {
				if (allowInsecure)  {
					this.getReactor().log("VOP: Negotiated insecure (plaintext) transport");
					this.handshakeComplete(null);
				}
				else
					this.handshakeAbort();
			}
			else
				this.handshakeAbort();
		}
	}

	@Override
	protected void handshakeCanProduce() {
		if (!_handshaking || _handshake_error || _sent_client_hello)
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
				_sent_client_hello = true;
		}

		if (_sent_client_hello && _ec_producer != null) {
			_ec_cons_lim = _HSHAKE_MAXLEN;
			try {
				_ec_producer.canProduce(_ec_cons_lim);
			} catch (IOException e) {
				this.handshakeAbort();
				return;
			}
		}
	}
}
