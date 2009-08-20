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
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.Channel;
import org.sonews.storage.StorageBackendException;

/**
 * Class handling the LIST command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ListCommand implements Command
{

  @Override
  public String[] getSupportedCommandStrings()
  {
    return new String[]{"LIST"};
  }

  @Override
  public boolean hasFinished()
  {
    return true;
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
    final String[] command = line.split(" ");
    
    if(command.length >= 2)
    {
      if(command[1].equalsIgnoreCase("OVERVIEW.FMT"))
      {
        conn.println("215 information follows");
        conn.println("Subject:\nFrom:\nDate:\nMessage-ID:\nReferences:\nBytes:\nLines:\nXref");
        conn.println(".");
      }
      else if(command[1].equalsIgnoreCase("NEWSGROUPS"))
      {
        conn.println("215 information follows");
        final List<Channel> list = Channel.getAll();
        for (Channel g : list)
        {
          conn.println(g.getName() + "\t" + "-");
        }
        conn.println(".");
      }
      else if(command[1].equalsIgnoreCase("SUBSCRIPTIONS"))
      {
        conn.println("215 information follows");
        conn.println(".");
      }
      else if(command[1].equalsIgnoreCase("EXTENSIONS"))
      {
        conn.println("202 Supported NNTP extensions.");
        conn.println("LISTGROUP");
        conn.println("XDAEMON");
        conn.println("XPAT");
        conn.println(".");
      }
      else if(command[1].equalsIgnoreCase("ACTIVE"))
      {
        // TODO: Implement wildcards for LIST ACTIVE
        printGroupInfo(conn);
      }
      else
      {
        conn.println("500 unknown argument to LIST command");
      }
    }
    else
    {
      printGroupInfo(conn);
    }
  }

  private void printGroupInfo(NNTPConnection conn)
    throws IOException, StorageBackendException
  {
    final List<Channel> groups = Channel.getAll();
    if (groups != null)
    {
      conn.println("215 list of newsgroups follows");
      for (Channel g : groups)
      {
        if (!g.isDeleted())
        {
          String writeable = g.isWriteable() ? " y" : " n";
          // Indeed first the higher article number then the lower
          conn.println(g.getName() + " " + g.getLastArticleNumber() + " " + g.getFirstArticleNumber() + writeable);
        }
      }
      conn.println(".");
    }
    else
    {
      conn.println("500 server database malfunction");
    }
  }

}
