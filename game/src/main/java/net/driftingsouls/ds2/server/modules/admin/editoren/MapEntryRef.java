package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.util.Map;

public class MapEntryRef<K,V>
{
	private K key;
	private V value;

	public MapEntryRef(K key, V value)
	{
		this.key = key;
		this.value = value;
	}

	public MapEntryRef(Map.Entry<K,V> entry)
	{
		this(entry.getKey(), entry.getValue());
	}

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
}
