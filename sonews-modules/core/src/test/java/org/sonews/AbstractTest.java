/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

package org.sonews;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Base class for every test performed by the TestBench. 
 * Connects to a NNTP Server and provides basic methods for sending and
 * receiving data.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public abstract class AbstractTest
{

  protected static PrintWriter log;
  
  static
  {
    try
    {
      log = new PrintWriter(new File("test.log"));
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  protected BufferedReader  in;
  protected PrintWriter     out;
  protected Socket          socket;
  
  /**
   * Connects to NNTP Server using for
   * @param host
   * @param port
   * @throws java.io.IOException
   * @throws java.net.UnknownHostException
   */
  public void connect(String host, int port)
    throws IOException, UnknownHostException
  {
    socket = new Socket(host, port);
    socket.setSoTimeout(10000);
    this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out = new PrintWriter(socket.getOutputStream());
  }
  
  protected void println(String line)
  {
    this.out.println(line);
    this.out.flush();
    
    log.println(">> " + line);
    log.flush();
  }
  
  protected String readln()
    throws IOException
  {
    String line = this.in.readLine();
    log.println("<< " + line);
    log.flush();
    return line;
  }
  
  public abstract int runTest() throws Exception;
  
}
