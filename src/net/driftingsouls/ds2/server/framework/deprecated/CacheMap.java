/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.deprecated;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map mit begrenzter Groesse. Sobald die maximale groesse erreicht wurde,
 * wird das am laengsten nicht mehr verwendete Element entfernt.
 * Die Implementierung ist nicht Thread-Safe.
 * 
 * @author Christopher Jung
 * @param <K> Typ des Parameterschluessels
 * @param <V> Typ des Parameterwerts
 * @deprecated Bitte auf Hibernate umstellen
 */
@Deprecated
public class CacheMap<K,V> implements Map<K,V> {
	private long maxsize;
	private Map<K,V> map;
	private Map<Object, Long> timestamps;
	
	/**
	 * Erstellt eine neue CacheMap
	 * @param maxsize die maximale Groesse der Map 
	 */
	public CacheMap(long maxsize) {
		this.maxsize = maxsize;
		map = new HashMap<K,V>((int)maxsize/4);
		timestamps = new HashMap<Object,Long>();
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public V get(Object key) {
		if( timestamps.containsKey(key) ) {
			timestamps.put(key, System.currentTimeMillis());
		}
		return map.get(key);
	}

	public V put(K key, V value) {
		if( map.size() + 1 > maxsize ) {
			deleteOldest();
		}
		return map.put(key, value);
	}

	public V remove(Object key) {
		timestamps.remove(key);
		return map.remove(key);
	}

	public void putAll(Map<? extends K, ? extends V> t) {
		if( map.size() + t.size() > maxsize ) {
			long count = -maxsize + map.size() + t.size();
			for( int i=0; i < count; i++ ) {
				deleteOldest();
			}
		}
		map.putAll(t);
	}

	public void clear() {
		map.clear();
	}
	
	protected Map<Object,Long> getTimeStamps() {
		return timestamps;
	}
	
	private abstract class CacheIterator<E,F,G> implements Iterator<E> {
		protected Iterator<Entry<F,G>> iter = null;
		protected Entry<F,G> current = null;
		
		CacheIterator(Iterator<Entry<F,G>> iter) {
			this.iter = iter;
		}
		
		public boolean hasNext() {
			return iter.hasNext();
		}

		public abstract E next();
		
		public void remove() {
			iter.remove();
			CacheMap.this.getTimeStamps().remove(current.getKey());
		}	
	}
	
	private class KeyIterator<E> extends CacheIterator<E,E,V> {
		KeyIterator(Iterator<Entry<E, V>> iter) {
			super(iter);
		}
		
		@Override
		public E next() {
			current = iter.next();
			return current.getKey();
		}
	}
	
	private class ValueIterator<E> extends CacheIterator<E,K,E> {
		ValueIterator(Iterator<Entry<K, E>> iter) {
			super(iter);
		}
		
		@Override
		public E next() {
			current = iter.next();
			return current.getValue();
		}
	}
	
	private class EntryIterator extends CacheIterator<Entry<K,V>,K,V> {
		EntryIterator(Iterator<Entry<K, V>> iter) {
			super(iter);
		}
		
		@Override
		public Entry<K,V> next() {
			current = iter.next();
			return current;
		}
	}
	
	private abstract class CacheSet<E,F,G> extends AbstractSet<E> {
		protected Set<Entry<F,G>> set = null;
		
		CacheSet(Set<Entry<F,G>> set) {
			this.set = set;
		}

		@Override
		public int size() {
			return set.size();
		}
	}
	
	private class KeySet<E> extends CacheSet<E,E,V> {
		KeySet(Set<Entry<E, V>> set) {
			super(set);
		}

		@Override
		public Iterator<E> iterator() {
			return new KeyIterator<E>(set.iterator());
		}
	}
	
	private class EntrySet extends CacheSet<Entry<K,V>,K,V> {
		EntrySet(Set<Entry<K, V>> set) {
			super(set);
		}

		@Override
		public Iterator<Entry<K,V>> iterator() {
			return new EntryIterator(set.iterator());
		}
	}
	
	private class Values extends AbstractCollection<V> {
		protected Set<Entry<K, V>> set = null;
		
		Values(Set<Entry<K, V>> set) {
			this.set = set;
		}
		
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator<V>(set.iterator());
		}

		@Override
		public int size() {
			return set.size();
		}
		
	}

	public Set<K> keySet() {
		return new KeySet<K>(map.entrySet());
	}

	public Collection<V> values() {
		return new Values(map.entrySet());
	}

	public Set<Entry<K, V>> entrySet() {
		return new EntrySet(map.entrySet());
	}

	private void deleteOldest() {
		long oldest = System.currentTimeMillis()+1;
		Object oldestKey = null;
		for( Object key : timestamps.keySet() ) {
			long value = timestamps.get(key);
			if( (oldestKey == null) || ( value <= oldest) ) {
				oldest = value;
				oldestKey = key;
			}
		}
		
		if( oldestKey != null ) {
			timestamps.remove(oldestKey);
			map.remove(oldestKey);
		}
	}
}
