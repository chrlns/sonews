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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * InputStream that can change its decoding charset while reading from the
 * underlying byte based stream.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class VarCharsetReader
{

  private final ByteBuffer buf = ByteBuffer.allocate(4096);
  private InputStream in;
  
  public VarCharsetReader(final InputStream in)
  {
    if(in == null)
    {
      throw new IllegalArgumentException("null InputStream");
    }
    this.in = in;
  }
  
  /**
   * Reads up to the next newline character and returns the line as String.
   * The String is decoded using the given charset.
   */
  public String readLine(Charset charset)
    throws IOException
  {    
    byte[] byteBuf = new byte[1];
    String bufStr;
    
    for(;;)
    {
      int read = this.in.read(byteBuf);
      if(read == 0)
      {
        continue;
      }
      else if(read == -1)
      {
        this.in = null;
        bufStr  = new String(this.buf.array(), 0, this.buf.position(), charset);
        break;
      }
      else if(byteBuf[0] == 10) // Is this safe? \n
      {
        bufStr  = new String(this.buf.array(), 0, this.buf.position(), charset);
        break;
      }
      else if(byteBuf[0] == 13) // \r
      { // Skip
        continue;
      }
      else
      {
        this.buf.put(byteBuf[0]);
      }
    }
    
    this.buf.clear();
    
    return bufStr;
  }
  
}
