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

import java.io.IOException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.crypto.VCryptoException;
import org.versile.crypto.VHash;
import org.versile.orb.entity.VBoolean;
import org.versile.orb.entity.VBytes;
import org.versile.orb.entity.VEntity;
import org.versile.orb.entity.VEntityError;
import org.versile.orb.entity.VIOContext;
import org.versile.orb.entity.VInteger;
import org.versile.orb.entity.VNone;
import org.versile.orb.entity.VString;
import org.versile.orb.entity.VTuple;
import org.versile.reactor.VReactor;
import org.versile.reactor.VReactorFunction;


/**
 * VTS client end-point.
 *
 * <p>Implements the client side of a Versile Transport Security channel.</p>
 */
public class VSecureClient extends VSecureChannel {

	enum _ProtocolStep {_SEND_PUBKEY, _GET_SERVER_SECRET};

	byte[] rand_c = null;
	byte[] rand_s = null;
	byte[] srand_c = null;
	byte[] srand_s = null;
	_ProtocolStep _proto_step = null;

	/**
	 * Set up secure channel.
	 *
	 * @param reactor owning reactor
	 * @param credentials credentials for authentication (or null)
	 * @throws VCryptoException
	 */
	public VSecureClient(VReactor reactor, VPrivateCredentials credentials)
			throws VCryptoException {
		super(reactor, credentials, new VSecureChannelConfig(), Role.CLIENT);
		_can_send_proto = true;
		if (keypair == null)
			if (identity != null || certs != null)
				throw new VCryptoException("Use of identity/certificates requires a key");
	}

	/**
	 * Set up secure channel.
	 *
	 * @param reactor owning reactor
	 * @param credentials credentials for authentication (or null)
	 * @param config channel config settings (defaults if null)
	 * @throws VCryptoException
	 */
	public VSecureClient(VReactor reactor, VPrivateCredentials credentials, VSecureChannelConfig config)
			throws VCryptoException {
		super(reactor, credentials, config, Role.CLIENT);
		_can_send_proto = true;
		if (keypair == null)
			if (identity != null || certs != null)
				throw new VCryptoException("Use of identity/certificates requires a key");
	}

	@Override
	protected void initHandshake()
			throws IOException {
		rand_c = rand.getBytes(32);
		srand_c = rand.getBytes(32);
		this._send_hello();
	}

	@Override
	protected void handshakeHandler(VEntity indata)
			throws IOException {
		try {
			if (_proto_step == _ProtocolStep._SEND_PUBKEY)
				this._send_pubkey(indata);
			else if (_proto_step == _ProtocolStep._GET_SERVER_SECRET)
				this._get_server_secret(indata);
		} catch (IOException e) {
			this._pc_abort();
			this._pp_abort();
			throw e;
		}
	}


	// VTS protocol step 2: client sends handshake data
	void _send_hello()
			throws IOException {
		LinkedList<Object> _ciphers = new LinkedList<Object>();
		for (CipherAndModes _cm: ciphers) {
			String c_name = _cm.getCipher();
			Object[] c_modes = _cm.getModes().toArray(new String[0]);
			_ciphers.addLast(new Object[] {c_name, c_modes});
		}

		Object[] o_mac = new Object[] {prf_hash_methods, _ciphers.toArray(), mac_hash_methods,
				rand_c, maxKeyLen, hshakeLim};
		VEntity msg = null;
		try {
			msg = VTuple.valueOf(o_mac);
		} catch (VEntityError e) {
			throw new IOException("Internal message encoding error");
		}
		_handshake_reader = VEntity._v_reader(new VIOContext());
		_handshake_writer = msg._v_writer(new VIOContext(), true);
		_proto_step = _ProtocolStep._SEND_PUBKEY;
		class Function implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				__cp_produce(false);
				return null;
			}
		}
		reactor.schedule(new Function());
	}

	// VTS protocol step 4: client sends public key and secret1
	void _send_pubkey(VEntity data)
			throws IOException {
		VTuple in_msg = null;
		try {
			in_msg = VTuple.valueOf(data);
		} catch (VEntityError e) {
			throw new IOException("Invalid protocol message, not a tuple");
		}
		if (in_msg.length() != 9)
			throw new IOException("Invalid protocol message, tuple length mismatch");

		String hmac_name = null;
		String cipher_name = null;
		String cipher_mode = null;
		String hash_name = null;
		byte[] s_rand = null;
		VTuple s_pubdata = null;
		VEntity s_credentials = in_msg.get(6);
		VEntity s_maxkeylen = in_msg.get(7);
		VEntity s_hshakelim = in_msg.get(8);
		try {
			hmac_name = VString.nativeOf(in_msg.get(0));
			cipher_name = VString.nativeOf(in_msg.get(1));
			cipher_mode = VString.nativeOf(in_msg.get(2));
			hash_name = VString.nativeOf(in_msg.get(3));
			s_rand = VBytes.nativeOf(in_msg.get(4));
			s_pubdata = VTuple.valueOf(in_msg.get(5));
		} catch (VEntityError e) {
			throw new IOException("Invalid protocol message");
		}

		// Validate negotiated handshake hash was in list sent by client
		boolean _hshake_hash_ok = false;
		for (String _name: prf_hash_methods)
			if (_name.equals(hmac_name)) {
				_hshake_hash_ok = true;
				break;
			}
		if (!_hshake_hash_ok)
			throw new IOException("Invalid negotiated handshake hash from server");

		// Validate negotiated ciper name and mode was in list sent by client
		List<String> _cmodes = null;
		for (CipherAndModes _cm: ciphers) {
			if (cipher_name.equals(_cm.getCipher())) {
				_cmodes = _cm.getModes();
				break;
			}
		}
		if (_cmodes == null)
			throw new IOException("Invalid negotiated cipher name from server");
		boolean _mode_ok = false;
		for (String _name: _cmodes)
			if (_name.equals(cipher_mode)) {
				_mode_ok = true;
				break;
			}
		if (!_mode_ok)
			throw new IOException("Invalid negotiated cipher mode from server");

		// Validate negotiated hash was in list sent by client
		boolean _hash_ok = false;
		for (String _name: mac_hash_methods)
			if (_name.equals(hash_name)) {
				_hash_ok = true;
				break;
			}
		if (!_hash_ok)
			throw new IOException("Invalid negotiated hash method from server");

		// Set negotiated connection parameters
		negPrfHash = hmac_name;
		negCipherName = cipher_name;
		negCipherMode = cipher_mode;
		negMacHash = hash_name;

		// Parse random data
		if (s_rand.length < 32)
			throw new IOException("Peer provided less than 32 random bytes");
		rand_s = s_rand;

		// Parse maximum RSA key length
		if (s_maxkeylen instanceof VNone)
			peerMaxKeylen = -1;
		else if (s_maxkeylen instanceof VInteger) {
			try {
				Number _mkeylen = VInteger.nativeOf(s_maxkeylen);
				if (_mkeylen instanceof Integer) {
					int mlen = (Integer)_mkeylen;
					if (mlen > 0)
						peerMaxKeylen = mlen;
					else
						throw new IOException("Invalid max key length");
				}
			} catch (VEntityError e) {
				throw new IOException("Error resolving max key length");
			}
		}

		// Parse maximum handshake length
		if (s_hshakelim instanceof VNone)
			peerHshakeLim = -1;
		else if (s_hshakelim instanceof VInteger) {
			try {
				Number _lim = VInteger.nativeOf(s_hshakelim);
				if (_lim instanceof Integer) {
					int lim = (Integer)_lim;
					if (lim > 0)
						peerHshakeLim = lim;
					else
						throw new IOException("Invalid handshake limit");
				}
			} catch (VEntityError e) {
				throw new IOException("Error resolving handshake limit");
			}
		}

		// Parse server public key
		peerPublicKey = this.parseRsaPubkeyData(s_pubdata);

		// Parse identity/certificate data
		X500Principal s_identity = null;
		CertPath s_certificates = null;
		if (!(s_credentials instanceof VNone)) {
			VTuple t_cred = null;
			boolean is_cert_path = false;
			try {
				t_cred = VTuple.valueOf(s_credentials);
				if (t_cred.length() != 2)
					throw new IOException("Invalid protocol credentials data");
				is_cert_path = VBoolean.nativeOf(t_cred.get(0));
			} catch (VEntityError e) {
				throw new IOException("Invalid protocol credentials data");
			}
			if (is_cert_path)
				s_certificates = this.parseCertPath(t_cred.get(1));
			else {
				// Parse identity
				try {
					s_identity = new X500Principal(VBytes.nativeOf(t_cred.get(1)));
				} catch (VEntityError e) {
					throw new IOException("Invalid credentials 'identity' data");
				} catch (IllegalArgumentException e) {
					throw new IOException("Invalid credentials 'identity' data");
				}
			}
		}

		// Authorize peer credentials
		peerIdentity = s_identity;
		peerCertificates = s_certificates;
		this.authorizeCredentials(s_identity, s_certificates);

		// Generate client secure random data
		srand_c = rand.getBytes(32);

		// Construct client public key data for peer
		VEntity c_pubkey = null;
		if (keypair != null) {
			RSAPublicKey pubkey = keypair.getPublic();
			String pubkey_cipher = "rsa";
			Object[] pk_data = new Object[5];
			pk_data[0] = pubkey.getModulus();
			pk_data[1] = pubkey.getPublicExponent();
			pk_data[2] = null;
			pk_data[3] = null;
			pk_data[4] = null;
			try {
				c_pubkey = VTuple.valueOf(new Object[] {pubkey_cipher, pk_data});
			} catch (VEntityError e) {
				throw new IOException("Error encoding server public key data");
			}
		}
		else
			c_pubkey = VNone.get();

		// Construct client credentials data for peer
		VEntity c_credentials = null;
		if (identity != null) {
			VBytes _der = new VBytes(identity.getEncoded());
			c_credentials = VTuple.fromElements(new VBoolean(false), _der);
		}
		else if (certs != null) {
			LinkedList<VBytes> certdata = new LinkedList<VBytes>();
			for (Certificate _cert: certs.getCertificates())
				try {
					certdata.addLast(new VBytes(_cert.getEncoded()));
				} catch (CertificateEncodingException e) {
					throw new IOException("Error encoding server certificate chain");
				}
			c_credentials = VTuple.fromElements(new VBoolean(true), new VTuple(certdata));
		}
		else
			c_credentials = VNone.get();

		// Prepare message to peer - plaintext content for encryption
		VBytes c_padding = new VBytes(new byte[0]);
		VTuple p_msg = VTuple.fromElements(c_pubkey, c_credentials, c_padding);

		// Prepare message to peer - encrypted content
		byte[] block_rand = rand.getBytes(32);
		byte[] keyseed = VBytes.concat("vts client sendkey".getBytes("ASCII"), block_rand, srand_c);
		byte[] enc_msg = null;
		try {
			enc_msg = this.blockcipherEncEntity(p_msg, keyseed);
		} catch (VCryptoException e) {
			throw new IOException("Internal encryption error");
		}

		// Prepare message to peer - plaintext header
		VHash hash;
		try {
			hash = VHash.getHashGenerator(negMacHash);
		} catch (VCryptoException e) {
			throw new IOException("Internal encryption error");
		}
		byte[] _p_msg_digest = hash.digestOf(p_msg._v_write(new VIOContext()));
		VTuple header = VTuple.fromElements(new VBytes(srand_c), new VBytes(block_rand),
											new VBytes(_p_msg_digest));

		byte[] enc_header = null;
		try {
			enc_header = this.rsaEncEntity(header, peerPublicKey);
		} catch (VCryptoException e) {
			throw new IOException("Internal encryption error");
		}

		// Prepare combined message for peer
		VTuple send_msg = VTuple.fromElements(new VBytes(enc_header), new VBytes(enc_msg));
		_handshake_writer = send_msg._v_writer(new VIOContext());
		if (keypair != null) {
			_handshake_reader = VEntity._v_reader(new VIOContext());
			_proto_step = _ProtocolStep._GET_SERVER_SECRET;
		}
		else {
			byte[] s_keyseed = VBytes.concat(rand_s, rand_c, srand_c);
			byte[] c_keyseed = VBytes.concat(rand_c, rand_s, srand_c);
			byte[][] keydata = null;
			try {
				keydata = this.generateKeys(s_keyseed, c_keyseed);
			} catch (VCryptoException e) {
				throw new IOException("Internal crypto error");
			}

			byte[] c_key = keydata[0];
			byte[] c_iv = keydata[1];
			byte[] c_mac = keydata[2];
			try {
				encrypter = this.genMsgEnc(c_key, c_iv, c_mac);
			} catch (VCryptoException e) {
				throw new IOException("Internal crypto error");
			}

			byte[] s_key = keydata[3];
			byte[] s_iv = keydata[4];
			byte[] s_mac = keydata[5];
			try {
				decrypter = this.genMsgDec(s_key, s_iv, s_mac);
			} catch (VCryptoException e) {
				throw new IOException("Internal crypto error");
			}

			_end_handshaking = true;
		}
		class Function implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				__cp_produce(false);
				return null;
			}
		}
		reactor.schedule(new Function());
	}

	// VTS protocol step 6: client receives secret2
	void _get_server_secret(VEntity data)
			throws IOException {
		byte[] _data = null;
		try {
			_data = VBytes.nativeOf(data);
		} catch (VEntityError e) {
			throw new IOException("Invalid protocol message");
		}

		// Decrypt received data
		VEntity msg = null;
		try {
			msg = this.rsaDecEntity(_data, keypair.getPrivate());
		} catch (VCryptoException e) {
			throw new IOException("Protocol message did not decrypt cleanly");
		}

		byte[] s_srand_s = null;
		try {
			s_srand_s = VBytes.nativeOf(msg);
		} catch (VEntityError e) {
			throw new IOException("Invalid format of decrypted protocol message");
		}
		if (s_srand_s.length < 32)
			throw new IOException("Protocol random data must be >= 32 bytes");
		srand_s = s_srand_s;

		// Finish protocol  handshake
		byte[] s_keyseed = VBytes.concat(rand_s, rand_c, srand_s, srand_c);
		byte[] c_keyseed = VBytes.concat(rand_c, rand_s, srand_c, srand_s);
		byte[][] keydata = null;
		try {
			keydata = this.generateKeys(s_keyseed, c_keyseed);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		byte[] c_key = keydata[0];
		byte[] c_iv = keydata[1];
		byte[] c_mac = keydata[2];
		try {
			encrypter = this.genMsgEnc(c_key, c_iv, c_mac);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		byte[] s_key = keydata[3];
		byte[] s_iv = keydata[4];
		byte[] s_mac = keydata[5];
		try {
			decrypter = this.genMsgDec(s_key, s_iv, s_mac);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		_handshaking = false;
		this.enablePlaintext();
		reactor.log("VTS: client handshake completed");

		class Function implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				__cp_produce(true);
				return null;
			}
		}
		reactor.schedule(new Function());
	}
}
