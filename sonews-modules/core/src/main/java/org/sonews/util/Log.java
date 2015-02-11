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

package org.sonews.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.sonews.config.Config;

/**
 * Provides logging and debugging methods.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Log extends Logger {

    private static final Log instance = new Log();

    private Log() {
        super("org.sonews", null);

        StreamHandler sHandler = new StreamHandler(System.out,
                new SimpleFormatter());
        addHandler(sHandler);

        Level level = Level.parse(Config.inst().get(Config.LOGLEVEL, "INFO"));
        setLevel(level);
        for (Handler handler : getHandlers()) {
            handler.setLevel(level);
        }

        LogManager.getLogManager().addLogger(this);
    }

    public static Logger get() {
        return instance;
    }
}
