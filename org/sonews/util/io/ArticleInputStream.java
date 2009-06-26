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

package org.sonews.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.sonews.daemon.storage.*;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Capsulates an Article to provide a raw InputStream.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleInputStream extends InputStream
{

  private byte[] buffer;
  private int    offset = 0;
  
  public ArticleInputStream(final Article art)
    throws IOException, UnsupportedEncodingException
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(art.getHeaderSource().getBytes("UTF-8"));
    out.write("\r\n\r\n".getBytes());
    out.write(art.getBody().getBytes(art.getBodyCharset()));
    out.flush();
    this.buffer = out.toByteArray();
  }
  
  public int read()
  {
    if(offset >= buffer.length)
    {
      return -1;
    }
    else
    {
      return buffer[offset++];
    }
  }
  
}
