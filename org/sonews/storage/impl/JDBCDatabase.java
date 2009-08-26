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

package org.sonews.storage.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.mail.Header;
import javax.mail.internet.MimeUtility;
import org.sonews.config.Config;
import org.sonews.util.Log;
import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Channel;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;

/**
 * JDBCDatabase facade class.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
// TODO: Refactor this class to reduce size (e.g. ArticleDatabase GroupDatabase)
public class JDBCDatabase implements Storage
{

  public static final int MAX_RESTARTS = 3;
  
  private Connection        conn = null;
  private PreparedStatement pstmtAddArticle1 = null;
  private PreparedStatement pstmtAddArticle2 = null;
  private PreparedStatement pstmtAddArticle3 = null;
  private PreparedStatement pstmtAddArticle4 = null;
  private PreparedStatement pstmtAddGroup0   = null;
  private PreparedStatement pstmtAddEvent = null;
  private PreparedStatement pstmtCountArticles = null;
  private PreparedStatement pstmtCountGroups   = null;
  private PreparedStatement pstmtDeleteArticle0 = null;
  private PreparedStatement pstmtDeleteArticle1 = null;
  private PreparedStatement pstmtDeleteArticle2 = null;
  private PreparedStatement pstmtDeleteArticle3 = null;
  private PreparedStatement pstmtGetArticle0 = null;
  private PreparedStatement pstmtGetArticle1 = null;
  private PreparedStatement pstmtGetArticleHeaders0 = null;
  private PreparedStatement pstmtGetArticleHeaders1 = null;
  private PreparedStatement pstmtGetArticleHeads = null;
  private PreparedStatement pstmtGetArticleIDs   = null;
  private PreparedStatement pstmtGetArticleIndex    = null;
  private PreparedStatement pstmtGetConfigValue = null;
  private PreparedStatement pstmtGetEventsCount0 = null;
  private PreparedStatement pstmtGetEventsCount1 = null;
  private PreparedStatement pstmtGetGroupForList = null;
  private PreparedStatement pstmtGetGroup0     = null;
  private PreparedStatement pstmtGetGroup1     = null;
  private PreparedStatement pstmtGetFirstArticleNumber = null;
  private PreparedStatement pstmtGetListForGroup       = null;
  private PreparedStatement pstmtGetLastArticleNumber  = null;
  private PreparedStatement pstmtGetMaxArticleID       = null;
  private PreparedStatement pstmtGetMaxArticleIndex    = null;
  private PreparedStatement pstmtGetOldestArticle      = null;
  private PreparedStatement pstmtGetPostingsCount      = null;
  private PreparedStatement pstmtGetSubscriptions  = null;
  private PreparedStatement pstmtIsArticleExisting = null;
  private PreparedStatement pstmtIsGroupExisting = null;
  private PreparedStatement pstmtPurgeGroup0     = null;
  private PreparedStatement pstmtPurgeGroup1     = null;
  private PreparedStatement pstmtSetConfigValue0 = null;
  private PreparedStatement pstmtSetConfigValue1 = null;
  private PreparedStatement pstmtUpdateGroup     = null;
  
  /** How many times the database connection was reinitialized */
  private int restarts = 0;
  
  /**
   * Rises the database: reconnect and recreate all prepared statements.
   * @throws java.lang.SQLException
   */
  protected void arise()
    throws SQLException
  {
    try
    {
      // Load database driver
      Class.forName(
              Config.inst().get(Config.STORAGE_DBMSDRIVER, "java.lang.Object"));

      // Establish database connection
      this.conn = DriverManager.getConnection(
              Config.inst().get(Config.STORAGE_DATABASE, "<not specified>"),
              Config.inst().get(Config.STORAGE_USER, "root"),
              Config.inst().get(Config.STORAGE_PASSWORD, ""));

      this.conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      if(this.conn.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE)
      {
        Log.get().warning("Database is NOT fully serializable!");
      }

      // Prepare statements for method addArticle()
      this.pstmtAddArticle1 = conn.prepareStatement(
        "INSERT INTO articles (article_id, body) VALUES(?, ?)");
      this.pstmtAddArticle2 = conn.prepareStatement(
        "INSERT INTO headers (article_id, header_key, header_value, header_index) " +
        "VALUES (?, ?, ?, ?)");
      this.pstmtAddArticle3 = conn.prepareStatement(
        "INSERT INTO postings (group_id, article_id, article_index)" +
        "VALUES (?, ?, ?)");
      this.pstmtAddArticle4 = conn.prepareStatement(
        "INSERT INTO article_ids (article_id, message_id) VALUES (?, ?)");

      // Prepare statement for method addStatValue()
      this.pstmtAddEvent = conn.prepareStatement(
        "INSERT INTO events VALUES (?, ?, ?)");
     
      // Prepare statement for method addGroup()
      this.pstmtAddGroup0 = conn.prepareStatement(
        "INSERT INTO groups (name, flags) VALUES (?, ?)");
      
      // Prepare statement for method countArticles()
      this.pstmtCountArticles = conn.prepareStatement(
        "SELECT Count(article_id) FROM article_ids");
      
      // Prepare statement for method countGroups()
      this.pstmtCountGroups = conn.prepareStatement(
        "SELECT Count(group_id) FROM groups WHERE " +
        "flags & " + Channel.DELETED + " = 0");
      
      // Prepare statements for method delete(article)
      this.pstmtDeleteArticle0 = conn.prepareStatement(
        "DELETE FROM articles WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");
      this.pstmtDeleteArticle1 = conn.prepareStatement(
        "DELETE FROM headers WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");
      this.pstmtDeleteArticle2 = conn.prepareStatement(
        "DELETE FROM postings WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");
      this.pstmtDeleteArticle3 = conn.prepareStatement(
        "DELETE FROM article_ids WHERE message_id = ?");

      // Prepare statements for methods getArticle()
      this.pstmtGetArticle0 = conn.prepareStatement(
        "SELECT * FROM articles  WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");
      this.pstmtGetArticle1 = conn.prepareStatement(
        "SELECT * FROM articles WHERE article_id = " +
        "(SELECT article_id FROM postings WHERE " +
        "article_index = ? AND group_id = ?)");
      
      // Prepare statement for method getArticleHeaders()
      this.pstmtGetArticleHeaders0 = conn.prepareStatement(
        "SELECT header_key, header_value FROM headers WHERE article_id = ? " +
        "ORDER BY header_index ASC");

      // Prepare statement for method getArticleHeaders(regular expr pattern)
      this.pstmtGetArticleHeaders1 = conn.prepareStatement(
        "SELECT p.article_index, h.header_value FROM headers h " +
          "INNER JOIN postings p ON h.article_id = p.article_id " +
          "INNER JOIN groups g ON p.group_id = g.group_id " +
            "WHERE g.name          =  ? AND " +
                  "h.header_key    =  ? AND " +
                  "p.article_index >= ? " +
        "ORDER BY p.article_index ASC");

      this.pstmtGetArticleIDs = conn.prepareStatement(
        "SELECT article_index FROM postings WHERE group_id = ?");
      
      // Prepare statement for method getArticleIndex
      this.pstmtGetArticleIndex = conn.prepareStatement(
              "SELECT article_index FROM postings WHERE " +
              "article_id = (SELECT article_id FROM article_ids " +
              "WHERE message_id = ?) " +
              " AND group_id = ?");

      // Prepare statements for method getArticleHeads()
      this.pstmtGetArticleHeads = conn.prepareStatement(
        "SELECT article_id, article_index FROM postings WHERE " +
        "postings.group_id = ? AND article_index >= ? AND " +
        "article_index <= ?");

      // Prepare statements for method getConfigValue()
      this.pstmtGetConfigValue = conn.prepareStatement(
        "SELECT config_value FROM config WHERE config_key = ?");

      // Prepare statements for method getEventsCount()
      this.pstmtGetEventsCount0 = conn.prepareStatement(
        "SELECT Count(*) FROM events WHERE event_key = ? AND " +
        "event_time >= ? AND event_time < ?");

      this.pstmtGetEventsCount1 = conn.prepareStatement(
        "SELECT Count(*) FROM events WHERE event_key = ? AND " +
        "event_time >= ? AND event_time < ? AND group_id = ?");
      
      // Prepare statement for method getGroupForList()
      this.pstmtGetGroupForList = conn.prepareStatement(
        "SELECT name FROM groups INNER JOIN groups2list " +
        "ON groups.group_id = groups2list.group_id " +
        "WHERE groups2list.listaddress = ?");

      // Prepare statement for method getGroup()
      this.pstmtGetGroup0 = conn.prepareStatement(
        "SELECT group_id, flags FROM groups WHERE Name = ?");
      this.pstmtGetGroup1 = conn.prepareStatement(
        "SELECT name FROM groups WHERE group_id = ?");

      // Prepare statement for method getLastArticleNumber()
      this.pstmtGetLastArticleNumber = conn.prepareStatement(
        "SELECT Max(article_index) FROM postings WHERE group_id = ?");

      // Prepare statement for method getListForGroup()
      this.pstmtGetListForGroup = conn.prepareStatement(
        "SELECT listaddress FROM groups2list INNER JOIN groups " +
        "ON groups.group_id = groups2list.group_id WHERE name = ?");

      // Prepare statement for method getMaxArticleID()
      this.pstmtGetMaxArticleID = conn.prepareStatement(
        "SELECT Max(article_id) FROM articles");
      
      // Prepare statement for method getMaxArticleIndex()
      this.pstmtGetMaxArticleIndex = conn.prepareStatement(
        "SELECT Max(article_index) FROM postings WHERE group_id = ?");
      
      // Prepare statement for method getOldestArticle()
      this.pstmtGetOldestArticle = conn.prepareStatement(
        "SELECT message_id FROM article_ids WHERE article_id = " +
        "(SELECT Min(article_id) FROM article_ids)");

      // Prepare statement for method getFirstArticleNumber()
      this.pstmtGetFirstArticleNumber = conn.prepareStatement(
        "SELECT Min(article_index) FROM postings WHERE group_id = ?");
      
      // Prepare statement for method getPostingsCount()
      this.pstmtGetPostingsCount = conn.prepareStatement(
        "SELECT Count(*) FROM postings NATURAL JOIN groups " +
        "WHERE groups.name = ?");
      
      // Prepare statement for method getSubscriptions()
      this.pstmtGetSubscriptions = conn.prepareStatement(
        "SELECT host, port, name FROM peers NATURAL JOIN " +
        "peer_subscriptions NATURAL JOIN groups WHERE feedtype = ?");
      
      // Prepare statement for method isArticleExisting()
      this.pstmtIsArticleExisting = conn.prepareStatement(
        "SELECT Count(article_id) FROM article_ids WHERE message_id = ?");
      
      // Prepare statement for method isGroupExisting()
      this.pstmtIsGroupExisting = conn.prepareStatement(
        "SELECT * FROM groups WHERE name = ?");
      
      // Prepare statement for method setConfigValue()
      this.pstmtSetConfigValue0 = conn.prepareStatement(
        "DELETE FROM config WHERE config_key = ?");
      this.pstmtSetConfigValue1 = conn.prepareStatement(
        "INSERT INTO config VALUES(?, ?)");

      // Prepare statements for method purgeGroup()
      this.pstmtPurgeGroup0 = conn.prepareStatement(
        "DELETE FROM peer_subscriptions WHERE group_id = ?");
      this.pstmtPurgeGroup1 = conn.prepareStatement(
        "DELETE FROM groups WHERE group_id = ?");

      // Prepare statement for method update(Group)
      this.pstmtUpdateGroup = conn.prepareStatement(
        "UPDATE groups SET flags = ?, name = ? WHERE group_id = ?");
    }
    catch(ClassNotFoundException ex)
    {
      throw new Error("JDBC Driver not found!", ex);
    }
  }
  
  /**
   * Adds an article to the database.
   * @param article
   * @return
   * @throws java.sql.SQLException
   */
  @Override
  public void addArticle(final Article article)
    throws StorageBackendException
  {
    try
    {
      this.conn.setAutoCommit(false);

      int newArticleID = getMaxArticleID() + 1;

      // Fill prepared statement with values;
      // writes body to article table
      pstmtAddArticle1.setInt(1, newArticleID);
      pstmtAddArticle1.setBytes(2, article.getBody());
      pstmtAddArticle1.execute();

      // Add headers
      Enumeration headers = article.getAllHeaders();
      for(int n = 0; headers.hasMoreElements(); n++)
      {
        Header header = (Header)headers.nextElement();
        pstmtAddArticle2.setInt(1, newArticleID);
        pstmtAddArticle2.setString(2, header.getName().toLowerCase());
        pstmtAddArticle2.setString(3, 
          header.getValue().replaceAll("[\r\n]", ""));
        pstmtAddArticle2.setInt(4, n);
        pstmtAddArticle2.execute();
      }
      
      // For each newsgroup add a reference
      List<Group> groups = article.getGroups();
      for(Group group : groups)
      {
        pstmtAddArticle3.setLong(1, group.getInternalID());
        pstmtAddArticle3.setInt(2, newArticleID);
        pstmtAddArticle3.setLong(3, getMaxArticleIndex(group.getInternalID()) + 1);
        pstmtAddArticle3.execute();
      }
      
      // Write message-id to article_ids table
      this.pstmtAddArticle4.setInt(1, newArticleID);
      this.pstmtAddArticle4.setString(2, article.getMessageID());
      this.pstmtAddArticle4.execute();

      this.conn.commit();
      this.conn.setAutoCommit(true);

      this.restarts = 0; // Reset error count
    }
    catch(SQLException ex)
    {
      try
      {
        this.conn.rollback();  // Rollback changes
      }
      catch(SQLException ex2)
      {
        Log.get().severe("Rollback of addArticle() failed: " + ex2);
      }
      
      try
      {
        this.conn.setAutoCommit(true); // and release locks
      }
      catch(SQLException ex2)
      {
        Log.get().severe("setAutoCommit(true) of addArticle() failed: " + ex2);
      }

      restartConnection(ex);
      addArticle(article);
    }
  }
  
  /**
   * Adds a group to the JDBCDatabase. This method is not accessible via NNTP.
   * @param name
   * @throws java.sql.SQLException
   */
  @Override
  public void addGroup(String name, int flags)
    throws StorageBackendException
  {
    try
    {
      this.conn.setAutoCommit(false);
      pstmtAddGroup0.setString(1, name);
      pstmtAddGroup0.setInt(2, flags);

      pstmtAddGroup0.executeUpdate();
      this.conn.commit();
      this.conn.setAutoCommit(true);
      this.restarts = 0; // Reset error count
    }
    catch(SQLException ex)
    {
      try
      {
        this.conn.rollback();
        this.conn.setAutoCommit(true);
      }
      catch(SQLException ex2)
      {
        ex2.printStackTrace();
      }

      restartConnection(ex);
      addGroup(name, flags);
    }
  }

  @Override
  public void addEvent(long time, int type, long gid)
    throws StorageBackendException
  {
    try
    {
      this.conn.setAutoCommit(false);
      this.pstmtAddEvent.setLong(1, time);
      this.pstmtAddEvent.setInt(2, type);
      this.pstmtAddEvent.setLong(3, gid);
      this.pstmtAddEvent.executeUpdate();
      this.conn.commit();
      this.conn.setAutoCommit(true);
      this.restarts = 0;
    }
    catch(SQLException ex)
    {
      try
      {
        this.conn.rollback();
        this.conn.setAutoCommit(true);
      }
      catch(SQLException ex2)
      {
        ex2.printStackTrace();
      }

      restartConnection(ex);
      addEvent(time, type, gid);
    }
  }

  @Override
  public int countArticles()
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      rs = this.pstmtCountArticles.executeQuery();
      if(rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        return -1;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return countArticles();
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
        restarts = 0;
      }
    }
  }

  @Override
  public int countGroups()
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      rs = this.pstmtCountGroups.executeQuery();
      if(rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        return -1;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return countGroups();
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
        restarts = 0;
      }
    }
  }

  @Override
  public void delete(final String messageID)
    throws StorageBackendException
  {
    try
    {
      this.conn.setAutoCommit(false);
      
      this.pstmtDeleteArticle0.setString(1, messageID);
      int rs = this.pstmtDeleteArticle0.executeUpdate();
      
      // We do not trust the ON DELETE CASCADE functionality to delete
      // orphaned references...
      this.pstmtDeleteArticle1.setString(1, messageID);
      rs = this.pstmtDeleteArticle1.executeUpdate();

      this.pstmtDeleteArticle2.setString(1, messageID);
      rs = this.pstmtDeleteArticle2.executeUpdate();

      this.pstmtDeleteArticle3.setString(1, messageID);
      rs = this.pstmtDeleteArticle3.executeUpdate();
      
      this.conn.commit();
      this.conn.setAutoCommit(true);
    }
    catch(SQLException ex)
    {
      throw new StorageBackendException(ex);
    }
  }

  @Override
  public Article getArticle(String messageID)
    throws StorageBackendException
  {
    ResultSet rs = null;
    try
    {
      pstmtGetArticle0.setString(1, messageID);
      rs = pstmtGetArticle0.executeQuery();

      if(!rs.next())
      {
        return null;
      }
      else
      {
        byte[] body     = rs.getBytes("body");
        String headers  = getArticleHeaders(rs.getInt("article_id"));
        return new Article(headers, body);
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticle(messageID);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
        restarts = 0; // Reset error count
      }
    }
  }
  
  /**
   * Retrieves an article by its ID.
   * @param articleID
   * @return
   * @throws StorageBackendException
   */
  @Override
  public Article getArticle(long articleIndex, long gid)
    throws StorageBackendException
  {  
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticle1.setLong(1, articleIndex);
      this.pstmtGetArticle1.setLong(2, gid);

      rs = this.pstmtGetArticle1.executeQuery();

      if(rs.next())
      {
        byte[] body    = rs.getBytes("body");
        String headers = getArticleHeaders(rs.getInt("article_id"));
        return new Article(headers, body);
      }
      else
      {
        return null;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticle(articleIndex, gid);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
        restarts = 0;
      }
    }
  }

  /**
   * Searches for fitting header values using the given regular expression.
   * @param group
   * @param start
   * @param end
   * @param headerKey
   * @param pattern
   * @return
   * @throws StorageBackendException
   */
  @Override
  public List<Pair<Long, String>> getArticleHeaders(Channel group, long start,
    long end, String headerKey, String patStr)
    throws StorageBackendException, PatternSyntaxException
  {
    ResultSet rs = null;
    List<Pair<Long, String>> heads = new ArrayList<Pair<Long, String>>();

    try
    {
      this.pstmtGetArticleHeaders1.setString(1, group.getName());
      this.pstmtGetArticleHeaders1.setString(2, headerKey);
      this.pstmtGetArticleHeaders1.setLong(3, start);

      rs = this.pstmtGetArticleHeaders1.executeQuery();

      // Convert the "NNTP" regex to Java regex
      patStr = patStr.replace("*", ".*");
      Pattern pattern = Pattern.compile(patStr);

      while(rs.next())
      {
        Long articleIndex = rs.getLong(1);
        if(end < 0 || articleIndex <= end) // Match start is done via SQL
        {
          String headerValue  = rs.getString(2);
          Matcher matcher = pattern.matcher(headerValue);
          if(matcher.matches())
          {
            heads.add(new Pair<Long, String>(articleIndex, headerValue));
          }
        }
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticleHeaders(group, start, end, headerKey, patStr);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }

    return heads;
  }

  private String getArticleHeaders(long articleID)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetArticleHeaders0.setLong(1, articleID);
      rs = this.pstmtGetArticleHeaders0.executeQuery();
      
      StringBuilder buf = new StringBuilder();
      if(rs.next())
      {
        for(;;)
        {
          buf.append(rs.getString(1)); // key
          buf.append(": ");
          String foldedValue = MimeUtility.fold(0, rs.getString(2));
          buf.append(foldedValue); // value
          if(rs.next())
          {
            buf.append("\r\n");
          }
          else
          {
            break;
          }
        }
      }
      
      return buf.toString();
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticleHeaders(articleID);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public long getArticleIndex(Article article, Group group)
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticleIndex.setString(1, article.getMessageID());
      this.pstmtGetArticleIndex.setLong(2, group.getInternalID());
      
      rs = this.pstmtGetArticleIndex.executeQuery();
      if(rs.next())
      {
        return rs.getLong(1);
      }
      else
      {
        return -1;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticleIndex(article, group);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  /**
   * Returns a list of Long/Article Pairs.
   * @throws java.sql.SQLException
   */
  @Override
  public List<Pair<Long, ArticleHead>> getArticleHeads(Group group, long first,
    long last)
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticleHeads.setLong(1, group.getInternalID());
      this.pstmtGetArticleHeads.setLong(2, first);
      this.pstmtGetArticleHeads.setLong(3, last);
      rs = pstmtGetArticleHeads.executeQuery();

      List<Pair<Long, ArticleHead>> articles 
        = new ArrayList<Pair<Long, ArticleHead>>();

      while (rs.next())
      {
        long aid  = rs.getLong("article_id");
        long aidx = rs.getLong("article_index");
        String headers = getArticleHeaders(aid);
        articles.add(new Pair<Long, ArticleHead>(aidx, 
                        new ArticleHead(headers)));
      }

      return articles;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticleHeads(group, first, last);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public List<Long> getArticleNumbers(long gid)
    throws StorageBackendException
  {
    ResultSet rs = null;
    try
    {
      List<Long> ids = new ArrayList<Long>();
      this.pstmtGetArticleIDs.setLong(1, gid);
      rs = this.pstmtGetArticleIDs.executeQuery();
      while(rs.next())
      {
        ids.add(rs.getLong(1));
      }
      return ids;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getArticleNumbers(gid);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
          restarts = 0; // Clear the restart count after successful request
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public String getConfigValue(String key)
    throws StorageBackendException
  {
    ResultSet rs = null;
    try
    {
      this.pstmtGetConfigValue.setString(1, key);

      rs = this.pstmtGetConfigValue.executeQuery();
      if(rs.next())
      {
        return rs.getString(1); // First data on index 1 not 0
      }
      else
      {
        return null;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getConfigValue(key);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
        restarts = 0; // Clear the restart count after successful request
      }
    }
  }

  @Override
  public int getEventsCount(int type, long start, long end, Channel channel)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      if(channel == null)
      {
        this.pstmtGetEventsCount0.setInt(1, type);
        this.pstmtGetEventsCount0.setLong(2, start);
        this.pstmtGetEventsCount0.setLong(3, end);
        rs = this.pstmtGetEventsCount0.executeQuery();
      }
      else
      {
        this.pstmtGetEventsCount1.setInt(1, type);
        this.pstmtGetEventsCount1.setLong(2, start);
        this.pstmtGetEventsCount1.setLong(3, end);
        this.pstmtGetEventsCount1.setLong(4, channel.getInternalID());
        rs = this.pstmtGetEventsCount1.executeQuery();
      }
      
      if(rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        return -1;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getEventsCount(type, start, end, channel);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  /**
   * Reads all Groups from the JDBCDatabase.
   * @return
   * @throws StorageBackendException
   */
  @Override
  public List<Channel> getGroups()
    throws StorageBackendException
  {
    ResultSet   rs;
    List<Channel> buffer = new ArrayList<Channel>();
    Statement   stmt   = null;

    try
    {
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT * FROM groups ORDER BY name");

      while(rs.next())
      {
        String name  = rs.getString("name");
        long   id    = rs.getLong("group_id");
        int    flags = rs.getInt("flags");
        
        Group group = new Group(name, id, flags);
        buffer.add(group);
      }

      return buffer;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getGroups();
    }
    finally
    {
      if(stmt != null)
      {
        try
        {
          stmt.close(); // Implicitely closes ResultSets
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public List<String> getGroupsForList(String listAddress)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetGroupForList.setString(1, listAddress);

      rs = this.pstmtGetGroupForList.executeQuery();
      List<String> groups = new ArrayList<String>();
      while(rs.next())
      {
        String group = rs.getString(1);
        groups.add(group);
      }
      return groups;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getGroupsForList(listAddress);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  /**
   * Returns the Group that is identified by the name.
   * @param name
   * @return
   * @throws StorageBackendException
   */
  @Override
  public Group getGroup(String name)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetGroup0.setString(1, name);
      rs = this.pstmtGetGroup0.executeQuery();

      if (!rs.next())
      {
        return null;
      }
      else
      {
        long id = rs.getLong("group_id");
        int flags = rs.getInt("flags");
        return new Group(name, id, flags);
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getGroup(name);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public List<String> getListsForGroup(String group)
    throws StorageBackendException
  {
    ResultSet     rs    = null;
    List<String>  lists = new ArrayList<String>();

    try
    {
      this.pstmtGetListForGroup.setString(1, group);
      rs = this.pstmtGetListForGroup.executeQuery();

      while(rs.next())
      {
        lists.add(rs.getString(1));
      }
      return lists;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getListsForGroup(group);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  private int getMaxArticleIndex(long groupID)
    throws StorageBackendException
  {
    ResultSet rs    = null;

    try
    {
      this.pstmtGetMaxArticleIndex.setLong(1, groupID);
      rs = this.pstmtGetMaxArticleIndex.executeQuery();

      int maxIndex = 0;
      if (rs.next())
      {
        maxIndex = rs.getInt(1);
      }

      return maxIndex;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getMaxArticleIndex(groupID);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  private int getMaxArticleID()
    throws StorageBackendException
  {
    ResultSet rs    = null;

    try
    {
      rs = this.pstmtGetMaxArticleID.executeQuery();

      int maxIndex = 0;
      if (rs.next())
      {
        maxIndex = rs.getInt(1);
      }

      return maxIndex;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getMaxArticleID();
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public int getLastArticleNumber(Group group)
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetLastArticleNumber.setLong(1, group.getInternalID());
      rs = this.pstmtGetLastArticleNumber.executeQuery();
      if (rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        return 0;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getLastArticleNumber(group);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public int getFirstArticleNumber(Group group)
    throws StorageBackendException
  {
    ResultSet rs = null;
    try
    {
      this.pstmtGetFirstArticleNumber.setLong(1, group.getInternalID());
      rs = this.pstmtGetFirstArticleNumber.executeQuery();
      if(rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        return 0;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getFirstArticleNumber(group);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  /**
   * Returns a group name identified by the given id.
   * @param id
   * @return
   * @throws StorageBackendException
   */
  public String getGroup(int id)
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetGroup1.setInt(1, id);
      rs = this.pstmtGetGroup1.executeQuery();

      if (rs.next())
      {
        return rs.getString(1);
      }
      else
      {
        return null;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getGroup(id);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public double getEventsPerHour(int key, long gid)
    throws StorageBackendException
  {
    String gidquery = "";
    if(gid >= 0)
    {
      gidquery = " AND group_id = " + gid;
    }
    
    Statement stmt = null;
    ResultSet rs   = null;
    
    try
    {
      stmt = this.conn.createStatement();
      rs = stmt.executeQuery("SELECT Count(*) / (Max(event_time) - Min(event_time))" +
        " * 1000 * 60 * 60 FROM events WHERE event_key = " + key + gidquery);
      
      if(rs.next())
      {
        restarts = 0; // reset error count
        return rs.getDouble(1);
      }
      else
      {
        return Double.NaN;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getEventsPerHour(key, gid);
    }
    finally
    {
      try
      {
        if(stmt != null)
        {
          stmt.close(); // Implicitely closes the result sets
        }
      }
      catch(SQLException ex)
      {
        ex.printStackTrace();
      }
    }
  }

  @Override
  public String getOldestArticle()
    throws StorageBackendException
  {
    ResultSet rs = null;

    try
    {
      rs = this.pstmtGetOldestArticle.executeQuery();
      if(rs.next())
      {
        return rs.getString(1);
      }
      else
      {
        return null;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getOldestArticle();
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public int getPostingsCount(String groupname)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetPostingsCount.setString(1, groupname);
      rs = this.pstmtGetPostingsCount.executeQuery();
      if(rs.next())
      {
        return rs.getInt(1);
      }
      else
      {
        Log.get().warning("Count on postings return nothing!");
        return 0;
      }
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getPostingsCount(groupname);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public List<Subscription> getSubscriptions(int feedtype)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      List<Subscription> subs = new ArrayList<Subscription>();
      this.pstmtGetSubscriptions.setInt(1, feedtype);
      rs = this.pstmtGetSubscriptions.executeQuery();
      
      while(rs.next())
      {
        String host  = rs.getString("host");
        String group = rs.getString("name");
        int    port  = rs.getInt("port");
        subs.add(new Subscription(host, port, feedtype, group));
      }
      
      return subs;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return getSubscriptions(feedtype);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Checks if there is an article with the given messageid in the JDBCDatabase.
   * @param name
   * @return
   * @throws StorageBackendException
   */
  @Override
  public boolean isArticleExisting(String messageID)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtIsArticleExisting.setString(1, messageID);
      rs = this.pstmtIsArticleExisting.executeQuery();
      return rs.next() && rs.getInt(1) == 1;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return isArticleExisting(messageID);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  /**
   * Checks if there is a group with the given name in the JDBCDatabase.
   * @param name
   * @return
   * @throws StorageBackendException
   */
  @Override
  public boolean isGroupExisting(String name)
    throws StorageBackendException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtIsGroupExisting.setString(1, name);
      rs = this.pstmtIsGroupExisting.executeQuery();
      return rs.next();
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return isGroupExisting(name);
    }
    finally
    {
      if(rs != null)
      {
        try
        {
          rs.close();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
  }

  @Override
  public void setConfigValue(String key, String value)
    throws StorageBackendException
  {
    try
    {
      conn.setAutoCommit(false);
      this.pstmtSetConfigValue0.setString(1, key);
      this.pstmtSetConfigValue0.execute();
      this.pstmtSetConfigValue1.setString(1, key);
      this.pstmtSetConfigValue1.setString(2, value);
      this.pstmtSetConfigValue1.execute();
      conn.commit();
      conn.setAutoCommit(true);
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      setConfigValue(key, value);
    }
  }
  
  /**
   * Closes the JDBCDatabase connection.
   */
  public void shutdown()
    throws StorageBackendException
  {
    try
    {
      if(this.conn != null)
      {
        this.conn.close();
      }
    }
    catch(SQLException ex)
    {
      throw new StorageBackendException(ex);
    }
  }

  @Override
  public void purgeGroup(Group group)
    throws StorageBackendException
  {
    try
    {
      this.pstmtPurgeGroup0.setLong(1, group.getInternalID());
      this.pstmtPurgeGroup0.executeUpdate();

      this.pstmtPurgeGroup1.setLong(1, group.getInternalID());
      this.pstmtPurgeGroup1.executeUpdate();
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      purgeGroup(group);
    }
  }
  
  private void restartConnection(SQLException cause)
    throws StorageBackendException
  {
    restarts++;
    Log.get().severe(Thread.currentThread()
      + ": Database connection was closed (restart " + restarts + ").");
    
    if(restarts >= MAX_RESTARTS)
    {
      // Delete the current, probably broken JDBCDatabase instance.
      // So no one can use the instance any more.
      JDBCDatabaseProvider.instances.remove(Thread.currentThread());
      
      // Throw the exception upwards
      throw new StorageBackendException(cause);
    }
    
    try
    {
      Thread.sleep(1500L * restarts);
    }
    catch(InterruptedException ex)
    {
      Log.get().warning("Interrupted: " + ex.getMessage());
    }
    
    // Try to properly close the old database connection
    try
    {
      if(this.conn != null)
      {
        this.conn.close();
      }
    }
    catch(SQLException ex)
    {
      Log.get().warning(ex.getMessage());
    }
    
    try
    {
      // Try to reinitialize database connection
      arise();
    }
    catch(SQLException ex)
    {
      Log.get().warning(ex.getMessage());
      restartConnection(ex);
    }
  }

  @Override
  public boolean update(Article article)
    throws StorageBackendException
  {
    // DELETE FROM headers WHERE article_id = ?

    // INSERT INTO headers ...

    // SELECT * FROM postings WHERE article_id = ? AND group_id = ?
  }

  /**
   * Writes the flags and the name of the given group to the database.
   * @param group
   * @throws StorageBackendException
   */
  @Override
  public boolean update(Group group)
    throws StorageBackendException
  {
    try
    {
      this.pstmtUpdateGroup.setInt(1, group.getFlags());
      this.pstmtUpdateGroup.setString(2, group.getName());
      this.pstmtUpdateGroup.setLong(3, group.getInternalID());
      int rs = this.pstmtUpdateGroup.executeUpdate();
      return rs == 1;
    }
    catch(SQLException ex)
    {
      restartConnection(ex);
      return update(group);
    }
  }

}
