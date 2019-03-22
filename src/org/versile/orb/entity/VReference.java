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

import java.util.List;

import org.versile.common.call.VCall;
import org.versile.common.util.VExceptionProxy;
import org.versile.orb.entity.decoder.VReferenceDecoder;


/**
 * Reference to a VFE VObject type.
 *
 * <p>References a (possibly remote) VFE VObject.</p>
 */
public abstract class VReference extends VObject {

	/**
	 * I/O context which holds the reference's ID space.
	 */
	protected VObjectIOContext ctx;

	/**
	 * Reference's ID in its I/O context.
	 */
	protected Number peer_id;

	/**
	 * Set up reference object.
	 *
	 * <p>Should normally not be instantiated directly, but instead be created
	 * with {@link VObjectIOContext#createPeerReference(Number)}.</p>
	 *
	 * @param ctx context for the reference
	 * @param peer_id reference ID on context
	 */
	public VReference(VObjectIOContext ctx, Number peer_id) {
		this.ctx = ctx;
		this.peer_id = peer_id;
	}

	// Notifies owning I/O context when the object is no longer referenced
	@Override
	protected void finalize() throws Throwable {
		try {
	         ctx.referenceDeref(peer_id);
	     } finally {
	         super.finalize();
	     }
	 }

	/**
	 * The references' owning I/O context
	 *
	 * <p>A VObject reference is local to an I/O context which provides an ID space
	 * for remote references. This method returns the context which holds this
	 * remote-object reference.</p>
	 *
	 * @return owning context
	 */
	public VObjectIOContext _v_context() {
		return ctx;
	}

	/**
	 * Perform a blocking method call on the referenced object.
	 *
	 * <p>Similar call format as {@link VObject}, however the call is not handled locally but
	 * is submitted to the referenced object.</p>
	 *
	 * @see org.versile.orb.entity.VObject#_v_call(java.util.List, org.versile.orb.entity.VObject._v_CallType, org.versile.orb.entity.VCallContext)
	 */
	@Override
	public abstract Object _v_call(List<Object> args, _v_CallType type, VCallContext ctx)
			throws Exception;

	/**
	 * Perform a non-blocking remote method call on the object.
	 *
	 * <p>Similar call format as {@link VObject}, however the call is not handled locally but
	 * is submitted to the referenced object.</p>
	 *
	 * @see org.versile.orb.entity.VObject#_v_call_nowait(java.util.List, org.versile.orb.entity.VObject._v_CallType, org.versile.orb.entity.VCallContext)
	 */
	@Override
	public abstract VCall<Object> _v_call_nowait(List<Object> args, _v_CallType type, VCallContext ctx);

	/**
	 * Should never be called, throws a runtime exception.
	 */
	@Override
	protected final Object _v_execute(List<Object> args, VCallContext ctx)
		throws VExceptionProxy, VCallError {
		throw new RuntimeException();
	}

	@Override
	public VEncoderData _v_encode(VIOContext ctx, boolean explicit)
			throws VEntityWriterException {
		if (!(ctx instanceof VObjectIOContext))
			throw new VEntityWriterException("Encoding VObject requires a VObjectIOContext");
		VEncoderData result = new VEncoderData(new byte[] {(byte)0xfd}, new byte[0]);
		result.addEmbedded(new VInteger(peer_id), false);
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
			reader.setDecoder(new VReferenceDecoder(ctx, true));
		} catch (VEntityReaderException e) {
			throw new RuntimeException();
		}
		return reader;
	}
}
