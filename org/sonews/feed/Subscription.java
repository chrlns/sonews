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

package org.sonews.feed;

/**
 * For every group that is synchronized with or from a remote newsserver 
 * a Subscription instance exists.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Subscription 
{

  private String host;
  private int    port;
  private int    feedtype;
  private String group;
  
  public Subscription(String host, int port, int feedtype, String group)
  {
    this.host     = host;
    this.port     = port;
    this.feedtype = feedtype;
    this.group    = group;
  }

  public int getFeedtype()
  {
    return feedtype;
  }

  public String getGroup()
  {
    return group;
  }

  public String getHost()
  {
    return host;
  }

  public int getPort()
  {
    return port;
  }
  
}
