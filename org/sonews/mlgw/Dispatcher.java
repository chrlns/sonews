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

package org.sonews.mlgw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.InternetAddress;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.storage.Headers;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;
import org.sonews.util.Stats;

/**
 * Dispatches messages from mailing list or newsserver or vice versa.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Dispatcher 
{

  static class PasswordAuthenticator extends Authenticator
  {
    
    @Override
    public PasswordAuthentication getPasswordAuthentication()
    {
      final String username = 
        Config.inst().get(Config.MLSEND_USER, "user");
      final String password = 
        Config.inst().get(Config.MLSEND_PASSWORD, "mysecret");

      return new PasswordAuthentication(username, password);
    }
    
  }
  
  /**
   * Posts a message that was received from a mailing list to the 
   * appropriate newsgroup.
   * @param msg
   */
  public static boolean toGroup(final Message msg)
  {
    try
    {
      Address[] to = msg.getAllRecipients(); // includes TO/CC/BCC
      if(to == null || to.length <= 0)
      {
        to = msg.getReplyTo();
      }

      if(to == null || to.length <= 0)
      {
        Log.msg("Skipping message because no recipient!", false);
        return false;
      }
      else
      {
        boolean      posted     = false;
        List<String> newsgroups = new ArrayList<String>();

        for (Address toa : to) // Address can have '<' '>' around
        {
          if (toa instanceof InternetAddress)
          {
            List<String> groups = StorageManager.current()
              .getGroupsForList((InternetAddress)toa);
            newsgroups.addAll(groups);
          }
        }

        if (newsgroups.size() > 0)
        {
          StringBuilder groups = new StringBuilder();
          for(int n = 0; n < newsgroups.size(); n++)
          {
            groups.append(newsgroups.get(n));
            if(n + 1 != newsgroups.size())
            {
              groups.append(',');
            }
          }
          Log.msg("Posting to group " + groups.toString(), true);

          // Create new Article object
          Article article = new Article(msg);
          article.setGroup(groups.toString());
          article.removeHeader(Headers.REPLY_TO);
          article.removeHeader(Headers.TO);

          // Write article to database
          if(!StorageManager.current().isArticleExisting(article.getMessageID()))
          {
            StorageManager.current().addArticle(article);
            Stats.getInstance().mailGatewayed(
              article.getHeader(Headers.NEWSGROUPS)[0]);
          }
          else
          {
            Log.msg("Article " + article.getMessageID() + " already existing.", true);
          }
          posted = true;
        }
        else
        {
          StringBuilder buf = new StringBuilder();
          for(Address toa : to)
          {
            buf.append(' ');
            buf.append(toa.toString());
          }
          Log.msg("No group for" + buf.toString(), false);
        }
        return posted;
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return false;
    }
  }
  
  /**
   * Mails a message received through NNTP to the appropriate mailing list.
   */
  public static void toList(Article article)
    throws IOException, MessagingException, StorageBackendException
  {
    // Get mailing lists for the group of this article
    List<String> listAddresses = new ArrayList<String>();
    String[]     groupnames    = article.getHeader(Headers.NEWSGROUPS)[0].split(",");
    
    for(String groupname : groupnames)
    {
      String listAddress = StorageManager.current().getListForGroup(groupname);
      if(listAddress != null)
      {
        listAddresses.add(listAddress);
      }
    }

    for(String listAddress : listAddresses)
    {
      // Compose message and send it via given SMTP-Host
      String smtpHost = Config.inst().get(Config.MLSEND_HOST, "localhost");
      int    smtpPort = Config.inst().get(Config.MLSEND_PORT, 25);
      String smtpUser = Config.inst().get(Config.MLSEND_USER, "user");
      String smtpPw   = Config.inst().get(Config.MLSEND_PASSWORD, "mysecret");
      String smtpFrom = Config.inst().get(
          Config.MLSEND_ADDRESS, article.getHeader(Headers.FROM)[0]);

      // TODO: Make Article cloneable()
      String group = article.getHeader(Headers.NEWSGROUPS)[0];
      article.getMessageID(); // Make sure an ID is existing
      article.removeHeader(Headers.NEWSGROUPS);
      article.removeHeader(Headers.PATH);
      article.removeHeader(Headers.LINES);
      article.removeHeader(Headers.BYTES);

      article.setHeader("To", listAddress);
      article.setHeader("Reply-To", listAddress);

      if(Config.inst().get(Config.MLSEND_RW_SENDER, false))
      {
        rewriteSenderAddress(article); // Set the SENDER address
      }

      SMTPTransport smtpTransport = new SMTPTransport(smtpHost, smtpPort);
      smtpTransport.send(article, smtpFrom, listAddress);
      smtpTransport.close();

      Stats.getInstance().mailGatewayed(group);
      Log.msg("MLGateway: Mail " + article.getHeader("Subject")[0] 
        + " was delivered to " + listAddress + ".", true);
    }
  }
  
  /**
   * Sets the SENDER header of the given MimeMessage. This might be necessary
   * for moderated groups that does not allow the "normal" FROM sender.
   * @param msg
   * @throws javax.mail.MessagingException
   */
  private static void rewriteSenderAddress(Article msg)
    throws MessagingException
  {
    String mlAddress = Config.inst().get(Config.MLSEND_ADDRESS, null);

    if(mlAddress != null)
    {
      msg.setHeader(Headers.SENDER, mlAddress);
    }
    else
    {
      throw new MessagingException("Cannot rewrite SENDER header!");
    }
  }
  
}
