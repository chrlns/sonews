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
 * Class handling the LISTGROUP command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ListGroupCommand extends AbstractCommand
{

  public ListGroupCommand(final NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }

  @Override
  public void processLine(final String commandName) 
    throws IOException, SQLException
  {
    final String[] command = commandName.split(" ");

    Group group;
    if(command.length >= 2)
    {
      group = Group.getByName(command[1]);
    }
    else
    {
      group = getCurrentGroup();
    }

    if (group == null)
    {
      printStatus(412, "no group selected; use GROUP <group> command");
      return;
    }

    List<Long> ids = group.getArticleNumbers();
    printStatus(211, ids.size() + " " +
      group.getFirstArticleNumber() + " " + 
      group.getLastArticleNumber() + " list of article numbers follow");
    for(long id : ids)
    {
      // One index number per line
      println(Long.toString(id));
    }
    println(".");
  }

}
