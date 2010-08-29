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
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Pair;

/**
 * <pre>
 *   XPAT header range|<message-id> pat [pat...]
 *
 *   The XPAT command is used to retrieve specific headers from
 *   specific articles, based on pattern matching on the contents of
 *   the header. This command was first available in INN.
 *
 *   The required header parameter is the name of a header line (e.g.
 *   "subject") in a news group article. See RFC-1036 for a list
 *   of valid header lines. The required range argument may be
 *   any of the following:
 *               an article number
 *               an article number followed by a dash to indicate
 *                  all following
 *               an article number followed by a dash followed by
 *                  another article number
 *
 *   The required message-id argument indicates a specific
 *   article. The range and message-id arguments are mutually
 *   exclusive. At least one pattern in wildmat must be specified
 *   as well. If there are additional arguments the are joined
 *   together separated by a single space to form one complete
 *   pattern. Successful responses start with a 221 response
 *   followed by a the headers from all messages in which the
 *   pattern matched the contents of the specified header line. This
 *   includes an empty list. Once the output is complete, a period
 *   is sent on a line by itself. If the optional argument is a
 *   message-id and no such article exists, the 430 error response
 *   is returned. A 502 response will be returned if the client only
 *   has permission to transfer articles.
 *
 *   Responses
 *
 *       221 Header follows
 *       430 no such article
 *       502 no permission
 *
 *   Response Data:
 *
 *       art_nr fitting_header_value
 * 
 * </pre>
 * [Source:"draft-ietf-nntp-imp-02.txt"] [Copyright: 1998 S. Barber]
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class XPatCommand implements Command
{

  @Override
  public String[] getSupportedCommandStrings()
  {
    return new String[]{"XPAT"};
  }
  
  @Override
  public boolean hasFinished()
  {
    return true;
  }

  @Override
  public String impliedCapability()
  {
    return null;
  }

  @Override
  public boolean isStateful()
  {
    return false;
  }

  @Override
  public void processLine(NNTPConnection conn, final String line, byte[] raw)
    throws IOException, StorageBackendException
  {
    if(conn.getCurrentChannel() == null)
    {
      conn.println("430 no group selected");
      return;
    }

    String[] command = line.split("\\p{Space}+");

    // There may be multiple patterns and Thunderbird produces
    // additional spaces between range and pattern
    if(command.length >= 4)
    {
      String header  = command[1].toLowerCase(Locale.US);
      String range   = command[2];
      String pattern = command[3];

      long start = -1;
      long end   = -1;
      if(range.contains("-"))
      {
        String[] rsplit = range.split("-", 2);
        start = Long.parseLong(rsplit[0]);
        if(rsplit[1].length() > 0)
        {
          end = Long.parseLong(rsplit[1]);
        }
      }
      else // TODO: Handle Message-IDs
      {
        start = Long.parseLong(range);
      }

      try
      {
        List<Pair<Long, String>> heads = StorageManager.current().
          getArticleHeaders(conn.getCurrentChannel(), start, end, header, pattern);
        
        conn.println("221 header follows");
        for(Pair<Long, String> head : heads)
        {
          conn.println(head.getA() + " " + head.getB());
        }
        conn.println(".");
      }
      catch(PatternSyntaxException ex)
      {
        ex.printStackTrace();
        conn.println("500 invalid pattern syntax");
      }
      catch(StorageBackendException ex)
      {
        ex.printStackTrace();
        conn.println("500 internal server error");
      }
    }
    else
    {
      conn.println("430 invalid command usage");
    }
  }

}
