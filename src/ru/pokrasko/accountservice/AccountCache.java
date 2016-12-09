package ru.pokrasko.accountservice;

import java.sql.SQLException;
import java.util.Map;

class AccountCache extends AbstractCache<Integer, Long> {
    private DatabaseHelper helper;

    AccountCache(DatabaseHelper helper) {
        this.helper = helper;
    }

    AccountCache(DatabaseHelper helper, int capacity) throws IllegalArgumentException {
        super(capacity);
        this.helper = helper;
    }

    AccountCache(DatabaseHelper helper, Map<Integer, Long> initialMap) {
        super(initialMap);
        this.helper = helper;
    }

    AccountCache(DatabaseHelper helper, Map<Integer, Long> initialMap, int capacity) throws IllegalArgumentException {
        super(initialMap, capacity);
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

    private synchronized boolean checkCacheAndRefresh(Integer key, Integer[] keyToRemove, Long[] value) throws SQLException {
        boolean isInCache = checkCache(key, keyToRemove);
        if (!isInCache) {
            value[0] = helper.get(key);
        }
        return isInCache;
    }

    private synchronized Integer checkCacheAndUpdate(Integer key, Long value) throws SQLException {
        Integer[] result = new Integer[1];
        checkCache(key, result);
        helper.update(key, value);
        return result[0];
    }

    @Override
    public Long get(Integer key) {
//        boolean isInCache;
//        synchronized (this) {
//            isInCache = list.remove(key);
//            if (isInCache) {
//                list.addFirst(key);
//            }
//        }
//        return isInCache ? map.get(key) : null;

        boolean isInCache;
        Integer[] keyToRemove = new Integer[1];
        Long[] value = new Long[1];
        try {
            isInCache = checkCacheAndRefresh(key, keyToRemove, value);
        } catch (SQLException e) {
            System.err.println("SQL exception while updating cache. " + e);
            return null;
        }
//        synchronized (this) {
//            if (!list.remove(key)) {
//                if (list.size() == capacity) {
//                    keyToRemove = list.pollLast();
//                }
//                list.addFirst(key);
//                try {
//                    value = helper.get(key);
//                } catch (SQLException e) {
//                }
//            }
//        }

        if (keyToRemove[0] != null) {
            map.remove(keyToRemove[0]);
        }
        if (isInCache) {
            return map.get(key);
        } else {
            map.put(key, value[0]);
            return value[0];
        }
    }

    @Override
    public void put(Integer key, Long value) {
//        Integer keyToRemove = null;
//        synchronized (this) {
//            if (list.remove(key)) {
//                --size;
//            }
//            if (size == capacity) {
//                keyToRemove = list.pollLast();
//                --size;
//            }
//            list.addFirst(key);
//        }
//        if (keyToRemove != null) {
//            map.remove(keyToRemove);
//        }
//        map.put(key, value);
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
