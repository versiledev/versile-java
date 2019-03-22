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

package org.versile.reactor.io.vec;


/**
 * VEC channel configuration parameters.
 *
 * <p>'bufferLength' is the buffer size for byte data input/output.</p>
 *
 * <p>'queueLength' is the queue size of held VEntity entities
 * reconstructed from serialized data.</p>
 *
 * <p>'stringEncoding' is an output string encoding to be set on the
 * entity channel's I/O context. It must be a standard encoding name
 * for the VEC protocol. If 'null' no new string encoding is set on the
 * context. The default value is "utf8".</p>
 */
public class VEntityChannelConfig {

	// When adding fields remember to update copyTo()
	int bufferLength = 4096;
	int queueLength = 10;
	String stringEncoding = "utf8";

	@Override
	public VEntityChannelConfig clone() {
		VEntityChannelConfig result = new VEntityChannelConfig();
		this.copyTo(result);
		return result;
	}

	public int getBufferLength() {
		return bufferLength;
	}

	public void setBufferLength(int bufferLength) {
		this.bufferLength = bufferLength;
	}

	public int getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}

	public String getStringEncoding() {
		return stringEncoding;
	}

	public void setStringEncoding(String stringEncoding) {
		this.stringEncoding = stringEncoding;
	}

	protected void copyTo(VEntityChannelConfig config) {
		config.bufferLength = bufferLength;
		config.queueLength = queueLength;
		config.stringEncoding = stringEncoding;
	}
}
