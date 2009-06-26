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

import org.sonews.daemon.Config;
import org.sonews.daemon.storage.Database;
import org.sonews.daemon.storage.Article;
import java.util.Date;

/**
 * The purger is started in configurable intervals to search
 * for old messages that can be purged.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Purger
{

  private long lifetime;
  
  public Purger()
  {
    this.lifetime = Config.getInstance().get("sonews.article.lifetime", 30) 
      * 24L * 60L * 60L * 1000L; // in Milliseconds
  }

  /**
   * Loops through all messages and deletes them if their time
   * has come.
   */
  void purge()
    throws Exception
  {
    System.out.println("Purging old messages...");

    for (;;)
    {
      // TODO: Delete articles directly in database
      Article art = null; //Database.getInstance().getOldestArticle();
      if (art == null) // No articles in the database
      {
        break;
      }

/*      if (art.getDate().getTime() < (new Date().getTime() + this.lifetime))
      {
 //       Database.getInstance().delete(art);
        System.out.println("Deleted: " + art);
      }
      else
      {
        break;
      }*/
    }
  }
  
  public static void main(String[] args)
  {
    try
    {
      Purger purger = new Purger();
      purger.purge();
      System.exit(0);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      System.exit(1);
    }
  }

}
