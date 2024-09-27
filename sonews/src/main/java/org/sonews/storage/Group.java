/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sonews.util.Log;
import org.sonews.util.Pair;

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
    public static final int MAILINGLIST = 1;

    /**
     * If this flag is set the Group is marked as readonly and the posting is
     * prohibited. This can be useful for groups that are synced only in one
     * direction.
     */
    public static final int READONLY = 2;

    /**
     * If this flag is set the Group is considered private and only visible to
     * clients of the private clients access list.
     */
    public static final int PRIVATE = 4;

    /**
     * If this flag is set the Group is local to this server, i.e., the news
     * are not peered.
     */
    public static final int LOCALE = 8;

    /**
     * If this flag is set the Group is marked as deleted and must not occur in
     * any output. The deletion is done lazily by a low priority daemon.
     */
    public static final int DELETED = 0x80;

    private static List<Group> allGroups = new ArrayList<>();
    private static Map<String, Group> allGroupNames = new HashMap<>();

    private static Group parseGroup(String str) {
        str = str.trim();
        String[] groupLineChunks = str.split("\\s+");
        if (groupLineChunks.length != 3) {
            Log.get().log(Level.WARNING, "Malformed group.conf line: {0}", str);
            return null;
        } else {
            Log.get().log(Level.INFO, "Found group {0}", groupLineChunks[0]);
            Group group = new Group(
                    groupLineChunks[0],
                    Long.parseLong(groupLineChunks[1]),
                    Integer.parseInt(groupLineChunks[2]));
            return group;
        }
    }

    /**
     * Reads and parses the groups.conf file if not done yet and returns
     * a list of loaded Group objects.
     *
     * If groups.conf cannot be read an empty list is returned, never null.
     *
     * @return List of all groups this server handles.
     */
    public static synchronized List<Group> getAll() {
        if (allGroups.isEmpty()) {
            try {
                allGroups = Files.lines(Paths.get("groups.conf"))
                        .filter(l -> !l.startsWith("#"))
                        .map(Group::parseGroup)
                        .filter(g -> g != null)
                        .collect(Collectors.toList());
            } catch (IOException ex) {
                Log.get().log(Level.WARNING, "Could not read groups.conf", ex);
                return allGroups;
            }

            allGroupNames = allGroups.stream().collect(
                    Collectors.toMap(Group::getName, Function.identity()));
        }
        return allGroups;
    }

    public static Group get(String name) {
        if(allGroups.isEmpty()) {
            getAll();
        }
        return allGroupNames.get(name);
    }


    private long id = 0;
    private int flags = -1;
    private String name = null;

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
        if (obj instanceof Group group) {
            return group.id == this.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (name + id).hashCode();
    }

    /**
     *
     * @param idx
     * @return
     * @throws StorageBackendException
     */
    public Article getArticle(long idx) throws StorageBackendException {
        return StorageManager.current().getArticle(idx, this.id);
    }

    public List<Pair<Long, Article>> getArticleHeads(final long first,
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
