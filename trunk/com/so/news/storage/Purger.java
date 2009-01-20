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

package com.so.news.storage;

import java.util.Date;

import com.so.news.Config;
import com.so.news.Debug;

/**
 * The purger is started in configurable intervals to search
 * for old messages that can be purged.
 * @author Christian Lins
 */
public class Purger extends Thread
{
  private int interval;
  
  public Purger()
  {
    setDaemon(true); // Daemons run only along with the main thread
    setPriority(Thread.MIN_PRIORITY);

    this.interval = Config.getInstance().get("n3tpd.article.lifetime", 30) * 24 * 60 * 60 * 1000; // Milliseconds
    if(this.interval < 0)
      this.interval = Integer.MAX_VALUE;
  }
  
  /**
   * Runloop of this Purger class.
   */
  @Override
  public void run()
  {
    for(;;)
    {
      purge();

      try
      {
        sleep(interval);
      }
      catch(InterruptedException e)
      {
        e.printStackTrace(Debug.getInstance().getStream());
      }
    }
  }

  /**
   * Loops through all messages and deletes them if their time
   * has come.
   */
  private void purge()
  {
    Debug.getInstance().log("Purging old messages...");

    try
    {
      for(;;)
      {
        Article art = null; //Database.getInstance().getOldestArticle();
        if(art == null) // No articles in the database
          break;
        
        if(art.getDate().getTime() < (new Date().getTime() + this.interval))
        {
          Database.getInstance().delete(art);
          Debug.getInstance().log("Deleted: " + art);
        }
        else
          break;
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
