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

package com.so.news.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.zip.CRC32;
import com.so.news.Config;
import com.so.news.util.StringTemplate;

/**
 * Database abstraction class.
 * @author Christian Lins (christian.lins@web.de)
 */
public class Database
{
  private static Database instance = null;
  
  /**
   * Initializes the Database subsystem, e.g. loading a JDBC driver and
   * connection to the Database Managment System.
   * This method is called when the daemon starts up or at the first
   * call to Database.getInstance().
   * @throws java.lang.Exception
   */
  public static void arise()
    throws Exception
  {
    // Tries to load the Database driver and establish a connection.
    if(instance == null)
      instance = new Database();
  }
  
  /**
   * @return Instance of the current Database backend. Returns null if an error
   * has occurred.
   */
  public static Database getInstance()
  {
    try
    {
      arise();
      return instance;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return null;
    }
  }
  
  private Connection conn = null;
  
  /**
   * Private constructor.
   * @throws java.lang.Exception
   */
  private Database()
    throws Exception
  {
    Class.forName(
            Config.getInstance().get("n3tpd.storage.dbmsdriver", ""));
    this.conn = DriverManager.getConnection(
            Config.getInstance().get("n3tpd.storage.database", ""),
            Config.getInstance().get("n3tpd.storage.user", "n3tpd_user"),
            Config.getInstance().get("n3tpd.storage.password", ""));
    this.conn.setAutoCommit(false);
  }
  
  /**
   * Adds an article to the database.
   * @param article
   * @return
   * @throws java.sql.SQLException
   */
  public boolean addArticle(Article article)
    throws SQLException
  {
    Statement stmt = this.conn.createStatement();

    String sql0 = "START TRANSACTION";
    String sql1 = "INSERT INTO articles (message_id,header,body)" +
            "VALUES('%mid', '%header', '%body')";
    StringTemplate tmpl = new StringTemplate(sql1);
    tmpl.set("body", article.getBody());
    tmpl.set("mid", article.getMessageID());
    tmpl.set("header", article.getHeaderSource());
    sql1 = tmpl.toString();
    
    String sql2 = "COMMIT";
    
    // Add statements as batch
    stmt.addBatch(sql0);
    stmt.addBatch(sql1);
    
    // TODO: For each newsgroup add a reference
    String sql = "INSERT INTO postings (group_id, article_id, article_index)" +
                 "VALUES (%gid, (SELECT article_id FROM articles WHERE message_id = '%mid')," +
                 " %idx)";
    
    tmpl = new StringTemplate(sql);
    tmpl.set("gid", article.getGroupID());
    tmpl.set("mid", article.getMessageID());
    tmpl.set("idx", getMaxArticleIndex() + 1);
    stmt.addBatch(tmpl.toString());
    
    // Commit
    stmt.addBatch(sql2);
    
    // And execute the batch
    stmt.executeBatch();
    
    return true;
  }
  
  /**
   * Adds a group to the Database.
   * @param name
   * @throws java.sql.SQLException
   */
  public boolean addGroup(String name)
    throws SQLException
  {
    CRC32 crc = new CRC32();
    crc.update(name.getBytes());
    
    long id = crc.getValue();
    
    Statement stmt = conn.createStatement();
    return 1 == stmt.executeUpdate("INSERT INTO Groups (ID, Name) VALUES (" + id + ", '" + name + "')");
  }
  
  public void delete(Article article)
  {
    
  }
  
  public void delete(Group group)
  {
    
  }
  
  public Article getArticle(String messageID)
    throws SQLException
  {
    Statement stmt = this.conn.createStatement();
    ResultSet rs =
      stmt.executeQuery("SELECT * FROM articles WHERE message_id = '" + messageID + "'");
    
    return new Article(rs);
  }
  
  public Article getArticle(long gid, long article_id)
    throws SQLException
  {
    Statement stmt = this.conn.createStatement();
    String sql = "SELECT * FROM articles WHERE article_id = " +
            "(SELECT article_id FROM postings WHERE " +
            "group_id = " + gid + " AND article_id = " + article_id +")";
    ResultSet rs =
      stmt.executeQuery(sql);
    
    if(rs.next())
      return new Article(rs);
    else
      return null;
  }
  
  public ResultSet getArticles()
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    return stmt.executeQuery("SELECT * FROM articles");
  }
  
  /**
   * Reads all Groups from the Database.
   * @return
   * @throws java.sql.SQLException
   */
  public ResultSet getGroups()
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM groups");
    
    return rs;
  }
  
  /**
   * Returns the Group that is identified by the name.
   * @param name
   * @return
   * @throws java.sql.SQLException
   */
  public Group getGroup(String name)
    throws SQLException
  {   
    Statement stmt = this.conn.createStatement();
    String sql = "SELECT group_id FROM groups WHERE Name = '%name'";
    StringTemplate tmpl = new StringTemplate(sql);
    tmpl.set("name", name);
    
    ResultSet rs = stmt.executeQuery(tmpl.toString());
  
    if(!rs.next())
      return null;
    else
    {
      long id = rs.getLong("group_id");
      return new Group(name, id);
    }
  }
  
  public int getMaxArticleIndex()
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(
      "SELECT Max(article_index) FROM postings");
    
    if(!rs.next())
      return 0;
    else
      return rs.getInt(1);
  }
  
  public int getLastArticleNumber(Group group)
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(
      "SELECT Max(article_index) FROM postings WHERE group_id = " + group.getID());
    
    if(!rs.next())
      return 0;
    else
      return rs.getInt(1);
  }
  
  public int getFirstArticleNumber(Group group)
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(
      "SELECT Min(article_index) FROM postings WHERE group_id = " + group.getID());
  
    if(!rs.next())
      return 0;
    else
      return rs.getInt(1);
  }
  
  /**
   * Returns a group name identified by the given id.
   * @param id
   * @return
   * @throws java.sql.SQLException
   */
  public String getGroup(int id)
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs   = stmt.executeQuery(
            "SELECT name FROM groups WHERE group_id = '" + id + "'");
    
    if(rs.next())
    {
      return rs.getString(1);
    }
    else
      return null;
  }
  
  public Article getOldestArticle()
    throws SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = 
      stmt.executeQuery("SELECT * FROM Articles WHERE Date = (SELECT Min(Date) FROM Articles)");
    
    if(rs.next())
      return new Article(rs);
    else
      return null;
  }
  
  /**
   * Checks if there is a group with the given name in the Database.
   * @param name
   * @return
   * @throws java.sql.SQLException
   */
  public boolean isGroupExisting(String name)
    throws SQLException
  {
    Statement stmt = this.conn.createStatement();
    ResultSet rs   = stmt.executeQuery("SELECT * FROM Groups WHERE Name = '" + name + "'");
    
    return rs.next();
  }
  
  public void updateArticle(Article article)
  {
    
  }
}
