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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sonews.util.Log;
import org.sonews.util.Pair;
import org.sonews.util.io.Resource;

/**
 * Represents a logical Group within this newsserver.
 *
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
     * If this flag is set the Group is marked as readonly and the posting is
     * prohibited. This can be useful for groups that are synced only in one
     * direction.
     */
    public static final int READONLY = 0x2;

    /**
     * If this flag is set the Group is marked as deleted and must not occur in
     * any output. The deletion is done lazily by a low priority daemon.
     */
    public static final int DELETED = 0x80;

    private static List<Group> allGroups = null;
    private static final Object allGroupsGate = new Object();
    private static final Map<String, Group> allGroupNames = new HashMap<>();

    private long id = 0;
    private int flags = -1;
    private String name = null;

    /**
     * @return List of all groups this server handles.
     */
    public static List<Group> getAll() {
        synchronized(allGroupsGate) {
            if(allGroups == null) {
                String groupsStr = Resource.getAsString("groups.conf", true);
                if(groupsStr == null) {
                    Log.get().log(Level.WARNING, "Could not read groups.conf");
                    return null;
                }

                allGroups = new ArrayList<>();
                String[] groupLines = groupsStr.split("\n");
                for(String groupLine : groupLines) {
                    if(groupLine.startsWith("#")) {
                        continue;
                    }

                    groupLine = groupLine.trim();
                    String[] groupLineChunks = groupLine.split("\\s+");
                    if(groupLineChunks.length != 3) {
                        Log.get().log(Level.WARNING, "Malformed group.conf line: {0}", groupLine);
                    } else {
                        Log.get().log(Level.INFO, "Found group {0}", groupLineChunks[0]);
                        Group group = new Group(
                                groupLineChunks[0],
                                Long.parseLong(groupLineChunks[1]),
                                Integer.parseInt(groupLineChunks[2]));
                        allGroups.add(group);
                        allGroupNames.put(groupLineChunks[0], group);
                    }
                }
            }
            return allGroups;
        }
    }

    public static Group get(String name) {
        if(allGroups == null) {
            getAll();
        }

        synchronized(allGroupNames) {
            return allGroupNames.get(name);
        }
    }

    /**
     * Constructor.
     *
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

    @Override
    public int hashCode() {
        return (name + id).hashCode();
    }

    public Article getArticle(long idx) throws StorageBackendException {
        return StorageManager.current().getArticle(idx, this.id);
    }

    public List<Pair<Long, ArticleHead>> getArticleHeads(final long first,
            final long last) throws StorageBackendException {
        return StorageManager.current().getArticleHeads(this, first, last);
    }

    public List<Long> getArticleNumbers() throws StorageBackendException {
        return StorageManager.current().getArticleNumbers(id);
    }

    public long getFirstArticleNumber() throws StorageBackendException {
        return StorageManager.current().getFirstArticleNumber(this);
    }

    public int getFlags() {
        return this.flags;
    }

    public long getIndexOf(Article art) throws StorageBackendException {
        return StorageManager.current().getArticleIndex(art, this);
    }

    /**
     * @return Internal group id used for referencing in the backend
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

    public long getLastArticleNumber() throws StorageBackendException {
        return StorageManager.current().getLastArticleNumber(this);
    }

    public String getName() {
        return name;
    }

    /**
     * Performs this.flags |= flag to set a specified flag and updates the data
     * in the JDBCDatabase.
     *
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
     * @throws StorageBackendException
     */
    public long getPostingsCount() throws StorageBackendException {
        return StorageManager.current().getPostingsCount(this.name);
    }

}
