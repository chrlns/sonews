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

package org.sonews.util;

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

    private static Log instance = new Log();

    private Log() {
        super("org.sonews", null);

        StreamHandler handler = new StreamHandler(System.out,
                new SimpleFormatter());
        Level level = Level.parse(Config.inst().get(Config.LOGLEVEL, "INFO"));
        handler.setLevel(level);
        addHandler(handler);
        setLevel(level);
        LogManager.getLogManager().addLogger(this);
    }

    public static Logger get() {
        Level level = Level.parse(Config.inst().get(Config.LOGLEVEL, "INFO"));
        instance.setLevel(level);
        return instance;
    }
}
