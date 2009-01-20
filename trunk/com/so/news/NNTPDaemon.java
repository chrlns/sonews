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

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Server component of the n3tpd.
 * @author Christian Lins
 * @author Dennis Schwerdel
 */
public class NNTPDaemon extends Thread
{
  private ServerSocket socket;

  public NNTPDaemon(boolean aux) throws IOException
  {
    int port; 
    if(!aux)
      port = Config.getInstance().get("n3tpd.port", 119);
    else
      port = Config.getInstance().get("n3tpd.auxport", 8080);
    
    int backlog = Config.getInstance().get("n3tpd.server.backlog", 10);
    
    // Create and bind the socket
    socket = new ServerSocket(port, backlog);
  }

  @Override
  public void run()
  {
    System.out.println("Daemon listening on port " + socket.getLocalPort() + " ...");
    
    while(isAlive() && !isInterrupted())
    {
      try
      {
        new NNTPConnection(socket.accept()).start();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
