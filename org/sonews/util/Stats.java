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

import java.util.Calendar;
import org.sonews.config.Config;
import org.sonews.storage.Channel;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;

/**
 * Class that capsulates statistical data gathering.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Stats 
{
      
  public static final byte CONNECTIONS    = 1;
  public static final byte POSTED_NEWS    = 2;
  public static final byte GATEWAYED_NEWS = 3;
  public static final byte FEEDED_NEWS    = 4;
  public static final byte MLGW_RUNSTART  = 5;
  public static final byte MLGW_RUNEND    = 6;

  private static Stats instance = new Stats();
  
  public static Stats getInstance()
  {
    return Stats.instance;
  }
  
  private Stats() {}
  
  private volatile int connectedClients = 0;

  /**
   * A generic method that writes event data to the storage backend.
   * If event logging is disabled with sonews.eventlog=false this method
   * simply does nothing.
   * @param type
   * @param groupname
   */
  private void addEvent(byte type, String groupname)
  {
    try
    {
      if (Config.inst().get(Config.EVENTLOG, true))
      {

        Channel group = Channel.getByName(groupname);
        if (group != null)
        {
          StorageManager.current().addEvent(
                  System.currentTimeMillis(), type, group.getInternalID());
        }
      } 
      else
      {
        Log.get().info("Group " + groupname + " does not exist.");
      }
    } 
    catch (StorageBackendException ex)
    {
      ex.printStackTrace();
    }
  }
  
  public void clientConnect()
  {
    this.connectedClients++;
  }
  
  public void clientDisconnect()
  {
    this.connectedClients--;
  }
  
  public int connectedClients()
  {
    return this.connectedClients;
  }
  
  public int getNumberOfGroups()
  {
    try
    {
      return StorageManager.current().countGroups();
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return -1;
    }
  }
  
  public int getNumberOfNews()
  {
    try
    {
      return StorageManager.current().countArticles();
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return -1;
    }
  }
  
  public int getYesterdaysEvents(final byte eventType, final int hour,
    final Channel group)
  {
    // Determine the timestamp values for yesterday and the given hour
    Calendar cal = Calendar.getInstance();
    int year  = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH);
    int dayom = cal.get(Calendar.DAY_OF_MONTH) - 1; // Yesterday
    
    cal.set(year, month, dayom, hour, 0, 0);
    long startTimestamp = cal.getTimeInMillis();
    
    cal.set(year, month, dayom, hour + 1, 0, 0);
    long endTimestamp = cal.getTimeInMillis();
    
    try
    {
      return StorageManager.current()
        .getEventsCount(eventType, startTimestamp, endTimestamp, group);
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return -1;
    }
  }
  
  public void mailPosted(String groupname)
  {
    addEvent(POSTED_NEWS, groupname);
  }
  
  public void mailGatewayed(String groupname)
  {
    addEvent(GATEWAYED_NEWS, groupname);
  }
  
  public void mailFeeded(String groupname)
  {
    addEvent(FEEDED_NEWS, groupname);
  }
  
  public void mlgwRunStart()
  {
    addEvent(MLGW_RUNSTART, "control");
  }
  
  public void mlgwRunEnd()
  {
    addEvent(MLGW_RUNEND, "control");
  }
  
  private double perHour(int key, long gid)
  {
    try
    {
      return StorageManager.current().getEventsPerHour(key, gid);
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return -1;
    }
  }
  
  public double postedPerHour(long gid)
  {
    return perHour(POSTED_NEWS, gid);
  }
  
  public double gatewayedPerHour(long gid)
  {
    return perHour(GATEWAYED_NEWS, gid);
  }
  
  public double feededPerHour(long gid)
  {
    return perHour(FEEDED_NEWS, gid);
  }
  
}
