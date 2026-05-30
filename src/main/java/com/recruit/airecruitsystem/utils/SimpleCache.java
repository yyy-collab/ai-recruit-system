package com.recruit.airecruitsystem.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SimpleCache<K, V> {
    private Map<K, V> cache = new ConcurrentHashMap<>();// 存数据
    private Map<K, Long> expireTime = new ConcurrentHashMap<>();// 存过期时间
    private long ttlMillis;  // 过期时间（毫秒）

    /**
     * @param ttlSeconds 过期时间（秒）
     */
    public SimpleCache(long ttlSeconds) {
        this.ttlMillis = ttlSeconds * 1000;
    }

    public void put(K key, V value) {
        cache.put(key, value);
        expireTime.put(key, System.currentTimeMillis() + ttlMillis);
    }

    public V get(K key) {
        Long expire = expireTime.get(key);
        if (expire == null) {
            return null;
        }
        if (System.currentTimeMillis() > expire) {
            // 过期了，删除
            cache.remove(key);
            expireTime.remove(key);
            return null;
        }
        return cache.get(key);
    }
}