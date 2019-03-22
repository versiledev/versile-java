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

import java.util.LinkedList;
import java.util.Vector;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.VEntityWriterException;


/**
 * Writer for serialized {@link VEntity} data.
 */
public final class VEntityWriter {

	VIOContext ctx;
	boolean explicit;
	VEntity entity = null;
	VByteBuffer write_data;
	boolean done = false;

	/**
	 * Set up writer with provided serialization parameters
	 *
	 * @param ctx serialization context
	 * @param explicit if true serialize with explicit encoding
	 */
	public VEntityWriter(VIOContext ctx, boolean explicit) {
		this.ctx = ctx;
		this.explicit = explicit;
		write_data = new VByteBuffer();
	}

	/**
	 * Write serialized data.
	 *
	 * @param num max bytes to write
	 * @return serialized data
	 */
	public synchronized byte[] write(int num) {
		if (entity == null || done)
			return new byte[0];
		byte[] result = write_data.pop(num);
		if (write_data.length() == 0) {
			entity = null;
			done = true;
		}
		return result;
	}

	/**
	 * Write all remaining serialized data.
	 *
	 * @return serialized data
	 */
	public synchronized byte[] writeAll() {
		return this.write(write_data.length());
	}

	/**
	 * Check if serialization of current object was completed.
	 *
	 * @return true if serialization was completed
	 */
	public synchronized boolean isDone() {
		return done;
	}

	/**
	 * Set another entity to serialize.
	 *
	 * @param entity entity to serialize
	 * @throws VEntityWriterException if previous serialization was not completed
	 */
	public synchronized void setEntity(VEntity entity)
			throws VEntityWriterException {
		if (this.entity != null && !done) {
			throw new VEntityWriterException("Current write operation not finished");
		}

		LinkedList<byte[]> headers = new LinkedList<byte[]>();
		LinkedList<VEncoderData.Embedded> embedded = new LinkedList<VEncoderData.Embedded>();
		LinkedList<byte[]> payloads = new LinkedList<byte[]>();
		embedded.addLast(new VEncoderData.Embedded(entity, explicit));

		while (embedded.size() > 0) {
			VEncoderData.Embedded _emb = embedded.removeFirst();
			VEntity element = _emb.getElement();
			boolean _explicit = _emb.isExplicit();
			VEncoderData data = element._v_encode(ctx, _explicit);
			byte[] _header = data.getHeader();
			if (_header != null)
				headers.addLast(_header);
			byte[] _payload = data.getPayload();
			if (_payload != null)
				payloads.addFirst(_payload);
			Vector<VEncoderData.Embedded> _elist = data.getEmbedded();
			for (int i = (_elist.size() - 1) ; i >= 0; i--) {
				VEncoderData.Embedded _lemb = _elist.elementAt(i);
				embedded.addFirst(_lemb);
			}
		}

		this.entity = entity;
		done = false;
		write_data.clear();
		for (byte[] data: headers)
			write_data.append(data);
		for (byte[] data: payloads)
			write_data.append(data);
	}

	/**
	 * Reset the writer, aborting any write operation in progress.
	 */
	public synchronized void reset() {
		entity = null;
		write_data.clear();
		done = false;
	}
}
