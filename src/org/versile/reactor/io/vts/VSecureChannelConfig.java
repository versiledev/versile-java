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

import java.util.Vector;

import org.versile.common.auth.VAuth;
import org.versile.common.util.VByteGenerator;
import org.versile.reactor.io.vts.VSecureChannel.CipherAndModes;


/**
 * VTS configuration parameters.
 *
 * <p>Max RSA key length is the maximum length (in bits) of an RSA key
 * used to identify a peer and used in the VTS handshake. The
 * default value is 4096. The value must be a multiple of 8.</p>
 *
 * <p>Handshake limit is the maximum number of bytes allowed in a
 * VTS protocol handshake message. The default value is 16384.</p>
 *
 * <p>Buffer length is the length of input/output buffers for the channel
 * producers and consumers of plaintext/ciphertext input and output data.
 * Default value is 4096.</p>
 *
 * <p>Secure random data provider for generation of random data parameters
 * during VTS protocol handshake. It may be null, in which case a default
 * secure random provider should be used.</p>
 *
 * <p>Padding provider is a provided of padding data for handshake ciphers and
 * for encrypted data post-handshake. It may be null, in which case a default
 * padding should be used.</p>
 *
 * <p>If non-null an authorizer is used with credentials received from VTS peer
 * during handshake.</p>
 *
 * <p>PRF hash methods is the set of names of hash methods allowed for the VTS
 * handshake, for the pseudo-random-function. It must consist of standard
 * VCA hash method names. If it is null then all VCA hash method names
 * are allowed.</p>
 *
 * <p>MAC hash methods is the set of names of hash methods allowed for the
 * use in MAC-based data validation in ciphers used by VTS. It must consist
 * of standard VCA hash method names. If it is null then all VCA hash method names
 * are allowed.</p>
 *
 * <p>Ciphers is the ciper names (map key) and associated cipher modes (elements
 * of map values) allowed for VTS block ciphers. It must consist of standard
 * VCA cipher names and cipher mode names. If it is null then a default set of VCA ciphers
 * is used (if supported), in the order of: "aes256", "blowfish", "blowfish128".</p>
 */
public class VSecureChannelConfig {

	// When adding fields remember to update copyTo()
	int max_rsa_keylen = 4096;
	int handshake_limit = 16384;
	int buf_len = 4096;
	VByteGenerator sec_rand = null;
	VByteGenerator padding = null;
	VAuth authorizer = null;
	String[] prf_hash_methods = null;
	String[] mac_hash_methods = null;
	Vector<CipherAndModes> ciphers = null;

	@Override
	public VSecureChannelConfig clone() {
		VSecureChannelConfig result = new VSecureChannelConfig();
		this.copyTo(result);
		return result;
	}

	public int getMaxRSAKeylen() {
		return max_rsa_keylen;
	}

	public void setMaxRSAKeylen(int max_rsa_keylen) {
		this.max_rsa_keylen = max_rsa_keylen;
	}

	public int getHandshakeLimit() {
		return handshake_limit;
	}

	public void setHandshakeLimit(int handshake_limit) {
		this.handshake_limit = handshake_limit;
	}

	public int getBufferLength() {
		return buf_len;
	}

	public void setBufferLength(int buf_len) {
		this.buf_len = buf_len;
	}

	public VByteGenerator getSecureRandom() {
		return sec_rand;
	}

	public void setSecureRandom(VByteGenerator sec_rand) {
		this.sec_rand = sec_rand;
	}

	public VByteGenerator getPadding() {
		return padding;
	}

	public void setPadding(VByteGenerator padding) {
		this.padding = padding;
	}

	public VAuth getAuthorizer() {
		return authorizer;
	}

	public void setAuthorizer(VAuth authorizer) {
		this.authorizer = authorizer;
	}

	public String[] getPRFHashMethods() {
		return prf_hash_methods;
	}

	public void setPRFHashMethods(String[] prf_hash_methods) {
		this.prf_hash_methods = prf_hash_methods;
	}

	public String[] getMACHashMethods() {
		return mac_hash_methods;
	}

	public void setMACHashMethods(String[] mac_hash_methods) {
		this.mac_hash_methods = mac_hash_methods;
	}

	public Iterable<CipherAndModes> getCiphers() {
		return ciphers;
	}

	public void setCiphers(Iterable<CipherAndModes> ciphers) {
		if (ciphers == null)
			this.ciphers = null;
		else {
			this.ciphers = new Vector<CipherAndModes>();
			for (CipherAndModes cipher: ciphers)
				this.ciphers.add(cipher);
		}
	}

	protected void copyTo(VSecureChannelConfig config) {
		config.max_rsa_keylen = max_rsa_keylen;
		config.handshake_limit = handshake_limit;
		config.buf_len = buf_len;
		config.sec_rand = sec_rand;
		config.padding = padding;
		config.authorizer = authorizer;
		config.prf_hash_methods = prf_hash_methods;
		config.mac_hash_methods = mac_hash_methods;
		config.ciphers = ciphers;
	}
}
