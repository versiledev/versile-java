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
 * VTS server end-point.
 *
 * <p>Implements the server side of a Versile Transport Security channel.</p>
 */
public class VSecureServer extends VSecureChannel {

	enum _ProtocolStep {_ACK_HELLO, _GET_PUBKEY};

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
	public VSecureServer(VReactor reactor, VPrivateCredentials credentials)
			throws VCryptoException {
		super(reactor, credentials, new VSecureChannelConfig(), Role.SERVER);
		if (credentials.getKeyPair() == null)
			throw new VCryptoException("VTS server requires credentials with a keypair");
	}

	/**
	 * Set up secure channel.
	 *
	 * @param reactor owning reactor
	 * @param credentials credentials for authentication (or null)
	 * @param config channel config settings (defaults if null)
	 * @throws VCryptoException
	 */
	public VSecureServer(VReactor reactor, VPrivateCredentials credentials, VSecureChannelConfig config)
			throws VCryptoException {
		super(reactor, credentials, config, Role.SERVER);
	}

	@Override
	protected void initHandshake() {
		_handshake_reader = VEntity._v_reader(new VIOContext());
		_proto_step = _ProtocolStep._ACK_HELLO;
	}

	@Override
	protected void handshakeHandler(VEntity indata)
			throws IOException {
		try {
			if (_proto_step == _ProtocolStep._ACK_HELLO)
				this._ack_hello(indata);
			else if (_proto_step == _ProtocolStep._GET_PUBKEY)
				this._get_pubkey(indata);
		} catch (IOException e) {
			this._pc_abort();
			this._pp_abort();
			throw e;
		}
	}

	// VTS protocol step 3: server sends crypto schemes and public key
	void _ack_hello(VEntity data)
			throws IOException {
		VTuple in_msg = null;
		try {
			in_msg = (VTuple) data;
		} catch (Exception e) {
			throw new IOException("Input protocol message must be a tuple");
		}
		if (in_msg.length() != 6)
			throw new IOException("Input protocol message must be a tuple of length 6");

		VTuple c_hhashes = null;
		VTuple c_ciphers = null;
		VTuple c_hashes = null;
		VBytes c_crand = null;
		VEntity c_maxkeylen = in_msg.get(4);
		VEntity c_hshakelim = in_msg.get(5);
		try {
			c_hhashes = VTuple.valueOf(in_msg.get(0));
			c_ciphers = VTuple.valueOf(in_msg.get(1));
			c_hashes = VTuple.valueOf(in_msg.get(2));
			c_crand = VBytes.valueOf(in_msg.get(3));
		} catch (VEntityError e) {
			throw new IOException("Bad input protocol message");
		}

		// Negotiate handshake hash method
		String handshake_hash = null;
		for (VEntity item: c_hhashes) {
			String _hash = null;
			try {
				_hash = VString.nativeOf(item);
			} catch (VEntityError e) {
				throw new IOException("Handshake hash method names must be strings");
			}
			if (handshake_hash == null)
				for (String name: prf_hash_methods)
					if (_hash.equals(name)) {
						handshake_hash = name;
						break;
					}
		}
		if (handshake_hash == null)
			throw new IOException("Could not negotiate a handshake hash method");

		// Negotiate cipher
		String cipher_name = null;
		String cipher_mode = null;
		for (VEntity item: c_ciphers) {
			VTuple pair = null;
			String cname = null;
			VTuple modes = null;
			try {
				pair = VTuple.valueOf(item);
				if (pair.length() != 2)
					throw new IOException("Main cipher entries must have length 2");
				cname = VString.nativeOf(pair.get(0));
				modes = VTuple.valueOf(pair.get(1));
			} catch (VEntityError e) {
				throw new IOException("Invalid cipher list in protocol message");
			}
			for (VEntity _mode: modes) {
				String modename = null;
				try {
					modename = VString.nativeOf(_mode);
				} catch (VEntityError e) {
					throw new IOException("Invalid cipher mode name in protocol message");
				}
				if (cipher_name == null) {
					List<String> _modes = null;
					for (CipherAndModes _cm: ciphers) {
						if (cname.equals(_cm.getCipher())) {
							_modes = _cm.getModes();
							break;
						}
					}
					if (_modes != null) {
						for (String mname: _modes)
							if (modename.equals(mname)) {
								cipher_name = cname;
								cipher_mode = mname;
								break;
							}
					}
				}
			}
		}
		if (cipher_name == null || cipher_mode == null)
			throw new IOException("Could not negotiate a cipher");

		// Negotiate cipher hash method
		String hash = null;
		for (VEntity item: c_hashes) {
			String _hash = null;
			try {
				_hash = VString.nativeOf(item);
			} catch (VEntityError e) {
				throw new IOException("Hash method names must be strings");
			}
			if (hash == null)
				for (String name: mac_hash_methods)
					if (_hash.equals(name)) {
						hash = name;
						break;
					}
		}
		if (hash == null)
			throw new IOException("Could not negotiate a cipher MAC hash method");

		// Parse random data
		if (c_crand.length() < 32)
			throw new IOException("Peer provided less than 32 random bytes");
		rand_c = c_crand.getValue();

		// Parse maximum RSA key length
		if (c_maxkeylen instanceof VNone)
			peerMaxKeylen = -1;
		else if (c_maxkeylen instanceof VInteger) {
			try {
				Number _mkeylen = VInteger.nativeOf(c_maxkeylen);
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
		if (c_hshakelim instanceof VNone)
			peerHshakeLim = -1;
		else if (c_hshakelim instanceof VInteger) {
			try {
				Number _lim = VInteger.nativeOf(c_hshakelim);
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

		// Generate server random data and prepare public key for export
		rand_s = rand.getBytes(32);
		RSAPublicKey pubkey = keypair.getPublic();
		String pubkey_cipher = "rsa";
		Object[] pk_data = new Object[5];
		pk_data[0] = pubkey.getModulus();
		pk_data[1] = pubkey.getPublicExponent();
		pk_data[2] = null;
		pk_data[3] = null;
		pk_data[4] = null;
		VTuple pubkey_data = null;
		try {
			pubkey_data = VTuple.valueOf(new Object[] {pubkey_cipher, pk_data});
		} catch (VEntityError e) {
			throw new IOException("Error encoding server public key data");
		}

		// Generate credentials
		VEntity credentials = null;
		if (identity != null) {
			VBytes _der = new VBytes(identity.getEncoded());
			credentials = VTuple.fromElements(new VBoolean(false), _der);
		}
		else if (certs != null) {
			LinkedList<VBytes> certdata = new LinkedList<VBytes>();
			for (Certificate _cert: certs.getCertificates())
				try {
					certdata.addLast(new VBytes(_cert.getEncoded()));
				} catch (CertificateEncodingException e) {
					throw new IOException("Error encoding server certificate chain");
				}
			credentials = VTuple.fromElements(new VBoolean(true), new VTuple(certdata));
		}
		else
			credentials = VNone.get();

		// Prepare a return value for peer
		VTuple msg = null;
		try {
			msg = VTuple.valueOf(new Object[] {handshake_hash, cipher_name, cipher_mode, hash,
								 rand_s, pubkey_data, credentials, maxKeyLen, hshakeLim});
		} catch (VEntityError e) {
			throw new IOException("Error encoding protocol message for sending to peer");
		}

		// Set negotiated connection parameters
		negPrfHash = handshake_hash;
		negCipherName = cipher_name;
		negCipherMode = cipher_mode;
		negMacHash = hash;

		_handshake_writer = msg._v_writer(new VIOContext(), true);
		_handshake_reader = VEntity._v_reader(new VIOContext());
		_proto_step = _ProtocolStep._GET_PUBKEY;
		class Function implements VReactorFunction {
			@Override
			public Object execute() throws Exception {
				__cp_produce(false);
				return null;
			}
		}
		reactor.schedule(new Function());
	}

	// VTS protocol step 5: server sends secret2
	void _get_pubkey(VEntity data)
			throws IOException {
		byte[] enc_header = null;
		byte[] enc_data = null;
		try {
			VTuple in_msg = VTuple.valueOf(data);
			if (in_msg.length() != 2)
				throw new IOException("Invalid protocol message, tuple length mismatch");
			enc_header = VBytes.nativeOf(in_msg.get(0));
			enc_data = VBytes.nativeOf(in_msg.get(1));
		} catch (VEntityError e) {
			throw new IOException("Invalid protocol message, not a tuple");
		}

		// Decode and parse header
		VEntity header = null;
		try {
			header = this.rsaDecEntity(enc_header, keypair.getPrivate());
		} catch (VCryptoException e) {
			throw new IOException("Could not decode encrypted protocol message header");
		}

		byte[] c_srand_c = null;
		byte[] c_block_rand = null;
		byte [] c_msg_hash = null;
		try {
			VTuple t_header = VTuple.valueOf(header);
			if (t_header.length() != 3)
				throw new IOException("Invalid protocol header data");
			c_srand_c = VBytes.nativeOf(t_header.get(0));
			c_block_rand = VBytes.nativeOf(t_header.get(1));
			c_msg_hash = VBytes.nativeOf(t_header.get(2));
		} catch (VEntityError e) {
			throw new IOException("Invalid protocol header data");
		}
		if (c_srand_c.length < 32)
			throw new IOException("Minimum 32 bytes random seed data required");
		if (c_block_rand.length < 32)
			throw new IOException("Minimum 32 bytes block random data required");
		srand_c = c_srand_c;

		// Decode and parse content
		byte[] keyseed = VBytes.concat("vts client sendkey".getBytes("ASCII"), c_block_rand, c_srand_c);
		VHash hash = null;
		try {
			hash = VHash.getHashGenerator(negMacHash);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}
		VEntity msg = null;
		try {
			msg = this.blockcipherDecEntity(enc_data, keyseed);
		} catch (VCryptoException e) {
			throw new IOException("Encrypted plaintext did not decode properly");
		}
		byte[] _msg_digest = hash.digestOf(msg._v_write(new VIOContext()));
		if (_msg_digest.length != c_msg_hash.length)
			throw new IOException("Encrypted plaintext did not validate with received digest");
		for (int i = 0; i < _msg_digest.length; i++)
			if (_msg_digest[i] != c_msg_hash[i])
				throw new IOException("Encrypted plaintext did not validate with received digest");

		VEntity c_keydata = null;
		VEntity c_credentials = null;
		try {
			VTuple t_msg = VTuple.valueOf(msg);
			if (t_msg.length() != 3)
				throw new IOException("Invalid format of decoded plaintext protocol message");
			c_keydata = t_msg.get(0);
			c_credentials = t_msg.get(1);
			VBytes.valueOf(t_msg.get(2)); // Validates last element is VBytes
		} catch (VEntityError e) {
			throw new IOException("Invalid format of decoded plaintext protocol message");
		}

		// Reconstruct client public key
		if (!(c_keydata instanceof VNone))
			peerPublicKey = this.parseRsaPubkeyData(c_keydata);

		// Parse identity/certificate data
		X500Principal c_identity = null;
		CertPath c_certificates = null;
		if (!(c_credentials instanceof VNone)) {
			VTuple t_cred = null;
			boolean is_cert_path = false;
			try {
				t_cred = VTuple.valueOf(c_credentials);
				if (t_cred.length() != 2)
					throw new IOException("Invalid protocol credentials data");
				is_cert_path = VBoolean.nativeOf(t_cred.get(0));
			} catch (VEntityError e) {
				throw new IOException("Invalid protocol credentials data");
			}
			if (is_cert_path)
				c_certificates = this.parseCertPath(t_cred.get(1));
			else {
				// Parse identity
				try {
					c_identity = new X500Principal(VBytes.nativeOf(t_cred.get(1)));
				} catch (VEntityError e) {
					throw new IOException("Invalid credentials 'identity' data");
				} catch (IllegalArgumentException e) {
					throw new IOException("Invalid credentials 'identity' data");
				}
			}
		}

		// Authorize peer credentials
		peerIdentity = c_identity;
		peerCertificates = c_certificates;
		this.authorizeCredentials(c_identity, c_certificates);

		// Complete server side of handshake
		byte[] s_keyseed = null;
		byte[] c_keyseed = null;
		boolean plain_produce = false;
		if (peerPublicKey != null) {
			srand_s = rand.getBytes(32);
			byte[] _msg = null;
			try {
				_msg = this.rsaEncEntity(new VBytes(srand_s), peerPublicKey);
			} catch (VCryptoException e) {
				throw new IOException("Internal crypto error");
			}
			_handshake_writer = new VBytes(_msg)._v_writer(new VIOContext());
			s_keyseed = VBytes.concat(rand_s, rand_c, srand_s, srand_c);
			c_keyseed = VBytes.concat(rand_c, rand_s, srand_c, srand_s);
			_end_handshaking = true;
		}
		else {
			s_keyseed = VBytes.concat(rand_s, rand_c, srand_c);
			c_keyseed = VBytes.concat(rand_c, rand_s, srand_c);
			_handshaking = false;
			plain_produce = true;
			reactor.log("VTS: server handshake completed");
		}

		byte[][] keydata = null;
		try {
			keydata = this.generateKeys(s_keyseed, c_keyseed);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		byte[] s_key = keydata[3];
		byte[] s_iv = keydata[4];
		byte[] s_mac = keydata[5];
		try {
			encrypter = this.genMsgEnc(s_key, s_iv, s_mac);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		byte[] c_key = keydata[0];
		byte[] c_iv = keydata[1];
		byte[] c_mac = keydata[2];
		try {
			decrypter = this.genMsgDec(c_key, c_iv, c_mac);
		} catch (VCryptoException e) {
			throw new IOException("Internal crypto error");
		}

		class Function implements VReactorFunction {
			boolean plain_prod;
			public Function(boolean plain_prod) {
				this.plain_prod = plain_prod;
			}
			@Override
			public Object execute() throws Exception {
				__cp_produce(plain_prod);
				return null;
			}
		}
		reactor.schedule(new Function(plain_produce));
	}
}
