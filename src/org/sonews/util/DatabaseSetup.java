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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.sonews.config.Config;
import org.sonews.util.io.Resource;

/**
 * Database setup utility class.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class DatabaseSetup 
{

  private static final Map<String, String> templateMap 
    = new HashMap<String, String>();
  private static final Map<String, StringTemplate> urlMap
    = new HashMap<String, StringTemplate>();
  private static final Map<String, String> driverMap
    = new HashMap<String, String>();
  
  static
  {
    templateMap.put("1", "helpers/database_mysql5_tmpl.sql");
    templateMap.put("2", "helpers/database_postgresql8_tmpl.sql");
    
    urlMap.put("1", new StringTemplate("jdbc:mysql://%HOSTNAME/%DB"));
    urlMap.put("2", new StringTemplate("jdbc:postgresql://%HOSTNAME/%DB"));
    
    driverMap.put("1", "com.mysql.jdbc.Driver");
    driverMap.put("2", "org.postgresql.Driver");
  }
  
  public static void main(String[] args)
    throws Exception
  {
    System.out.println("sonews Database setup helper");
    System.out.println("This program will create a initial database table structure");
    System.out.println("for the sonews Newsserver.");
    System.out.println("You need to create a database and a db user manually before!");
    
    System.out.println("Select DBMS type:");
    System.out.println("[1] MySQL 5.x or higher");
    System.out.println("[2] PostgreSQL 8.x or higher");
    System.out.print("Your choice: ");
    
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String dbmsType = in.readLine();
    String tmplName = templateMap.get(dbmsType);
    if(tmplName == null)
    {
      System.err.println("Invalid choice. Try again you fool!");
      main(args);
      return;
    }
    
    // Load JDBC Driver class
    Class.forName(driverMap.get(dbmsType));
    
    String tmpl = Resource.getAsString(tmplName, true);
    
    System.out.print("Database server hostname (e.g. localhost): ");
    String dbHostname = in.readLine();
    
    System.out.print("Database name: ");
    String dbName = in.readLine();

    System.out.print("Give name of DB user that can create tables: ");
    String dbUser = in.readLine();

    System.out.print("Password: ");
    String dbPassword = in.readLine();
    
    String url = urlMap.get(dbmsType)
      .set("HOSTNAME", dbHostname)
      .set("DB", dbName).toString();
    
    Connection conn = 
      DriverManager.getConnection(url, dbUser, dbPassword);
    conn.setAutoCommit(false);
    
    String[] tmplChunks = tmpl.split(";");
    
    for(String chunk : tmplChunks)
    {
      if(chunk.trim().equals(""))
      {
        continue;
      }
      
      Statement stmt = conn.createStatement();
      stmt.execute(chunk);
    }
    
    conn.commit();
    conn.setAutoCommit(true);
    
    // Create config file
    
    System.out.println("Ok");
  }
  
}
