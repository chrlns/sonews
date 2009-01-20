/*
 *   StarOffice News Server
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

package com.so.news;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * Provides logging and debugging methods.
 * @author Christian Lins
 */
public class Debug
{
  private static Debug instance = null;
  
  /**
   * Returns the singelton instance of this class.
   */
  public static Debug getInstance()
  {
    if(instance == null)
      instance = new Debug();
    
    return instance;
  }
  
  private PrintStream out = System.err;
  
  /**
   * This class is a singelton class. The constructor is private to prevent
   * the creation of more than one instance.
   */
  private Debug()
  {
    try
    {
      String filename = Config.getInstance().get(Config.CONFIG_N3TPD_LOGFILE, "n3tpd.log");
      
      this.out = new PrintStream(new FileOutputStream(filename));
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
  }
  
  /**
   * Returns the debug output PrintStream. By default this is System.err.
   */
  public PrintStream getStream()
  {
    return out;
  }
  
  /**
   * Writes the given message to the debug output.
   * @param msg A String message or an object.
   */
  public void log(Object msg)
  {
    log(out, msg);
    log(System.out, msg);
  }
  
  /**
   * Writes the given debug message to the given PrintStream.
   * @param out
   * @param msg
   */
  public void log(PrintStream out, Object msg)
  {
    out.print(new Date().toString());
    out.print(": ");
    out.println(msg.toString());
    out.flush();
  }
}
