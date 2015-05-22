/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

import org.sonews.config.Config;
import org.sonews.daemon.DaemonRunner;
import org.sonews.util.Log;

/**
 * Daemon polling for new mails in a POP3 account to be delivered to newsgroups.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class MailPoller extends DaemonRunner {

    static class PasswordAuthenticator extends Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            final String username
                    = Config.inst().get(Config.MLPOLL_USER, "user");
            final String password
                    = Config.inst().get(Config.MLPOLL_PASSWORD, "mysecret");

            return new PasswordAuthentication(username, password);
        }
    }

    @Override
    public void run() {
        Log.get().info("Starting Mailinglist Poller...");
        int errors = 0;
        while (daemon.isRunning()) {
            try {
				// Wait some time between runs. At the beginning has advantages,
                // because the wait is not skipped if an exception occurs.
                Thread.sleep(60000 * (errors + 1)); // one minute * errors

                final String host
                        = Config.inst().get(Config.MLPOLL_HOST, "samplehost");
                final String username
                        = Config.inst().get(Config.MLPOLL_USER, "user");
                final String password
                        = Config.inst().get(Config.MLPOLL_PASSWORD, "mysecret");

                // Create empty properties
                Properties props = System.getProperties();
                props.put("mail.pop3.host", host);
                props.put("mail.mime.address.strict", "false");

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
                for (Message message : messages) {
                    if (Dispatcher.toGroup(message)
                            || Config.inst().get(Config.MLPOLL_DELETEUNKNOWN, false)) {
                        // Delete the message
                        message.setFlag(Flag.DELETED, true);
                    }
                }

                // Close connection
                folder.close(true); // true to expunge deleted messages
                store.close();
                errors = 0;
            } catch (NoSuchProviderException ex) {
                Log.get().severe(ex.toString());
                daemon.requestShutdown();
            } catch (AuthenticationFailedException ex) {
				// AuthentificationFailedException may be thrown if credentials are
                // bad or if the Mailbox is in use (locked).
                ex.printStackTrace();
                errors = errors < 5 ? errors + 1 : errors;
            } catch (InterruptedException ex) {
                System.out.println("sonews: " + this + " returns: " + ex);
                return;
            } catch (MessagingException ex) {
                ex.printStackTrace();
                errors = errors < 5 ? errors + 1 : errors;
            } catch (Exception ex) {
                ex.printStackTrace();
                errors = errors < 5 ? errors + 1 : errors;
            }
        }
        Log.get().severe("MailPoller exited.");
    }
}
