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

package org.sonews.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.sonews.config.Config;

/**
 * Keeps the reference to a java.util.logging.Logger instance named "org.sonews".
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Log extends Logger {

    private static Log instance = null;

    public Log() {
        super("org.sonews", null);

        SimpleFormatter formatter = new SimpleFormatter();
        StreamHandler streamHandler = new StreamHandler(System.out, formatter);
        
        addHandler(streamHandler);

        Level level = Level.parse(Config.inst().get(Config.LOGLEVEL, "INFO"));
        System.out.println("Log level: " + level);
        setLevel(level);
        for (Handler handler : getHandlers()) {
            handler.setLevel(level);
        }
    }

    public synchronized static Logger get() {
        if (instance == null) {
            // We keep a strong reference to our logger, because LogManager
            // only keeps a weak reference that may be garbage collected
            instance = new Log();
            if (!LogManager.getLogManager().addLogger(instance)) {
                // Should not happen
                System.err.println("Failed to register logger.");
            }
        }
        return instance;
    }
}
