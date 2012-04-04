package org.apache.ode.spi.exec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public abstract class ListMap<K, V> implements List<V> {
	HashMap<K, V> map = new HashMap<K, V>();
	ArrayList<K> order = new ArrayList<K>();
	//TODO it is possible that a key is inserted multiple times with only one entry in the map. Should update the order update to ignore duplicates
	static public <V, K> V get(K key, List<V> listMap) {
		if (!(listMap instanceof ListMap)) {
			return null;
		}
		ListMap<K, V> lmap = (ListMap<K, V>) listMap;
		Map<K, V> mapView = lmap.mapView();
		return mapView.get(key); 
	}

	public Map<K, V> mapView() {
		return map;
	}

	public abstract K getKey(V value);

	public K getConfirmedKey(V value) {
		K key = getKey(value);
		if (key == null) {
			throw new IllegalArgumentException("ListMap key must always be present");
		}
		return key;
	}

	@Override
	public boolean add(V e) {
		K key = getConfirmedKey(e);
		if (map.put(key, e) == null) {
			return false;
		}
		return order.add(key);
	}

	@Override
	public void add(int index, V e) {
		K key = getConfirmedKey(e);
		map.put(key, e);
		order.add(index, key);
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		List<K> tempKeys = new ArrayList<K>();
		boolean result = true;
		for (V v : c) {
			K key = getConfirmedKey(v);
			if (map.put(key, v) == null) {
				result = false;
			}
			tempKeys.add(key);
		}

		return result && order.addAll(tempKeys);
	}

	@Override
	public boolean addAll(int index, Collection<? extends V> c) {
		List<K> tempKeys = new ArrayList<K>();
		boolean result = true;
		for (V v : c) {
			K key = getConfirmedKey(v);
			if (map.put(key, v) == null) {
				result = false;
			}
		}
		return result && order.addAll(index, tempKeys);
	}

	@Override
	public void clear() {
		map.clear();
		order.clear();

	}

	@Override
	public boolean contains(Object o) {
		return map.containsValue(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!map.containsValue(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public V get(int index) {
		return map.get(order.get(index));
	}

	@Override
	public int indexOf(Object o) {
		try {
			K key = getConfirmedKey((V) o);
			return order.indexOf(key);
		} catch (ClassCastException ce) {
			return -1;
		}

	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Iterator<V> iterator() {
		return map.values().iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		try {
			K key = getConfirmedKey((V) o);
			return order.lastIndexOf(key);
		} catch (ClassCastException ce) {
			return -1;
		}
	}

	@Override
	public ListIterator<V> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<V> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		try {
			K key = getConfirmedKey((V) o);
			if (!order.remove(key)) {
				return false;
			}
			if (map.remove(key) == null) {
				return false;
			}
			return true;
		} catch (ClassCastException ce) {
			return false;
		}
	}

	@Override
	public V remove(int index) {
		K key = order.remove(index);
		if (key == null) {
			return null;
		}
		return map.remove(key);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = true;
		for (Object o : c) {
			try {
				K key = getConfirmedKey((V) o);
				if (!order.remove(key)) {
					result = false;
				}
				if (map.remove(key) == null) {
					result = false;
				}
			} catch (ClassCastException ce) {
				result = false;
			}
		}

		return result;

	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V set(int index, V element) {
		K key = getConfirmedKey(element);
		order.set(index, key);
		return map.put(key, element);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		return map.values().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return map.values().toArray(a);
	}
}
