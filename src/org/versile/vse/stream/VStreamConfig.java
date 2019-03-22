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

package org.versile.vse.stream;


/**
 * Configuration parameters for a stream connection.
 *
 * <p>Default configuration parameters are defined for object-based communication. For
 * byte communication, {@link VByteStreamConfig has more appropriate defaults.}</p>
 *
 * <ul>
 *
 *   <li><i>maxReadPackages</i> is the max parallel read packages, default is 10.</li>
 *
 *   <li><i>maxReadPkgSize</i> is max size of a read package, default is 1000.</li>
 *
 *   <li><i>maxWritePackages</i> is the max parallel write packages, default is 10.</li>
 *
 *   <li><i>maxWritePkgSize</i> is max size of a write package, default is 1000.</li>
 *
 *   <li><i>maxCalls</i> is the max pending remote calls in addition to read
 *   packages, default is 5.</li>
 *
 *   <li><i>requiredMode</i> is a required combination of stream mode flags for the stream
 *   mode which must be enabled on the stream, default is null (no mode requirement).</li>
 *
 *   <li><i>readAhead</i> defines whether a stream is set up with read-ahead, default is false.</li>
 *
 *   <li><i>requestEOS</i> defines whether a stream is set up with end-of-stream notification
 *   on read streams, default is true.</li>
 *
 * </ul>
 */
public class VStreamConfig {

	int maxReadPackages = 10;
	int maxReadPkgSize = 1000;
	int maxWritePackages = 10;
	int maxWritePkgSize = 1000;
	int maxCalls = 5;
	VStreamMode requiredMode = null;
	boolean readAhead = false;
	boolean requestEOS = true;

	@Override
	public VStreamConfig clone() {
		VStreamConfig result = new VStreamConfig();
		this.copyTo(result);
		return result;
	}

	public int getMaxReadPackages() {
		return maxReadPackages;
	}

	public void setMaxReadPackages(int maxReadPackages) {
		this.maxReadPackages = maxReadPackages;
	}

	public int getMaxReadPkgSize() {
		return maxReadPkgSize;
	}

	public void setMaxReadPkgSize(int maxReadPkgSize) {
		this.maxReadPkgSize = maxReadPkgSize;
	}

	public int getMaxWritePackages() {
		return maxWritePackages;
	}

	public void setMaxWritePackages(int maxWritePackages) {
		this.maxWritePackages = maxWritePackages;
	}

	public int getMaxWritePkgSize() {
		return maxWritePkgSize;
	}

	public void setMaxWritePkgSize(int maxWritePkgSize) {
		this.maxWritePkgSize = maxWritePkgSize;
	}

	public int getMaxCalls() {
		return maxCalls;
	}

	public void setMaxCalls(int maxCalls) {
		this.maxCalls = maxCalls;
	}

	public VStreamMode getRequiredMode() {
		if (requiredMode != null)
			return requiredMode.clone();
		else
			return null;
	}

	public void setRequiredMode(VStreamMode requiredMode) {
		this.requiredMode = requiredMode.clone();
	}

	public boolean isReadAhead() {
		return readAhead;
	}

	public void setReadAhead(boolean readAhead) {
		this.readAhead = readAhead;
	}

	public boolean isRequestEOS() {
		return requestEOS;
	}

	public void setRequestEOS(boolean requestEOS) {
		this.requestEOS = requestEOS;
	}

	protected void copyTo(VStreamConfig config) {
		config.maxReadPackages = maxReadPackages;
		config.maxReadPkgSize = maxReadPkgSize;
		config.maxWritePackages = maxWritePackages;
		config.maxWritePkgSize = maxWritePkgSize;
		config.maxCalls = maxCalls;
		config.readAhead = readAhead;
		config.requestEOS = requestEOS;
	}
}
