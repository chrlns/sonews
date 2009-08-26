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

import java.util.List;
import org.sonews.feed.Subscription;
import org.sonews.util.Pair;

/**
 * A generic storage backend interface.
 * @author Christian Lins
 * @since sonews/1.0
 */
public interface Storage
{

  /**
   * Stores the given Article in the storage.
   * @param art
   * @throws StorageBackendException
   */
  void addArticle(Article art)
    throws StorageBackendException;

  void addEvent(long timestamp, int type, long groupID)
    throws StorageBackendException;

  void addGroup(String groupname, int flags)
    throws StorageBackendException;

  int countArticles()
    throws StorageBackendException;

  int countGroups()
    throws StorageBackendException;

  void delete(String messageID)
    throws StorageBackendException;

  Article getArticle(String messageID)
    throws StorageBackendException;

  Article getArticle(long articleIndex, long groupID)
    throws StorageBackendException;

  List<Pair<Long, ArticleHead>> getArticleHeads(Group group, long first, long last)
    throws StorageBackendException;

  List<Pair<Long, String>> getArticleHeaders(Channel channel, long start, long end,
    String header, String pattern)
    throws StorageBackendException;

  long getArticleIndex(Article art, Group group)
    throws StorageBackendException;

  List<Long> getArticleNumbers(long groupID)
    throws StorageBackendException;

  String getConfigValue(String key)
    throws StorageBackendException;

  int getEventsCount(int eventType, long startTimestamp, long endTimestamp,
    Channel channel)
    throws StorageBackendException;

  double getEventsPerHour(int key, long gid)
    throws StorageBackendException;

  int getFirstArticleNumber(Group group)
    throws StorageBackendException;

  Group getGroup(String name)
    throws StorageBackendException;

  List<Channel> getGroups()
    throws StorageBackendException;

  /**
   * Retrieves the collection of groupnames that are associated with the
   * given list address.
   * @param inetaddress
   * @return
   * @throws StorageBackendException
   */
  List<String> getGroupsForList(String listAddress)
    throws StorageBackendException;

  int getLastArticleNumber(Group group)
    throws StorageBackendException;

  /**
   * Returns a list of email addresses that are related to the given
   * groupname. In most cases the list may contain only one entry.
   * @param groupname
   * @return
   * @throws StorageBackendException
   */
  List<String> getListsForGroup(String groupname)
    throws StorageBackendException;

  String getOldestArticle()
    throws StorageBackendException;

  int getPostingsCount(String groupname)
    throws StorageBackendException;

  List<Subscription> getSubscriptions(int type)
    throws StorageBackendException;

  boolean isArticleExisting(String messageID)
    throws StorageBackendException;

  boolean isGroupExisting(String groupname)
    throws StorageBackendException;

  void purgeGroup(Group group)
    throws StorageBackendException;

  void setConfigValue(String key, String value)
    throws StorageBackendException;

  /**
   * Updates headers and channel references of the given article.
   * @param article
   * @return
   * @throws StorageBackendException
   */
  boolean update(Article article)
    throws StorageBackendException;

  boolean update(Group group)
    throws StorageBackendException;

}
