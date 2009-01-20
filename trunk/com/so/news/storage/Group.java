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
import java.util.ArrayList;
import java.util.List;
import com.so.news.Debug;

/**
 * Represents a logical Group within this newsserver.
 * @author Christian Lins
 */
public class Group
{
  private long   id;
  private String name;

  /**
   * Private constructor.
   * @param name
   * @param id
   */
  Group(String name, long id)
  {
    this.id   = id;
    this.name = name;
  }
  
  /**
   * Returns a Group identified by its full name.
   * @param name
   * @return
   */
  public static Group getByName(String name)
  {
    try
    {
      return Database.getInstance().getGroup(name);
    }
    catch(SQLException ex)
    {
      System.err.println(ex.getLocalizedMessage());
      ex.printStackTrace(Debug.getInstance().getStream());
      return null;
    }
  }

  /**
   * Returns a list of all groups this server handles.
   * @return
   */
  public static ArrayList<Group> getAll()
  {
    ArrayList<Group> buffer = new ArrayList<Group>();
    
    try
    {
      ResultSet rs = Database.getInstance().getGroups();
      
      while(rs.next())
      {
        String name = rs.getString("name");
        long   id   = rs.getLong("group_id");
        
        Group group = new Group(name, id);
        buffer.add(group);
      }
    }
    catch(SQLException ex)
    {
      ex.printStackTrace(Debug.getInstance().getStream());
      System.err.println(ex.getLocalizedMessage());
    }
    
    return buffer;
  }

  public List<Article> getAllArticles()
    throws SQLException
  {
    return getAllArticles(getFirstArticle(), getLastArticle());
  }

  public List<Article> getAllArticles(int first, int last)
  {
    return null;
  }

  public int getFirstArticle()
    throws SQLException
  {
    return Database.getInstance().getFirstArticleNumber(this);
  }

  public long getID()
  {
    return id;
  }

  public int getLastArticle()
    throws SQLException
  {
    return Database.getInstance().getLastArticleNumber(this);
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public int getEstimatedArticleCount()
    throws SQLException
  {
    if (getLastArticle() < getFirstArticle())
      return 0;
    return getLastArticle() - getFirstArticle() + 1;
  }

}
