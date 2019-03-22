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

package org.versile.common.util;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;


/**
 * Simple bus for passing objects to a set of registered listeners.
 *
 * @param <T> the object type passed over the bus
 */
public class VSimpleBus<T> {

	Hashtable<Long, WeakReference<VSimpleBusListener<T>>> _listeners;
	VLinearIDProvider _l_id_gen;

	public VSimpleBus() {
		_listeners = new Hashtable<Long, WeakReference<VSimpleBusListener<T>>>();
		_l_id_gen = new VLinearIDProvider();
	}

	/**
	 * Registers a listener for receiving message bus objects.
	 *
	 * <p>Added listeners are tracked only with weak references. It is
	 * the responsibility of the caller to retain a reference until
	 * the listener can be unregistered.</p>
	 *
	 * @param listener listener for message bus objects
	 * @return listener ID for the listener
	 */
	public synchronized long register(VSimpleBusListener<T> listener) {
		long _id = _l_id_gen.getID();
		_listeners.put(_id, new WeakReference<VSimpleBusListener<T>>(listener));
		return _id;
	}

	/**
	 * Unregisters a listener.
	 *
	 * @param listenerID listener's ID from listener registration
	 */
	public synchronized void unregisterID(long listenerID) {
		_listeners.remove(listenerID);
	}

	/**
	 * Unregisters a listener.
	 *
	 * @param listener listener object
	 */
	public synchronized void unregisterObj(VSimpleBusListener<T> listener) {
		HashSet<Long> _ids = new HashSet<Long>();
		Enumeration<Long> keys = _listeners.keys();
		while(keys.hasMoreElements()) {
			long _id = keys.nextElement();
			VSimpleBusListener<T> _listener = _listeners.get(_id).get();
			if (_listener == listener)
				_ids.add(_id);
		}
		for (long _id: _ids)
			_listeners.remove(_id);
	}

	/**
	 * Pushes an object to all bus listeners.
	 *
	 * <p>It is important to be aware that an object push is resolved immediately
	 * within the context of this method, and so the target listeners must take care
	 * not to perform any action which could perform a dead-lock with the code which
	 * triggered sending the object.</p>
	 *
	 * @param obj object to push
	 */
	public synchronized void push(T obj) {
		HashSet<Long> _discard_ids = new HashSet<Long>();
		Enumeration<Long> keys = _listeners.keys();
		while(keys.hasMoreElements()) {
			long _id = keys.nextElement();
			VSimpleBusListener<T> _listener = _listeners.get(_id).get();
			if (_listener == null)
				_discard_ids.add(_id);
			else
				_listener.busPush(obj);
		}
		for (long _id: _discard_ids)
			_listeners.remove(_id);
	}
}
