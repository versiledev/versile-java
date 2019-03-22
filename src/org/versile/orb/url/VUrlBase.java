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

package org.versile.orb.url;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.versile.common.auth.VPrivateCredentials;
import org.versile.common.call.VCall;
import org.versile.orb.entity.VObject;
import org.versile.orb.link.VLink;


/**
 * Reference to a resource identified by a VRI.
 *
 * <p>Must be implemented by derived classes to resolve VRIs for a particular
 * {@link org.versile.orb.link.VLink} link subsystem.</p>
 */
public abstract class VUrlBase {

	/**
	 * Initiates connection to a VRI resource.
	 *
	 * <p>Uses a dummy local gateway object for the local side of the connection.</p>
	 *
	 * <p>Blocking DNS resolution, but otherwise may return before a connection
	 * is fully established.</p>
	 *
	 * @return resolver for URL resource
	 * @throws VUrlException cannot initiate connection
	 */
	public VUrlResolver connect()
		throws VUrlException {
		return this.connect(null);
	}

	/**
	 * Initiates connection to a VRI resource.
	 *
	 * <p>Blocking DNS resolution, but otherwise may return before a connection
	 * is fully established.</p>
	 *
	 * @param credentials credentials for authenticating (or null)
	 * @return resolver for VRI resource
	 * @throws VUrlException cannot initiate connection
	 */
	public VUrlResolver connect(VPrivateCredentials credentials)
			throws VUrlException {
		return this.connect(credentials, null);
	}

	/**
	 * Initiates connection to a VRI resource.
	 *
	 * <p>Blocking DNS resolution, but otherwise may return before a connection
	 * is fully established.</p>
	 *
	 * <p>Credentials are only allowed for secure connections
	 * (VTS or TLS transport) and must be null for insecure (plaintext) connections.</p>
	 *
	 * @param credentials credentials for authenticating (or null)
	 * @param gw local gateway for resulting link (or null)
	 * @return resolver for VRI resource
	 * @throws VUrlException cannot initiate connection
	 */
	public abstract VUrlResolver connect(VPrivateCredentials credentials, VObject gw)
			throws VUrlException;

	/**
	 * Initiates non-blocking connect to a VRI resource with a dummy local gateway.
	 *
	 * <p>Non-blocking call which returns immediately.</p>
	 *
	 * @return reference to resulting resolver
	 */
	public VCall<VUrlResolver> nowaitConnect() {
		return this.nowaitConnect(null);
	}

	/**
	 * Initiates non-blocking connect to a VRI resource.
	 *
	 * <p>Non-blocking call which returns immediately.</p>
	 *
	 * @param credentials credentials for authenticating (or null)
	 * @return reference to resulting resolver
	 */
	public VCall<VUrlResolver> nowaitConnect(VPrivateCredentials credentials) {
		return this.nowaitConnect(credentials, null);
	}

	/**
	 * Initiates non-blocking connect to a VRI resource.
	 *
	 * <p>Non-blocking call which returns immediately.</p>
	 *
	 * <p>Keypair, identity and certificates are only allowed for secure connections
	 * (VTS or TLS transport) and must be null for insecure (plaintext) connections.</p>
	 *
	 * @param gw local gateway for resulting link (or null)
	 * @param credentials credentials for authenticating (or null)
	 * @return reference to resulting resolver
	 */
	public abstract VCall<VUrlResolver> nowaitConnect(VPrivateCredentials credentials, VObject gw);

	/**
	 * Creates a VRI for the provided input parameters.
	 *
	 * @param scheme scheme
	 * @param domain domain
	 * @param port port number (default if negative)
	 * @param path path components (empty if null)
	 * @param query_name query name (no query if null)
	 * @param query_args query arguments (none if null)
	 * @param query_named_args named query arguments (none if null)
	 * @return generated VRI string
	 * @throws VUrlException error creating VRI string
	 */
	public static String createVRI(String scheme, String domain, int port, String[] path,
			String query_name, Object[] query_args, Map<String, Object> query_named_args)
		throws VUrlException {
		String result = "";

		scheme = scheme.toLowerCase();
		if (!(scheme.equals("vop")))
			throw new VUrlException("Unknown scheme");
		result = scheme + "://";

		for (String s: new String[] {":", "/", "&", "=", "'"})
			if(domain.contains(s))
				throw new VUrlException("Illegal character in domain");
		result += domain;
		if (port >= 0)
			result += ":" + port;
		result += "/";

		if (path != null) {
			for (String path_comp : path) {
				try {
					result += URLEncoder.encode(path_comp, "utf8") + "/";
				} catch (UnsupportedEncodingException e) {
					throw new VUrlException("Could not encode path component");
				}
			}
		}

		if (query_name != null)
			result += VUrlBase.createQuery(query_name, query_args, query_named_args);
		else if (query_args != null || query_named_args != null)
			throw new VUrlException("Query arguments without query name");

		return result;
	}

	/**
	 * Creates a VRI query component for the provided input parameters.
	 *
	 * @param query_name query name
	 * @param query_args query arguments (none if null)
	 * @param query_named_args named query arguments (none if null)
	 * @return generated VRI string
	 * @throws VUrlException error creating VRI string
	 */
	public static String createQuery(String query_name, Object[] query_args, Map<String, Object> query_named_args)
		throws VUrlException {
		if (query_name == null)
			throw new VUrlException("Query name required");

		String result = "";

		try {
			result += URLEncoder.encode(query_name, "utf8");
		} catch (UnsupportedEncodingException e) {
			throw new VUrlException("Could not encode path component");
		}

		if (query_args != null && query_args.length > 0) {
			for (Object arg: query_args) {
				String s_arg = VUrlData.argToString(arg);
				try {
					result += "&" + URLEncoder.encode(s_arg, "utf8");
				} catch (UnsupportedEncodingException e) {
					throw new VUrlException("Could not encode path component");
				}
			}
		}

		if (query_named_args != null && !query_named_args.isEmpty()) {
			for (String name: query_named_args.keySet()) {
				Object arg = query_named_args.get(name);
				String s_name = VUrlData.argToString(name);
				String s_arg = VUrlData.argToString(arg);
				try {
					result += "&" + URLEncoder.encode(s_name, "utf8");
					result += "=" + URLEncoder.encode(s_arg, "utf8");
				} catch (UnsupportedEncodingException e) {
					throw new VUrlException("Could not encode path component");
				}
			}
		}

		return result;
	}

	/**
	 * Splits a VRI into a VRI top-level reference and a relative VRI component.
	 *
	 * <p>Returns an absolute VRI to the top-level resource referenced by the VRI,
	 * and a relative reference to the VRI resource which is relative to the
	 * returned top-level VRI.</p>
	 *
	 * <p>Splitting a VRI does not fully validate the input VRI, it only performs
	 * sufficient processing to identify a point in the input VRI to make a split.</p>
	 *
	 * @param url VRI string to parse
	 * @return VRI split as an absolute and relative component
	 * @throws VUrlException unable to split
	 */
	public static SplitUrl split(String url)
		throws VUrlException {
		int split_pos = url.indexOf("//");
		if (split_pos < 0)
			throw new VUrlException();
		split_pos = url.indexOf("/", split_pos+2);
		if (split_pos < 0)
			throw new VUrlException();
		return new SplitUrl(url.substring(0, split_pos+1), url.substring(split_pos));
	}

	/**
	 * Merges an absolute and relative VRI reference into a single absolute VRI.
	 *
	 * <p>Splitting a VRI does not validate the inputs or the resulting merged VRI string,
	 * it only performs sufficient processing to check both strings have a '/'
	 * character at the merge point.</p>
	 *
	 * @param absolute VRI string for top-level resource
	 * @param relative relative path to top-level resource for target resource
	 * @return absolute VRI for combined input absolute VRI and relative path
	 * @throws VUrlException invalid character at merge point
	 */
	public static String merge(String absolute, String relative)
		throws VUrlException {
		if (!absolute.endsWith("/") || !relative.startsWith("/"))
			throw new VUrlException();
		return absolute + relative.substring(1);
	}

	/**
	 * Creates a VRI resolver.
	 *
	 * @param link associated link
	 * @param urldata VRI data to resolve
	 * @return resolver
	 */
	protected static VUrlResolver createResolver(VLink link, VUrlData urldata) {
		return new VUrlResolver(link, urldata);
	}

	/**
	 * Result of a {@link VUrlBase#split(String)} operation.
	 */
	public static class SplitUrl {
		String absolute;
		String relative;
		SplitUrl(String absolute, String relative) {
			this.absolute = absolute;
			this.relative = relative;
		}
		/**
		 * Get absolute VRI component to top-level resource.
		 *
		 * @return absolute VRI to top-level resource
		 */
		public String getAbsolute() {
			return absolute;
		}
		/**
		 * Get resource path relative to top-level resource.
		 *
		 * @return relative resource path
		 */
		public String getRelative() {
			return relative;
		}

	}
}
