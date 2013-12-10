/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sonews.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a Map that will loose its stored values after a
 * configurable amount of time. This class may be used to cache config values
 * for example.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@SuppressWarnings("serial")
public class TimeoutMap<K, V> extends ConcurrentHashMap<K, V> {

    private int timeout = 60000; // 60 sec
    private transient Map<K, Long> timeoutMap = new HashMap<K, Long>();

    /**
     * Constructor.
     * 
     * @param timeout
     *            Timeout in milliseconds
     */
    public TimeoutMap(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * Uses default timeout (60 sec).
     */
    public TimeoutMap() {
    }

    /**
     * 
     * @param key
     * @return true if key is still valid.
     */
    protected boolean checkTimeOut(Object key) {
        synchronized (this.timeoutMap) {
            if (this.timeoutMap.containsKey(key)) {
                long keytime = this.timeoutMap.get(key);
                if ((System.currentTimeMillis() - keytime) < this.timeout) {
                    return true;
                } else {
                    remove(key);
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return checkTimeOut(key);
    }

    @Override
    public synchronized V get(Object key) {
        if (checkTimeOut(key)) {
            return super.get(key);
        } else {
            return null;
        }
    }

    @Override
    public V put(K key, V value) {
        synchronized (this.timeoutMap) {
            removeStaleKeys();
            this.timeoutMap.put(key, System.currentTimeMillis());
            return super.put(key, value);
        }
    }

    /**
     * @param arg0
     * @return
     */
    @Override
    public V remove(Object arg0) {
        synchronized (this.timeoutMap) {
            this.timeoutMap.remove(arg0);
            V val = super.remove(arg0);
            return val;
        }
    }

    protected void removeStaleKeys() {
        synchronized (this.timeoutMap) {
            Set<Object> keySet = new HashSet<Object>(this.timeoutMap.keySet());
            for (Object key : keySet) {
                // The key/value is removed by the checkTimeOut() method if true
                checkTimeOut(key);
            }
        }
    }
}
