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

import java.util.Map;
import java.util.HashMap;

/**
 * 
 * @author Christian Lins
 */
class CommandLineConfig extends AbstractConfig {

    private static final CommandLineConfig instance = new CommandLineConfig();

    public static CommandLineConfig getInstance() {
        return instance;
    }

    private final Map<String, String> values = new HashMap<String, String>();

    private CommandLineConfig() {
    }

    @Override
    public String get(String key, String def) {
        synchronized (this.values) {
            if (this.values.containsKey(key)) {
                def = this.values.get(key);
            }
        }
        return def;
    }

    @Override
    public void set(String key, String val) {
        synchronized (this.values) {
            this.values.put(key, val);
        }
    }
}
