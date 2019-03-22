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

import java.util.Vector;


/**
 * Internal data structure for {@link VEntity} serialization encoders.
 *
 * <p>Primarily intended for internal use by the Versile Java framework.</p>
 */
public final class VEncoderData {

	/**
	 * Internal data structure for encoder data.
	 */
	public static class Embedded {
		VEntity element;
		boolean explicit;

		public Embedded(VEntity element, boolean explicit) {
			this.element = element;
			this.explicit = explicit;
		}
		public VEntity getElement() {
			return element;
		}

		public boolean isExplicit() {
			return explicit;
		}
	}

	byte[] header, payload;
	Vector<Embedded> embedded;

	public VEncoderData(byte[] header, byte[] payload) {
		this.header = header;
		this.payload = payload;
		embedded = new Vector<Embedded>();
	}

	public void addEmbedded(VEntity element, boolean explicit) {
		embedded.add(new Embedded(element, explicit));
	}

	public Vector<Embedded> getEmbedded() {
		return embedded;
	}

	public byte[] getHeader() {
		return header;
	}

	public byte[] getPayload() {
		return payload;
	}
}
