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

package org.sonews.mlgw;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.util.io.ArticleInputStream;

/**
 * Connects to a SMTP server and sends a given Article to it.
 * @author Christian Lins
 */
class SMTPTransport
{

  protected BufferedReader in;
  protected PrintWriter    out;
  protected Socket         socket;

  public SMTPTransport(String host, int port)
    throws IOException, UnknownHostException
  {
    socket = new Socket(host, port);
    this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out = new PrintWriter(socket.getOutputStream());

    // Read helo from server
    String line = this.in.readLine();
    if(line == null || !line.startsWith("220 "))
    {
      throw new IOException("Invalid helo from server: " + line);
    }

    // Send HELO to server
    this.out.println("HELO " + Config.inst().get(Config.HOSTNAME, "localhost"));
    this.out.flush();
    line = this.in.readLine();
    if(line == null || !line.startsWith("250 "))
    {
      throw new IOException("Unexpected reply: " + line);
    }
  }

  public SMTPTransport(String host)
    throws IOException
  {
    this(host, 25);
  }

  public void close()
    throws IOException
  {
    this.out.println("QUIT");
    this.out.flush();
    this.in.readLine();

    this.socket.close();
  }

  public void send(Article article, String mailFrom, String rcptTo)
    throws IOException
  {
    assert(article != null);
    assert(mailFrom != null);
    assert(rcptTo != null);

    this.out.println("MAIL FROM: " + mailFrom);
    this.out.flush();
    String line = this.in.readLine();
    if(line == null || !line.startsWith("250 "))
    {
      throw new IOException("Unexpected reply: " + line);
    }

    this.out.println("RCPT TO: " + rcptTo);
    this.out.flush();
    line  = this.in.readLine();
    if(line == null || !line.startsWith("250 "))
    {
      throw new IOException("Unexpected reply: " + line);
    }

    this.out.println("DATA");
    this.out.flush();
    line = this.in.readLine();
    if(line == null || !line.startsWith("354 "))
    {
      throw new IOException("Unexpected reply: " + line);
    }

    ArticleInputStream   artStream = new ArticleInputStream(article);
    BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());
    FileOutputStream     fileStream = new FileOutputStream("smtp.dump");
    for(int b = artStream.read(); b >= 0; b = artStream.read())
    {
      outStream.write(b);
      fileStream.write(b);
    }

    // Flush the binary stream; important because otherwise the output
    // will be mixed with the PrintWriter.
    outStream.flush();
    fileStream.flush();
    fileStream.close();
    this.out.print("\r\n.\r\n");
    this.out.flush();
    line = this.in.readLine();
    if(line == null || !line.startsWith("250 "))
    {
      throw new IOException("Unexpected reply: " + line);
    }
  }

}
