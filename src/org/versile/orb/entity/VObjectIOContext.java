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

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.versile.common.util.VLinearIDProvider;


/**
 * An I/O context which provides an ID space for {@link VObject} references.
 */
public abstract class VObjectIOContext extends VIOContext {

	/**
	 * Tracked context ID space for local objects.
	 */
	protected Map<Number, LocalObject> local_obj;

	/**
	 * Context IDs of local objects tracked on the context.
	 */
	protected Map<VObject, Number> local_p_ids;

	/**
	 * Lock object for synchronizing access to local ID space fields.
	 */
	protected Lock local_lock;

	VLinearIDProvider local_id_gen;

	/**
	 * Remote object references tracked on the context by peer ID.
	 */
	protected Map<Number, PeerObject> peer_obj;

	/**
	 * Lock object for synchronizing access to context peer object IDs.
	 */
	protected Lock peer_lock;

	/**
	 * Sets up skeleton I/O context.
	 */
	public VObjectIOContext() {
		// Numbers should be tracked in normalized format, see VInteger.normalize()
		local_obj = new Hashtable<Number, LocalObject>();
		local_p_ids = new Hashtable<VObject, Number>();
		local_lock = new ReentrantLock();
		local_id_gen = new VLinearIDProvider();
		peer_obj = new Hashtable<Number, PeerObject>();
		peer_lock = new ReentrantLock();
	}

	/**
	 * Returns a context peer ID for a locally provided {@link VObject}.
	 *
	 * @param obj local object
	 * @param lazy if true lazy-generate a peer ID
	 * @return object peer ID in this context
	 * @throws VEntityError could not retrieve or generate peer ID
	 */
	public Number localToPeerID(VObject obj, boolean lazy)
		throws VEntityError {
		if (obj instanceof VReference)
			if (((VReference)obj)._v_context() == this)
				throw new VEntityError("Object is a remote reference in this context");

		synchronized(local_lock) {
			Number peer_id = local_p_ids.get(obj);
			if (peer_id == null) {
				if (lazy) {
					peer_id = VInteger.normalize(local_id_gen.getID());
					local_p_ids.put(obj, peer_id);
					LocalObject l_obj = new LocalObject(obj, 0);
					local_obj.put(peer_id,  l_obj);
				}
				else
					throw new VEntityError("Local object not registered in this context");
			}
			return peer_id;
		}
	}

	/**
	 * Returns the locally implemented {@link VObject} referenced by peer ID in this context.
	 *
	 * @param peer_id peer ID of object in this context
	 * @return object identified by the peer ID
	 * @throws VEntityError could not retrieve object for provided ID
	 */
	public VObject localFromPeerID(Number peer_id)
		throws VEntityError {
		peer_id = VInteger.normalize(peer_id);
		synchronized(local_lock) {
			LocalObject entry = local_obj.get(peer_id);
			if (entry == null)
				throw new VEntityError("Peer ID not active on context");
			return entry.obj;
		}
	}

	/**
	 * Add a send (write) count for a locally provided {@link VObject}.
	 *
	 * <p>This method should normally be called whenever a {@link VObject} is serialized
	 * in this serialization context</p>
	 *
	 * @param peer_id peer ID of the object in this context
	 * @throws VEntityError invalid peer ID in this context
	 */
	public void localAddSend(Number peer_id)
		throws VEntityError {
		peer_id = VInteger.normalize(peer_id);
		synchronized(local_lock) {
			LocalObject entry = local_obj.get(peer_id);
			if (entry == null)
				throw new VEntityError("Peer ID not active on context");
			entry.send_count++;
		}
	}

	/**
	 * Retrieves a {@link VReference} to a remote {@link VObject} for this context.
	 *
	 * <p>If a {@link VReference} to the object exists then that already existing object is
	 * returned, otherwise a new object is generated.</p>
	 *
	 * @param peer_id peer ID in this context for the remote VObject
	 * @param lazy if True lazy-register peer ID as a remote reference
	 * @return reference to the remote object
	 * @throws VEntityError invalid peer ID in this context
	 */
	public VReference referenceFromPeerID(Number peer_id, boolean lazy)
		throws VEntityError {
		peer_id = VInteger.normalize(peer_id);
		synchronized(peer_lock) {
			PeerObject entry = peer_obj.get(peer_id);
			VReference result = null;
			if (entry != null) {
				result = entry.ref.get();
				if (result == null) {
					// Reference was garbage collected locally, create a new reference
					result = this.createPeerReference(peer_id);
					entry.ref = new WeakReference<VReference>(result);
				}
			}
			else if (lazy) {
				result = this.createPeerReference(peer_id);
				entry = new PeerObject(result, 0);
				peer_obj.put(peer_id,  entry);
			}
			else
				throw new VEntityError("Peer ID not registered on context");

			return result;
		}
	}

	/**
	 * Add a receive (read) count for a remote {@link VObject} reference.
	 *
	 * <p>This method should normally be called whenever a VReference is read
	 * in this serialization context.</p>
	 *
	 * @param peer_id peer ID of the remote {@link VObject} in this context
	 * @throws VEntityError invalid peer ID in this context
	 */
	public void referenceAddRecv(Number peer_id)
		throws VEntityError {
		peer_id = VInteger.normalize(peer_id);
		synchronized(peer_lock) {
			PeerObject entry = peer_obj.get(peer_id);
			if (entry == null)
				throw new VEntityError("Peer ID not registered on context");
			entry.recv_count++;
		}
	}

	/**
	 * Notifies the context a {@link VReference} has no remaining references.
	 *
	 * <p>The default implementation does nothing, derived classes can override.</p>
	 *
	 * <p>Called by {@link VReference#finalize()} during finalization.</p>
	 *
	 * @param peer_id peer ID of the remote {@link VObject} in this context
	 */
	public abstract void referenceDeref(Number peer_id);

	/**
	 * Create a peer object reference object for the reference's id on the context.
	 *
	 * @param peer_id reference ID on context
	 * @return associated referencing object
	 */
	protected abstract VReference createPeerReference(Number peer_id);

	/**
	 * Internal structure for tracking local object references.
	 */
	protected class LocalObject {
		public VObject obj;
		public long send_count;

		public LocalObject(VObject obj, long send_count) {
			this.obj = obj;
			this.send_count = send_count;
		}
	}

	/**
	 * Internal structure for tracking references to remote objects.
	 */
	protected class PeerObject {
		public WeakReference<VReference> ref;
		public long recv_count;

		public PeerObject(VReference obj, long recv_count) {
			this.ref = new WeakReference<VReference>(obj);
			this.recv_count = recv_count;
		}
	}
}
