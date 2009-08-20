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

package org.sonews.storage;

import java.util.ArrayList;
import java.util.List;
import org.sonews.util.Pair;

/**
 * A logical communication Channel is the a generic structural element for sets
 * of messages; e.g. a Newsgroup for a set of Articles.
 * A Channel can either be a real set of messages or an aggregated set of
 * several subsets.
 * @author Christian Lins
 * @since sonews/1.0
 */
public abstract class Channel
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
  public static final int DELETED     = 0x80;

  public static List<Channel> getAll()
  {
    List<Channel> all = new ArrayList<Channel>();

    /*List<Channel> agroups = AggregatedGroup.getAll();
    if(agroups != null)
    {
      all.addAll(agroups);
    }*/

    List<Channel> groups = Group.getAll();
    if(groups != null)
    {
      all.addAll(groups);
    }

    return all;
  }

  public static Channel getByName(String name)
  {
    return Group.getByName(name);
  }

  public abstract Article getArticle(long idx)
    throws StorageBackendException;

  public abstract List<Pair<Long, ArticleHead>> getArticleHeads(
    final long first, final long last)
    throws StorageBackendException;

  public abstract List<Long> getArticleNumbers()
    throws StorageBackendException;

  public abstract long getFirstArticleNumber()
    throws StorageBackendException;

  public abstract long getIndexOf(Article art)
    throws StorageBackendException;

  public abstract long getInternalID();

  public abstract long getLastArticleNumber()
    throws StorageBackendException;

  public abstract String getName();
  
  public abstract long getPostingsCount()
    throws StorageBackendException;

  public abstract boolean isDeleted();

  public abstract boolean isWriteable();

}
