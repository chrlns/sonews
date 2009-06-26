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
import org.sonews.util.StringTemplate;

/**
 * Views the group settings and allows editing.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class SonewsGroupServlet extends AbstractSonewsServlet
{

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    synchronized(this)
    {
      connectToNewsserver();
      String name   = req.getParameter("name");
      String action = req.getParameter("action");

      if("set_flags".equals(action))
      {

      }
      else if("set_mladdress".equals(action))
      {
        
      }

      StringTemplate tmpl = getTemplate("SonewsGroupServlet.tmpl");
      tmpl.set("SERVERNAME", hello.split(" ")[2]);
      tmpl.set("TITLE", "Group " + name);
      tmpl.set("GROUPNAME", name);

      resp.getWriter().println(tmpl.toString());
      resp.getWriter().flush();
      resp.setStatus(HttpServletResponse.SC_OK);

      disconnectFromNewsserver();
    }
  }
  
}
