package ru.pokrasko.accountservice;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLRUCache<K, V> implements Cache<K, V> {
    private static final int MAX_CAPACITY = 4096;

    private int capacity;
    private int size;

    private LinkedList<K> list = new LinkedList<>();
    private Map<K, V> map = new ConcurrentHashMap<>();

    public ConcurrentLRUCache() {
        this.capacity = MAX_CAPACITY;
    }

    public ConcurrentLRUCache(int capacity) throws IllegalArgumentException {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("the cache capacity should be from 1 to 4096 elements");
        }
        this.capacity = capacity;
    }

    @Override
    public V get(K key) {
        boolean isInCache;
        synchronized (this) {
            isInCache = list.remove(key);
            if (isInCache) {
                list.addFirst(key);
            }
        }
        return isInCache ? map.get(key) : null;
    }

    @Override
    public void put(K key, V value) {
        K keyToRemove = null;
        synchronized (this) {
            if (list.remove(key)) {
                --size;
            }
            if (size == capacity) {
                keyToRemove = list.pollLast();
                --size;
            }
            list.addFirst(key);
        }
        if (keyToRemove != null) {
            map.remove(keyToRemove);
        }
        map.put(key, value);
    }
}
