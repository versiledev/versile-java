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

import org.versile.orb.entity.decoder.VTaggedDecoder;


/**
 * Represents the VFE VTagged type.
 *
 * <p>Holds a {@link VEntity} value together with an ordered list of tag values.</p>
 */
public final class VTagged extends VEntity {

	VEntity value;
	VEntity[] tags;

	/**
	 * Initialized with defined value and tags.
	 *
	 * @param value value held by object
	 * @param tags associated tags
	 */
	public VTagged(VEntity value, Iterable<? extends VEntity> tags) {
		this.value = value;

		if (tags != null) {
			int size = 0;
			LinkedList<VEntity> tmp_list = new LinkedList<VEntity>();
			for (VEntity item: tags) {
				tmp_list.addLast(item);
				size++;
			}
			this.tags = new VEntity[size];
			for (int i = 0; i < size; i++)
				this.tags[i] = tmp_list.removeFirst();
		}
		else
			this.tags = new VEntity[0];
	}

	/**
	 * Initialized with defined value and tags.
	 *
	 * @param value value held by object
	 * @param tags associated tags
	 */
	public VTagged(VEntity value, VEntity[] tags) {
		this.value = value;

		if (tags != null) {
			int size = 0;
			LinkedList<VEntity> tmp_list = new LinkedList<VEntity>();
			for (VEntity item: tags) {
				tmp_list.addLast(item);
				size++;
			}
			this.tags = new VEntity[size];
			for (int i = 0; i < size; i++)
				this.tags[i] = tmp_list.removeFirst();
		}
		else
			this.tags = new VEntity[0];
	}

	/**
	 * Get value held by the tagged-value object.
	 *
	 * @return value
	 */
	public VEntity getValue() {
		return value;
	}

	/**
	 * Get tags set on the tagged-value object.
	 *
	 * @return tags
	 */
	public VEntity[] getTags() {
		return tags;
	}

	@Override
	public Object _v_native() {
		return this;
	}

	@Override
	public String toString() {
		String result = "VTagged(" + value + ":";
		boolean first = true;
		for (VEntity item: tags) {
			if (first)
				first = false;
			else
				result += ", ";
			result += item;
		}
		result += ")";
		return result;
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		byte[] header;
		if (explicit)
			header = new byte[] {(byte)0xfe};
		else
			header = new byte[0];
		VEncoderData result = new VEncoderData(header, new byte[0]);
		LinkedList<VEntity> items = new LinkedList<VEntity>();
		items.addLast(value);
		for (VEntity item: tags)
			items.addLast(item);
		result.addEmbedded(new VTuple(items), false);
		return result;
	}

	/**
	 * Generate a reader for reading this entity class from (explicit) serialized data.
	 *
	 * @param ctx serialization I/O context
	 * @return reader
	 */
	public static VEntityReader _v_reader(VIOContext ctx) {
		VEntityReader reader = new VEntityReader();
		try {
			reader.setDecoder(new VTaggedDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VTagged) {
			VTagged o_tagged = (VTagged) other;
			if (!o_tagged.value.equals(value))
				return false;
			if (o_tagged.tags.length != tags.length)
				return false;
			for (int i = 0; i < tags.length; i++)
				if (!o_tagged.tags[i].equals(tags[i]))
					return false;
			return true;
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode() ^ tags.hashCode();
	}
}
