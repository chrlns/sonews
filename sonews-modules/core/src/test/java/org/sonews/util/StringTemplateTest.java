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

package org.sonews.util;

import junit.framework.TestCase;
import org.sonews.util.StringTemplate;

/**
 * Unit test for class org.sonews.util.StringTemplate.
 * @author Christian Lins 
 * @see org.sonews.util.StringTemplate
 * @since sonews/0.5.0
 */
public class StringTemplateTest extends TestCase
{

  private static final String template = "Hello %WORLD and others!";
  
  public StringTemplateTest()
  {
    super("StringTemplateTest");
  }

  public void testCtor()
  {
    StringTemplate st;
    
    try
    {
      st = new StringTemplate(null);
      fail("Should have raised an IllegalArgumentException");
    }
    catch(IllegalArgumentException ex) {}
      
    st = new StringTemplate(template);
    assertNotNull(st);
    
    try
    {
      st = new StringTemplate(template, null);
      fail("Should have raised an IllegalArgumentException");
    }
    catch(IllegalArgumentException ex) {}
    
    st = new StringTemplate(template, "?");
    assertNotNull(st);

    st = new StringTemplate(template, "%");
    assertNotNull(st);
  }
  
  public void testSetter()
  {
    StringTemplate st = new StringTemplate(template);
    
    try
    {
      st.set("WORLD", null);
      fail("Should have raised an IllegalArgumentException");
    }
    catch(IllegalArgumentException ex){}
    
    st.set("WORLD", "Universe");
  }
  
  public void testToString()
  {
    StringTemplate st = new StringTemplate(template);
    st.set("WORLD", "Universe");
    
    String result = st.toString();
    assertNotNull(result);
  }
  
}
