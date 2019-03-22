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

import org.versile.common.util.VByteBuffer;
import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.decoder.VExceptionDecoder;


/**
 * Represents the VFE VBoolean type.
 *
 * <p>Holds exception data which is a (possibly empty) ordered list of entities.
 * Note that because this class does not inherit from Exception it cannot be
 * thrown directly as an exception, instead an exception proxy should be
 * generated via {@link #getProxy()}.</p>
 */
public final class VException extends VEntity {

	VEntity[] value;

	/**
	 * Initialize empty exception.
	 */
	public VException() {
		this.value = new VEntity[0];
	}

	/**
	 * Initialize with provided exception value elements.
	 *
	 * @param value iterable with exception elements
	 */
	public VException(VEntity[] value) {
		this.value = new VEntity[value.length];
		for (int i = 0; i < value.length; i++)
			this.value[i] = value[i];
	}

	/**
	 * Initialize with provided exception value elements.
	 *
	 * @param value iterable with exception elements
	 */
	public VException(Iterable<? extends VEntity> value) {
		int size = 0;
		LinkedList<VEntity> tmp_list = new LinkedList<VEntity>();
		for (VEntity item: value) {
			tmp_list.addLast(item);
			size++;
		}
		this.value = new VEntity[size];
		for (int i = 0; i < size; i++)
			this.value[i] = tmp_list.removeFirst();
	}

	/**
	 * Initialize with the provided message.
	 *
	 * <p>Creates an exception with a single {@link VString} element.</p>
	 *
	 * @param msg message
	 */
	public VException(String msg) {
		this.value = new VEntity[] {new VString(msg)};
	}

	@Override
	public VEntity[] _v_native() {
		return value;
	}

	@Override
	public String toString() {
		String result = "VException(";
		boolean first = true;
		for (VEntity item: value) {
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
		VByteBuffer header = new VByteBuffer();
		if (explicit)
			header.append(new byte[] {(byte)0xf7});
		header.append(VInteger.posint_to_netbytes(value.length));
		VEncoderData result = new VEncoderData(header.popAll(), new byte[0]);
		for (VEntity item: value)
			result.addEmbedded(item,  true);
		return result;
	}

	/**
	 * Returns a proxy to the {@link VException}.
	 *
	 * @return proxy exception
	 */
	public VExceptionProxy getProxy() {
		return new VExceptionProxy(this);
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
			reader.setDecoder(new VExceptionDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VException) {
			VException o_exc = (VException) other;
			if (o_exc.value.length != value.length)
				return false;
			for (int i = 0; i < value.length; i++)
				if (!o_exc.value[i].equals(value[i]))
					return false;
			return true;
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
