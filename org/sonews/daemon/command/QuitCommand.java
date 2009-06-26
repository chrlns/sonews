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
import java.sql.SQLException;
import org.sonews.daemon.NNTPConnection;

/**
 * Implementation of the QUIT command; client wants to shutdown the connection.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class QuitCommand extends AbstractCommand
{

  public QuitCommand(final NNTPConnection conn)
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
    throws IOException, SQLException
  {    
    printStatus(205, "cya");
    
    this.connection.shutdownInput();
    this.connection.shutdownOutput();
  }

}
