package com.qykj.finmaster.common.utils.copy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
public class CopierCache<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = -3030974255959635618L;
	
	private int cacheSize;
    public CopierCache(int cacheSize) {
        super(16, (float) 0.75, true);
        this.cacheSize = cacheSize;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > cacheSize;
    }

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}