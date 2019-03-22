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
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import org.versile.orb.entity.VInteger;


/**
 * VRI data of a parsed VRI string.
 */
public class VUrlData {

	String scheme;
	String host;
	int port;
	String[] path;
	String query_name;
	Object[] query_args;
	Map<String, Object> query_kargs;

	/**
	 * Generates VRI data from a VRI string.
	 *
	 * <p>VRIs are resolved from a format
     * 'scheme://domain:port/path/[query_name[?query_args]]' where
     * query_args has the format 'name1=value1&name2=value2[..]'</p>
	 *
	 * @param url VRI string to parse
	 * @throws VUrlException invalid VRI string
	 */
	public VUrlData(String url)
		throws VUrlException {

		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new VUrlException();
		}

		scheme = uri.getScheme().toLowerCase();
		host = uri.getHost();
		port = uri.getPort();
		if (host == null || host.length() == 0)
			throw new VUrlException("No domain set");

		// Get path and query elements
		String raw_path = uri.getRawPath();
		if (raw_path == null || !raw_path.startsWith("/"))
			throw new VUrlException("Path must begin with '/'");
		String[] _path_and_query = uri.getRawPath().split("/");
		String[] _path = _path_and_query;
		String rawquery = null;
		if (raw_path.equals("/"))
			_path = new String[0];
		else if (raw_path.endsWith("/")) {
			_path = new String[_path_and_query.length-1];
			for (int i = 0; i < _path.length; i++)
				_path[i] = _path_and_query[i+1];
		}
		else {
			_path = new String[_path_and_query.length-2];
			for (int i = 0; i < _path.length; i++)
				_path[i] = _path_and_query[i+1];
			rawquery = _path_and_query[_path_and_query.length-1];
		}

		// Decode path elements
		path = new String[_path.length];
		for (int i = 0; i < _path.length; i++)
			try {
				path[i] = URLDecoder.decode(_path[i], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new VUrlException("Invalid path encoding");
			}

		if (rawquery != null) {
			String[] q_components = rawquery.split("&");
			if (q_components.length < 1 || q_components[0].isEmpty())
				throw new VUrlException("Query arguments without a query name");
			try {
				query_name = URLDecoder.decode(q_components[0], "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				throw new VUrlException("Invalid query name encoding");
			}
			if (query_name == null)
				throw new VUrlException("Query arguments without a query name");
			LinkedList<Object> args = new LinkedList<Object>();
			query_kargs = new Hashtable<String, Object>();
			for (int i = 1; i < q_components.length; i++) {
				String arg = q_components[i];
				if (arg == null)
					throw new VUrlException("Empty query argument");
				String arg_name = null;
				String arg_sval = null;
				if (arg.contains("=")) {
					String[] split = arg.split("=");
					if (split.length != 2 || split[0] == null || split[1] == null)
						throw new VUrlException("Invalid name=value query argument encoding");
					arg_name = split[0];
					try {
						arg_name = URLDecoder.decode(arg_name, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new VUrlException("Invalid query argument name encoding");
					}
					arg_sval = split[1];
				}
				else
					arg_sval = arg;
				try {
					arg_sval = URLDecoder.decode(arg_sval, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new VUrlException("Invalid query argument name encoding");
				}

				// Resolve string-encoded arguments to the appropriate type and value
				Object arg_val = VUrlData.argFromString(arg_sval);

				if (arg_name == null)
					args.addLast(arg_val);
				else {
					if (query_kargs.get(arg_name) != null)
						throw new VUrlException("Duplicate query argument name");
					query_kargs.put(arg_name, arg_val);
				}
			}
			query_args = args.toArray(new Object[0]);
		}
		else {
			query_args = null;
			query_kargs = null;
		}
	}

	/**
	 * Decodes a string-encoded VRI argument value to its native (typed) representation.
	 *
	 * @param arg string-encoded VRI argument value
	 * @return decoded value of appropriate type
	 * @throws VUrlException invalid argument encoding
	 */
	public static Object argFromString(String arg)
		throws VUrlException {
		if (!arg.contains(":"))
			return arg;
		if (arg.startsWith("str:"))
			return arg.substring(4);
		else if (arg.startsWith("int:")) {
			String num_str = arg.substring(4);
			try {
				return VInteger.normalize(new BigInteger(num_str));
			} catch (Exception e) {
				throw new VUrlException("Invalid argument encoding");
			}
		}
		else if (arg.startsWith("bool:")) {
			String bool_str = arg.substring(4);
			if (bool_str.equals("True"))
				return true;
			else if (bool_str.equals("False"))
				return false;
			else
				throw new VUrlException("Invalid argument encoding");
		}
		else
			return arg;
	}

	/**
	 * Converts a VRI query argument to its string representation.
	 *
	 * @param obj argument to convert
	 * @return string representation
	 * @throws VUrlException argument cannot be converted
	 */
	public static String argToString(Object obj)
		throws VUrlException {
		if (obj instanceof String) {
			String str = (String)obj;
			if (str.contains(":"))
				return "str:" + str;
			else
				return str;
		}
		else if (obj instanceof Boolean) {
			if ((Boolean)obj)
				return "bool:True";
			else
				return "bool:False";
		}
		else if (obj instanceof Integer || obj instanceof Long || obj instanceof BigInteger)
			return "int:" + obj;
		else
			throw new VUrlException("");
	}

	/**
	 * VRI scheme name.
	 *
	 * @return scheme name
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * VRI domain component.
	 *
	 * @return domain
	 */
	public String getDomain() {
		return host;
	}

	/**
	 * VRI port component.
	 *
	 * @return port number (-1 if not set)
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * VRI path elements.
	 *
	 * @return path elements
	 */
	public String[] getPath() {
		return path;
	}

	/**
	 * VRI query name.
	 *
	 * @return query name (null if no query)
	 */
	public String getQueryName() {
		return query_name;
	}

	/**
	 * VRI query arguments.
	 *
	 * @return query arguments (null if no query)
	 */
	public Object[] getQueryArgs() {
		return query_args;
	}

	/**
	 * VRI named query arguments.
	 *
	 * @return named query arguments (null if no query)
	 */
	public Map<String, Object> getQueryNamedArgs() {
		return query_kargs;
	}
}
