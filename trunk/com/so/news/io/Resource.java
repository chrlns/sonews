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

package com.so.news.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Provides method for loading of resources.
 * @author Christian Lins
 */
public class Resource
{
  /**
   * Loads a file as array of byte. As the file is completely loaded into
   * memory this method should only be used with small files.
   * @param file
   * @return
   */
  public static byte[] getBytes(File file)
  {
    try
    {
      FileInputStream in = new FileInputStream(file);
      byte[] buffer = new byte[(int)file.length()];
      
      in.read(buffer);
      
      return buffer;
    }
    catch(IOException ex)
    {
      System.err.println(ex.getLocalizedMessage());
      return null;
    }
  }
  
  /**
   * Loads a resource and returns it as URL reference.
   * The Resource's classloader is used to load the resource, not
   * the System's ClassLoader so it may be safe to use this method
   * in a sandboxed environment.
   * @return
   */
  public static URL getAsURL(String name)
  {
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
      return url.openStream();
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
    try
    {
      BufferedReader in  = new BufferedReader(
          new InputStreamReader(getAsStream(name), Charset.forName("UTF-8")));
      StringBuffer   buf = new StringBuffer();

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
  }
}
