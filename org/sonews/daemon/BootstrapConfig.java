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

package org.sonews.daemon;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.sonews.util.AbstractConfig;

/**
 * Manages the bootstrap configuration. It MUST contain all config values
 * that are needed to establish a database connection.
 * For further configuration values use the Config class instead as that class
 * stores its values within the database.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class BootstrapConfig extends AbstractConfig
{
  
  /** Key constant. If value is "true" every I/O is written to logfile 
   * (which is a lot!) 
   */
  public static final String DEBUG              = "sonews.debug";
  
  /** Key constant. Value is classname of the JDBC driver */
  public static final String STORAGE_DBMSDRIVER = "sonews.storage.dbmsdriver";
  
  /** Key constant. Value is JDBC connect String to the database. */
  public static final String STORAGE_DATABASE   = "sonews.storage.database";
  
  /** Key constant. Value is the username for the DBMS. */
  public static final String STORAGE_USER       = "sonews.storage.user";
  
  /** Key constant. Value is the password for the DBMS. */
  public static final String STORAGE_PASSWORD   = "sonews.storage.password";
  
  /** Key constant. Value is the name of the host which is allowed to use the
   *  XDAEMON command; default: "localhost" */
  public static final String XDAEMON_HOST       = "sonews.xdaemon.host";

  /** The config key for the filename of the logfile */
  public static final String LOGFILE = "sonews.log";
  
  /** The filename of the config file that is loaded on startup */
  public static volatile String FILE               = "sonews.conf";

  private static final Properties defaultConfig = new Properties();
  
  private static BootstrapConfig instance = null;
  
  static
  {
    // Set some default values
    defaultConfig.setProperty(STORAGE_DATABASE, "jdbc:mysql://localhost/sonews");
    defaultConfig.setProperty(STORAGE_DBMSDRIVER, "com.mysql.jdbc.Driver");
    defaultConfig.setProperty(STORAGE_USER, "sonews_user");
    defaultConfig.setProperty(STORAGE_PASSWORD, "mysecret");
    defaultConfig.setProperty(DEBUG, "false");
  }
  
  /**
   * Note: this method is not thread-safe
   * @return A Config instance
   */
  public static synchronized BootstrapConfig getInstance()
  {
    if(instance == null)
    {
      instance = new BootstrapConfig();
    }
    return instance;
  }

  // Every config instance is initialized with the default values.
  private final Properties settings = (Properties)defaultConfig.clone();

  /**
   * Config is a singelton class with only one instance at time.
   * So the constructor is private to prevent the creation of more
   * then one Config instance.
   * @see Config.getInstance() to retrieve an instance of Config
   */
  private BootstrapConfig()
  {
    try
    {
      // Load settings from file
      load();
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Loads the configuration from the config file. By default this is done
   * by the (private) constructor but it can be useful to reload the config
   * by invoking this method.
   * @throws IOException
   */
  public void load() 
    throws IOException
  {
    FileInputStream in = null;
    
    try
    {
      in = new FileInputStream(FILE);
      settings.load(in);
    }
    catch (FileNotFoundException e)
    {
      // MUST NOT use Log otherwise endless loop
      System.err.println(e.getMessage());
      save();
    }
    finally
    {
      if(in != null)
        in.close();
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
    FileOutputStream out = null;
    try
    {
      out = new FileOutputStream(FILE);
      settings.store(out, "SONEWS Config File");
      out.flush();
    }
    catch(IOException ex)
    {
      throw ex;
    }
    finally
    {
      if(out != null)
        out.close();
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
  public String get(String key, String def)
  {
    return settings.getProperty(key, def);
  }

  /**
   * Sets the value for a given key.
   * @param key
   * @param value
   */
  public void set(final String key, final String value)
  {
    settings.setProperty(key, value);
  }

}
