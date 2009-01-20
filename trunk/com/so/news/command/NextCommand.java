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
import com.so.news.NNTPConnection;
import com.so.news.storage.Article;
import com.so.news.storage.Group;

/**
 * Class handling the NEXT and LAST command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 */
public class NextCommand extends Command
{
  public NextCommand(NNTPConnection conn)
  {
    super(conn);
  }

  public boolean process(String[] command) throws IOException
  {
    String commandName = command[0];
    if (!(commandName.equalsIgnoreCase("NEXT") || commandName
        .equalsIgnoreCase("LAST")))
      return false;
    // untested, RFC977 complient
    Article currA = getCurrentArticle();
    Group currG = getCurrentGroup();
    if (currA == null)
    {
      printStatus(420, "no current article has been selected");
      return true;
    }
    if (currG == null)
    {
      printStatus(412, "no newsgroup selected");
      return true;
    }
    Article article;
    if (commandName.equalsIgnoreCase("NEXT"))
    {
      article = currA.nextArticleInGroup();
      if (article == null)
      {
        printStatus(421, "no next article in this group");
        return true;
      }
    }
    else
    {
      article = currA.prevArticleInGroup();
      if (article == null)
      {
        printStatus(422, "no previous article in this group");
        return true;
      }
    }
    setCurrentArticle(article);
    printStatus(223, article.getNumberInGroup() + " " + article.getMessageID()
        + " article retrieved - request text separately");
    return true;
  }

}
