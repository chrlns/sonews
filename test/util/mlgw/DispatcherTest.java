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

package test.util.mlgw;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.sonews.mlgw.Dispatcher;

/**
 * Tests the methods of class org.sonews.mlgw.Dispatcher.
 * @author Christian Lins
 * @since sonews/1.0.3
 */
public class DispatcherTest extends TestCase
{

  public DispatcherTest()
  {
    super("DispatcherTest");
  }

  public void testChunkListPost()
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
  {
    Dispatcher disp = new Dispatcher();

    Class  clazz             = disp.getClass();
    Method methChunkListPost = clazz.getDeclaredMethod("chunkListPost", String.class);
    methChunkListPost.setAccessible(true);

    try
    {
      // disp.chunkListPost(null)
      methChunkListPost.invoke(disp, null);
      fail("Should have raised an IllegalArgumentException");
    }
    catch(IllegalArgumentException ex){}

    // disp.chunkListPost("")
    Object obj = methChunkListPost.invoke(disp, "");
    assertNull(obj);

    // disp.chunkListPost("listPostValue is of form <mailto:dev@openoffice.org>")
    obj = methChunkListPost.invoke(disp, "listPostValue is of form <mailto:dev@openoffice.org>");
    assertNotNull(obj);
    assertEquals("dev@openoffice.org", (String)obj);

    // disp.chunkListPost("<mailto:frisbee-users@fun.rec.uk.sun.com")
    obj = methChunkListPost.invoke(disp, "<mailto:frisbee-users@fun.rec.uk.sun.com");
    assertNotNull(obj);
    assertEquals("frisbee-users@fun.rec.uk.sun.com", (String)obj);
  }

}
