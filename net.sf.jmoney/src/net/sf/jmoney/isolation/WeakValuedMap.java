/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.isolation;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WeakValuedMap<K, V> {

	/**
	 * We define our own subclass of WeakReference which contains not only the
	 * value but also the key to make it easier to find the entry in the HashMap
	 * after it's been garbage collected.
	 */
	private static class WeakValueReference<K, V> extends WeakReference<V> {
		/**
		 * This key is always set when this weak reference is created and added
		 * to the map. However, there are two reasons why an entry may be
		 * removed from the map. An entry may be removed because there are no
		 * strong references to the value, but an entry may also be removed by
		 * an explicit call to the <code>remove</code> method. It is possible
		 * that an entry may be removed by a call to the <code>remove</code>
		 * method and then later the reference queue is processed and a second
		 * attempt is made to remove the entry. You may think this is not a
		 * problem because the attempt to remove the entry the second time will
		 * simply do nothing. However, it is possible that another entry has
		 * been put in the map by a call to the <code>put</code> method using
		 * the same key value. The queue processor would then be removing not a
		 * dead entry but an active entry.
		 * 
		 * This can be solved as follows. If an entry is explicitly deleted from
		 * the map then the entry is removed from the map AND the key in this
		 * reference object is set to null.
		 */
		private K key;

		private WeakValueReference(V value, K key, ReferenceQueue<V> queue) {
			super(value, queue);
			this.key = key;
		}
	}

	private Map<K, WeakValueReference<K, V>> map = new HashMap<K, WeakValueReference<K, V>>();

	private ReferenceQueue<V> referenceQueue = new ReferenceQueue<V>();

	/**
	 * 
	 * @param key the key which must not be null
	 * @param value the value to which a weak reference is maintained
	 */
	public void put(K key, V value) {
		removeDeadEntries();
		
		map.put(
				key, 
				new WeakValueReference<K, V>(value, key, referenceQueue));
	}

	/**
	 * 
	 * @param key
	 * @return the value if it is in the map, or null if
	 * 		either the key was not in the map or if the value
	 * 		had been garbage collected
	 */
	public V get(K key) {
		WeakReference<V> valueReference = map.get(key);
		if (valueReference == null) {
			return null;
		} else {
			return valueReference.get();
		}
	}

	public void remove(K key) {
		/*
		 * This line is necessary to stop the reference queue processor from
		 * deleting an active entry if the same key was re-used for a new
		 * object.
		 */
		map.get(key).key = null;

		map.remove(key);
	}

	/**
	 * Check the queue for weak references to the value objects
	 * that no longer exist.  It really does not matter where this is
	 * done as long as it is done on the thread for this class (this
	 * class is not thread-safe).
	 */
	private void removeDeadEntries() {
		Reference<? extends V> valueReference = referenceQueue.poll();
		while (valueReference != null) {
			Object key = ((WeakValueReference<?,?>) valueReference).key;
			if (key != null) {
				map.remove(key);
			}
			valueReference = referenceQueue.poll();
		}
	}
}
