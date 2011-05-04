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

package test.unit.util;

import junit.framework.TestCase;
import org.sonews.util.TimeoutMap;

/**
 * Unit test for class org.sonews.util.TimeoutMap.
 * @author Christian Lins
 * @since sonews/0.5.0
 * @see org.sonews.util.TimeoutMap
 */
public class TimeoutMapTest extends TestCase
{

  public TimeoutMapTest()
  {
    super("TimeoutMapTest");
  }
  
  public void testTimeoutBehaviour()
  {
    TimeoutMap<String, Object> tm = new TimeoutMap<String, Object>(1000);
    Object testobj = new Object();
    
    tm.put("testkey", testobj);
    
    assertNotNull(tm.get("testkey"));
    assertTrue(tm.containsKey("testkey"));
    assertTrue(tm.contains(testobj));
    
    try
    {
      Thread.sleep(800);
    }
    catch(InterruptedException ex) { ex.printStackTrace(); }
    
    assertNotNull(tm.get("testkey"));
    assertTrue(tm.containsKey("testkey"));
    assertTrue(tm.contains(testobj));
    
    try
    {
      Thread.sleep(200);
    }
    catch(InterruptedException ex) { ex.printStackTrace(); }
    
    assertNull(tm.get("testkey"));
    assertFalse(tm.containsKey("testkey"));
    assertFalse(tm.contains(testobj));
  }
  
}
