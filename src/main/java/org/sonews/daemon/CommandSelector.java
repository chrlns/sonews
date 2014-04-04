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

package org.sonews.daemon;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sonews.daemon.command.Command;
import org.sonews.daemon.command.UnsupportedCommand;
import org.sonews.util.Log;
import org.sonews.util.io.Resource;

/**
 * Selects the correct command processing class.
 * 
 * @author Christian Lins
 * @since sonews/1.0
 */
public class CommandSelector {

    private static Map<Thread, CommandSelector> instances = new ConcurrentHashMap<>();
    private static Map<String, Class<?>> commandClassesMapping = new ConcurrentHashMap<>();

    static {
        String[] classes = Resource.getAsString("commands.list", true)
                .split("\n");
        for (String className : classes) {
            if (className.charAt(0) == '#') {
                // Skip comments
                continue;
            }

            try {
                addCommandHandler(className);
            } catch (ClassNotFoundException ex) {
                Log.get().log(Level.WARNING, "Could not load command class: {0}", ex);
            } catch (InstantiationException ex) {
                Log.get().log(Level.SEVERE, "Could not instantiate command class: {0}", ex);
            } catch (IllegalAccessException ex) {
                Log.get().log(Level.SEVERE, "Could not access command class: {0}", ex);
            }
        }
    }

    public static void addCommandHandler(String className)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Class<?> clazz = Class.forName(className);
        Command cmd = (Command) clazz.newInstance();
        String[] cmdStrs = cmd.getSupportedCommandStrings();
        for (String cmdStr : cmdStrs) {
            commandClassesMapping.put(cmdStr, clazz);
        }
    }

    public static Set<String> getCommandNames() {
        return commandClassesMapping.keySet();
    }

    public static CommandSelector getInstance() {
        CommandSelector csel = instances.get(Thread.currentThread());
        if (csel == null) {
            csel = new CommandSelector();
            instances.put(Thread.currentThread(), csel);
        }
        return csel;
    }

    private Map<String, Command> commandMapping = new HashMap<String, Command>();
    private Command unsupportedCmd = new UnsupportedCommand();

    private CommandSelector() {
    }

    public Command get(String commandName) {
        try {
            commandName = commandName.toUpperCase();
            Command cmd = this.commandMapping.get(commandName);

            if (cmd == null) {
                Class<?> clazz = commandClassesMapping.get(commandName);
                if (clazz == null) {
                    cmd = this.unsupportedCmd;
                } else {
                    cmd = (Command) clazz.newInstance();
                    this.commandMapping.put(commandName, cmd);
                }
            } else if (cmd.isStateful()) {
                cmd = cmd.getClass().newInstance();
            }

            return cmd;
        } catch (Exception ex) {
            ex.printStackTrace();
            return this.unsupportedCmd;
        }
    }
}
