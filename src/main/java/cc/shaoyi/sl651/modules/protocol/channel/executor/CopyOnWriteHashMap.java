package cc.shaoyi.sl651.modules.protocol.channel.executor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年06月01日 16:26
 */
public class CopyOnWriteHashMap<K,V> extends ConcurrentHashMap<K,V> {

	volatile ConcurrentHashMap<K, V> view;

	private ConcurrentHashMap<K, V> duplicate(ConcurrentHashMap<K, V> original) {
		return new ConcurrentHashMap<>(original);
	}

	public CopyOnWriteHashMap(ConcurrentHashMap<K, V> that) {
		this.view = duplicate(that);
	}

	public CopyOnWriteHashMap() {
		this(new ConcurrentHashMap<>());
	}

	@Override
	public int size() {
		return view.size();
	}

	@Override
	public boolean isEmpty() {
		return view.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return view.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return view.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return view.get(key);
	}

	@Override
	public KeySetView<K, V> keySet() {
		return view.keySet();
	}

	@Override
	public Collection<V> values() {
		return view.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return view.entrySet();
	}

	@Override
	public String toString() {
		return view.toString();
	}



	@Override
	public V put(K key, V value) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			V result = newCore.put(key, value);
			view = newCore; // volatile write
			return result;
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			V result = newCore.remove(key);
			view = newCore; // volatile write
			return result;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			newCore.putAll(t);
			view = newCore; // volatile write
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			view = new ConcurrentHashMap<>(); // volatile write
		}
	}

}
