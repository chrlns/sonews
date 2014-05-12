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

package org.sonews.test.unit.daemon;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.sonews.daemon.sync.SynchronousNNTPConnection;
import org.sonews.daemon.command.ArticleCommand;
import org.sonews.daemon.command.CapabilitiesCommand;
import org.sonews.daemon.command.GroupCommand;
import org.sonews.daemon.command.UnsupportedCommand;

/**
 * Unit test for class NNTPConnection.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class NNTPConnectionTest extends TestCase
{

  public void testLineReceived()
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
  {
    SynchronousNNTPConnection conn = null;

    try
    {
      try
      {
        conn = new SynchronousNNTPConnection(null);
        fail("Should have raised an IllegalArgumentException");
      }
      catch(IOException ex) {ex.printStackTrace();}
    }
    catch(IllegalArgumentException ex){}
    /*
    try
    {
      conn = new SynchronousNNTPConnection(SocketChannel.open());
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }

    assertNotNull(conn);*/

    // Make interesting methods accessible
    Class  clazz           = conn.getClass();
    Method methTryReadLock = clazz.getDeclaredMethod("tryReadLock", null);
    methTryReadLock.setAccessible(true);
    Method methLineReceived = clazz.getDeclaredMethod("lineReceived", new byte[0].getClass());
    methLineReceived.setAccessible(true);

    try
    {
      // conn.lineReceived(null);
      methLineReceived.invoke(conn, null);
      fail("Should have raised an IllegalArgumentException");
    }
    catch(IllegalArgumentException ex){}

    try
    {
      // conn.lineReceived(new byte[0]);
      methLineReceived.invoke(conn, new byte[0]);
      fail("Should have raised IllegalStateException");
    }
    catch(InvocationTargetException ex){}

    boolean tryReadLock = (Boolean)methTryReadLock.invoke(conn, null);
    assertTrue(tryReadLock);

    // conn.lineReceived("MODE READER".getBytes());
    methLineReceived.invoke(conn, "MODE READER".getBytes());

    // conn.lineReceived("sdkfsdjnfksjfdng ksdf gksjdfngk nskfng ksndfg ".getBytes());
    methLineReceived.invoke(conn, "sdkfsdjnfksjfdng ksdf ksndfg ".getBytes());

    // conn.lineReceived(new byte[1024]); // Too long
    methLineReceived.invoke(conn, new byte[1024]);

    Method mpcmdl = conn.getClass().getDeclaredMethod("parseCommandLine", String.class);
    mpcmdl.setAccessible(true);

    Object result = mpcmdl.invoke(conn, "");
    assertNotNull(result);
    assertTrue(result instanceof UnsupportedCommand);

    result = mpcmdl.invoke(conn, "aRtiCle");
    assertNotNull(result);
    assertTrue(result instanceof ArticleCommand);

    result = mpcmdl.invoke(conn, "capAbilItIEs");
    assertNotNull(result);
    assertTrue(result instanceof CapabilitiesCommand);

    result = mpcmdl.invoke(conn, "grOUp");
    assertNotNull(result);
    assertTrue(result instanceof GroupCommand);
  }

}
