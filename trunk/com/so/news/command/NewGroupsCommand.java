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

package com.so.news.command;

import java.io.IOException;
import java.util.ArrayList;
import com.so.news.NNTPConnection;
import com.so.news.storage.Group;

/**
 * Class handling the NEWGROUPS command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 */
public class NewGroupsCommand extends Command
{
  public NewGroupsCommand(NNTPConnection conn)
  {
    super(conn);
  }

  public boolean process(String[] command) throws IOException
  {
    String commandName = command[0];
    if (!commandName.equalsIgnoreCase("NEWGROUPS"))
      return false;
    if (command.length != 3)
      return false;
    // untested, not RFC977 complient
    try
    {
      // Timestamp date = new Timestamp ( new SimpleDateFormat ("yyMMdd
      // HHmmss").parse(command[1] + " " + command[2] ).getTime()) ;
      printStatus(231, "list of new newsgroups follows");
      ArrayList<Group> list = Group.getAll();// (date) ;
      for (Group g : list)
      {
        printTextLine(g.getName() + " " + g.getLastArticle() + " "
            + g.getFirstArticle() + " y");
      }
      println(".");
      flush();
      return true;
    }
    catch (Exception e)
    {
      printStatus(511, "listing failed - invalid date format");
      return true;
    }
  }

}
