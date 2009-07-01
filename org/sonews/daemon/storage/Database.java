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

package org.sonews.daemon.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.Header;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import org.sonews.daemon.BootstrapConfig;
import org.sonews.util.Log;
import org.sonews.feed.Subscription;
import org.sonews.util.Pair;

/**
 * Database facade class.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
// TODO: Refactor this class to reduce size (e.g. ArticleDatabase GroupDatabase)
public class Database
{

  public static final int MAX_RESTARTS = 3;
  
  private static final Map<Thread, Database> instances 
    = new ConcurrentHashMap<Thread, Database>();
  
  /**
   * @return Instance of the current Database backend. Returns null if an error
   * has occurred.
   */
  public static Database getInstance(boolean create)
    throws SQLException
  {
    if(!instances.containsKey(Thread.currentThread()) && create)
    {
      Database db = new Database();
      db.arise();
      instances.put(Thread.currentThread(), db);
      return db;
    }
    else
    {
      return instances.get(Thread.currentThread());
    }
  }
  
  public static Database getInstance()
    throws SQLException
  {
    return getInstance(true);
  }
  
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
  private PreparedStatement pstmtGetArticle0 = null;
  private PreparedStatement pstmtGetArticle1 = null;
  private PreparedStatement pstmtGetArticleHeaders  = null;
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
  private PreparedStatement pstmtGetPostingsCount      = null;
  private PreparedStatement pstmtGetSubscriptions  = null;
  private PreparedStatement pstmtIsArticleExisting = null;
  private PreparedStatement pstmtIsGroupExisting = null;
  private PreparedStatement pstmtSetConfigValue0 = null;
  private PreparedStatement pstmtSetConfigValue1 = null;
  
  /** How many times the database connection was reinitialized */
  private int restarts = 0;
  
  /**
   * Rises the database: reconnect and recreate all prepared statements.
   * @throws java.lang.SQLException
   */
  private void arise()
    throws SQLException
  {
    try
    {
      // Load database driver
      Class.forName(
              BootstrapConfig.getInstance().get(BootstrapConfig.STORAGE_DBMSDRIVER, "java.lang.Object"));

      // Establish database connection
      this.conn = DriverManager.getConnection(
              BootstrapConfig.getInstance().get(BootstrapConfig.STORAGE_DATABASE, "<not specified>"),
              BootstrapConfig.getInstance().get(BootstrapConfig.STORAGE_USER, "root"),
              BootstrapConfig.getInstance().get(BootstrapConfig.STORAGE_PASSWORD, ""));

      this.conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      if(this.conn.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE)
      {
        Log.msg("Warning: Database is NOT fully serializable!", false);
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
        "flags & " + Group.DELETED + " = 0");
      
      // Prepare statements for method delete(article)
      this.pstmtDeleteArticle0 = conn.prepareStatement(
        "DELETE FROM articles WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");

      // Prepare statements for methods getArticle()
      this.pstmtGetArticle0 = conn.prepareStatement(
        "SELECT * FROM articles  WHERE article_id = " +
        "(SELECT article_id FROM article_ids WHERE message_id = ?)");
      this.pstmtGetArticle1 = conn.prepareStatement(
        "SELECT * FROM articles WHERE article_id = " +
        "(SELECT article_id FROM postings WHERE " +
        "article_index = ? AND group_id = ?)");
      
      // Prepare statement for method getArticleHeaders()
      this.pstmtGetArticleHeaders = conn.prepareStatement(
        "SELECT header_key, header_value FROM headers WHERE article_id = ? " +
        "ORDER BY header_index ASC");
      
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
  public void addArticle(final Article article)
    throws SQLException
  {
    try
    {
      this.conn.setAutoCommit(false);

      int newArticleID = getMaxArticleID() + 1;

      // Fill prepared statement with values;
      // writes body to article table
      pstmtAddArticle1.setInt(1, newArticleID);
      pstmtAddArticle1.setBytes(2, article.getBody().getBytes());
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
        pstmtAddArticle3.setLong(1, group.getID());
        pstmtAddArticle3.setInt(2, newArticleID);
        pstmtAddArticle3.setLong(3, getMaxArticleIndex(group.getID()) + 1);
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
        Log.msg("Rollback of addArticle() failed: " + ex2, false);
      }
      
      try
      {
        this.conn.setAutoCommit(true); // and release locks
      }
      catch(SQLException ex2)
      {
        Log.msg("setAutoCommit(true) of addArticle() failed: " + ex2, false);
      }

      restartConnection(ex);
      addArticle(article);
    }
  }
  
  /**
   * Adds a group to the Database. This method is not accessible via NNTP.
   * @param name
   * @throws java.sql.SQLException
   */
  public void addGroup(String name, int flags)
    throws SQLException
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
      this.conn.rollback();
      this.conn.setAutoCommit(true);
      restartConnection(ex);
      addGroup(name, flags);
    }
  }
  
  public void addEvent(long time, byte type, long gid)
    throws SQLException
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
      this.conn.rollback();
      this.conn.setAutoCommit(true);

      restartConnection(ex);
      addEvent(time, type, gid);
    }
  }
  
  public int countArticles()
    throws SQLException
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
        rs.close();
        restarts = 0;
      }
    }
  }
  
  public int countGroups()
    throws SQLException
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
        rs.close();
        restarts = 0;
      }
    }
  }
  
  public void delete(final String messageID)
    throws SQLException
  {
    try
    {
      this.conn.setAutoCommit(false);
      
      this.pstmtDeleteArticle0.setString(1, messageID);
      int rs = this.pstmtDeleteArticle0.executeUpdate();
      
      // We trust the ON DELETE CASCADE functionality to delete
      // orphaned references
      
      this.conn.commit();
      this.conn.setAutoCommit(true);
    }
    catch(SQLException ex)
    {
      throw ex;
    }
  }
  
  public Article getArticle(String messageID)
    throws SQLException
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
        String body     = new String(rs.getBytes("body"));
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
        rs.close();
        restarts = 0; // Reset error count
      }
    }
  }
  
  /**
   * Retrieves an article by its ID.
   * @param articleID
   * @return
   * @throws java.sql.SQLException
   */
  public Article getArticle(long articleIndex, long gid)
    throws SQLException
  {  
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticle1.setLong(1, articleIndex);
      this.pstmtGetArticle1.setLong(2, gid);

      rs = this.pstmtGetArticle1.executeQuery();

      if(rs.next())
      {
        String body    = new String(rs.getBytes("body"));
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
        rs.close();
        restarts = 0;
      }
    }
  }
  
  public String getArticleHeaders(long articleID)
    throws SQLException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetArticleHeaders.setLong(1, articleID);
      rs = this.pstmtGetArticleHeaders.executeQuery();
      
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
        rs.close();
    }
  }
  
  public long getArticleIndex(Article article, Group group)
    throws SQLException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticleIndex.setString(1, article.getMessageID());
      this.pstmtGetArticleIndex.setLong(2, group.getID());
      
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
        rs.close();
    }
  }
  
  /**
   * Returns a list of Long/Article Pairs.
   * @throws java.sql.SQLException
   */
  public List<Pair<Long, ArticleHead>> getArticleHeads(Group group, int first, int last)
    throws SQLException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetArticleHeads.setLong(1, group.getID());
      this.pstmtGetArticleHeads.setInt(2, first);
      this.pstmtGetArticleHeads.setInt(3, last);
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
        rs.close();
    }
  }
  
  public List<Long> getArticleNumbers(long gid)
    throws SQLException
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
        rs.close();
        restarts = 0; // Clear the restart count after successful request
      }
    }
  }
  
  public String getConfigValue(String key)
    throws SQLException
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
        rs.close();
        restarts = 0; // Clear the restart count after successful request
      }
    }
  }
  
  public int getEventsCount(byte type, long start, long end, Group group)
    throws SQLException
  {
    ResultSet rs = null;
    
    try
    {
      if(group == null)
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
        this.pstmtGetEventsCount1.setLong(4, group.getID());
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
      return getEventsCount(type, start, end, group);
    }
    finally
    {
      if(rs != null)
        rs.close();
    }
  }
  
  /**
   * Reads all Groups from the Database.
   * @return
   * @throws java.sql.SQLException
   */
  public List<Group> getGroups()
    throws SQLException
  {
    ResultSet   rs;
    List<Group> buffer = new ArrayList<Group>();
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
        stmt.close(); // Implicitely closes ResultSets
    }
  }
  
  public String getGroupForList(InternetAddress listAddress)
    throws SQLException
  {
    ResultSet rs = null;
    
    try
    {
      this.pstmtGetGroupForList.setString(1, listAddress.getAddress());

      rs = this.pstmtGetGroupForList.executeQuery();
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
      return getGroupForList(listAddress);
    }
    finally
    {
      if(rs != null)
        rs.close();
    }
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
        rs.close();
    }
  }
  
  public String getListForGroup(String group)
    throws SQLException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetListForGroup.setString(1, group);
      rs = this.pstmtGetListForGroup.executeQuery();
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
      return getListForGroup(group);
    }
    finally
    {
      if(rs != null)
        rs.close();
    }
  }
  
  private int getMaxArticleIndex(long groupID)
    throws SQLException
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
        rs.close();
    }
  }
  
  private int getMaxArticleID()
    throws SQLException
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
        rs.close();
    }
  }
  
  public int getLastArticleNumber(Group group)
    throws SQLException
  {
    ResultSet rs = null;

    try
    {
      this.pstmtGetLastArticleNumber.setLong(1, group.getID());
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
        rs.close();
    }
  }
  
  public int getFirstArticleNumber(Group group)
    throws SQLException
  {
    ResultSet rs = null;
    try
    {
      this.pstmtGetFirstArticleNumber.setLong(1, group.getID());
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
        rs.close();
    }
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
        rs.close();
    }
  }
  
  public double getNumberOfEventsPerHour(int key, long gid)
    throws SQLException
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
      return getNumberOfEventsPerHour(key, gid);
    }
    finally
    {
      if(stmt != null)
      {
        stmt.close();
      }
      
      if(rs != null)
      {
        rs.close();
      }
    }
  }
  
  public int getPostingsCount(String groupname)
    throws SQLException
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
        Log.msg("Warning: Count on postings return nothing!", true);
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
        rs.close();
    }
  }
  
  public List<Subscription> getSubscriptions(int feedtype)
    throws SQLException
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
        rs.close();
    }
  }

  /**
   * Checks if there is an article with the given messageid in the Database.
   * @param name
   * @return
   * @throws java.sql.SQLException
   */
  public boolean isArticleExisting(String messageID)
    throws SQLException
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
        rs.close();
    }
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
        rs.close();
    }
  }
  
  public void setConfigValue(String key, String value)
    throws SQLException
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
   * Closes the Database connection.
   */
  public void shutdown()
    throws SQLException
  {
    if(this.conn != null)
    {
      this.conn.close();
    }
  }
  
  private void restartConnection(SQLException cause)
    throws SQLException
  {
    restarts++;
    Log.msg(Thread.currentThread() 
      + ": Database connection was closed (restart " + restarts + ").", false);
    
    if(restarts >= MAX_RESTARTS)
    {
      // Delete the current, probably broken Database instance.
      // So no one can use the instance any more.
      Database.instances.remove(Thread.currentThread());
      
      // Throw the exception upwards
      throw cause;
    }
    
    try
    {
      Thread.sleep(1500L * restarts);
    }
    catch(InterruptedException ex)
    {
      Log.msg("Interrupted: " + ex.getMessage(), false);
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
      Log.msg(ex.getMessage(), true);
    }
    
    try
    {
      // Try to reinitialize database connection
      arise();
    }
    catch(SQLException ex)
    {
      Log.msg(ex.getMessage(), true);
      restartConnection(ex);
    }
  }

}
