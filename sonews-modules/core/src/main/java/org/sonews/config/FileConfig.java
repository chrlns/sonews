/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import org.sonews.util.Log;

/**
 * Manages the bootstrap configuration. It MUST contain all config values that
 * are needed to establish a database connection. For further configuration
 * values use the Config class instead as that class stores its values within
 * the database.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class FileConfig extends AbstractConfig {

    private static final Properties defaultConfig = new Properties();
    private static FileConfig instance = null;

    static {
        // Set some default values
        defaultConfig.setProperty(Config.STORAGE_DATABASE, "sonews");
        defaultConfig.setProperty(Config.STORAGE_HOST, "localhost");
        defaultConfig.setProperty(Config.STORAGE_PORT, "5984");
        defaultConfig.setProperty(Config.STORAGE_USER, "sonews_user");
        defaultConfig.setProperty(Config.STORAGE_PASSWORD, "mysecret");
    }

    public static synchronized FileConfig getInstance() {
        if (instance == null) {
            instance = new FileConfig();
        }
        return instance;
    }

    // Every config instance is initialized with the default values.
    private final Properties settings = (Properties) defaultConfig.clone();

    /**
     * Config is a singelton class with only one instance at time. So the
     * constructor is private to prevent the creation of more then one Config
     * instance.
     *
     * @see Config.getInstance() to retrieve an instance of Config
     */
    private FileConfig() {
        try {
            // Load settings from file
            load();
        } catch (final IOException ex) {
            Log.get().log(Level.SEVERE, "Exception while loading config file", ex);
        }
    }

    /**
     * Loads the configuration from the config file. By default this is done by
     * the (private) constructor but it can be useful to reload the config by
     * invoking this method.
     *
     * @throws IOException
     */
    public final void load() throws IOException {
        final String cfgFile = Config.inst().get(Config.LEVEL_CLI,
                Config.CONFIGFILE, "sonews.conf");
        try (FileInputStream in = new FileInputStream(cfgFile)) {
            settings.load(in);
        } catch (final FileNotFoundException e) {
            // MUST NOT use Log otherwise endless loop
            System.err.println(e.getMessage());
            save();
        }
    }

    /**
     * Saves this Config to the config file. By default this is done at program
     * end.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void save() throws FileNotFoundException, IOException {
        final String cfgFile = Config.inst().get(Config.LEVEL_CLI,
                Config.CONFIGFILE, "sonews.conf");
        try (FileOutputStream out = new FileOutputStream(cfgFile)) {
            settings.store(out, "SONEWS Config File");
            out.flush();
        } catch (final IOException ex) {
            throw ex;
        }
    }

    /**
     * Returns the value that is stored within this config identified by the
     * given key. If the key cannot be found the default value is returned.
     *
     * @param key
     *            Key to identify the value.
     * @param def
     *            The default value that is returned if the key is not found in
     *            this Config.
     * @return
     */
    @Override
    public String get(final String key, final String def) {
        return settings.getProperty(key, def);
    }

    /**
     * Sets the value for a given key.
     *
     * @param key
     * @param value
     */
    @Override
    public void set(final String key, final String value) {
        settings.setProperty(key, value);
    }
}
