/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import com.so.news.util.StringTemplate;

/**
 *
 * @author chris
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
