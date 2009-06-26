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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.http.HttpServlet;
import org.sonews.util.StringTemplate;
import org.sonews.util.io.Resource;

/**
 * Base class for all sonews servlets.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class AbstractSonewsServlet extends HttpServlet
{

  public static final String TemplateRoot = "org/sonews/web/tmpl/";
  
  protected String        hello = null;
  
  private BufferedReader  in     = null;
  private PrintWriter     out    = null;
  private Socket          socket = null;
  
  protected void connectToNewsserver()
    throws IOException
  {
    // Get sonews port from properties
    String port = System.getProperty("sonews.port", "9119");
    String host = System.getProperty("sonews.host", "localhost");
    
    try
    {
      this.socket = new Socket(host, Integer.parseInt(port));

      this.in     = new BufferedReader(
        new InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream());

      hello = in.readLine(); // Read hello message
    }
    catch(IOException ex)
    {
      System.out.println("sonews.host=" + host);
      System.out.println("sonews.port=" + port);
      System.out.flush();
      throw ex;
    }
  }
  
  protected void disconnectFromNewsserver()
  {
    try
    {
      printlnToNewsserver("QUIT");
      out.close();
      readlnFromNewsserver(); // Wait for bye message
      in.close();
      socket.close();
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }
  }
  
  protected StringTemplate getTemplate(String res)
  {
    StringTemplate tmpl = new StringTemplate(
      Resource.getAsString(TemplateRoot + "AbstractSonewsServlet.tmpl", true));
    String content    = Resource.getAsString(TemplateRoot + res, true);
    String stylesheet = System.getProperty("sonews.web.stylesheet", "style.css");
    
    tmpl.set("CONTENT", content);
    tmpl.set("STYLESHEET", stylesheet);
    
    return new StringTemplate(tmpl.toString());
  }
  
  protected void printlnToNewsserver(final String line)
  {
    this.out.println(line);
    this.out.flush();
  }
  
  protected String readlnFromNewsserver()
    throws IOException
  {
    return this.in.readLine();
  }
  
}
