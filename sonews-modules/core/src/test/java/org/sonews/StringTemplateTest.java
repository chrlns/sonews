/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

package org.sonews;

import org.sonews.util.StringTemplate;

/**
 * Tests the StringTemplate class.
 * @author Christian Lins
 * @since sonews/0.5.0
 * @see org.sonews.util.StringTemplate
 */
public class StringTemplateTest 
{

  public static void main(String[] args)
  {
    StringTemplate templ 
      = new StringTemplate("SELECT %row FROM %table WHERE %row = ich");
    
    templ.set("row", "name");
    templ.set("table", "UserTable");
    
    System.out.println(templ.toString());
  }

}
