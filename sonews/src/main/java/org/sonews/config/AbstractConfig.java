/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
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

package org.sonews.config;

/**
 * Base class for Config and BootstrapConfig.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public abstract class AbstractConfig {

    public abstract String get(String key, String defVal);

    public int get(final String key, final int defVal) {
        return Integer.parseInt(get(key, Integer.toString(defVal)));
    }

    public boolean get(String key, boolean defVal) {
        String val = get(key, Boolean.toString(defVal));
        return Boolean.parseBoolean(val);
    }

    /**
     * Returns a long config value specified via the given key.
     *
     * @param key
     * @param defVal
     * @return
     */
    public long get(String key, long defVal) {
        String val = get(key, Long.toString(defVal));
        return Long.parseLong(val);
    }

    protected abstract void set(String key, String val);
}
