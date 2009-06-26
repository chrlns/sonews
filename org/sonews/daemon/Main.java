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

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Date;
import org.sonews.feed.FeedManager;
import org.sonews.mlgw.MailPoller;
import org.sonews.daemon.storage.Database;
import org.sonews.util.Log;
import org.sonews.util.io.Resource;

/**
 * Startup class of the daemon.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Main
{
  
  private Main()
  {
  }

  /** Version information of the sonews daemon */
  public static final String VERSION = "sonews/0.5.0";
  public static final Date   STARTDATE = new Date();
  
  /**
   * The main entrypoint.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception
  {
    System.out.println(VERSION);
    Thread.currentThread().setName("Mainthread");

    // Command line arguments
    boolean feed    = false;  // Enable feeding?
    boolean mlgw    = false;  // Enable Mailinglist gateway?
    int     port    = -1;
    
    for(int n = 0; n < args.length; n++)
    {
      if(args[n].equals("-c") || args[n].equals("-config"))
      {
        BootstrapConfig.FILE = args[++n];
        System.out.println("Using config file " + args[n]);
      }
      else if(args[n].equals("-dumpjdbcdriver"))
      {
        System.out.println("Available JDBC drivers:");
        Enumeration<Driver> drvs =  DriverManager.getDrivers();
        while(drvs.hasMoreElements())
        {
          System.out.println(drvs.nextElement());
        }
        return;
      }
      else if(args[n].equals("-feed"))
      {
        feed = true;
      }
      else if(args[n].equals("-h") || args[n].equals("-help"))
      {
        printArguments();
        return;
      }
      else if(args[n].equals("-mlgw"))
      {
        mlgw = true;
      }
      else if(args[n].equals("-p"))
      {
        port = Integer.parseInt(args[++n]);
      }
    }
    
    // Try to load the Database;
    // Do NOT USE Config or Log classes before this point because they require
    // a working Database connection.
    try
    {
      Database.getInstance();
      
      // Make sure some elementary groups are existing
      if(!Database.getInstance().isGroupExisting("control"))
      {
        Database.getInstance().addGroup("control", 0);
        Log.msg("Group 'control' created.", true);
      }
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
      System.err.println("Database initialization failed with " + ex.toString());
      System.err.println("Make sure you have specified the correct database" +
        " settings in sonews.conf!");
      return;
    }
    
    ChannelLineBuffers.allocateDirect();
    
    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    
    // Start the listening daemon
    if(port <= 0)
    {
      port = Config.getInstance().get(Config.PORT, 119);
    }
    final NNTPDaemon daemon = NNTPDaemon.createInstance(port);
    daemon.start();
    
    // Start Connections purger thread...
    Connections.getInstance().start();
    
    // Start mailinglist gateway...
    if(mlgw)
    {
      new MailPoller().start();
    }
    
    // Start feeds
    if(feed)
    {
      FeedManager.startFeeding();
    }
    
    // Wait for main thread to exit (setDaemon(false))
    daemon.join();
  }
  
  private static void printArguments()
  {
    String usage = Resource.getAsString("helpers/usage", true);
    System.out.println(usage);
  }

}
