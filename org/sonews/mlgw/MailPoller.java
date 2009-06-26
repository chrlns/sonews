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

import java.util.Properties;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import org.sonews.daemon.Config;
import org.sonews.daemon.AbstractDaemon;
import org.sonews.util.Log;
import org.sonews.util.Stats;

/**
 * Daemon polling for new mails in a POP3 account to be delivered to newsgroups.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class MailPoller extends AbstractDaemon
{

  static class PasswordAuthenticator extends Authenticator
  {
    
    @Override
    public PasswordAuthentication getPasswordAuthentication()
    {
      final String username = 
        Config.getInstance().get(Config.MLPOLL_USER, "user");
      final String password = 
        Config.getInstance().get(Config.MLPOLL_PASSWORD, "mysecret");

      return new PasswordAuthentication(username, password);
    }
    
  }
  
  @Override
  public void run()
  {
    Log.msg("Starting Mailinglist Poller...", false);
    int errors = 0;
    while(isRunning() && errors < 5)
    {
      try
      {
        // Wait some time between runs. At the beginning has advantages,
        // because the wait is not skipped if an exception occurs.
        Thread.sleep(60000 * (errors + 1)); // one minute * errors
        
        final String host     = 
          Config.getInstance().get(Config.MLPOLL_HOST, "samplehost");
        final String username = 
          Config.getInstance().get(Config.MLPOLL_USER, "user");
        final String password = 
          Config.getInstance().get(Config.MLPOLL_PASSWORD, "mysecret");
        
        Stats.getInstance().mlgwRunStart();
        
        // Create empty properties
        Properties props = System.getProperties();
        props.put("mail.pop3.host", host);

        // Get session
        Session session = Session.getInstance(props);

        // Get the store
        Store store = session.getStore("pop3");
        store.connect(host, 110, username, password);

        // Get folder
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        // Get directory
        Message[] messages = folder.getMessages();

        // Dispatch messages and delete it afterwards on the inbox
        for(Message message : messages)
        {
          String subject = message.getSubject();
          System.out.println("MLGateway: message with subject \"" + subject + "\" received.");
          if(Dispatcher.toGroup(message)
            || Config.getInstance().get(Config.MLPOLL_DELETEUNKNOWN, false))
          {
            // Delete the message
            message.setFlag(Flag.DELETED, true);
          }
        }

        // Close connection 
        folder.close(true); // true to expunge deleted messages
        store.close();
        errors = 0;
        
        Stats.getInstance().mlgwRunEnd();
      }
      catch(NoSuchProviderException ex)
      {
        Log.msg(ex.toString(), false);
        shutdown();
      }
      catch(AuthenticationFailedException ex)
      {
        // AuthentificationFailedException may be thrown if credentials are
        // bad or if the Mailbox is in use (locked).
        ex.printStackTrace();
        errors++;
      }
      catch(InterruptedException ex)
      {
        System.out.println("sonews: " + this + " returns.");
        return;
      }
      catch(MessagingException ex)
      {
        ex.printStackTrace();
        errors++;
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
        errors++;
      }
    }
    Log.msg("MailPoller exited.", false);
  }
  
}
