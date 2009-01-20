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

package com.so.news.command;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.so.news.Debug;
import com.so.news.NNTPConnection;
import com.so.news.storage.Article;

/**
 * Class handling the OVER/XOVER command.
 * 
 * Description of the XOVER command:
 * 
 * XOVER [range]
 *
 * The XOVER command returns information from the overview
 * database for the article(s) specified.
 *
 * The optional range argument may be any of the following:
 *              an article number
 *              an article number followed by a dash to indicate
 *                 all following
 *              an article number followed by a dash followed by
 *                 another article number
 *
 * If no argument is specified, then information from the
 * current article is displayed. Successful responses start
 * with a 224 response followed by the overview information
 * for all matched messages. Once the output is complete, a
 * period is sent on a line by itself. If no argument is
 * specified, the information for the current article is
 * returned.  A news group must have been selected earlier,
 * else a 412 error response is returned. If no articles are
 * in the range specified, a 420 error response is returned
 * by the server. A 502 response will be returned if the
 * client only has permission to transfer articles.
 *
 * Each line of output will be formatted with the article number,
 * followed by each of the headers in the overview database or the
 * article itself (when the data is not available in the overview
 * database) for that article separated by a tab character.  The
 * sequence of fields must be in this order: subject, author,
 * date, message-id, references, byte count, and line count. Other
 * optional fields may follow line count. Other optional fields may
 * follow line count. These fields are specified by examining the
 * response to the LIST OVERVIEW.FMT command. Where no data exists,
 * a null field must be provided (i.e. the output will have two tab
 * characters adjacent to each other). Servers should not output
 * fields for articles that have been removed since the XOVER database
 * was created.
 *
 * The LIST OVERVIEW.FMT command should be implemented if XOVER
 * is implemented. A client can use LIST OVERVIEW.FMT to determine
 * what optional fields  and in which order all fields will be
 * supplied by the XOVER command. 
 *
 * Note that any tab and end-of-line characters in any header
 * data that is returned will be converted to a space character.
 *
 * Responses:
 *
 *   224 Overview information follows
 *   412 No news group current selected
 *   420 No article(s) selected
 *   502 no permission
 * 
 * @author Christian Lins
 */
public class OverCommand extends Command
{
  public OverCommand(NNTPConnection conn)
  {
    super(conn);
  }
  
  public boolean process(String[] command)
    throws Exception
  {
    if(getCurrentGroup() == null)
    {
      printStatus(412, "No news group current selected");
      return false;
    }
    
    // If no parameter was specified, show information about
    // the currently selected article(s)
    if(command.length == 1)
    {
      Article art = getCurrentArticle();
      if(art == null)
      {
        printStatus(420, "No article(s) selected");
        return false;
      }
      
      String o = buildOverview(art, -1);
      printText(o);
    }
    // otherwise print information about the specified range
    else
    {
      int artStart = -1;
      int artEnd   = -1;
      String[] nums = command[1].split("-");
      if(nums.length > 1)
      {
        try
        {
          artStart = Integer.parseInt(nums[0]);
        }
        catch(Exception e) 
        {
          artStart = Integer.parseInt(command[1]);
        }
        try
        {
          artEnd = Integer.parseInt(nums[1]);
        }
        catch(Exception e) {}
      }

      printStatus(224, "Overview information follows");
      for(int n = artStart; n <= artEnd; n++)
      {
        Article art = Article.getByNumberInGroup(getCurrentGroup(), n);
        if(art == null)
        {
          Debug.getInstance().log("Article (gid=" + getCurrentGroup() + ", art=" + n + " is null!");
        }
        else
        {
          printTextPart(buildOverview(art, n) + NEWLINE);
        }
      }
      println(".");
      flush();
    }
    
    return true;
  }
  
  private String buildOverview(Article art, int nr)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    StringBuilder overview = new StringBuilder();
    overview.append(nr);
    overview.append('\t');
    overview.append(art.getHeader().get("Subject"));
    overview.append('\t');
    overview.append(art.getHeader().get("From"));
    overview.append('\t');
    overview.append(sdf.format(art.getDate()));
    overview.append('\t');
    overview.append(art.getHeader().get("Message-ID"));
    overview.append('\t');
    overview.append(art.getHeader().get("References"));
    overview.append('\t');
    overview.append(art.getHeader().get("Bytes"));
    overview.append('\t');
    overview.append(art.getHeader().get("Lines"));
    
    return overview.toString();
  }
}
