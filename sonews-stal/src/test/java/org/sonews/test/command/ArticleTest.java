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

package org.sonews.test.command;

import org.sonews.test.AbstractTest;

/**
 * Tests the ARTICLE command.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleTest extends AbstractTest
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
    
    // Select a group (we assume that local.test is existing)
    println("GROUP local.test");
    line = readln();
    if(!line.startsWith("211 "))
    {
      println("GROUP test");
      line = readln();
      if(!line.startsWith("211 "))
      {
        return 3;
      }
    }
    
    // Retrieve the first article
    println("ARTICLE " + line.split(" ")[2]);
    line = readln();
    if(!line.startsWith("220 "))
    {
      return 4;
    }
    
    while(!line.equals("."))
    {
      line = readln(); 
    }
    
    // Retrieve currently selected article (without a parameter number!)
    println("ARTICLE");
    line = readln();
    if(!line.startsWith("220 "))
    {
      return 5;
    }
    
    while(!line.equals("."))
    {
      line = readln(); 
    }
    
    println("QUIT");
    line = readln();
    if(!line.startsWith("205 "))
    {
      return 2;
    }
    
    return 0;
  }
  
}
