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
import org.springframework.stereotype.Component;


/**
 * A default "Unsupported Command". Simply returns error code 500 and a
 * "command not supported" message.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
public class UnsupportedCommand implements Command {

    /**
     * @return Always returns null.
     */
    @Override
    public String[] getSupportedCommandStrings() {
        return new String[]{"*"};
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
            throws IOException {
        conn.println("500 command not supported");
    }
}
