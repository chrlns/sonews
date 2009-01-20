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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.so.news.Config;
import com.so.news.Debug;

/**
 * Represents a newsgroup article.
 * @author Christian Lins
 * @author Denis Schwerdel
 */
public class Article
{
  /**
   * Loads the Article identified by the given ID from the Database.
   * @param messageID
   * @return null if Article is not found or if an error occurred.
   */
  public static Article getByMessageID(String messageID)
  {
    try
    {
      return Database.getInstance().getArticle(messageID);
    }
    catch(SQLException ex)
    {
      ex.printStackTrace(Debug.getInstance().getStream());
      return null;
    }
  }
  
  public static Article getByNumberInGroup(Group group, int number)
    throws SQLException
  {
    long gid = group.getID(); 
    return Database.getInstance().getArticle(gid, number); // Is number her correct?
  }
  
  private String              body      = "";
  private long                groupID   = -1;
  private Map<String, String> header    = new HashMap<String, String>();
  private int                 numberInGroup = -1;
  private String              msgID     = null;
  
  /**
   * Default constructor.
   */
  public Article()
  {
  }
  
  /**
   * Creates a new Article object using the date from the given
   * ResultSet. It is expected that ResultSet.next() was already
   * called by the Database class.
   * This construction has only package visibility.
   * @param rs
   */
  Article(ResultSet rs)
    throws SQLException
  {
    this.body  = rs.getString("body");
    this.msgID = rs.getString("message_id");
    
    // Parse the header
    parseHeader(rs.getString("header"));
  }
  
  /**
   * Parses the header fields and puts them into a map for faster access.
   * TODO: There could be fields that go over more than one line, some
   * bad clients do create them.
   * @param hsrc
   */
  private void parseHeader(String hsrc)
  {
    String[] lines = hsrc.split("\n");
    
    for(String line : lines)
    {
      String[] kv = line.split(":");
      if(kv.length < 2)
      {
        Debug.getInstance().log("Invalid header field: " + line);
        continue;
      }
      else
      {
        // Set value in the header hash map
        String value = kv[1];
        for(int n = 2; n < kv.length; n++)
          value += ":" + kv[n];
        this.header.put(kv[0], value);
      }
    }
  }
  
  /**
   * Returnes the next Article in the group of this Article.
   * @return
   */
  public Article nextArticleInGroup()
  {
    return null;
  }

  /**
   * Returns the previous Article in the group of this Article.
   * @return
   */
  public Article prevArticleInGroup()
  {
    return null;
  }

  /**
   * Generates a message id for this article and sets it into
   * the header HashMap.
   */
  private String generateMessageID()
  {
    this.msgID = "<" + UUID.randomUUID() + "@"
        + Config.getInstance().get("n3tpd.hostname", "localhost") + ">";
    
    this.header.put("Message-ID", msgID);
    
    return msgID;
  }

  /**
   * Tries to delete this article.
   * @return false if the article could not be deleted, otherwise true
   */
  public boolean delete()
  {
    return false;
  }
  
  /**
   * Checks if all necessary header fields are within this header.
   */
  private void validateHeader()
  {    
    // Forces a MessageID creation if not existing
    getMessageID();
    
    // Check if the references are correct...
    String rep = header.get("In-Reply-To");
    if(rep == null) // Some clients use only references instead of In-Reply-To
      return; //rep = header.get("References");
    
    String ref = getMessageID();
    
    if(rep != null && !rep.equals(""))
    {
      Article art = null; //TODO // getByMessageID(rep, articleDir);
      if(art != null)
      {
        ref = art.header.get("References") + " " + rep;
      }
    }
    header.put("References", ref);
  }

  /**
   * Returns the body string.
   */
  public String getBody()
  {
    return body;
  }
  
  /**
   * @return Numerical ID of the associated Group.
   */
  long getGroupID()
  {
    if(groupID == -1) // If the GroupID was not determined yet
    {
      // Determining GroupID
      String   newsgroups = this.header.get("Newsgroups");
      if(newsgroups != null)
      {
        String[] newsgroup  = newsgroups.split(",");
        // Crossposting is not supported
        try
        {
          Group group;
          if(newsgroup.length > 0)
            group = Database.getInstance().getGroup(newsgroup[0].trim());
          else
            group = Database.getInstance().getGroup(newsgroups.trim());
          // TODO: What to do if Group does not exist?
          this.groupID = group.getID();
        }
        catch(SQLException ex)
        {
          ex.printStackTrace(Debug.getInstance().getStream());
          System.err.println(ex.getLocalizedMessage());
        }
      }
      else
        System.err.println("Should never happen: Article::getGroupID");
    }
    return this.groupID;
  }

  public void setBody(String body)
  {
    this.body = body;
  }

  public int getNumberInGroup()
  {
    return this.numberInGroup;
  }
  
  public void setHeader(HashMap<String, String> header)
  {
    this.header = header;
  }

  public void setNumberInGroup(int id)
  {
    this.numberInGroup = id;
  }

  public String getMessageID()
  {
    if(msgID == null)
      msgID = generateMessageID();
    return msgID;
  }

  /**
   * @return Header source code of this Article.
   */
  public String getHeaderSource()
  {
    StringBuffer buf = new StringBuffer();
    
    for(Entry<String, String> entry : this.header.entrySet())
    {
      buf.append(entry.getKey());
      buf.append(":");
      buf.append(entry.getValue());
      buf.append("\n");
    }
    
    return buf.toString();
  }
  
  public Map<String, String> getHeader()
  {
    return this.header;
  }
  
  public Date getDate()
  {
    try
    {
      String date = this.header.get("Date");
      return new Date(Date.parse(date));
    }
    catch(Exception e)
    {
      e.printStackTrace(Debug.getInstance().getStream());
      return null;
    }
  }

  public void setDate(Date date)
  {
    this.header.put("Date", date.toString());
  }
  
  @Override
  public String toString()
  {
    return getMessageID();
  }
}
