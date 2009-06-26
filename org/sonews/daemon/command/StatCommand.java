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
import org.sonews.daemon.storage.Article;
import org.sonews.daemon.NNTPConnection;

/**
 * Implementation of the STAT command.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class StatCommand extends AbstractCommand
{

  public StatCommand(final NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }

  // TODO: Method has various exit points => Refactor!
  @Override
  public void processLine(final String line)
    throws IOException, SQLException
  {
    final String[] command = line.split(" ");

    Article article = null;
    if(command.length == 1)
    {
      article = getCurrentArticle();
      if(article == null)
      {
        printStatus(420, "no current article has been selected");
        return;
      }
    }
    else if(command[1].matches(NNTPConnection.MESSAGE_ID_PATTERN))
    {
      // Message-ID
      article = Article.getByMessageID(command[1]);
      if (article == null)
      {
        printStatus(430, "no such article found");
        return;
      }
    }
    else
    {
      // Message Number
      try
      {
        long aid = Long.parseLong(command[1]);
        article = Article.getByArticleNumber(aid, getCurrentGroup());
      }
      catch(NumberFormatException ex)
      {
        ex.printStackTrace();
      }
      catch(SQLException ex)
      {
        ex.printStackTrace();
      }
      if (article == null)
      {
        printStatus(423, "no such article number in this group");
        return;
      }
      setCurrentArticle(article);
    }
    
    printStatus(223, article.getIndexInGroup(getCurrentGroup()) + " " + article.getMessageID()
          + " article retrieved - request text separately");
  }
  
}
