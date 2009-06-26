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

package org.sonews.daemon.command;

import java.io.IOException;
import org.sonews.daemon.NNTPConnection;
import org.sonews.util.io.Resource;

/**
 * This command provides a short summary of the commands that are
 * understood by this implementation of the server.  The help text will
 * be presented as a multi-line data block following the 100 response
 * code (taken from RFC).
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class HelpCommand extends AbstractCommand
{
  
  public HelpCommand(final NNTPConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean hasFinished()
  {
    return true;
  }
  
  @Override
  public void processLine(final String line)
    throws IOException
  {
    printStatus(100, "help text follows");
    
    final String[] help = Resource
      .getAsString("helpers/helptext", true).split("\n");
    for(String hstr : help)
    {
      println(hstr);
    }
    
    println(".");
  }
  
}
