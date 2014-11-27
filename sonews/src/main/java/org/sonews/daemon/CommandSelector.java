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
import java.util.concurrent.ConcurrentHashMap;
import org.sonews.Application;

import org.sonews.daemon.command.Command;
import org.sonews.daemon.command.UnsupportedCommand;
import org.sonews.util.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Selects the correct command processing class.
 *
 * @author Christian Lins
 * @since sonews/1.0
 */
public class CommandSelector {

    private static Map<Thread, CommandSelector> instances = new ConcurrentHashMap<>();

    public static CommandSelector getInstance() {
        CommandSelector csel = instances.get(Thread.currentThread());
        if (csel == null) {
            csel = new CommandSelector();
            instances.put(Thread.currentThread(), csel);
        }
        return csel;
    }

    private final Map<String, Command> commandMapping = new HashMap<>();

    public CommandSelector() {
        ApplicationContext context = new AnnotationConfigApplicationContext(Application.class);
        Map<String, Command> commands = context.getBeansOfType(Command.class);
        
        for(Command command : commands.values()) {
            String[] cmdStrings = command.getSupportedCommandStrings();
            for(String cmdString : cmdStrings) {
                Log.get().info("Command " + cmdString + " processed with " + command.getClass());
                commandMapping.put(cmdString, command);
            }
        }
    }

    public Command get(String commandName) {
        Command cmd = commandMapping.get(commandName);
        if (cmd == null) {
            cmd = commandMapping.get("*");
        }
        return cmd;
    }
}
