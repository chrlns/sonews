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

import java.util.List;
import com.so.news.NNTPConnection;
import com.so.news.storage.Article;
import com.so.news.storage.Group;

/**
 * Class handling the LISTGROUP command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 */
public class ListGroupCommand extends Command
{
  public ListGroupCommand(NNTPConnection conn)
  {
    super(conn);
  }

  public boolean process(String[] command) 
    throws Exception
  {
    String commandName = command[0];
    if (!commandName.equalsIgnoreCase("LISTGROUP"))
      return false;
    // untested, RFC977 complient
    Group group = null;
    if (command.length >= 2)
    {
      group = Group.getByName(command[1]);
    }
    else
    {
      group = getCurrentGroup();
    }
    if (group == null)
    {
      printStatus(412, "Not currently in newsgroup");
      return true;
    }
    List<Article> list = group.getAllArticles();
    printStatus(211, "list of article numbers follow"); // argh, bad english in
    // RFC
    for (Article a : list)
    {
      printTextLine("" + a.getNumberInGroup());
    }
    println(".");
    flush();
    return true;
  }

}
