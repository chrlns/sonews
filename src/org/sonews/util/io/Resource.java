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

package org.sonews.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Provides method for loading of resources.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Resource
{
  
  /**
   * Loads a resource and returns it as URL reference.
   * The Resource's classloader is used to load the resource, not
   * the System's ClassLoader so it may be safe to use this method
   * in a sandboxed environment.
   * @return
   */
  public static URL getAsURL(final String name)
  {
    if(name == null)
    {
      return null;
    }

    return Resource.class.getClassLoader().getResource(name);
  }
  
  /**
   * Loads a resource and returns an InputStream to it.
   * @param name
   * @return
   */
  public static InputStream getAsStream(String name)
  {
    try
    {
      URL url = getAsURL(name);
      if(url == null)
      {
        return null;
      }
      else
      {
        return url.openStream();
      }
    }
    catch(IOException e)
    {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Loads a plain text resource.
   * @param withNewline If false all newlines are removed from the 
   * return String
   */
  public static String getAsString(String name, boolean withNewline)
  {
    if(name == null)
      return null;

    BufferedReader in = null;
    try
    {
      InputStream ins = getAsStream(name);
      if(ins == null)
        return null;

      in = new BufferedReader(
        new InputStreamReader(ins, Charset.forName("UTF-8")));
      StringBuffer buf = new StringBuffer();

      for(;;)
      {
        String line = in.readLine();
        if(line == null)
          break;

        buf.append(line);
        if(withNewline)
          buf.append('\n');
      }

      return buf.toString();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      return null;
    }
    finally
    {
      try
      {
        if(in != null)
          in.close();
      }
      catch(IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }

}
