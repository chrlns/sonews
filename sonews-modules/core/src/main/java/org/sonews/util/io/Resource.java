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

package org.sonews.util.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.sonews.util.Log;

/**
 * Provides method for loading of resources.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Resource {
   
    public static Stream<String> getLines(String resource) {
        Stream<String> lines = null;
        
        try {
            Path path = Paths.get(Resource.class.getResource(resource).toURI());
            lines = Files.lines(path);
        } catch(IOException | URISyntaxException ex) {
            Log.get().warning(ex.getMessage());
            Log.get().log(Level.ALL, ex.getMessage(), ex);
        }
        
        return lines;
    }
}
