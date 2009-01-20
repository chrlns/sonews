/*
 *   StarOffice News Server
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

package com.so.news;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manages the n3tpd configuration.
 * @author Christian Lins
 */
public class Config
{
  /** The filename of the logfile */
  public static final String CONFIG_N3TPD_LOGFILE = "n3tpd.logfile";
  
  /** The filename of the config file that is loaded on startup */
  public static final String FILE                 = "n3tpd.conf";

  private static final Properties defaultConfig = new Properties();
  
  private static Config instance = null;
  
  static
  {
    // Set some default values
    defaultConfig.setProperty("n3tpd.article.lifetime", "300"); // 300 days
    defaultConfig.setProperty("n3tpd.article.maxsize", "100");  // 100 kbyte
    defaultConfig.setProperty("n3tpd.port", "119");
    defaultConfig.setProperty("n3tpd.auxport", "8080");
    defaultConfig.setProperty("n3tpd.server.backlog", "10");
    defaultConfig.setProperty("n3tpd.hostname", "localhost");
    defaultConfig.setProperty("n3tpd.storage.database", "jdbc:mysql://localhost/n3tpd_data");
    defaultConfig.setProperty("n3tpd.storage.dbmsdriver", "com.mysql.jdbc.Driver");
    defaultConfig.setProperty("n3tpd.storage.user", "n3tpd_user");
    defaultConfig.setProperty("n3tpd.storage.password", "mysecret");
    
    instance = new Config();
  }
  
  /**
   * @return A Config instance
   */
  public static Config getInstance()
  {
    return instance;
  }

  // Every config instance is initialized with the default values.
  private Properties settings = (Properties)defaultConfig.clone();

  /**
   * Config is a singelton class with only one instance at time.
   * So the constructor is private to prevent the creation of more
   * then one Config instance.
   * @see Config.getInstance() to retrieve an instance of Config
   */
  private Config()
  {
    try
    {
      // Load settings from file
      load();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Loads the configuration from the config file. By default this is done
   * by the (private) constructor but it can be useful to reload the config
   * by invoking this method.
   * @throws IOException
   */
  public void load() throws IOException
  {
    try
    {
      settings.load(new FileInputStream(FILE));
    }
    catch (FileNotFoundException e)
    {
      save();
    }
  }

  /**
   * Saves this Config to the config file. By default this is done
   * at program end.
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void save() throws FileNotFoundException, IOException
  {
    settings.store(new FileOutputStream(FILE), "N3TPD Config File");
  }
  
  /**
   * Returns the value that is stored within this config
   * identified by the given key. If the key cannot be found
   * the default value is returned.
   * @param key Key to identify the value.
   * @param def The default value that is returned if the key
   * is not found in this Config.
   * @return
   */
  public String get(String key, String def)
  {
    return settings.getProperty(key, def);
  }

  /**
   * Returns the value that is stored within this config
   * identified by the given key. If the key cannot be found
   * the default value is returned.
   * @param key Key to identify the value.
   * @param def The default value that is returned if the key
   * is not found in this Config.
   * @return
   */
  public int get(String key, int def)
  {
    try
    {
      String val = get(key);
      return Integer.parseInt(val);
    }
    catch(Exception e)
    {
      return def;
    }
  }
  
  /**
   * Returns the value that is stored within this config
   * identified by the given key. If the key cannot be found
   * the default value is returned.
   * @param key Key to identify the value.
   * @param def The default value that is returned if the key
   * is not found in this Config.
   * @return
   */
  public long get(String key, long def)
  {
    try
    {
      String val = get(key);
      return Long.parseLong(val);
    }
    catch(Exception e)
    {
      return def;
    }
  }

  /**
   * Returns the value for the given key or null if the
   * key is not found in this Config.
   * @param key
   * @return
   */
  private String get(String key)
  {
    return settings.getProperty(key);
  }

  /**
   * Sets the value for a given key.
   * @param key
   * @param value
   */
  public void set(String key, String value)
  {
    settings.setProperty(key, value);
  }

}
