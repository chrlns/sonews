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

import org.sonews.util.Log;
import java.sql.SQLException;
import org.sonews.daemon.storage.Database;
import org.sonews.util.AbstractConfig;
import org.sonews.util.TimeoutMap;

/**
 * Provides access to the program wide configuration that is stored within
 * the server's database.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Config extends AbstractConfig
{

  /** Config key constant. Value is the maximum article size in kilobytes. */
  public static final String ARTICLE_MAXSIZE   = "sonews.article.maxsize";
  
  /** Config key constant. Value: Amount of news that are feeded per run. */
  public static final String FEED_NEWSPERRUN   = "sonews.feed.newsperrun";
  public static final String FEED_PULLINTERVAL = "sonews.feed.pullinterval";
  public static final String HOSTNAME          = "sonews.hostname";
  public static final String PORT              = "sonews.port";
  public static final String TIMEOUT           = "sonews.timeout";
  public static final String MLPOLL_DELETEUNKNOWN = "sonews.mlpoll.deleteunknown";
  public static final String MLPOLL_HOST       = "sonews.mlpoll.host";
  public static final String MLPOLL_PASSWORD   = "sonews.mlpoll.password";
  public static final String MLPOLL_USER       = "sonews.mlpoll.user";
  public static final String MLSEND_ADDRESS    = "sonews.mlsend.address";
  public static final String MLSEND_RW_FROM    = "sonews.mlsend.rewrite.from";
  public static final String MLSEND_RW_SENDER  = "sonews.mlsend.rewrite.sender";
  public static final String MLSEND_HOST       = "sonews.mlsend.host";
  public static final String MLSEND_PASSWORD   = "sonews.mlsend.password";
  public static final String MLSEND_PORT       = "sonews.mlsend.port";
  public static final String MLSEND_USER       = "sonews.mlsend.user";
  
  public static final String[] AVAILABLE_KEYS = {
    Config.ARTICLE_MAXSIZE,
    Config.FEED_NEWSPERRUN,
    Config.FEED_PULLINTERVAL,
    Config.HOSTNAME,
    Config.MLPOLL_DELETEUNKNOWN,
    Config.MLPOLL_HOST,
    Config.MLPOLL_PASSWORD,
    Config.MLPOLL_USER,
    Config.MLSEND_ADDRESS,
    Config.MLSEND_HOST,
    Config.MLSEND_PASSWORD,
    Config.MLSEND_PORT,
    Config.MLSEND_RW_FROM,
    Config.MLSEND_RW_SENDER,
    Config.MLSEND_USER,
    Config.PORT,
    Config.TIMEOUT
  };

  private static Config instance = new Config();
  
  public static Config getInstance()
  {
    return instance;
  }
  
  private final TimeoutMap<String, String> values 
    = new TimeoutMap<String, String>();
  
  private Config()
  {
    super();
  }
  
  /**
   * Returns the config value for the given key or the defaultValue if the
   * key is not found in config.
   * @param key
   * @param defaultValue
   * @return
   */
  public String get(String key, String defaultValue)
  {
    try
    {
      String configValue = values.get(key);
      if(configValue == null)
      {
        configValue = Database.getInstance().getConfigValue(key);
        if(configValue == null)
        {
          return defaultValue;
        }
        else
        {
          values.put(key, configValue);
          return configValue;
        }
      }
      else
      {
        return configValue;
      }
    }
    catch(SQLException ex)
    {
      Log.msg(ex.getMessage(), false);
      return defaultValue;
    }
  }
  
  /**
   * Sets the config value which is identified by the given key.
   * @param key
   * @param value
   */
  public void set(String key, String value)
  {
    values.put(key, value);
    
    try
    {
      // Write values to database
      Database.getInstance().setConfigValue(key, value);
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
    }
  }
  
}
