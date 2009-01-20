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
 * Class handling the LIST command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 */
public class ListCommand extends Command
{
  public ListCommand(NNTPConnection conn)
  {
    super(conn);
  }

  public boolean process(String[] command) 
    throws Exception
  {
    if (command.length >= 2)
    {
      if (command[1].equalsIgnoreCase("OVERVIEW.FMT"))
      {
        printStatus(215, "information follows");
        printText("Subject:\nFrom:\nDate:\nMessage-ID:\nReferences:\nBytes:\nLines:");
        return true;
      }
      if (command[1].equalsIgnoreCase("NEWSGROUPS"))
      {
        printStatus(215, "information follows");
        ArrayList<Group> list = Group.getAll();
        for (Group g : list)
        {
          printTextLine(g.getName() + "\t" + "-");
        }
        println(".");
        flush();
        return true;
      }
      if (command[1].equalsIgnoreCase("SUBSCRIPTIONS"))
      {
        printStatus(215, "information follows");
        println(".");
        flush();
        return true;
      }
      if (command[1].equalsIgnoreCase("EXTENSIONS"))
      {
        printStatus(202, "Supported NNTP extensions.");
        printTextLine("LISTGROUP");
        println(".");
        flush();
        return true;
      }
      return false;
    }
    printStatus(215, "list of newsgroups follows");
    for (Group g : Group.getAll())
    {
      //if(g.getEstimatedArticleCount() <= 0)
      //  continue;
      
      printTextLine(g.getName() + " " + g.getLastArticle() + " "
          + g.getFirstArticle() + " y");
    }
    println(".");
    flush();
    return true;
  }

}
