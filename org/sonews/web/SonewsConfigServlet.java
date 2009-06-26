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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonews.util.StringTemplate;
import org.sonews.util.io.Resource;

/**
 * Servlet providing a configuration web interface.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class SonewsConfigServlet extends AbstractSonewsServlet
{
  
  private static final long serialVersionUID = 2432543253L;
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    synchronized(this)
    {
      connectToNewsserver();
      String which = req.getParameter("which");

      if(which != null && which.equals("config"))
      {
        whichConfig(req, resp);
      }
      else if(which != null && which.equals("groupadd"))
      {
        whichGroupAdd(req, resp);
      }
      else if(which != null && which.equals("groupdelete"))
      {
        whichGroupDelete(req, resp);
      }
      else
      {
        whichNone(req, resp);
      }

      disconnectFromNewsserver();
    }
  }
  
  private void whichConfig(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    StringBuilder keys = new StringBuilder();

    Set pnames = req.getParameterMap().keySet();
    for(Object obj : pnames)
    {
      String pname = (String)obj;
      if(pname.startsWith("configkey:"))
      {
        String value = req.getParameter(pname);
        String key   = pname.split(":")[1];
        if(!value.equals("<not set>"))
        {
          printlnToNewsserver("XDAEMON SET " + key + " " + value);
          readlnFromNewsserver();
          
          keys.append(key); 
          keys.append("<br/>");
        }
      }
    }
    
    StringTemplate tmpl = getTemplate("ConfigUpdated.tmpl");
    
    tmpl.set("UPDATED_KEYS", keys.toString());
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getWriter().println(tmpl.toString());
    resp.getWriter().flush();
  }
  
  private void whichGroupAdd(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    String[] groupnames = req.getParameter("groups").split("\n");
    
    for(String groupname : groupnames)
    {
      groupname = groupname.trim();
      if(groupname.equals(""))
      {
        continue;
      }

      printlnToNewsserver("XDAEMON GROUPADD " + groupname + " 0");
      String line = readlnFromNewsserver();
      if(!line.startsWith("200 "))
      {
        System.out.println("Warning " + groupname + " probably not created!");
      }
    }
    
    StringTemplate tmpl = getTemplate("GroupAdded.tmpl");
    
    tmpl.set("GROUP", req.getParameter("groups"));
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getWriter().println(tmpl.toString());
    resp.getWriter().flush();
  }
  
  private void whichGroupDelete(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    String groupname = req.getParameter("group");
    printlnToNewsserver("XDAEMON GROUPDEL " + groupname);
    String line = readlnFromNewsserver();
    if(!line.startsWith("200 "))
      throw new IOException(line);
    
    StringTemplate tmpl = getTemplate("GroupDeleted.tmpl");
    
    tmpl.set("GROUP", groupname);
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getWriter().println(tmpl.toString());
    resp.getWriter().flush();
  }
  
  private void whichNone(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    StringTemplate tmpl = getTemplate("SonewsConfigServlet.tmpl");
    
    // Retrieve config keys from server
    List<String> configKeys = new ArrayList<String>();
    printlnToNewsserver("XDAEMON LIST CONFIGKEYS");
    String line = readlnFromNewsserver();
    if(!line.startsWith("200 "))
      throw new IOException("XDAEMON command not supported!");
    for(;;)
    {
      line = readlnFromNewsserver();
      if(line.equals("."))
        break;
      else
        configKeys.add(line);
    }
    
    // Construct config table
    StringBuilder strb = new StringBuilder();
    for(String key : configKeys)
    {
      strb.append("<tr><td><code>");
      strb.append(key);
      strb.append("</code></td><td>");
      
      // Retrieve config value from server
      String value = "<not set>";
      printlnToNewsserver("XDAEMON GET " + key);
      line = readlnFromNewsserver();
      if(line.startsWith("200 "))
      {
        value = readlnFromNewsserver();
        readlnFromNewsserver(); // Read the "."
      }
      
      strb.append("<input type=text name=\"configkey:");
      strb.append(key);
      strb.append("\" value=\"");
      strb.append(value);
      strb.append("\"/></td></tr>");
    }
    tmpl.set("CONFIG", strb.toString());
    
    // Retrieve served newsgroup names from server
    List<String> groups = new ArrayList<String>();
    printlnToNewsserver("LIST");
    line = readlnFromNewsserver();
    if(line.startsWith("215 "))
    {
      for(;;)
      {
        line = readlnFromNewsserver();
        if(line.equals("."))
        {
          break;
        }
        else
        {
          groups.add(line.split(" ")[0]);
        }
      }
    }
    else
      throw new IOException("Error issuing LIST command!");
    
    // Construct groups list
    StringTemplate tmplGroupList = new StringTemplate(
      Resource.getAsString("org/sonews/web/tmpl/GroupList.tmpl", true));
    strb = new StringBuilder();
    for(String group : groups)
    {
      tmplGroupList.set("GROUPNAME", group);
      strb.append(tmplGroupList.toString());
    }
    tmpl.set("GROUP", strb.toString());
    
    // Set server name
    tmpl.set("SERVERNAME", hello.split(" ")[2]);
    tmpl.set("TITLE", "Configuration");
    
    resp.getWriter().println(tmpl.toString());
    resp.getWriter().flush();
    resp.setStatus(HttpServletResponse.SC_OK);
  }

}
