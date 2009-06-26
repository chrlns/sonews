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
import java.nio.charset.Charset;
import java.sql.SQLException;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.storage.Article;
import org.sonews.daemon.storage.Group;

/**
 * Base class for all command handling classes.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public abstract class AbstractCommand
{

  protected NNTPConnection connection;

  public AbstractCommand(final NNTPConnection connection)
  {
    this.connection = connection;
  }

  protected Article getCurrentArticle()
  {
    return connection.getCurrentArticle();
  }

  protected Group getCurrentGroup()
  {
    return connection.getCurrentGroup();
  }

  protected void setCurrentArticle(final Article current)
  {
    connection.setCurrentArticle(current);
  }

  protected void setCurrentGroup(final Group current)
  {
    connection.setCurrentGroup(current);
  }
  
  public abstract void processLine(String line)
    throws IOException, SQLException;
  
  protected void println(final String line)
    throws IOException
  {
    connection.println(line);
  }
  
  protected void println(final String line, final Charset charset)
    throws IOException
  {
    connection.println(line, charset);
  }
  
  protected void printStatus(final int status, final String msg)
    throws IOException
  {
    println(status + " " + msg);
  }
  
  public abstract boolean hasFinished();

}
