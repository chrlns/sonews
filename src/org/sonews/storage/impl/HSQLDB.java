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

import java.util.List;
import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Channel;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;

/**
 *
 * @author Christian Lins
 * @since sonews/1.1
 */
public class HSQLDB implements Storage {

	public void addArticle(Article art) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void addEvent(long timestamp, int type, long groupID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void addGroup(String groupname, int flags) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int countArticles() throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int countGroups() throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void delete(String messageID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Article getArticle(String messageID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Article getArticle(long articleIndex, long groupID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<Pair<Long, String>> getArticleHeaders(Channel channel, long start, long end, String header, String pattern) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<Pair<Long, ArticleHead>> getArticleHeads(Group group, long first, long last) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getArticleIndex(Article art, Group group) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<Long> getArticleNumbers(long groupID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getConfigValue(String key) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getEventsCount(int eventType, long startTimestamp, long endTimestamp, Channel channel) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public double getEventsPerHour(int key, long gid) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getFirstArticleNumber(Group group) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Group getGroup(String name) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<Channel> getGroups() throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<String> getGroupsForList(String listAddress) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getLastArticleNumber(Group group) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<String> getListsForGroup(String groupname) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getOldestArticle() throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getPostingsCount(String groupname) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public List<Subscription> getSubscriptions(int type) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isArticleExisting(String messageID) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isGroupExisting(String groupname) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void purgeGroup(Group group) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setConfigValue(String key, String value) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean update(Article article) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean update(Group group) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
