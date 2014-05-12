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
package org.sonews.acl;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonews.daemon.NNTPConnection;

import org.sonews.daemon.command.Command;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;

/**
 *
 * @author František Kučera (frantovo.cz)
 */
public class AuthInfoCommand implements Command {

    private static final Logger log = Logger.getLogger(AuthInfoCommand.class
            .getName());
    private static final String[] SUPPORTED_COMMANDS = { "AUTHINFO" };

    @Override
    public boolean hasFinished() {
        return true;
    }

    @Override
    public String impliedCapability() {
        return "AUTHINFO";
    }

    @Override
    public boolean isStateful() {
        // TODO: make it statefull?
        return false;
    }

    @Override
    public String[] getSupportedCommandStrings() {
        return SUPPORTED_COMMANDS;
    }

    @Override
    public void processLine(NNTPConnection conn, String line, byte[] rawLine)
            throws IOException, StorageBackendException {
        Pattern commandPattern = Pattern.compile("AUTHINFO (USER|PASS) (.*)",
                Pattern.CASE_INSENSITIVE);
        Matcher commandMatcher = commandPattern.matcher(line);

        if (commandMatcher.matches()) {

            if (conn.getUser() != null && conn.getUser().isAuthenticated()) {
                conn.println("502 Command unavailable (you are already authenticated)");
            } else if ("USER".equalsIgnoreCase(commandMatcher.group(1))) {
                conn.setUser(new User(commandMatcher.group(2)));
                conn.println("381 Password required"); // ask user for his
                                                       // password
                log.log(Level.FINE,
                        "User ''{0}'' greets us. We are waiting for his password.",
                        conn.getUser().getUserName());
            } else if ("PASS".equalsIgnoreCase(commandMatcher.group(1))) {
                if (conn.getUser() == null) {
                    conn.println("482 Authentication commands issued out of sequence");
                } else {

                    char[] password = commandMatcher.group(2).toCharArray();
                    // TODO: StorageManager should return User object instead of
                    // boolean (so there could be transferred some additional
                    // information about user)
                    boolean goodPassword = StorageManager.current()
                            .authenticateUser(conn.getUser().getUserName(),
                                    password);
                    Arrays.fill(password, '*');

                    if (goodPassword) {
                        conn.println("281 Authentication accepted");
                        conn.getUser().setAuthenticated(true);
                        log.log(Level.INFO,
                                "User ''{0}'' has been succesfully authenticated.",
                                conn.getUser().getUserName());
                    } else {
                        log.log(Level.INFO,
                                "User ''{0}'' has provided wrong password.",
                                conn.getUser().getUserName());
                        conn.setUser(null);
                        conn.println("481 Authentication failed: wrong password");
                    }

                }
            } else {
                // impossible, see commandPattern
                conn.println("500 Unknown command");
            }

        } else {
            conn.println("500 Unknown command, expecting AUTHINFO USER username or AUTHINFO PASS password ");
        }
    }
}
