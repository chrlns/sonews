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

import java.util.Date;
import org.sonews.config.Config;

/**
 * Provides logging and debugging methods.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Log
{
  
  public static boolean isDebug()
  {
    // We must use FileConfig here otherwise we come
    // into hell's kittchen when using the Logger within the
    // Database class.
    return Config.inst().get(Config.DEBUG, false);
  }
  
  /**
   * Writes the given message to the debug output.
   * @param msg A String message or an object.
   * @param If true this message is only shown if debug mode is enabled.
   */
  public static void msg(final Object msg, boolean debug)
  {
    if(isDebug() || !debug)
    {
      synchronized(System.out)
      {
        System.out.print(new Date().toString());
        System.out.print(": ");
        System.out.println(msg);
        System.out.flush();
      }
    }
  }

}
