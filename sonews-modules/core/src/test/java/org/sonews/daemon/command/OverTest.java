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

package org.sonews.daemon.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.sonews.AbstractTest;

/**
 * Tests the OVER/XOVER command.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class OverTest extends AbstractTest
{

  @Override
  public int runTest()
    throws Exception
  {
    // Send HELLO to server
    String line = readln();
    if(!line.startsWith("200 "))
    {
      return 1;
    }

    // Determine available groups
    println("LIST");
    line = readln();
    if(!line.startsWith("215 "))
    {
      return 2;
    }

    List<String> groups = new ArrayList<String>();
    line = readln();
    for(;;)
    {
      if(line.equals("."))
      {
        break;
      }
      else
      {
        groups.add(line);
        line = readln();
      }
    }

    if(groups.size() <= 0)
    {
      return 3;
    }

    // Test OVER command on every group
    for(String group : groups)
    {
      String groupName = group.split(" ")[0];
      println("GROUP " + groupName);
      line = readln();
      if(!line.startsWith("211 "))
      {
        return 4;
      }

      String[] lineToks = line.split(" ");
      println("XOVER " + lineToks[2] + "-" + lineToks[3]);
      line = readln();
      if(line.startsWith("423"))
      {
        continue;
      }
      else if(!line.startsWith("224 "))
      {
        return 5;
      }
      
      line = readln();
      for(;;)
      {
        if(line == null)
        {
          return 7;
        }
        else if(line.equals("."))
        {
          break;
        }

        // Validate the line
        lineToks = line.split("\t");
        if(lineToks.length < 6)
        {
          return 6;
        }
        else
        {
          Integer.parseInt(lineToks[0]);

          //SimpleDateFormat sdf = new SimpleDateFormat(group)

          if(!lineToks[4].startsWith("<") && !lineToks[4].endsWith(">"))
          {
            log.println("Invalid Message-ID: " + lineToks[1]);
            log.flush();
            return 8;
          }
        }

        line = readln();
      }
    }
    
    return 0;
  }
  
}
