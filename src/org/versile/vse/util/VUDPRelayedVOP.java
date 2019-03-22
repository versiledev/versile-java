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

package org.versile.vse.util;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCall;
import org.versile.common.call.VCallCancelled;
import org.versile.common.call.VCallException;
import org.versile.common.call.VCallExceptionHandler;
import org.versile.common.call.VCallResultHandler;
import org.versile.common.util.VCombiner;
import org.versile.common.util.VObjectIdentifier;
import org.versile.common.util.VCombiner.Pair;
import org.versile.crypto.rand.VSecureRandom;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEncoderData;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VEntityWriterException;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTagged;
import org.versile.orb.entity.VTaggedParseError;
import org.versile.orb.module.VModuleDecoder;
import org.versile.orb.module.VModuleError;
import org.versile.reactor.io.vudp.VUDPRelayedVOPConfig;
import org.versile.reactor.io.vudp.VUDPRelayedVOPConnecter;
import org.versile.vse.VSECode;


/**
 * A VUDP Relay to a VOP link.
 *
 * <p>The held handler is a reference to a handler which implements the VUDP Relay
 * 'Client Relay' interface.</p>
 *
 * <p>The relayed Versile UDP Transport must transport a VOP link
 * connection. As VOP connects two peers taking a role of 'client' and 'server',
 * each side of the connection must know which role to perform. {@link #isVopClient}
 * defines which role this side of the connection should take.</p>
 */
public class VUDPRelayedVOP extends VUDPRelay {

	/**
	 * VSE code for the VUDPRelay type.
	 */
	static VSECode VSE_CODE = new VSECode(new String[] {"util", "udp_vop"},
			new VInteger[] {new VInteger(0), new VInteger(8)},
			new VObjectIdentifier(4, 3));

	boolean _is_vop_client;

	/**
	 * Set up a relay.
	 *
	 * @param handler handler to object implementing relay service
	 * @param isVopClient if true take VOP client role, otherwise server
	 */
	public VUDPRelayedVOP(VProxy handler, boolean isVopClient) {
		super(handler);
		_is_vop_client = isVopClient;
	}

	// Set after initiating connect
	byte[] _l_token;
	VUDPRelayedVOPConnecter _connecter;
	VCall<Object> _rel_params;
	Timer _timer = null;
	float _timeout = -1f;
	Date _conn_start_t = null;

	/**
	 * Resolves (blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with no credentials, no local gateway, and a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @return peer link gateway
	 * @throws VCallException error establishing relayed connection
	 */
	public VProxy relayVop(boolean isClient)
			throws VCallException {
		return this.relayVop(isClient, null, null, null, 30.0f);
	}

	/**
	 * Resolves (blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with no local gateway and a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @return peer link gateway
	 * @throws VCallException error establishing relayed connection
	 */
	public VProxy relayVop(boolean isClient, VPrivateCredentials credentials)
			throws VCallException {
		return this.relayVop(isClient, credentials, null, null, 30.0f);
	}

	/**
	 * Resolves (blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @return peer link gateway
	 * @throws VCallException error establishing relayed connection
	 */
	public VProxy relayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway)
			throws VCallException {
		return this.relayVop(isClient, credentials, gateway, null, 30.0f);
	}

	/**
	 * Resolves (blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @param config additional config (or null)
	 * @return peer link gateway
	 * @throws VCallException error establishing relayed connection
	 */
	public VProxy relayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway, VUDPRelayedVOPConfig config)
			throws VCallException {
		return this.relayVop(isClient, credentials, gateway, config, 30.0f);
	}

	/**
	 * Resolves (blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 *  and negotiates a Versile Object Protocol link with the peer over the UDP based
	 *  transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @param config additional config (or null)
	 * @param timeout connection handshake timeout in seconds (no timeout if negative)
	 * @return peer link gateway
	 * @throws VCallException error establishing relayed connection
	 */
	public VProxy relayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway, VUDPRelayedVOPConfig config, float timeout)
			throws VCallException {
		VCall<VProxy> async_result = this.nowaitRelayVop(isClient, credentials, gateway, config, timeout);
		try {
			return async_result.getResult();
		} catch (VCallCancelled e) {
			throw new VCallException(e);
		}
	}

	/**
	 * Resolves (non-blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with no credentials, no local gateway, and a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @return reference to peer link gateway
	 */
	public VCall<VProxy> nowaitRelayVop(boolean isClient) {
		return this.nowaitRelayVop(isClient, null, null, null, 30.0f);
	}

	/**
	 * Resolves (non-blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with no local gateway and a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @return reference to peer link gateway
	 */
	public VCall<VProxy> nowaitRelayVop(boolean isClient, VPrivateCredentials credentials) {
		return this.nowaitRelayVop(isClient, credentials, null, null, 30.0f);
	}

	/**
	 * Resolves (non-blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @return reference to peer link gateway
	 */
	public VCall<VProxy> nowaitRelayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway) {
		return this.nowaitRelayVop(isClient, credentials, gateway, null, 30.0f);
	}

	/**
	 * Resolves (non-blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 * and negotiates a Versile Object Protocol link with the peer over the UDP based
	 * transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * <p>Connects with a 30 second timeout.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @param config additional config (or null)
	 * @return reference to peer link gateway
	 */
	public VCall<VProxy> nowaitRelayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway, VUDPRelayedVOPConfig config) {
		return this.nowaitRelayVop(isClient, credentials, gateway, config, 30.0f);
	}

	/**
	 * Resolves (non-blocking) a relayed Versile UDP Transport connection as a VOP link.
	 *
	 * <p>The method call negotiates a Versile UDP Transport with a peer via the UDP relay,
	 *  and negotiates a Versile Object Protocol link with the peer over the UDP based
	 *  transport. The peer gateway of the negotiated link is returned as a result.</p>
	 *
	 * @param isClient if true connect as VOP client, otherwise VOP server
	 * @param credentials VOP credentials (or null)
	 * @param gateway gateway object to send to peer (or null)
	 * @param config additional config (or null)
	 * @param timeout connection handshake timeout in seconds (no timeout if negative)
	 * @return reference to peer link gateway
	 */
	public VCall<VProxy> nowaitRelayVop(boolean isClient, VPrivateCredentials credentials, VObject gateway, VUDPRelayedVOPConfig config, float timeout) {
		_timeout = timeout;

		_connecter = new VUDPRelayedVOPConnecter(isClient, credentials, gateway, config);

		// Initiate handshake
		_conn_start_t = new Date();
		_l_token = new VSecureRandom().getBytes(32);
		_rel_params = _handler.nowait("connect", _l_token, _connecter);

		// Set timer for relay connect timeout
		if (timeout >= 0) {
			class Task extends TimerTask {
				@Override
				public void run() {
					_connecter.getPeerGateway().cancel();
					_rel_params.cancel();
				}
			}
			_timer = new Timer();
			_timer.schedule(new Task(), (long)(1000*timeout));
		}

		// Set handlers for performing connect
		class ResHandler implements VCallResultHandler<Object> {
			@Override
			public void callback(Object result) {
				String host;
				int port;
				byte[] r_token;
				try {
					Object[] _objs = (Object[]) result;
					if (_objs.length != 3)
						throw new Exception();
					host = VString.nativeOf(_objs[0]);
					port = VInteger.nativeOf(_objs[1]).intValue();
					if (port < 1024 || port > 65535) {
						throw new Exception();
					}
					r_token = VBytes.nativeOf(_objs[2]);
				}
				catch (Exception e) {
					_connecter.getPeerGateway().cancel();
					return;
				}
				if (_timer != null)
					_timer.cancel();
				float tm_out = _timeout;
				if (tm_out >= 0)
					tm_out = Math.max(_timeout-(new Date().getTime()-_conn_start_t.getTime())/1000.f, 0);
				try {
					_connecter.connectUdp(host, port, _l_token, r_token, 0.1f, 10.0f, tm_out);
				} catch (IOException e) {
					// SILENT
				}
			}
		}
		class ExcHandler implements VCallExceptionHandler {
			@Override
			public void callback(Exception e) {
				if (_timer != null)
					_timer.cancel();
				_rel_params.cancel();
				_connecter.getPeerGateway().cancel();
			}
		}
		_rel_params.addHandlerPair(new ResHandler(), new ExcHandler());

		return _connecter.getPeerGateway();
	}

	/**
	 * True if this connection side should be VOP client.
	 *
	 * @return true if VOP client, false if VOP server
	 */
	public boolean isVopClient() {
		return _is_vop_client;
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		return this._v_as_tagged(ctx)._v_encode(ctx, explicit);
	}

	@Override
	public VTagged _v_as_tagged(VIOContext ctx) {
		VEntity[] _tags = VUDPRelayedVOP.VSE_CODE.getTags(ctx);
		VEntity[] tags = new VEntity[_tags.length+1];
		for (int i = 0; i < _tags.length; i++)
			tags[i] = _tags[i];
		tags[tags.length-1] = new VBoolean(_is_vop_client);
		VEntity value = _handler.get();
		return new VTagged(value, tags);
	}

	/**
	 * Get VSE decoder for tag data.
	 *
	 * @return decoder
	 * @throws VTaggedParseError
	 */
	static public VModuleDecoder.Decoder _v_vse_decoder() {
		class Decoder extends VModuleDecoder.Decoder {
			@Override
			public Pair decode(Object value, Object[] tags) throws VModuleError {
				if (tags == null || tags.length != 1)
					throw new VModuleError("Encoding takes a single residual tags");
				boolean is_vop_client;
				try {
					is_vop_client = VBoolean.nativeOf(tags[0]);
				} catch (VEntityError e) {
					throw new VModuleError("Residual tag must be a boolean", e);
				}
				VProxy handler = null;
				try {
					handler = VProxy.valueOf(value);
				} catch (VEntityError e1) {
					throw new VModuleError("Encoding value must be an object reference", e1);
				}

				Vector<Object> comb_items = new Vector<Object>();
				comb_items.add(new VUDPRelayedVOP(handler, is_vop_client));
				return new VCombiner.Pair(VCombiner.identity(), comb_items);
			}
		}
		return new Decoder();
	}
}
