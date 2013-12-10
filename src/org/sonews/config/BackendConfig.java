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
package org.sonews.config;

import java.util.logging.Level;

import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;
import org.sonews.util.TimeoutMap;

/**
 * Provides access to the program wide configuration that is stored within the
 * server's database.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class BackendConfig extends AbstractConfig {

    private static BackendConfig instance = new BackendConfig();

    public static BackendConfig getInstance() {
        return instance;
    }

    private final TimeoutMap<String, String> values = new TimeoutMap<String, String>();

    private BackendConfig() {
        super();
    }

    /**
     * Returns the config value for the given key or the defaultValue if the key
     * is not found in config.
     * 
     * @param key
     * @param defaultValue
     * @return
     */
    @Override
    public String get(String key, String defaultValue) {
        try {
            String configValue = values.get(key);
            if (configValue == null) {
                if (StorageManager.current() == null) {
                    Log.get().warning(
                            "BackendConfig not available, using default.");
                    return defaultValue;
                }

                configValue = StorageManager.current().getConfigValue(key);
                if (configValue == null) {
                    return defaultValue;
                } else {
                    values.put(key, configValue);
                    return configValue;
                }
            } else {
                return configValue;
            }
        } catch (StorageBackendException ex) {
            Log.get().log(Level.SEVERE, "Storage backend problem", ex);
            return defaultValue;
        }
    }

    /**
     * Sets the config value which is identified by the given key.
     * 
     * @param key
     * @param value
     */
    @Override
    public void set(String key, String value) {
        values.put(key, value);

        try {
            // Write values to database
            StorageManager.current().setConfigValue(key, value);
        } catch (StorageBackendException ex) {
            ex.printStackTrace();
        }
    }
}
