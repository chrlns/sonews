/*
 *   StarOffice News Server
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

package org.sonews;

import org.sonews.daemon.command.HelloQuitTest;
import org.sonews.daemon.command.ArticleTest;
import java.util.LinkedList;
import java.util.List;
import org.sonews.daemon.command.CapabilitiesTest;
import org.sonews.daemon.command.GroupTest;
import org.sonews.daemon.command.ListGroupTests;
import org.sonews.daemon.command.ListTest;
import org.sonews.daemon.command.NewGroupsTest;
import org.sonews.daemon.command.NextTest;
import org.sonews.daemon.command.OverTest;
import org.sonews.daemon.command.PostTest;

/**
 * Run this class to perform a full test.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class TestBench 
{
  
  public static void main(String[] args)
  {
    System.out.println("TestBench for NNTP (RFC3799) based servers ");
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

    List<AbstractTest> tests = new LinkedList<AbstractTest>();
    
    // Add tests to perform
    tests.add(new HelloQuitTest());
    tests.add(new PostTest());    // Post before Article
    tests.add(new ArticleTest());
    tests.add(new CapabilitiesTest());
    tests.add(new GroupTest());
    tests.add(new ListGroupTests());
    tests.add(new ListTest());
    tests.add(new NewGroupsTest());
    tests.add(new NextTest());
    tests.add(new OverTest());
    
    // Perform all tests
    for(AbstractTest test : tests)
    {
      try
      {
        test.connect(host, port);
        int result = test.runTest();
        System.out.print(test.getClass().getName() + " finished with exit code " + result + "\t => ");
        if(result == 0)
        {
          System.out.println("SUCCESS");
        }
        else
        {
          System.out.println("FAILURE");
        }
      }
      catch(Exception ex)
      {
        System.out.println("Test " + test.getClass().getName() + " failed: " + ex.getLocalizedMessage());
        ex.printStackTrace();
      }
    }
  }
  
}
