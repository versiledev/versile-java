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
 * Configuration parameters for a byte streamer.
 *
 * <p>Overrides some of the default which are more appropriate for a byte connection.</p>
 *
 * <ul>
 *   <li><i>maxReadPkgSize</i> is max size of a read package, default is 100000.</li>
 *   <li><i>maxWritePkgSize</i> is max size of a write package, default is 100000.</li>
 * </ul>
 */
public class VByteStreamerConfig extends VStreamerConfig {

	public VByteStreamerConfig() {
		this.setMaxReadPkgSize(100000);
		this.setMaxWritePkgSize(100000);
	}
}
