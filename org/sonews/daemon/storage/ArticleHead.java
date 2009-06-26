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

package org.sonews.daemon.storage;

import java.io.ByteArrayInputStream;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

/**
 * An article with no body only headers.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleHead 
{

  protected InternetHeaders headers;
  
  protected ArticleHead()
  {
  }
  
  public ArticleHead(String headers)
  {
    try
    {
      // Parse the header
      this.headers = new InternetHeaders(
          new ByteArrayInputStream(headers.getBytes()));
    }
    catch(MessagingException ex)
    {
      ex.printStackTrace();
    }
  }
  
  /**
   * Returns the header field with given name.
   * @param name
   * @return Header values or empty string.
   */
  public String[] getHeader(String name)
  {
    String[] ret = this.headers.getHeader(name);
    if(ret == null)
    {
      ret = new String[]{""};
    }
    return ret;
  }
  
  /**
   * Sets the header value identified through the header name.
   * @param name
   * @param value
   */
  public void setHeader(String name, String value)
  {
    this.headers.setHeader(name, value);
  }
  
}
