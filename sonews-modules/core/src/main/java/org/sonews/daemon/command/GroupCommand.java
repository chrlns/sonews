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
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;

import org.springframework.stereotype.Component;

/**
 * Class handling the GROUP command.
 *
 * <pre>
 *  Syntax
 *    GROUP group
 *
 *  Responses
 *    211 number low high group     Group successfully selected
 *    411                           No such newsgroup
 *
 *  Parameters
 *    group     Name of newsgroup
 *    number    Estimated number of articles in the group
 *    low       Reported low water mark
 *    high      Reported high water mark
 * </pre>
 *
 * (from RFC 3977)
 *
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
@Component
public class GroupCommand implements Command {

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "GROUP" };
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
        return true;
    }

    @Override
    public void processLine(NNTPConnection conn, final String line, byte[] raw)
            throws IOException, StorageBackendException {
        final String[] command = line.split(" ");

        Group group;
        if (command.length >= 2) {
            group = Group.get(command[1]);
            if (group == null || group.isDeleted()) {
                conn.println("411 no such news group");
            } else {
                conn.setCurrentGroup(group);
                conn.println("211 " + group.getPostingsCount() + " "
                        + group.getFirstArticleNumber() + " "
                        + group.getLastArticleNumber() + " " + group.getName()
                        + " group selected");
            }
        } else {
            conn.println("500 no group name given");
        }
    }
}
