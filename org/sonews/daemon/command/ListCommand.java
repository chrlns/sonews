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
import java.sql.SQLException;
import java.util.List;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.storage.Group;

/**
 * Class handling the LIST command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ListCommand extends AbstractCommand
{

  public ListCommand(final NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }
  
  @Override
  public void processLine(final String line)
    throws IOException, SQLException
  {
    final String[] command = line.split(" ");
    
    if (command.length >= 2)
    {
      if (command[1].equalsIgnoreCase("OVERVIEW.FMT"))
      {
        printStatus(215, "information follows");
        println("Subject:\nFrom:\nDate:\nMessage-ID:\nReferences:\nBytes:\nLines:\nXref");
        println(".");
      }
      else if (command[1].equalsIgnoreCase("NEWSGROUPS"))
      {
        printStatus(215, "information follows");
        final List<Group> list = Group.getAll();
        for (Group g : list)
        {
          println(g.getName() + "\t" + "-");
        }
        println(".");
      }
      else if (command[1].equalsIgnoreCase("SUBSCRIPTIONS"))
      {
        printStatus(215, "information follows");
        println(".");
      }
      else if (command[1].equalsIgnoreCase("EXTENSIONS"))
      {
        printStatus(202, "Supported NNTP extensions.");
        println("LISTGROUP");
        println(".");
 
      }
      else
      {
        printStatus(500, "unknown argument to LIST command");
      }
    }
    else
    {
      final List<Group> groups = Group.getAll();
      if(groups != null)
      {
        printStatus(215, "list of newsgroups follows");
        for (Group g : groups)
        {
          // Indeed first the higher article number then the lower
          println(g.getName() + " " + g.getLastArticleNumber() + " "
              + g.getFirstArticleNumber() + " y");
        }
        println(".");
      }
      else
      {
        printStatus(500, "server database malfunction");
      }
    }
  }

}
