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

package com.so.news.util;

import java.util.HashMap;

/**
 * Class that allows simple String template handling.
 * @author Christian Lins (christian.lins@web.de)
 */
public class StringTemplate 
{
  private String                  str               = null;
  private String                  templateDelimiter = "%";
  private HashMap<String, String> templateValues    = new HashMap<String, String>();
  
  public StringTemplate(String str, String templateDelimiter)
  {
    this.str               = str;
    this.templateDelimiter = templateDelimiter;
  }
  
  public StringTemplate(String str)
  {
    this(str, "%");
  }
  
  public void set(String template, String value)
  {
    this.templateValues.put(template, value);
  }
  
  public void set(String template, long value)
  {
    set(template, Long.toString(value));
  }
  
  public void set(String template, double value)
  {
    set(template, Double.toString(value));
  }
  
  public void set(String template, Object obj)
  {
    set(template, obj.toString());
  }
  
  @Override
  public String toString()
  {
    String ret = new String(str);
    
    for(String key : this.templateValues.keySet())
    {
      String value = this.templateValues.get(key);
      ret = ret.replace(templateDelimiter + key, value);
    }
    
    return ret;
  }
}
