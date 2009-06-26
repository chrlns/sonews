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

package org.sonews.web;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonews.daemon.Main;
import org.sonews.util.StringTemplate;

/**
 * Main sonews webpage servlet.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class SonewsServlet extends AbstractSonewsServlet
{

  private static final long serialVersionUID = 2392837459834L;

  @Override
  public void doGet(HttpServletRequest res, HttpServletResponse resp)
    throws IOException
  {
    synchronized(this)
    {
      connectToNewsserver();

      String line;
      int    connectedClients = 0;
      int    hostedGroups     = 0;
      int    hostedNews       = 0;

      printlnToNewsserver("XDAEMON LOG CONNECTED_CLIENTS");

      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      line = readlnFromNewsserver();
      connectedClients = Integer.parseInt(line);
      line = readlnFromNewsserver(); // Read the "."

      printlnToNewsserver("XDAEMON LOG HOSTED_NEWS");
      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      line = readlnFromNewsserver();
      hostedNews = Integer.parseInt(line);
      line = readlnFromNewsserver(); // read the "."

      printlnToNewsserver("XDAEMON LOG HOSTED_GROUPS");
      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      line = readlnFromNewsserver();
      hostedGroups = Integer.parseInt(line);
      line = readlnFromNewsserver(); // read the "."

      printlnToNewsserver("XDAEMON LOG POSTED_NEWS_PER_HOUR");
      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      String postedNewsPerHour = readlnFromNewsserver();
      readlnFromNewsserver();

      printlnToNewsserver("XDAEMON LOG GATEWAYED_NEWS_PER_HOUR");
      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      String gatewayedNewsPerHour = readlnFromNewsserver();
      line = readlnFromNewsserver();

      printlnToNewsserver("XDAEMON LOG FEEDED_NEWS_PER_HOUR");
      line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("XDAEMON command not allowed by server");
      }
      String feededNewsPerHour = readlnFromNewsserver();
      line = readlnFromNewsserver();

      StringTemplate tmpl = getTemplate("SonewsServlet.tmpl");
      tmpl.set("SERVERNAME", hello.split(" ")[2]);
      tmpl.set("STARTDATE", Main.STARTDATE);
      tmpl.set("ACTIVE_CONNECTIONS", connectedClients);
      tmpl.set("STORED_NEWS", hostedNews);
      tmpl.set("SERVED_NEWSGROUPS", hostedGroups);
      tmpl.set("POSTED_NEWS", postedNewsPerHour);
      tmpl.set("GATEWAYED_NEWS", gatewayedNewsPerHour);
      tmpl.set("FEEDED_NEWS", feededNewsPerHour);
      tmpl.set("TITLE", "Overview");

      resp.getWriter().println(tmpl.toString());
      resp.getWriter().flush();
      resp.setStatus(HttpServletResponse.SC_OK);

      disconnectFromNewsserver();
    }
  }
  
}
