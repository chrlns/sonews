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

package org.sonews.daemon.command;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import org.sonews.daemon.BootstrapConfig;
import org.sonews.daemon.Config;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.storage.Database;
import org.sonews.daemon.storage.Group;
import org.sonews.feed.FeedManager;
import org.sonews.feed.Subscription;
import org.sonews.util.Stats;

/**
 * The XDAEMON command allows a client to get/set properties of the
 * running server daemon. Only locally connected clients are allowed to
 * use this command.
 * The restriction to localhost connection can be suppressed by overriding
 * the sonews.xdaemon.host bootstrap config property.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class XDaemonCommand extends AbstractCommand
{
  
  public XDaemonCommand(NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }

  // TODO: Refactor this method to reduce complexity!
  @Override
  public void processLine(String line) throws IOException, SQLException
  {
    InetSocketAddress addr = (InetSocketAddress)connection.getChannel().socket()
      .getRemoteSocketAddress();
    if(addr.getHostName().equals(
      BootstrapConfig.getInstance().get(BootstrapConfig.XDAEMON_HOST, "localhost")))
    {
      String[] commands = line.split(" ", 4);
      if(commands.length == 3 && commands[1].equalsIgnoreCase("LIST"))
      {
        if(commands[2].equalsIgnoreCase("CONFIGKEYS"))
        {
          printStatus(200, "list of available config keys follows");
          for(String key : Config.AVAILABLE_KEYS)
          {
            println(key);
          }
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("PEERINGRULES"))
        {
          List<Subscription> pull = 
            Database.getInstance().getSubscriptions(FeedManager.TYPE_PULL);
          List<Subscription> push =
            Database.getInstance().getSubscriptions(FeedManager.TYPE_PUSH);
          printStatus(200,"list of peering rules follows");
          for(Subscription sub : pull)
          {
            println("PULL " + sub.getHost() + ":" + sub.getPort() 
              + " " + sub.getGroup());
          }
          for(Subscription sub : push)
          {
            println("PUSH " + sub.getHost() + ":" + sub.getPort() 
              + " " + sub.getGroup());
          }
          println(".");
        }
        else
        {
          printStatus(501, "unknown sub command");
        }
      }
      else if(commands.length == 3 && commands[1].equalsIgnoreCase("DELETE"))
      {
        Database.getInstance().delete(commands[2]);
        printStatus(200, "article " + commands[2] + " deleted");
      }
      else if(commands.length == 4 && commands[1].equalsIgnoreCase("GROUPADD"))
      {
        Database.getInstance().addGroup(commands[2], Integer.parseInt(commands[3]));
        printStatus(200, "group " + commands[2] + " created");
      }
      else if(commands.length == 3 && commands[1].equalsIgnoreCase("GROUPDEL"))
      {
        Group group = Database.getInstance().getGroup(commands[2]);
        if(group == null)
        {
          printStatus(400, "group not found");
        }
        else
        {
          group.setFlag(Group.DELETED);
          printStatus(200, "group " + commands[2] + " marked as deleted");
        }
      }
      else if(commands.length == 4 && commands[1].equalsIgnoreCase("SET"))
      {
        String key = commands[2];
        String val = commands[3];
        Config.getInstance().set(key, val);
        printStatus(200, "new config value set");
      }
      else if(commands.length == 3 && commands[1].equalsIgnoreCase("GET"))
      {
        String key = commands[2];
        String val = Config.getInstance().get(key, null);
        if(val != null)
        {
          printStatus(200, "config value for " + key + " follows");
          println(val);
          println(".");
        }
        else
        {
          printStatus(400, "config value not set");
        }
      }
      else if(commands.length >= 3 && commands[1].equalsIgnoreCase("LOG"))
      {
        Group group = null;
        if(commands.length > 3)
        {
          group = Group.getByName(commands[3]);
        }

        if(commands[2].equalsIgnoreCase("CONNECTED_CLIENTS"))
        {
          printStatus(200, "number of connections follow");
          println(Integer.toString(Stats.getInstance().connectedClients()));
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("POSTED_NEWS"))
        {
          printStatus(200, "hourly numbers of posted news yesterday");
          for(int n = 0; n < 24; n++)
          {
            println(n + " " + Stats.getInstance()
              .getYesterdaysEvents(Stats.POSTED_NEWS, n, group));
          }
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("GATEWAYED_NEWS"))
        {
          printStatus(200, "hourly numbers of gatewayed news yesterday");
          for(int n = 0; n < 24; n++)
          {
            println(n + " " + Stats.getInstance()
              .getYesterdaysEvents(Stats.GATEWAYED_NEWS, n, group));
          }
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("TRANSMITTED_NEWS"))
        {
          printStatus(200, "hourly numbers of news transmitted to peers yesterday");
          for(int n = 0; n < 24; n++)
          {
            println(n + " " + Stats.getInstance()
              .getYesterdaysEvents(Stats.FEEDED_NEWS, n, group));
          }
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("HOSTED_NEWS"))
        {
          printStatus(200, "number of overall hosted news");
          println(Integer.toString(Stats.getInstance().getNumberOfNews()));
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("HOSTED_GROUPS"))
        {
          printStatus(200, "number of hosted groups");
          println(Integer.toString(Stats.getInstance().getNumberOfGroups()));
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("POSTED_NEWS_PER_HOUR"))
        {
          printStatus(200, "posted news per hour");
          println(Double.toString(Stats.getInstance().postedPerHour(-1)));
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("FEEDED_NEWS_PER_HOUR"))
        {
          printStatus(200, "feeded news per hour");
          println(Double.toString(Stats.getInstance().feededPerHour(-1)));
          println(".");
        }
        else if(commands[2].equalsIgnoreCase("GATEWAYED_NEWS_PER_HOUR"))
        {
          printStatus(200, "gatewayed news per hour");
          println(Double.toString(Stats.getInstance().gatewayedPerHour(-1)));
          println(".");
        }
        else
        {
          printStatus(501, "unknown sub command");
        }
      }
      else
      {
        printStatus(500, "invalid command usage");
      }
    }
    else
    {
      printStatus(500, "not allowed");
    }
  }
  
}
