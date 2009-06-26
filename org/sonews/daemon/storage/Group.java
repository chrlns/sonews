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

import java.sql.SQLException;
import java.util.List;
import org.sonews.util.Log;
import org.sonews.util.Pair;

/**
 * Represents a logical Group within this newsserver.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Group
{

  /** 
   * If this flag is set the Group is no real newsgroup but a mailing list
   * mirror. In that case every posting and receiving mails must go through
   * the mailing list gateway.
   */
  public static final int MAILINGLIST = 0x1;
  
  /**
   * If this flag is set the Group is marked as readonly and the posting
   * is prohibited. This can be useful for groups that are synced only in
   * one direction.
   */
  public static final int READONLY    = 0x2;

  /**
   * If this flag is set the Group is marked as deleted and must not occur
   * in any output. The deletion is done lazily by a low priority daemon.
   */
  public static final int DELETED     = 0x128;
  
  private long   id     = 0;
  private int    flags  = -1;
  private String name   = null;
  
  /**
   * Returns a Group identified by its full name.
   * @param name
   * @return
   */
  public static Group getByName(final String name)
  {
    try
    {
      return Database.getInstance().getGroup(name);
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * @return List of all groups this server handles.
   */
  public static List<Group> getAll()
  {
    try
    {
      return Database.getInstance().getGroups();
    }
    catch(SQLException ex)
    {
      Log.msg(ex.getMessage(), false);
      return null;
    }
  }
  
  /**
   * Private constructor.
   * @param name
   * @param id
   */
  Group(final String name, final long id, final int flags)
  {
    this.id    = id;
    this.flags = flags;
    this.name  = name;
  }

  @Override
  public boolean equals(Object obj)
  {
    if(obj instanceof Group)
    {
      return ((Group)obj).id == this.id;
    }
    else
    {
      return false;
    }
  }
    
  public List<Pair<Long, ArticleHead>> getArticleHeads(final int first, final int last)
    throws SQLException
  {
    return Database.getInstance().getArticleHeads(this, first, last);
  }
  
  public List<Long> getArticleNumbers()
    throws SQLException
  {
    return Database.getInstance().getArticleNumbers(id);
  }

  public int getFirstArticleNumber()
    throws SQLException
  {
    return Database.getInstance().getFirstArticleNumber(this);
  }

  /**
   * Returns the group id.
   */
  public long getID()
  {
    assert id > 0;

    return id;
  }
  
  public boolean isMailingList()
  {
    return (this.flags & MAILINGLIST) != 0;
  }

  public int getLastArticleNumber()
    throws SQLException
  {
    return Database.getInstance().getLastArticleNumber(this);
  }

  public String getName()
  {
    return name;
  }

  /**
   * Performs this.flags |= flag to set a specified flag and updates the data
   * in the Database.
   * @param flag
   */
  public void setFlag(final int flag)
  {
    this.flags |= flag;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * @return Number of posted articles in this group.
   * @throws java.sql.SQLException
   */
  public int getPostingsCount()
    throws SQLException
  {
    return Database.getInstance().getPostingsCount(this.name);
  }

}
