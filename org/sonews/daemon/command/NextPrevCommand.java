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
import org.sonews.daemon.storage.Article;
import org.sonews.daemon.storage.Group;

/**
 * Class handling the NEXT and LAST command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class NextPrevCommand extends AbstractCommand
{

  public NextPrevCommand(final NNTPConnection conn)
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
    final Article currA = getCurrentArticle();
    final Group   currG = getCurrentGroup();
    
    if (currA == null)
    {
      printStatus(420, "no current article has been selected");
      return;
    }
    
    if (currG == null)
    {
      printStatus(412, "no newsgroup selected");
      return;
    }
    
    final String[] command = line.split(" ");

    if(command[0].equalsIgnoreCase("NEXT"))
    {
      selectNewArticle(currA, currG, 1);
    }
    else if(command[0].equalsIgnoreCase("PREV"))
    {
      selectNewArticle(currA, currG, -1);
    }
    else
    {
      printStatus(500, "internal server error");
    }
  }
  
  private void selectNewArticle(Article article, Group grp, final int delta)
    throws IOException, SQLException
  {
    assert article != null;

    article = Article.getByArticleNumber(article.getIndexInGroup(grp) + delta, grp);

    if(article == null)
    {
      printStatus(421, "no next article in this group");
    }
    else
    {
      setCurrentArticle(article);
      printStatus(223, article.getIndexInGroup(getCurrentGroup()) + " " + article.getMessageID() + " article retrieved - request text separately");
    }
  }

}
