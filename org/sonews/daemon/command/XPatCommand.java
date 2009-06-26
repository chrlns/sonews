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
import java.sql.SQLException;
import org.sonews.daemon.NNTPConnection;

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
 * </pre>
 * [Source:"draft-ietf-nntp-imp-02.txt"] [Copyright: 1998 S. Barber]
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class XPatCommand extends AbstractCommand
{

  public XPatCommand(final NNTPConnection conn)
  {
    super(conn);
  }
  
  @Override
  public boolean hasFinished()
  {
    return true;
  }

  @Override
  public void processLine(final String line) 
    throws IOException, SQLException
  {
    printStatus(500, "not (yet) supported");
  }

}
