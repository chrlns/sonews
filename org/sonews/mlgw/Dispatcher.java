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
import org.sonews.daemon.Config;
import org.sonews.daemon.storage.Article;
import org.sonews.util.io.ArticleInputStream;
import org.sonews.daemon.storage.Database;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.sonews.daemon.storage.Headers;
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
        Config.getInstance().get(Config.MLSEND_USER, "user");
      final String password = 
        Config.getInstance().get(Config.MLSEND_PASSWORD, "mysecret");

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
        Log.msg("Skipping message because no receipient!", true);
        return false;
      }
      else
      {
        boolean posted = false;
        for(Address toa : to) // Address can have '<' '>' around
        {
          if(!(toa instanceof InternetAddress))
          {
            continue;
          }
          String group = Database.getInstance()
            .getGroupForList((InternetAddress)toa);
          if(group != null)
          {
            Log.msg("Posting to group " + group, true);

            // Create new Article object
            Article article = new Article(msg);
            article.setGroup(group);
            
            // Write article to database
            if(!Database.getInstance().isArticleExisting(article.getMessageID()))
            {
              Database.getInstance().addArticle(article);
              Stats.getInstance().mailGatewayed(
                article.getHeader(Headers.NEWSGROUPS)[0]);
            }
            else
            {
              Log.msg("Article " + article.getMessageID() + " already existing.", true);
              // TODO: It may be possible that a ML mail is posted to several
              // ML addresses...
            }
            posted = true;
          }
          else
          {
            Log.msg("No group for " + toa, true);
          }
        } // end for
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
    throws IOException, MessagingException, SQLException
  {
    // Get mailing lists for the group of this article
    List<String> listAddresses = new ArrayList<String>();
    String[]     groupnames    = article.getHeader(Headers.NEWSGROUPS)[0].split(",");
    
    for(String groupname : groupnames)
    {
      String listAddress = Database.getInstance().getListForGroup(groupname);
      if(listAddress != null)
      {
        listAddresses.add(listAddress);
      }
    }

    for(String listAddress : listAddresses)
    {
      // Compose message and send it via given SMTP-Host
      String smtpHost = Config.getInstance().get(Config.MLSEND_HOST, "localhost");
      int    smtpPort = Config.getInstance().get(Config.MLSEND_PORT, 25);
      String smtpUser = Config.getInstance().get(Config.MLSEND_USER, "user");
      String smtpPw   = Config.getInstance().get(Config.MLSEND_PASSWORD, "mysecret");

      Properties props    = System.getProperties();
      props.put("mail.smtp.localhost", 
        Config.getInstance().get(Config.HOSTNAME, "localhost"));
      props.put("mail.smtp.from",  // Used for MAIL FROM command
        Config.getInstance().get(
          Config.MLSEND_ADDRESS, article.getHeader(Headers.FROM)[0]));
      props.put("mail.smtp.host", smtpHost);
      props.put("mail.smtp.port", smtpPort);
      props.put("mail.smtp.auth", "true");

      Address[] address = new Address[1];
      address[0] = new InternetAddress(listAddress);

      ArticleInputStream in = new ArticleInputStream(article);
      Session session = Session.getDefaultInstance(props, new PasswordAuthenticator());
      MimeMessage msg = new MimeMessage(session, in);
      msg.setRecipient(Message.RecipientType.TO, address[0]);
      msg.setReplyTo(address);
      msg.removeHeader(Headers.NEWSGROUPS);
      msg.removeHeader(Headers.PATH);
      msg.removeHeader(Headers.LINES);
      msg.removeHeader(Headers.BYTES);
      
      if(Config.getInstance().get(Config.MLSEND_RW_SENDER, false))
      {
        rewriteSenderAddress(msg); // Set the SENDER address
      }
      
      if(Config.getInstance().get(Config.MLSEND_RW_FROM, false))
      {
        rewriteFromAddress(msg);   // Set the FROM address
      }
      
      msg.saveChanges();

      // Send the mail
      Transport transport = session.getTransport("smtp");
      transport.connect(smtpHost, smtpPort, smtpUser, smtpPw);
      transport.sendMessage(msg, msg.getAllRecipients());
      transport.close();

      Stats.getInstance().mailGatewayed(article.getHeader(Headers.NEWSGROUPS)[0]);
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
  private static void rewriteSenderAddress(MimeMessage msg)
    throws MessagingException
  {
    String mlAddress = Config.getInstance().get(Config.MLSEND_ADDRESS, null);

    if(mlAddress != null)
    {
      msg.setSender(new InternetAddress(mlAddress));
    }
    else
    {
      throw new MessagingException("Cannot rewrite SENDER header!");
    }
  }
  
  /**
   * Sets the FROM header of the given MimeMessage. This might be necessary
   * for moderated groups that does not allow the "normal" FROM sender.
   * @param msg
   * @throws javax.mail.MessagingException
   */
  private static void rewriteFromAddress(MimeMessage msg)
    throws MessagingException
  {
    Address[] froms  = msg.getFrom();
    String mlAddress = Config.getInstance().get(Config.MLSEND_ADDRESS, null);

    if(froms.length > 0 && froms[0] instanceof InternetAddress 
      && mlAddress != null)
    {
      InternetAddress from = (InternetAddress)froms[0];
      from.setAddress(mlAddress);
      msg.setFrom(from);
    }
    else
    {
      throw new MessagingException("Cannot rewrite FROM header!");
    }    
  }
  
}
