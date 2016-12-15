package ru.pokrasko.accountservice;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class AccountCache {
    private static final int MAX_CAPACITY = 65536;

    private int capacity;

    private LinkedList<Integer> list = new LinkedList<>();
    private Map<Integer, Long> map = new ConcurrentHashMap<>();

    private DatabaseHelper helper;

    AccountCache(DatabaseHelper helper, Map<Integer, Long> initialMap) {
        this(helper, initialMap, MAX_CAPACITY);
    }

    private AccountCache(DatabaseHelper helper, Map<Integer, Long> initialMap, int capacity)
            throws IllegalArgumentException {
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
                for (Map.Entry<Integer, Long> entry : initialMap.entrySet()) {
                    this.map.put(entry.getKey(), entry.getValue());
                    list.add(entry.getKey());
                    if (this.map.size() == capacity) {
                        break;
                    }
                }
            }
        }
        this.helper = helper;
    }

    private boolean checkCache(Integer key, Integer[] keyToRemove) {
        boolean isInCache = list.remove(key);
        if (!isInCache) {
            if (list.size() == capacity) {
                keyToRemove[0] = list.pollLast();
            }
            list.addFirst(key);
        }
        return isInCache;
    }

    private synchronized boolean checkCacheAndRefresh(Integer key, Integer[] keyToRemove, Long[] value)
            throws SQLException {
        boolean isInCache = checkCache(key, keyToRemove);
        if (!isInCache) {
            value[0] = helper.get(key);
        }
        return isInCache;
    }

    private synchronized Integer checkCacheAndUpdate(Integer key, Long value)
            throws SQLException {
        Integer[] result = new Integer[1];
        checkCache(key, result);
        helper.put(key, value);
        return result[0];
    }

    Long get(Integer key) {
        boolean isInCache;
        Integer[] keyToRemove = new Integer[1];
        Long[] value = new Long[1];
        try {
            isInCache = checkCacheAndRefresh(key, keyToRemove, value);
        } catch (SQLException e) {
            System.err.println("SQL exception while updating cache. " + e);
            return null;
        }

        if (keyToRemove[0] != null) {
            map.remove(keyToRemove[0]);
        }
        if (isInCache) {
            return map.get(key);
        } else {
            if (value[0] != null) {
                map.put(key, value[0]);
            }
            return value[0];
        }
    }

    void put(Integer key, Long value) {
        Integer keyToRemove;
        try {
            keyToRemove = checkCacheAndUpdate(key, value);
        } catch (SQLException e) {
            System.err.println("SQL exception while updating cache. " + e);
            return;
        }

        if (keyToRemove != null) {
            map.remove(keyToRemove);
        }
        map.put(key, value);
    }
}
