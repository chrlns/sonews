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
import org.sonews.util.Log;
import org.sonews.util.Pair;

/**
 * Represents a logical Group within this newsserver.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Group {

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
	public static final int READONLY = 0x2;

	/**
	 * If this flag is set the Group is marked as deleted and must not occur
	 * in any output. The deletion is done lazily by a low priority daemon.
	 */
	public static final int DELETED = 0x80;

	private long id = 0;
	private int flags = -1;
	private String name = null;

	/**
	 * @return List of all groups this server handles.
	 */
	public static List<Group> getAll() {
		try {
			return StorageManager.current().getGroups();
		} catch (StorageBackendException ex) {
			Log.get().severe(ex.toString());
			return null;
		}
	}

	/**
	 * Constructor.
	 * @param name
	 * @param id
	 * @param flags
	 */
	public Group(final String name, final long id, final int flags) {
		this.id = id;
		this.flags = flags;
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			return ((Group) obj).id == this.id;
		} else {
			return false;
		}
	}

	public Article getArticle(long idx)
			throws StorageBackendException {
		return StorageManager.current().getArticle(idx, this.id);
	}

	public List<Pair<Long, ArticleHead>> getArticleHeads(final long first, final long last)
			throws StorageBackendException {
		return StorageManager.current().getArticleHeads(this, first, last);
	}

	public List<Long> getArticleNumbers()
			throws StorageBackendException {
		return StorageManager.current().getArticleNumbers(id);
	}

	public long getFirstArticleNumber()
			throws StorageBackendException {
		return StorageManager.current().getFirstArticleNumber(this);
	}

	public int getFlags() {
		return this.flags;
	}

	public long getIndexOf(Article art)
			throws StorageBackendException {
		return StorageManager.current().getArticleIndex(art, this);
	}

	/**
	 * Returns the group id.
	 */
	public long getInternalID() {
		assert id > 0;
		return id;
	}

	public boolean isDeleted() {
		return (this.flags & DELETED) != 0;
	}

	public boolean isMailingList() {
		return (this.flags & MAILINGLIST) != 0;
	}

	public boolean isWriteable() {
		return true;
	}

	public long getLastArticleNumber()
			throws StorageBackendException {
		return StorageManager.current().getLastArticleNumber(this);
	}

	public String getName() {
		return name;
	}

	/**
	 * Performs this.flags |= flag to set a specified flag and updates the data
	 * in the JDBCDatabase.
	 * @param flag
	 */
	public void setFlag(final int flag) {
		this.flags |= flag;
	}

	public void unsetFlag(final int flag) {
		this.flags &= ~flag;
	}

	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return Number of posted articles in this group.
	 * @throws java.sql.SQLException
	 */
	public long getPostingsCount()
			throws StorageBackendException {
		return StorageManager.current().getPostingsCount(this.name);
	}

	/**
	 * Updates flags and name in the backend.
	 */
	public void update()
			throws StorageBackendException {
		StorageManager.current().update(this);
	}
}
