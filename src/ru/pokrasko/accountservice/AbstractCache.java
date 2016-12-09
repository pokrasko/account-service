package ru.pokrasko.accountservice;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractCache<K, V> implements Cache<K, V> {
    private static final int MAX_CAPACITY = 65536;

    int capacity;
    private int size;

    LinkedList<K> list = new LinkedList<>();
    Map<K, V> map = new ConcurrentHashMap<>();

    AbstractCache() {
        this(null, MAX_CAPACITY);
    }

    AbstractCache(int capacity) throws IllegalArgumentException {
        this(null, capacity);
    }

    AbstractCache(Map<K, V> initialMap) {
        this(initialMap, MAX_CAPACITY);
    }

    AbstractCache(Map<K, V> initialMap, int capacity) throws IllegalArgumentException {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("the cache capacity should be from 1 to 4096 elements");
        }
        this.capacity = capacity;

        if (initialMap != null && initialMap.size() <= capacity) {
            this.map = initialMap;
            list.addAll(initialMap.keySet());
        } else {
            this.map = new ConcurrentHashMap<>();
            if (initialMap != null) {
                for (Map.Entry<K, V> entry : initialMap.entrySet()) {
                    this.map.put(entry.getKey(), entry.getValue());
                    list.add(entry.getKey());
                    if (this.map.size() == capacity) {
                        break;
                    }
                }
            }
        }
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
