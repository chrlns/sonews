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
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;
import org.springframework.stereotype.Component;

/**
 * Class handling the NEXT and LAST command.
 *
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
@Component
public class NextPrevCommand implements Command {

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "NEXT", "PREV" };
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

    @Override
    public void processLine(NNTPConnection conn, final String line, byte[] raw)
            throws IOException, StorageBackendException {
        final Article currA = conn.getCurrentArticle();
        final Group currG = conn.getCurrentGroup();

        if (currA == null) {
            conn.println("420 no current article has been selected");
            return;
        }

        if (currG == null) {
            conn.println("412 no newsgroup selected");
            return;
        }

        final String[] command = line.split(" ");

        if (command[0].equalsIgnoreCase("NEXT")) {
            selectNewArticle(conn, currA, currG, 1);
        } else if (command[0].equalsIgnoreCase("PREV")) {
            selectNewArticle(conn, currA, currG, -1);
        } else {
            conn.println("500 internal server error");
        }
    }

    private void selectNewArticle(NNTPConnection conn, Article article,
            Group grp, final int delta) throws IOException,
            StorageBackendException {
        assert article != null;

        article = grp.getArticle(grp.getIndexOf(article) + delta);

        if (article == null) {
            conn.println("421 no next article in this group");
        } else {
            conn.setCurrentArticle(article);
            conn.println("223 " + conn.getCurrentGroup().getIndexOf(article)
                    + " " + article.getMessageID()
                    + " article retrieved - request text separately");
        }
    }
}
