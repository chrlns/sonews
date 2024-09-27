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

/**
 * TestBench that performs a - yes - performance test. The test runs until
 * the opened sockets reach the systems maximum. The test has now valid end
 * as it will throw IOErrors at the end.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class PerfTestBench 
{

  public static void main(String[] args)
    throws Exception
  {
    System.out.println("Performance TestBench for NNTP (RFC3799) based servers ");
    if(args.length < 1)
    {
      System.out.println("Usage: TestBench <host>[:port]");
      return;
    }
    
    String[] hostport = args[0].split(":");
    String host = hostport[0];
    int    port = 119;
    if(hostport.length == 2)
    {
      port = Integer.parseInt(hostport[1]);
    }
    
    for(int n = 0; true; n++)
    {
      PerfTest pf = new PerfTest();
      pf.connect(host, port);
      pf.runTest();
      
      if(n % 100 == 0)
      {
        System.out.println("Test #" + n);
        System.out.flush();
      }
    }
  }
  
}
