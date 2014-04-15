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
import org.sonews.storage.Article;
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;

/**
 * Class handling the ARTICLE, BODY and HEAD commands.
 * 
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ArticleCommand implements Command {

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "ARTICLE", "BODY", "HEAD" };
    }

    @Override
    public boolean hasFinished() {
        return true;
    }

    @Override
    public String impliedCapability() {
        return null;
    }

    @Override
    public boolean isStateful() {
        return false;
    }

    // TODO: Refactor this method to reduce its complexity!
    @Override
    public void processLine(NNTPConnection conn, final String line, byte[] raw)
            throws IOException {
        final String[] command = line.split(" ");

        Article article = null;
        long artIndex = -1;
        if (command.length == 1) {
            article = conn.getCurrentArticle();
            if (article == null) {
                conn.println("420 no current article has been selected");
                return;
            }
        } else if (command[1].matches(NNTPConnection.MESSAGE_ID_PATTERN)) {
            // Message-ID
            article = Article.getByMessageID(command[1]);
            if (article == null) {
                conn.println("430 no such article found");
                return;
            }
        } else {
            // Message Number
            try {
                Group currentGroup = conn.getCurrentChannel();
                if (currentGroup == null) {
                    conn.println("400 no group selected");
                    return;
                }

                artIndex = Long.parseLong(command[1]);
                article = currentGroup.getArticle(artIndex);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            } catch (StorageBackendException ex) {
                ex.printStackTrace();
            }

            if (article == null) {
                conn.println("423 no such article number in this group");
                return;
            }
            conn.setCurrentArticle(article);
        }

        if (command[0].equalsIgnoreCase("ARTICLE")) {
            conn.println("220 " + artIndex + " " + article.getMessageID()
                    + " article retrieved - head and body follow");
            conn.println(article.getHeaderSource());
            conn.println("");
            conn.println(article.getBody());
            conn.println(".");
        } else if (command[0].equalsIgnoreCase("BODY")) {
            conn.println("222 " + artIndex + " " + article.getMessageID()
                    + " body");
            conn.println(article.getBody());
            conn.println(".");
        } /*
           * HEAD: This command is mandatory.
           * 
           * Syntax HEAD message-id HEAD number HEAD
           * 
           * Responses
           * 
           * First form (message-id specified) 221 0|n message-id Headers follow
           * (multi-line) 430 No article with that message-id
           * 
           * Second form (article number specified) 221 n message-id Headers
           * follow (multi-line) 412 No newsgroup selected 423 No article with
           * that number
           * 
           * Third form (current article number used) 221 n message-id Headers
           * follow (multi-line) 412 No newsgroup selected 420 Current article
           * number is invalid
           * 
           * Parameters number Requested article number n Returned article
           * number message-id Article message-id
           */else if (command[0].equalsIgnoreCase("HEAD")) {
            conn.println("221 " + artIndex + " " + article.getMessageID()
                    + " Headers follow (multi-line)");
            conn.println(article.getHeaderSource());
            conn.println(".");
        }
    }
}
