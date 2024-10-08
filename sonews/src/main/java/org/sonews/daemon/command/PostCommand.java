/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import org.sonews.config.Config;
import org.sonews.daemon.NNTPConnection;
import org.sonews.feed.FeedManager;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.Headers;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Implementation of the POST command. This command requires multiple lines from
 * the client, so the handling of asynchronous reading is a little tricky to
 * handle.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
@Scope("prototype")
public class PostCommand implements Command {

    private Article article;
    private int lineCount = 0;
    private long bodySize = 0;
    private InternetHeaders headers = null;
    
    // Size in kbytes, default is 128k
    private final long maxBodySize = Config.inst().get(Config.ARTICLE_MAXSIZE, 128) * 1024L; 
    
    private PostState state = PostState.WaitForLineOne;
    private final ByteArrayOutputStream bufBody = new ByteArrayOutputStream();
    private final StringBuilder strHead = new StringBuilder();

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "POST" };
    }

    @Override
    public boolean hasFinished() {
        return this.state == PostState.Finished;
    }

    @Override
    public String impliedCapability() {
        return null;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    /**
     * Process the given line String. line.trim() was called by NNTPConnection.
     *
     * @param conn
     * @param line
     * @param raw
     * @throws java.io.IOException
     * @throws org.sonews.storage.StorageBackendException
     */
    @Override
    // TODO: Refactor this method to reduce complexity!
    public void processLine(NNTPConnection conn, String line, byte[] raw)
            throws IOException, StorageBackendException
    {
        switch (state) {
            case WaitForLineOne ->  {
                if (line.equalsIgnoreCase("POST")) {
                    this.article = StorageManager.createArticle();
                    state = PostState.ReadingHeaders;

                    conn.println("340 send article to be posted. End with <CR-LF>.<CR-LF>");
                } else {
                    conn.println("500 invalid command usage");
                }
            }
            case ReadingHeaders ->  {
                strHead.append(line);
                strHead.append(NNTPConnection.NEWLINE);

                if ("".equals(line) || ".".equals(line)) {
                    // we finally met the blank line
                    // separating headers from body

                    try {
                        // Parse the header using the InternetHeader class from
                        // JavaMail API
                        headers = new InternetHeaders(new ByteArrayInputStream(
                                strHead.toString().trim().getBytes(conn.getCurrentCharset())));

                        // add the header entries for the article
                        article.setHeaders(headers);
                    } catch (MessagingException ex) {
                        Log.get().log(Level.INFO, ex.getLocalizedMessage(), ex);
                        conn.println("500 posting failed - invalid header");
                        state = PostState.Finished;
                    }

                    // Change charset for reading body;
                    // for multipart messages UTF-8 is returned
                    // conn.setCurrentCharset(article.getBodyCharset());

                    state = PostState.ReadingBody;

                    // WTF: do we need articles without bodies?
                    if (".".equals(line)) {
                        // Post an article without body
                        postArticle(conn, article);
                        state = PostState.Finished;
                    }
                }
            }
            case ReadingBody ->  {
                if (".".equals(line)) {
                    // Set some headers needed for Over command
                    headers.setHeader(Headers.LINES, Integer.toString(lineCount));
                    headers.setHeader(Headers.BYTES, Long.toString(bodySize));

                    byte[] body = bufBody.toByteArray();
                    if (body.length >= 2) {
                        // Remove trailing CRLF
                        body = Arrays.copyOf(body, body.length - 2);
                    }
                    article.setBody(body); // set the article body

                    postArticle(conn, article);
                    state = PostState.Finished;
                } else {
                    bodySize += line.length() + 1;
                    lineCount++;

                    // Add line to body buffer
                    bufBody.write(raw, 0, raw.length);
                    bufBody.write(NNTPConnection.NEWLINE.getBytes("UTF-8"));

                    if (bodySize > maxBodySize) {
                        conn.println("500 article is too long");
                        state = PostState.Finished;
                    }
                }
            }
            default -> {
                // Should never happen
                Log.get().severe("PostCommand::processLine(): already finished...");
            }
        }
    }

    /**
     * Article is a control message and needs special handling.
     *
     * @param article
     */
    private void controlMessage(NNTPConnection conn, Article article)
            throws IOException {
        String[] ctrl = article.getHeader(Headers.CONTROL)[0].split(" ");
        if (ctrl.length == 2) // "cancel <mid>"
        {
            try {
                StorageManager.current().delete(ctrl[1]);

                // Move cancel message to "control" group
                article.setHeader(Headers.NEWSGROUPS, "control");
                StorageManager.current().addArticle(article);
                conn.println("240 article cancelled");
            } catch (StorageBackendException ex) {
                Log.get().severe(ex.toString());
                conn.println("500 internal server error");
            }
        } else {
            conn.println("441 unknown control header");
        }
    }

    private void supersedeMessage(NNTPConnection conn, Article article)
            throws IOException {
        try {
            String oldMsg = article.getHeader(Headers.SUPERSEDES)[0];
            StorageManager.current().delete(oldMsg);
            StorageManager.current().addArticle(article);
            conn.println("240 article replaced");
        } catch (StorageBackendException ex) {
            Log.get().severe(ex.toString());
            conn.println("500 internal server error");
        }
    }

    private void postArticle(NNTPConnection conn, Article article)
            throws IOException {
        //article.setUser(conn.getUser());
        // FIXME: Setting the user for authentication at article is wrong
        // Set it at the StorageManager instead

        if (article.getHeader(Headers.CONTROL)[0].length() > 0) {
            controlMessage(conn, article);
        } else if (article.getHeader(Headers.SUPERSEDES)[0].length() > 0) {
            supersedeMessage(conn, article);
        } else { // Post the article regularily
            // Circle check; note that Path can already contain the hostnamehere
            String host = Config.inst().get(Config.HOSTNAME, null);
            if (article.getHeader(Headers.PATH)[0].indexOf(host + "!", 1) > 0) {
                Log.get().log(Level.INFO, "{0} skipped for host {1}",
                        new Object[] { article.getMessageID(), host });
                conn.println("441 I know this article already");
                return;
            }

            // Try to create the article in the database or post it to
            // appropriate mailing list
            try {
                boolean success = false;
                String[] groupnames = article.getHeader(Headers.NEWSGROUPS)[0]
                        .split(",");
                for (String groupname : groupnames) {
                    Group group = Group.get(groupname);
                    if (group != null && !group.isDeleted()) {
                        if (group.isMailingList() /*&& !conn.isLocalConnection()*/) {
                            // Send to mailing list; the Dispatcher writes
                            // statistics to database
                            // FIXME success = Dispatcher.toList(article,
                            // group.getName());
                        } else {
                            // Store in database
                            if (!StorageManager.current().isArticleExisting(article.getMessageID())) {
                                StorageManager.current().addArticle(article);
                            } else {
                                Log.get().info("Article exists and is not stored.");
                            }
                            success = true;
                        }
                    }
                } // end for

                if (success) {
                    conn.println("240 article posted ok");
                    FeedManager.queueForPush(article);
                } else {
                    conn.println("441 newsgroup not found or configuration error");
                }
            } catch (StorageBackendException ex) {
                ex.printStackTrace();
                conn.println("500 internal server error");
            }
        }
    }

}
