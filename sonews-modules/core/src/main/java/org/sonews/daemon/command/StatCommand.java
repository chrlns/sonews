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
package org.sonews.daemon.command;

import java.io.IOException;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.sync.SynchronousNNTPConnection;
import org.sonews.storage.Article;
import org.sonews.storage.StorageBackendException;
import org.springframework.stereotype.Component;

/**
 * Implementation of the STAT command.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
public class StatCommand implements Command {

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "STAT" };
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

    // TODO: Method has various exit points => Refactor!
    @Override
    public void processLine(NNTPConnection conn, final String line, byte[] raw)
            throws IOException, StorageBackendException {
        final String[] command = line.split(" ");

        Article article = null;
        if (command.length == 1) {
            article = conn.getCurrentArticle();
            if (article == null) {
                conn.println("420 no current article has been selected");
                return;
            }
        } else if (command[1].matches(SynchronousNNTPConnection.MESSAGE_ID_PATTERN)) {
            // Message-ID
            article = Article.getByMessageID(command[1]);
            if (article == null) {
                conn.println("430 no such article found");
                return;
            }
        } else {
            // Message Number
            try {
                long aid = Long.parseLong(command[1]);
                article = conn.getCurrentGroup().getArticle(aid);
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

        conn.println("223 " + conn.getCurrentGroup().getIndexOf(article)
                + " " + article.getMessageID()
                + " article retrieved - request text separately");
    }
}
