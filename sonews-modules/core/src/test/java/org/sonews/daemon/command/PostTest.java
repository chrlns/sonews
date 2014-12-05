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

package org.sonews.daemon.command;

import org.sonews.AbstractTest;

/**
 * Tests the POST command.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class PostTest extends AbstractTest
{
  
  @Override
  public int runTest()
    throws Exception
  {
    String line = readln();
    if(!line.startsWith("200 "))
    {
      return 1;
    }
    
    println("POST");
    line = readln();
    if(!line.startsWith("340 "))
    {
      return 1;
    }
    
    // Post a sample article
    postArticle("local.test");
    line = readln();
    if(line.startsWith("441 "))
    {
      println("POST");
      line = readln();
      if(!line.startsWith("340 "))
      {
        return 2;
      }
      
      postArticle("test");
      line = readln();
    }

    if(!line.startsWith("240 "))
    {
      return 3;
    }

    return 0;
  }
  
  private void postArticle(String toGroup)
  {
    println("Subject: A simple test mail");
    println("From: NNTP TestBench <testbench@sonews.org>");
    println("Newsgroups: " + toGroup);
    println("");
    println("Hello World!");
    println(".");
  }

}
