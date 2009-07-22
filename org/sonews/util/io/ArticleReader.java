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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.sonews.config.Config;
import org.sonews.util.Log;

/**
 * Reads an news article from a NNTP server.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleReader 
{

  private BufferedOutputStream out;
  private BufferedInputStream  in;
  private String               messageID;
  
  public ArticleReader(String host, int port, String messageID)
    throws IOException, UnknownHostException
  {
    this.messageID = messageID;

    // Connect to NNTP server
    Socket socket = new Socket(host, port);
    this.out = new BufferedOutputStream(socket.getOutputStream());
    this.in  = new BufferedInputStream(socket.getInputStream());
    String line = readln(this.in);
    if(!line.startsWith("200 "))
    {
      throw new IOException("Invalid hello from server: " + line);
    }
  }
  
  private boolean eofArticle(byte[] buf)
  {
    if(buf.length < 4)
    {
      return false;
    }
    
    int l = buf.length - 1;
    return buf[l-3] == 10 // '*\n'
        && buf[l-2] == '.'                   // '.'
        && buf[l-1] == 13 && buf[l] == 10;  // '\r\n'
  }
  
  public byte[] getArticleData()
    throws IOException, UnsupportedEncodingException
  {
    long maxSize = Config.inst().get(Config.ARTICLE_MAXSIZE, 1024) * 1024L;

    try
    {
      this.out.write(("ARTICLE " + this.messageID + "\r\n").getBytes("UTF-8"));
      this.out.flush();

      String line = readln(this.in);
      if(line.startsWith("220 "))
      {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        
        while(!eofArticle(buf.toByteArray()))
        {
          for(int b = in.read(); b != 10; b = in.read())
          {
            buf.write(b);
          }

          buf.write(10);
          if(buf.size() > maxSize)
          {
            Log.msg("Skipping message that is too large: " + buf.size(), false);
            return null;
          }
        }
        
        return buf.toByteArray();
      }
      else
      {
        Log.msg("ArticleReader: " + line, false);
        return null;
      }
    }
    catch(IOException ex)
    {
      throw ex;
    }
    finally
    {
      this.out.write("QUIT\r\n".getBytes("UTF-8"));
      this.out.flush();
      this.out.close();
    }
  }
  
  private String readln(InputStream in)
    throws IOException
  {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    for(int b = in.read(); b != 10 /* \n */; b = in.read())
    {
      buf.write(b);
    }
    
    return new String(buf.toByteArray());
  }

}
