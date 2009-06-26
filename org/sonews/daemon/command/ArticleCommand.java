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
import org.sonews.daemon.storage.Group;

/**
 * Class handling the ARTICLE, BODY and HEAD commands.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ArticleCommand extends AbstractCommand
{
  
  public ArticleCommand(final NNTPConnection connection)
  {
    super(connection);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }

  // TODO: Refactor this method to reduce its complexity!
  @Override
  public void processLine(final String line)
    throws IOException
  {
    final String[] command = line.split(" ");
    
    Article article  = null;
    long    artIndex = -1;
    if (command.length == 1)
    {
      article = getCurrentArticle();
      if (article == null)
      {
        printStatus(420, "no current article has been selected");
        return;
      }
    }
    else if (command[1].matches(NNTPConnection.MESSAGE_ID_PATTERN))
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
        Group currentGroup = connection.getCurrentGroup();
        if(currentGroup == null)
        {
          printStatus(400, "no group selected");
          return;
        }
        
        artIndex = Long.parseLong(command[1]);
        article  = Article.getByArticleNumber(artIndex, currentGroup);
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

    if(command[0].equalsIgnoreCase("ARTICLE"))
    {
      printStatus(220, artIndex + " " + article.getMessageID()
          + " article retrieved - head and body follow");
      
      println(article.getHeaderSource());
      
      println("");
      println(article.getBody(), article.getBodyCharset());
      println(".");
    }
    else if(command[0].equalsIgnoreCase("BODY"))
    {
      printStatus(222, artIndex + " " + article.getMessageID() + " body");
      println(article.getBody(), article.getBodyCharset());
      println(".");
    }
    
    /*
     * HEAD: This command is mandatory.
     *
     * Syntax
     *    HEAD message-id
     *    HEAD number
     *    HEAD
     *
     * Responses
     *
     * First form (message-id specified)
     *  221 0|n message-id    Headers follow (multi-line)
     *  430                   No article with that message-id
     *
     * Second form (article number specified)
     *  221 n message-id      Headers follow (multi-line)
     *  412                   No newsgroup selected
     *  423                   No article with that number
     *
     * Third form (current article number used)
     *  221 n message-id      Headers follow (multi-line)
     *  412                   No newsgroup selected
     *  420                   Current article number is invalid
     *
     * Parameters
     *  number        Requested article number
     *  n             Returned article number
     *  message-id    Article message-id
     */
    else if(command[0].equalsIgnoreCase("HEAD"))
    {
      printStatus(221, artIndex + " " + article.getMessageID()
          + " Headers follow (multi-line)");
      
      println(article.getHeaderSource());
      println(".");
    }
  }  
  
}
