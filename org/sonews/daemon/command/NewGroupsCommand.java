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
import org.sonews.daemon.NNTPConnection;

/**
 * Class handling the NEWGROUPS command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class NewGroupsCommand extends AbstractCommand
{

  public NewGroupsCommand(final NNTPConnection conn)
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

    if(command.length == 3)
    {
      printStatus(231, "list of new newsgroups follows");

      // Currently we do not store a group's creation date;
      // so we return an empty list which is a valid response
      println(".");
    }
    else
    {
      printStatus(500, "invalid command usage");
    }
  }

}
