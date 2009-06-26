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

import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DSimple;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that creates chart images and returns them as raw PNG images.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class SonewsChartServlet extends AbstractSonewsServlet
{
  
  private ITrace2D createProcessMails24(String title, String cmd)
    throws IOException
  {
    int[] data = read24Values(cmd);
    ITrace2D trace = new Trace2DSimple(title);
    trace.addPoint(0.0, 0.0); // Start
    
    for(int n = 0; n < 24; n++)
    {
      trace.addPoint(n, data[n]);
    }

    return trace;
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    synchronized(this)
    {
      MemoryBitmapChart chart = new MemoryBitmapChart();

      String name  = req.getParameter("name");
      String group = req.getParameter("group");
      ITrace2D trace;
      String   cmd = "XDAEMON LOG";

      if(name.equals("feedednewsyesterday"))
      {
        cmd = cmd + " TRANSMITTED_NEWS";
        cmd = group != null ? cmd + " " + group : cmd;
        trace = createProcessMails24(
          "To peers transmitted mails yesterday", cmd);
      }
      else if(name.equals("gatewayednewsyesterday"))
      {
        cmd = cmd + " GATEWAYED_NEWS";
        cmd = group != null ? cmd + " " + group : cmd;
        trace = createProcessMails24(
          "Gatewayed mails yesterday", cmd);
      }
      else
      {
        cmd = cmd + " POSTED_NEWS";
        cmd = group != null ? cmd + " " + group : cmd;
        trace = createProcessMails24(
          "Posted mails yesterday", cmd);
      }
      chart.addTrace(trace);

      resp.getOutputStream().write(chart.getRawData(500, 400));
      resp.setContentType(chart.getContentType());
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }
  
  private int[] read24Values(String command)
    throws IOException
  {
    int[] values = new int[24];
    connectToNewsserver();
    printlnToNewsserver(command);
    String line = readlnFromNewsserver();
    if(!line.startsWith("200 "))
      throw new IOException(command + " not supported!");
    
    for(int n = 0; n < 24; n++)
    {
      line = readlnFromNewsserver();
      values[n] = Integer.parseInt(line.split(" ")[1]);
    }
    
    line = readlnFromNewsserver(); // "."
    
    disconnectFromNewsserver();
    return values;
  }
  
}
