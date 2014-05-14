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
 * Test: connects to server, waits for initial hello and quits.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class HelloQuitTest extends AbstractTest
{

  @Override
  public int runTest()
    throws Exception
  {
    String line = readln();
    if(!line.startsWith("200 "))
      return 1;
    
    println("QUIT");
    line = readln();
    if(!line.startsWith("205 "))
      return 2;
    
    return 0;
  }
  
}
