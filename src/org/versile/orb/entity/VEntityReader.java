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
import java.util.Iterator;

import org.versile.common.util.VByteBuffer;
import org.versile.orb.entity.decoder.VEntityDecoderBase;


/**
 * Reader for decoding a {@link VEntity} from a serialized representation.
 */
public final class VEntityReader {

	LinkedList<Iterator<VEntityDecoderBase>> h_decoders;
	LinkedList<VEntity> e_results;
	LinkedList<PayloadData> p_decoders;
	VEntityDecoderBase h_decoder = null;
	VEntityDecoderBase p_dec = null;
	boolean initialized = false;
	VEntity result = null;
	boolean failed = false;

	/**
	 * Create uninitialized reader.
	 */
	public VEntityReader() {
		h_decoders = new LinkedList<Iterator<VEntityDecoderBase>>();
		e_results = new LinkedList<VEntity>();
		p_decoders = new LinkedList<PayloadData>();
	}

	class PayloadData {
		public VEntityDecoderBase decoder;
		public int num_embedded;

		public PayloadData(VEntityDecoderBase decoder, int num_embedded) {
			this.decoder = decoder;
			this.num_embedded = num_embedded;
		}
	}

	/**
	 * Reads byte data from a buffer.
	 *
	 * <p>Data which is read is popped off the buffer. The reader will stop reading when
	 * the entity has been fully decoded, there is a decoder error condition, or there
	 * is no more data available in the buffer.</p>
	 *
	 * @param data input data
	 * @return number of bytes read
	 * @throws VEntityReaderException
	 */
	public synchronized int read(VByteBuffer data)
			throws VEntityReaderException {
		this.checkFailed();

		int buf_len = data.length();
		try {
			while (true) {
				if (h_decoder != null || h_decoders.size() > 0) {
					if (h_decoder == null) {
						Iterator<VEntityDecoderBase> h_it = h_decoders.peekLast();
						if (h_it.hasNext()) {
							h_decoder = h_it.next();
						}
						else {
							h_decoders.removeLast();
							continue;
						}
					}
					if (h_decoder.decodeHeader(data)) {
						LinkedList<VEntityDecoderBase> emb = h_decoder.getEmbeddedDecoders();
						int num_embedded = 0;
						if (emb != null) {
							h_decoders.addLast(emb.iterator());
							num_embedded = emb.size();
						}
						PayloadData pdata = new PayloadData(h_decoder, num_embedded);
						p_decoders.addLast(pdata);
						h_decoder = null;
					}
					else
						break;
				}
				else if (p_dec != null || p_decoders.size() > 0) {
					if (p_dec == null) {
						PayloadData pdata = p_decoders.removeLast();
						p_dec = pdata.decoder;
						if (pdata.num_embedded > 0) {
							LinkedList<VEntity> p_emb = new LinkedList<VEntity>();
							for (int i = 0; i < pdata.num_embedded; i++)
								p_emb.addLast(e_results.removeLast());
							p_dec.putEmbeddedEntities(p_emb);
						}
					}
					if (p_dec.decodePayload(data)) {
						e_results.addLast(p_dec.getResult());
							p_dec = null;
					}
					else
						break;
				}

				if (h_decoder == null && h_decoders.size() == 0 && p_decoders.size() == 0) {
					if (e_results.size() == 1) {
						result = e_results.removeFirst();
						break;
					}
					else
						throw new VEntityReaderException("Entity read error");
				}
			}
		} catch (VEntityReaderException e) {
			failed = true;
			throw e;
		}

		return buf_len - data.length();
	}

	/**
	 * Checks whether the reader has completed reconstructing an entity.
	 *
	 * @return true if a {@link VEntity} has been fully decoded
	 * @throws VEntityReaderException reader error
	 */
	public synchronized boolean done ()
			throws VEntityReaderException {
		this.checkFailed();
		return (result != null);
	}

	/**
	 * Get a fully decoded {@link VEntity}.
	 *
	 * @return decoded value
	 * @throws VEntityReaderException decoding not completed, or other reader error
	 */
	public synchronized VEntity getResult()
			throws VEntityReaderException {
		this.checkFailed();
		return result;
	}

	/**
	 * Initializes the reader with a decoder.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 *
	 * @param decoder decoder to set
	 * @throws VEntityReaderException another decode operation currently active
	 */
	public void setDecoder(VEntityDecoderBase decoder)
		throws VEntityReaderException {
		this.checkFailed();
		if (initialized)
			throw new VEntityReaderException("Anoder decoder already active");
		LinkedList<VEntityDecoderBase> h_list = new LinkedList<VEntityDecoderBase>();
		h_list.addLast(decoder);
		h_decoders.addLast(h_list.iterator());
		initialized = true;
	}

	/**
	 * Resets the reader, clearing any currently set decoders.
	 *
	 * <p>Intended primarily for internal use by the Versile Java framework.</p>
	 */
	public void reset() {
		initialized = false;
		h_decoders.clear();
		p_decoders.clear();
		result = null;
		failed = false;
	}

	// Checks whether a failure condition has been set on the reader
	private void checkFailed()
		throws VEntityReaderException {
		if (failed)
			throw new VEntityReaderException("Reader had an earlier failure");
	}
}
