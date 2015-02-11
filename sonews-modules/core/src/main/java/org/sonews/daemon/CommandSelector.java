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

package org.sonews.daemon;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.sonews.daemon.command.Command;
import org.sonews.util.Log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Selects the correct command processing class.
 *
 * @author Christian Lins
 * @since sonews/1.0
 */
@Component
public class CommandSelector {

    private final Map<String, Command> commandMapping = new HashMap<>();

    @Autowired
    private ApplicationContext context;

    public CommandSelector() {
    }

    @PostConstruct
    protected void init() {
        Map<String, Command> commands = context.getBeansOfType(Command.class);

        for (Command command : commands.values()) {
            String[] cmdStrings = command.getSupportedCommandStrings();
            for (String cmdString : cmdStrings) {
                Log.get().log(Level.INFO, "Command {0} processed with {1}", new Object[]{cmdString, command.getClass()});
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
