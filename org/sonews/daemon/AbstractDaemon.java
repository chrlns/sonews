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

import java.sql.SQLException;
import org.sonews.daemon.storage.Database;
import org.sonews.util.Log;

/**
 * Base class of all sonews threads.
 * Instances of this class will be automatically registered at the ShutdownHook
 * to be cleanly exited when the server is forced to exit.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public abstract class AbstractDaemon extends Thread
{

  /** This variable is write synchronized through setRunning */
  private boolean isRunning = false;

  /**
   * Protected constructor. Will be called by derived classes.
   */
  protected AbstractDaemon()
  {
    setDaemon(true); // VM will exit when all threads are daemons
    setName(getClass().getSimpleName());
  }
  
  /**
   * @return true if shutdown() was not yet called.
   */
  public boolean isRunning()
  {
    synchronized(this)
    {
      return this.isRunning;
    }
  }
  
  /**
   * Marks this thread to exit soon. Closes the associated Database connection
   * if available.
   * @throws java.sql.SQLException
   */
  void shutdownNow()
    throws SQLException
  {
    synchronized(this)
    {
      this.isRunning = false;
      Database db = Database.getInstance(false);
      if(db != null)
      {
        db.shutdown();
      }
    }
  }
  
  /**
   * Calls shutdownNow() but catches SQLExceptions if occurring.
   */
  public void shutdown()
  {
    try
    {
      shutdownNow();
    }
    catch(SQLException ex)
    {
      Log.msg(ex, true);
    }
  }
  
  /**
   * Starts this daemon.
   */
  @Override
  public void start()
  {
    synchronized(this)
    {
      this.isRunning = true;
    }
    super.start();
  }
  
}
