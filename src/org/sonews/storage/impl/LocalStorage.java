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

import java.util.HashMap;
import java.util.List;

import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;

/**
 * Local file system storage.
 * @since sonews/1.1
 * @author Christian Lins
 */
public class LocalStorage implements Storage {

	private HashMap<String, Article> articles = new HashMap<String, Article>();
	private HashMap<String, Integer> groups = new HashMap<String, Integer>();
	
	@Override
	public void addArticle(Article art) throws StorageBackendException {
		// TODO Auto-generated method stub

	}

	/**
	 * Not implemented yet.
	 */
	@Override
	public void addEvent(long timestamp, int type, long groupID)
			throws StorageBackendException {
	}

	@Override
	public void addGroup(String groupname, int flags)
			throws StorageBackendException {
		this.groups.put(groupname, flags);
	}

	@Override
	public int countArticles() throws StorageBackendException {
		return this.articles.size();
	}

	@Override
	public int countGroups() throws StorageBackendException {
		return this.groups.size();
	}

	/**
	 * Not implemented yet.
	 * @param messageID
	 * @throws StorageBackendException
	 */
	@Override
	public void delete(String messageID) throws StorageBackendException {
	}

	@Override
	public Article getArticle(String messageID) throws StorageBackendException {
		return this.articles.get(messageID);
	}

	@Override
	public Article getArticle(long articleIndex, long groupID)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Pair<Long, ArticleHead>> getArticleHeads(Group group,
			long first, long last) throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Pair<Long, String>> getArticleHeaders(Group group, long start,
			long end, String header, String pattern)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getArticleIndex(Article art, Group group)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Long> getArticleNumbers(long groupID)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Not yet supported.
	 * @param key
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public String getConfigValue(String key) throws StorageBackendException {
		return null;
	}

	/**
	 * Not yet supported.
	 * @param eventType
	 * @param startTimestamp
	 * @param endTimestamp
	 * @param group
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public int getEventsCount(int eventType, long startTimestamp,
			long endTimestamp, Group group) throws StorageBackendException {
		return 0;
	}

	/**
	 * Not yet supported.
	 * @param key
	 * @param gid
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public double getEventsPerHour(int key, long gid)
			throws StorageBackendException {
		return 0;
	}

	@Override
	public int getFirstArticleNumber(Group group)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Group getGroup(String name) throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Group> getGroups() throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Not yet supported.
	 * @param listAddress
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public List<String> getGroupsForList(String listAddress)
			throws StorageBackendException {
		return null;
	}

	@Override
	public int getLastArticleNumber(Group group) throws StorageBackendException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Not yet supported.
	 * @param groupname
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public List<String> getListsForGroup(String groupname)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Not yet supported.
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public String getOldestArticle() throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPostingsCount(String groupname)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Not yet supported.
	 * @param type
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public List<Subscription> getSubscriptions(int type)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isArticleExisting(String messageID)
			throws StorageBackendException {
		return this.articles.containsKey(messageID);
	}

	@Override
	public boolean isGroupExisting(String groupname)
			throws StorageBackendException {
		return this.groups.containsKey(groupname);
	}

	/**
	 * Not yet supported.
	 * @param group
	 * @throws StorageBackendException
	 */
	@Override
	public void purgeGroup(Group group) throws StorageBackendException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setConfigValue(String key, String value)
			throws StorageBackendException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean update(Article article) throws StorageBackendException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Group group) throws StorageBackendException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Not yet supported.
	 * @param username
	 * @param password
	 * @return
	 * @throws StorageBackendException
	 */
	@Override
	public boolean authenticateUser(String username, char[] password)
			throws StorageBackendException {
		// TODO Auto-generated method stub
		return false;
	}

}
