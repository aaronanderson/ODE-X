package org.apache.ode.spi;

import org.apache.ignite.cache.CacheEntry;

public class CacheEntryImpl<K, V> implements CacheEntry<K, V> {
	private final K key;
	private final V value;

	public CacheEntryImpl(K key, V value) {
		super();
		this.key = key;
		this.value = value;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparable version() {
		throw new UnsupportedOperationException();
	}

}
