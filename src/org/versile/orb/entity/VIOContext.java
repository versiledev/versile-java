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

package org.versile.orb.entity;


/**
 * I/O context for serializing or de-serializing {@link VEntity} data.
 *
 * <p>A {@link VIOContext} can be used with types whose encoding does not
 * rely on an I/O context ID space such as {@link VInteger}, however it
 * cannot be used with e.g. {@link VObject} which requires a
 * {@link VObjectIOContext}.</p>
 */
public class VIOContext {

	String strEncoding = null;
	String strDecoding = null;
	boolean preferOIDEncoding = true;

	/**
	 * Set up a default I/O context.
	 */
	public VIOContext() {
	}

	/**
	 * {@link VString} encoding format set on context
	 *
	 * <p>The encoding format is used with {@link VString} serialization for
	 * the serialized-representation format which does not include codec.</p>
	 *
	 * @return encoding format (or null)
	 */
	public String getStrEncoding() {
		return strEncoding;
	}

	/**
	 * Sets a {@link VString} encoding format on the context
	 *
	 * <p>The encoding format is used with {@link VString} serialization for
	 * the serialized-representation format which does not include codec.</p>
	 *
	 * @param str_encoding encoding format
	 */
	public void setStrEncoding(String str_encoding) {
		this.strEncoding = str_encoding;
	}

	/**
	 * {@link VString} decoding format set on context
	 *
	 * <p>The decoding format is used with {@link VString} de-serialization for
	 * the serialized-representation format which does not include codec.</p>
	 */
	public String getStrDecoding() {
		return strDecoding;
	}

	/**
	 * Sets a {@link VString} decoding format on the context
	 *
	 * <p>The decoding format is used with {@link VString} de-serialization for
	 * the serialized-representation format which does not include codec.</p>
	 *
	 * @param str_decoding encoding format
	 */
	public void setStrDecoding(String str_decoding) {
		this.strDecoding = str_decoding;
	}

	/**
	 * Check if OID encoding is the preferred VSE encoding format.
	 *
	 * @return true if OID encoding preferred
	 */
	public boolean isPreferOIDEncoding() {
		return preferOIDEncoding;
	}

	/**
	 * Set VSE encoding preference.
	 *
	 * @param preferOIDEncoding true if OID encoding is preferred over name/version
	 */
	public void setPreferOIDEncoding(boolean preferOIDEncoding) {
		this.preferOIDEncoding = preferOIDEncoding;
	}


}
