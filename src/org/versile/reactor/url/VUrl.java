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

package org.versile.reactor.url;


import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCall;
import org.versile.common.call.VCallCancelled;
import org.versile.common.call.VCallException;
import org.versile.common.call.VCallOperationException;
import org.versile.orb.entity.VObject;
import org.versile.orb.entity.VProxy;
import org.versile.orb.url.VUrlBase;
import org.versile.orb.url.VUrlData;
import org.versile.orb.url.VUrlException;
import org.versile.orb.url.VUrlResolver;
import org.versile.orb.url.VUrlResolver.ResolveResult;


/**
 * Versile Resource Identifier (VRI) resolution with reactor-based links.
 *
 * <p>{@link VUrl} objects should be created via the {@link #parse(String)}
 * factory method.</p>
 */
public abstract class VUrl extends VUrlBase {

	/**
	 * Parsed VRI data.
	 */
	protected VUrlData urldata;

	/**
	 * Set up URL object.
	 *
	 * @param urldata parsed VRI data
	 */
	public VUrl(VUrlData urldata) {
		this.urldata = urldata;
	}

	/**
	 * Parse a VRI string.
	 *
	 * @param url VRI string to parse
	 * @return URL object
	 * @throws VUrlException invalid VRI string or unsupported VRI scheme
	 */
	public static VUrl parse(String url)
		throws VUrlException {
		VUrlData urldata = new VUrlData(url);
		if (urldata.getScheme().equals("vop"))
			return new VopUrl(urldata);
		else
			throw new VUrlException("Unsupported URL scheme");
	}

	@Override
	public VUrlResolver connect(VPrivateCredentials credentials, VObject gw)
            		throws VUrlException {
		return this.connect(credentials, gw, null);
	}

	/**
	 * Initiates connection to a VRI resource.
	 *
	 * <p>Blocking DNS resolution, but otherwise may return before a connection
	 * is fully established.</p>
	 *
	 * <p>Credentials are only allowed for secure connections (VTS or TLS transport)
	 * and must be null for insecure plaintext connections.</p>
	 *
	 * @param credentials credentials for authenticating (or null)
	 * @param gw local gateway for resulting link (or null)
	 * @param config link configuration parameters (or null)
	 * @return resolver for VRI resource
	 * @throws VUrlException cannot initiate connection
	 */
	public VUrlResolver connect(VPrivateCredentials credentials, VObject gw, VUrlConfig config)
            		throws VUrlException {
		if (gw == null)
			gw = new VObject();
		VCall<VUrlResolver> call = this.nowaitConnect(credentials, gw, config);
		try {
			call.waitResult();
			return (VUrlResolver)(call.getResult());
		} catch (Exception e) {
			throw new VUrlException("Connect error");
		}
	}

	@Override
	public VCall<VUrlResolver> nowaitConnect(VPrivateCredentials credentials, VObject gw) {
		return this.nowaitConnect(credentials, gw, null);
	}

	/**
	 * Initiates non-blocking connect to a VRI resource.
	 *
	 * <p>Non-blocking call which returns immediately.</p>
	 *
	 * <p>Credentials are only allowed for secure connections (VTS or TLS transport)
	 * and must be null for insecure plaintext connections.</p>
	 *
	 * @param credentials credentials for authenticating (or null)
	 * @param gw local gateway for resulting link (or null)
	 * @param config link configuration data (or null)
	 * @return reference to resulting resolver
	 */
	public abstract VCall<VUrlResolver> nowaitConnect(VPrivateCredentials credentials, VObject gw,
													  VUrlConfig config);

	/**
	 * Resolves a VRI with a dummy local gateway (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @return resulting resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolve(String url)
		throws VUrlException {
		return VUrl.resolve(url, null, null, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @return resolved resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolve(String url, VPrivateCredentials credentials)
		throws VUrlException {
		return VUrl.resolve(url, credentials, null, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @return resolved resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolve(String url, VPrivateCredentials credentials, VObject gw)
		throws VUrlException {
		return VUrl.resolve(url, credentials, gw, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
     * @param config link config parameters (or null)
	 * @return resolved resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolve(String url, VPrivateCredentials credentials, VObject gw, VUrlConfig config)
		throws VUrlException {
		VCall<Object> call = VUrl.nowaitResolve(url, credentials, gw, config);
		VUrlException _exc;
		try {
			return call.getResult();
		} catch (VCallCancelled e) {
			// Should never happen
			_exc = new VUrlException(e);
		} catch (VCallException e) {
			Exception e2 = e.getException();
			if (e2 instanceof VUrlException)
				_exc = (VUrlException) e2;
			else
				_exc = new VUrlException(e2);
		}
		throw _exc;
	}

	/**
	 * Resolves a VRI with a dummy local gateway (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @return resolved resource and associated link
	 * @throws VUrlException error resolving the VRI
	 */
	public static ResolveResult resolveWithLink(String url)
		throws VUrlException {
		return VUrl.resolveWithLink(url, null, null, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @return resolved resource and associated link
	 * @throws VUrlException error resolving the VRI
	 */
	public static ResolveResult resolveWithLink(String url, VPrivateCredentials credentials)
		throws VUrlException {
		return VUrl.resolveWithLink(url, credentials, null, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @return resolved resource and associated link
	 * @throws VUrlException error resolving the VRI
	 */
	public static ResolveResult resolveWithLink(String url, VPrivateCredentials credentials, VObject gw)
		throws VUrlException {
		return VUrl.resolveWithLink(url, credentials, gw, null);
	}

	/**
	 * Resolves a VRI (blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
     * @param config link config parameters (or null)
	 * @return resolved resource and associated link
	 * @throws VUrlException error resolving the VRI
	 */
	public static ResolveResult resolveWithLink(String url, VPrivateCredentials credentials,
												VObject gw, VUrlConfig config)
		throws VUrlException {
		VCall<ResolveResult> call = VUrl.nowaitResolveWithLink(url, credentials, gw, config);
		VUrlException _exc;
		try {
			return call.getResult();
		} catch (VCallCancelled e) {
			// Should never happen
			_exc = new VUrlException(e);
		} catch (VCallException e) {
			Exception e2 = e.getException();
			if (e2 instanceof VUrlException)
				_exc = (VUrlException) e2;
			else
				_exc = new VUrlException(e2);
		}
		throw _exc;
	}

	/**
	 * Resolves a VRI (blocking) with an insecure connection.
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Normally only secure connections should be used, and one of the {@link #resolve}
	 * methods should be used instead.</p>
	 *
	 * @param url the VRI to resolve
	 * @return resolved resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolveInsecure(String url)
		throws VUrlException {
		return VUrl.resolveInsecure(url, null);
	}

	/**
	 * Resolves a VRI (blocking) with an insecure connection.
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Normally only secure connections should be used, and one of the {@link #resolve}
	 * methods should be used instead.</p>
	 *
	 * @param url the VRI to resolve
	 * @param gw local gateway (or null)
	 * @return resolved resource
	 * @throws VUrlException error resolving the VRI
	 */
	public static Object resolveInsecure(String url, VObject gw)
		throws VUrlException {

		VCall<Object> call = VUrl.nowaitResolveInsecure(url, gw);
		VUrlException _exc;
		try {
			return call.getResult();
		} catch (VCallCancelled e) {
			// Should never happen
			_exc = new VUrlException(e);
		} catch (VCallException e) {
			Exception e2 = e.getException();
			if (e2 instanceof VUrlException)
				_exc = (VUrlException) e2;
			else
				_exc = new VUrlException(e2);
		}
		throw _exc;
	}

	/**
	 * Resolves a VRI with a dummy local gateway (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @return reference to the resulting resource
	 */
	public static VCall<Object> nowaitResolve(String url) {
		return VUrl.nowaitResolve(url, null, null, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @return reference to the resolved resource
	 */
	public static VCall<Object> nowaitResolve(String url, VPrivateCredentials credentials) {
		return VUrl.nowaitResolve(url, credentials, null, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @return reference to the resolved resource
	 */
	public static VCall<Object> nowaitResolve(String url, VPrivateCredentials credentials, VObject gw) {
		return VUrl.nowaitResolve(url, credentials, gw, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @param config link configuration data (or null)
	 * @return reference to the resolved resource
	 */
	public static VCall<Object> nowaitResolve(String url, VPrivateCredentials credentials,
				 	                          VObject gw, VUrlConfig config) {
		VUrlResolver resolver;
		try {
			VUrl vurl = VUrl.parse(url);
			resolver = vurl.connect(credentials, gw, config);
		} catch (VUrlException e) {
			try {
				VCall<Object> result = new VCall<Object>();
				result.pushException(e);
				return result;
			} catch (VCallOperationException e1) {
				// Should never happen
				throw new RuntimeException(e1);
			}
		}
		return resolver.nowaitResolve();
	}

	/**
	 * Resolves a VRI with a dummy local gateway (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @return reference to resolved resource and associated link
	 */
	public static VCall<ResolveResult> nowaitResolveWithLink(String url) {
		return VUrl.nowaitResolveWithLink(url, null, null, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Lazy-creates a parser for globally registered {@link org.versile.orb.module.VModule}.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @return reference to resolved resource and associated link
	 */
	public static VCall<ResolveResult> nowaitResolveWithLink(String url, VPrivateCredentials credentials) {
		return VUrl.nowaitResolveWithLink(url, credentials, null, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @return reference to resolved resource and associated link
	 */
	public static VCall<ResolveResult> nowaitResolveWithLink(String url, VPrivateCredentials credentials, VObject gw) {
		return VUrl.nowaitResolveWithLink(url, credentials, gw, null);
	}

	/**
	 * Resolves a VRI (non-blocking).
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * @param url the VRI to resolve
	 * @param credentials credentials for connecting (or null)
	 * @param gw local gateway (or null)
	 * @param config link configuration data (or null)
	 * @return reference to resolved resource and associated link
	 */
	public static VCall<ResolveResult> nowaitResolveWithLink(String url, VPrivateCredentials credentials,
    				 	                                     VObject gw, VUrlConfig config) {
		VUrlResolver resolver;
		try {
			VUrl vurl = VUrl.parse(url);
			resolver = vurl.connect(credentials, gw, config);
		} catch (VUrlException e) {
			try {
				VCall<ResolveResult> result = new VCall<ResolveResult>();
				result.pushException(e);
				return result;
			} catch (VCallOperationException e1) {
				// Should never happen
				throw new RuntimeException(e1);
			}
		}
		return resolver.nowaitResolveWithLink();
	}

	/**
	 * Resolves a VRI (non-blocking) with an insecure connection.
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Normally only secure connections should be used, and one of the {@link #resolve}
	 * methods should be used instead.</p>
	 *
	 * @param url the VRI to resolve
	 * @return reference to the resolved resource
	 */
	public static VCall<Object> nowaitResolveInsecure(String url) {
		return VUrl.nowaitResolveInsecure(url, null);
	}

	/**
	 * Resolves a VRI (non-blocking) with an insecure connection.
	 *
	 * <p>Performs VRI parsing, connecting to target, and path/query resolution.</p>
	 *
	 * <p>Normally only secure connections should be used, and one of the {@link #resolve}
	 * methods should be used instead.</p>
	 *
	 * @param url the VRI to resolve
	 * @param gw local gateway (or null)
	 * @return reference to the resolved resource
	 */
	public static VCall<Object> nowaitResolveInsecure(String url, VObject gw) {
		VUrlConfig config = new VUrlConfig();
		config.setInsecureEnabled(true);
		config.setVtsEnabled(false);
		return VUrl.nowaitResolve(url, null, gw, config);
	}

	/**
	 * Resolves a relative VRI reference (blocking).
	 *
	 * <p>Alias for {@link VUrlResolver#relative(VProxy, String)}.</p>
	 *
	 * @param gw top gateway for relative VRI
	 * @param relative_url VRI relative to gw
	 * @return resolved VRI
	 */
	public static Object relative(VProxy gw, String relative_url)
		throws VUrlException {
		return VUrlResolver.relative(gw, relative_url);
	}

	/**
	 * Resolves a relative VRI reference (asynchronous).
	 *
	 * <p>Alias for {@link VUrlResolver#nowaitRelative(VProxy, String)}.</p>
	 *
	 * @param gw top gateway for relative VRI
	 * @param relative_url VRI relative to gw
	 * @return reference to resolved VRI
	 */
	public static VCall<Object> nowaitRelative(VProxy gw, String relative_url) {
			return VUrlResolver.nowaitRelative(gw, relative_url);
		}
}
