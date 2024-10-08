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

import java.io.IOException;

import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.StorageBackendException;

import org.springframework.stereotype.Component;

/**
 * Implementation of the QUIT command; client wants to shutdown the connection.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
public class QuitCommand implements Command {

    @Override
    public String[] getSupportedCommandStrings() {
        return new String[] { "QUIT" };
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
        conn.println("205 bye");
        conn.close();
    }
}
