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
 * Configuration parameters for a streamer.
 *
 * <ul>
 *
 *   <li><i>writeLim</i> is the max pending data elements for peer write
 *   operations. The default is 10e7.</li>
 *
 *   <li><i>writeStep</i> is the step increment for updating write limit for
 *   peer. If negative the step increment is half of 'writeLim'. The
 *   default is -1.</li>
 *
 * </ul>
 */
public class VStreamerConfig extends VStreamConfig {
	long writeLim = 10000000;
	long writeStep = -1;

	public long getWriteLim() {
		return writeLim;
	}

	public void setWriteLim(long writeLim) {
		this.writeLim = writeLim;
	}

	public long getWriteStep() {
		return writeStep;
	}

	public void setWriteStep(long writeStep) {
		this.writeStep = writeStep;
	}

	protected void copyTo(VStreamConfig config) {
		super.copyTo(config);
	}
}
