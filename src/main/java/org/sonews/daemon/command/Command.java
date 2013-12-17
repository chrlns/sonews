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
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.StorageBackendException;

/**
 * Interface for pluggable NNTP commands handling classes.
 * 
 * @author Christian Lins
 * @since sonews/0.6.0
 */
public interface Command {

    /**
     * @return true if this instance can be reused.
     */
    boolean hasFinished();

    /**
     * Returns capability string that is implied by this command class. MAY
     * return null if the command is required by the NNTP standard.
     */
    String impliedCapability();

    boolean isStateful();

    String[] getSupportedCommandStrings();

    void processLine(NNTPConnection conn, String line, byte[] rawLine)
            throws IOException, StorageBackendException;
}
