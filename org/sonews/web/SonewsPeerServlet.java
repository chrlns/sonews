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
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonews.util.StringTemplate;

/**
 * Servlet that shows the Peers and the Peering Rules.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class SonewsPeerServlet extends AbstractSonewsServlet
{

  private static final long serialVersionUID = 245345346356L;
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    synchronized(this)
    {
      connectToNewsserver();
      StringTemplate tmpl = getTemplate("SonewsPeerServlet.tmpl");

      // Read peering rules from newsserver
      printlnToNewsserver("XDAEMON LIST PEERINGRULES");
      String line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        throw new IOException("Unexpected reply: " + line);
      }

      // Create FEED_RULES String
      HashSet<String> peers        = new HashSet<String>();
      StringBuilder   feedRulesStr = new StringBuilder();
      for(;;)
      {
        line = readlnFromNewsserver();
        if(line.equals("."))
        {
          break;
        }
        else
        {
          feedRulesStr.append(line);
          feedRulesStr.append("<br/>");

          String[] lineChunks = line.split(" ");
          peers.add(lineChunks[1]);
        }
      }

      // Create PEERS string
      StringBuilder peersStr = new StringBuilder();
      for(String peer : peers)
      {
        peersStr.append(peer);
        peersStr.append("<br/>");
      }

      // Set server name
      tmpl.set("PEERS", peersStr.toString());
      tmpl.set("PEERING_RULES", feedRulesStr.toString());
      tmpl.set("SERVERNAME", hello.split(" ")[2]);
      tmpl.set("TITLE", "Peers");

      resp.getWriter().println(tmpl.toString());
      resp.getWriter().flush();
      resp.setStatus(HttpServletResponse.SC_OK);
      disconnectFromNewsserver();
    }
  }
  
}
