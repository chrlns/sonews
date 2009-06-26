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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.SQLException;
import java.util.Locale;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetHeaders;
import org.sonews.daemon.Config;
import org.sonews.util.Log;
import org.sonews.mlgw.Dispatcher;
import org.sonews.daemon.storage.Article;
import org.sonews.daemon.storage.Database;
import org.sonews.daemon.storage.Group;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.storage.Headers;
import org.sonews.feed.FeedManager;
import org.sonews.util.Stats;

/**
 * Implementation of the POST command. This command requires multiple lines
 * from the client, so the handling of asynchronous reading is a little tricky
 * to handle.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class PostCommand extends AbstractCommand
{
  
  private final Article article   = new Article();
  private int           lineCount = 0;
  private long          bodySize  = 0;
  private InternetHeaders headers = null;
  private long          maxBodySize  = 
    Config.getInstance().get(Config.ARTICLE_MAXSIZE, 128) * 1024L; // Size in bytes
  private PostState     state     = PostState.WaitForLineOne;
  private final StringBuilder strBody   = new StringBuilder();
  private final StringBuilder strHead   = new StringBuilder();
  
  public PostCommand(final NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return this.state == PostState.Finished;
  }

  /**
   * Process the given line String. line.trim() was called by NNTPConnection.
   * @param line
   * @throws java.io.IOException
   * @throws java.sql.SQLException
   */
  @Override // TODO: Refactor this method to reduce complexity!
  public void processLine(String line)
    throws IOException, SQLException
  {
    switch(state)
    {
      case WaitForLineOne:
      {
        if(line.equalsIgnoreCase("POST"))
        {
          printStatus(340, "send article to be posted. End with <CR-LF>.<CR-LF>");
          state = PostState.ReadingHeaders;
        }
        else
        {
          printStatus(500, "invalid command usage");
        }
        break;
      }
      case ReadingHeaders:
      {
        strHead.append(line);
        strHead.append(NNTPConnection.NEWLINE);
        
        if("".equals(line) || ".".equals(line))
        {
          // we finally met the blank line
          // separating headers from body
          
          try
          {
            // Parse the header using the InternetHeader class from JavaMail API
            headers = new InternetHeaders(
              new ByteArrayInputStream(strHead.toString().trim()
                .getBytes(connection.getCurrentCharset())));

            // add the header entries for the article
            article.setHeaders(headers);
          }
          catch (MessagingException e)
          {
            e.printStackTrace();
            printStatus(500, "posting failed - invalid header");
            state = PostState.Finished;
            break;
          }

          // Change charset for reading body; 
          // for multipart messages UTF-8 is returned
          connection.setCurrentCharset(article.getBodyCharset());
          
          state = PostState.ReadingBody;
          
          if(".".equals(line))
          {
            // Post an article without body
            postArticle(article);
            state = PostState.Finished;
          }
        }
        break;
      }
      case ReadingBody:
      {
        if(".".equals(line))
        {    
          // Set some headers needed for Over command
          headers.setHeader(Headers.LINES, Integer.toString(lineCount));
          headers.setHeader(Headers.BYTES, Long.toString(bodySize));
          
          if(strBody.length() >= 2)
          {
            strBody.deleteCharAt(strBody.length() - 1); // Remove last newline
            strBody.deleteCharAt(strBody.length() - 1); // Remove last CR
          }
          article.setBody(strBody.toString()); // set the article body
          
          postArticle(article);
          state = PostState.Finished;
        }
        else
        {
          bodySize += line.length() + 1;
          lineCount++;
          
          // Add line to body buffer
          strBody.append(line);
          strBody.append(NNTPConnection.NEWLINE);
          
          if(bodySize > maxBodySize)
          {
            printStatus(500, "article is too long");
            state = PostState.Finished;
            break;
          }
          
          // Check if this message is a MIME-multipart message and needs a
          // charset change
          try
          {
            line = line.toLowerCase(Locale.ENGLISH);
            if(line.startsWith(Headers.CONTENT_TYPE))
            {
              int idxStart = line.indexOf("charset=") + "charset=".length();
              int idxEnd   = line.indexOf(";", idxStart);
              if(idxEnd < 0)
              {
                idxEnd = line.length();
              }

              if(idxStart > 0)
              {
                String charsetName = line.substring(idxStart, idxEnd);
                if(charsetName.length() > 0 && charsetName.charAt(0) == '"')
                {
                  charsetName = charsetName.substring(1, charsetName.length() - 1);
                }

                try
                {
                  connection.setCurrentCharset(Charset.forName(charsetName));
                }
                catch(IllegalCharsetNameException ex)
                {
                  Log.msg("PostCommand: " + ex, false);
                }
                catch(UnsupportedCharsetException ex)
                {
                  Log.msg("PostCommand: " + ex, false);
                }
              } // if(idxStart > 0)
            }
          }
          catch(Exception ex)
          {
            ex.printStackTrace();
          }
        }
        break;
      }
      default:
        Log.msg("PostCommand::processLine(): already finished...", false);
    }
  }
  
  /**
   * Article is a control message and needs special handling.
   * @param article
   */
  private void controlMessage(Article article)
    throws IOException
  {
    String[] ctrl = article.getHeader(Headers.CONTROL)[0].split(" ");
    if(ctrl.length == 2) // "cancel <mid>"
    {
      try
      {
        Database.getInstance().delete(ctrl[1]);
        
        // Move cancel message to "control" group
        article.setHeader(Headers.NEWSGROUPS, "control");
        Database.getInstance().addArticle(article);
        printStatus(240, "article cancelled");
      }
      catch(SQLException ex)
      {
        Log.msg(ex, false);
        printStatus(500, "internal server error");
      }
    }
    else
    {
      printStatus(441, "unknown Control header");
    }
  }
  
  private void supersedeMessage(Article article)
    throws IOException
  {
    try
    {
      String oldMsg = article.getHeader(Headers.SUPERSEDES)[0];
      Database.getInstance().delete(oldMsg);
      Database.getInstance().addArticle(article);
      printStatus(240, "article replaced");
    }
    catch(SQLException ex)
    {
      Log.msg(ex, false);
      printStatus(500, "internal server error");
    }
  }
  
  private void postArticle(Article article) 
    throws IOException
  {
    if(article.getHeader(Headers.CONTROL)[0].length() > 0)
    {
      controlMessage(article);
    }
    else if(article.getHeader(Headers.SUPERSEDES)[0].length() > 0)
    {
      supersedeMessage(article);
    }
    else // Post the article regularily
    {
      // Try to create the article in the database or post it to
      // appropriate mailing list
      try
      {
        boolean success = false;
        String[] groupnames = article.getHeader(Headers.NEWSGROUPS)[0].split(",");
        for(String groupname : groupnames)
        {
          Group group = Database.getInstance().getGroup(groupname);
          if(group != null)
          {
            if(group.isMailingList() && !connection.isLocalConnection())
            {
              // Send to mailing list; the Dispatcher writes 
              // statistics to database
              Dispatcher.toList(article);
              success = true;
            }
            else
            {
              // Store in database
              if(!Database.getInstance().isArticleExisting(article.getMessageID()))
              {
                Database.getInstance().addArticle(article);

                // Log this posting to statistics
                Stats.getInstance().mailPosted(
                  article.getHeader(Headers.NEWSGROUPS)[0]);
              }
              success = true;
            }
          }
        } // end for

        if(success)
        {
          printStatus(240, "article posted ok");
          FeedManager.queueForPush(article);
        }
        else
        {
          printStatus(441, "newsgroup not found");
        }
      }
      catch(AddressException ex)
      {
        Log.msg(ex.getMessage(), true);
        printStatus(441, "invalid sender address");
      }
      catch(MessagingException ex)
      {
        // A MessageException is thrown when the sender email address is
        // invalid or something is wrong with the SMTP server.
        System.err.println(ex.getLocalizedMessage());
        printStatus(441, ex.getClass().getCanonicalName() + ": " + ex.getLocalizedMessage());
      }
      catch(SQLException ex)
      {
        ex.printStackTrace();
        printStatus(500, "internal server error");
      }
    }
  }

}
