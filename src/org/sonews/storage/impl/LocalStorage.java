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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Log;
import org.sonews.util.Pair;

/**
 * Local file system storage. LocalStorage is a simple method for storing news
 * in the local filesystem of the server running SONEWS. The news saved one file
 * per news and are properly indexed for faster access. Although it is not
 * recommended to use LocalStorage for large installations as the performance
 * will decrease with growing numbers of news stored. Additionally, there are
 * hard limits dependending on the underlying OS and filesystem.
 *
 * Directory structure: $BASE$: Base directory of the LocalStorage, e.g.
 * /var/share/sonews/stor0 $BASE$/news/: contains the news mails, one file per
 * news named by its Message-ID $BASE$/index/: contains index files referencing
 * the files in ../news
 *
 * @since sonews/1.1
 * @author Christian Lins
 */
public class LocalStorage implements Storage {

    /** Memory cache of loaded articles. Key is the Message-ID of the articles */
    private final Map<String, Article> articles = new HashMap<String, Article>();

    /** Map<Groupname, Groupflags> */
    private final Map<String, Integer> groups = new HashMap<String, Integer>();

    /** Map<Groupname, Map<Art. Idx. in Group, Message-ID>> */
    private final Map<String, Map<Integer, String>> groupArtIdxMsgID = new HashMap<String, Map<Integer, String>>();

    private String base;

    public LocalStorage(String base) {
        this.base = base;
        if (!this.base.endsWith("/")) {
            this.base += "/";
        }

        // Load groups
        readGroupsFile();

        // Build news indices
        buildIndices();
    }

    private void buildIndices() {

    }

    private String friendlyID(String id) {
        return id.substring(1, id.length() - 1);
    }

    private void writeGroupsFile() {
        try {
            File file = new File(base + "groups");
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf;
            for (Entry<String, Integer> entry : this.groups.entrySet()) {
                buf = entry.getKey().getBytes("UTF-8");
                out.write(buf);
                out.write(";".getBytes("UTF-8"));
                buf = Integer.toString(entry.getValue()).getBytes("UTF-8");
                out.write(buf);
                out.write("\n".getBytes("UTF-8"));
            }
            out.flush();
            out.close();
        } catch (IOException ex) {
            Log.get().log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    private void readGroupsFile() {
        try {
            File file = new File(base + "groups");
            if (file.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), Charset.forName("UTF-8")));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] entry = line.split(";");
                    this.groups.put(entry[0], Integer.parseInt(entry[1]));
                }
                in.close();
            }
        } catch (IOException ex) {
            Log.get().log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void addArticle(Article art) throws StorageBackendException {
        try {
            synchronized (this) {
                String mid = friendlyID(art.getMessageID());
                // Write body and header of Article in separate files on disk
                File file = new File(base + "news/" + mid + ".body");
                FileOutputStream out = new FileOutputStream(file);
                out.write(art.getBody());
                out.flush();
                out.close();

                file = new File(base + "news/" + mid + ".head");
                out = new FileOutputStream(file);
                out.write(art.getHeaderSource().getBytes("UTF-8"));
                out.flush();
                out.close();

                // Add Article info to in memory cache
                this.articles.put(mid, art);
            }
        } catch (IOException ex) {
            throw new StorageBackendException(ex);
        }
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
        synchronized (this) {
            this.groups.put(groupname, flags);
            writeGroupsFile();
        }
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
     *
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
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public List<Pair<Long, ArticleHead>> getArticleHeads(Group group,
            long first, long last) throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public List<Pair<Long, String>> getArticleHeaders(Group group, long start,
            long end, String header, String pattern)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public long getArticleIndex(Article art, Group group)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public List<Long> getArticleNumbers(long groupID)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    /**
     * Not yet supported.
     *
     * @param key
     * @return Always null
     * @throws StorageBackendException
     */
    @Override
    public String getConfigValue(String key) throws StorageBackendException {
        return null;
    }

    /**
     * Not yet supported.
     *
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
     *
     * @param key
     * @param gid
     * @return
     * @throws StorageBackendException
     */
    @Override
    public double getEventsPerHour(int key, long gid)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public int getFirstArticleNumber(Group group)
            throws StorageBackendException {
        int firstArtNum = Integer.MAX_VALUE;
        Map<Integer, String> idxs = this.groupArtIdxMsgID.get(group.getName());
        if(idxs != null) {
            for(int idx : idxs.keySet()) {
                if(idx < firstArtNum) {
                    firstArtNum = idx;
                }
            }
        }
        if(firstArtNum == Integer.MAX_VALUE) {
            // Group is empty
            firstArtNum = 0;
        }
        return firstArtNum;
    }

    @Override
    public Group getGroup(String name) throws StorageBackendException {
        if (this.groups.containsKey(name)) {
            int groupID = this.groups.get(name);
            return new Group(name, groupID, 0); // TODO flags are always zero
        } else {
            Log.get().info("Group " + name + " not found in configuration");
            return null;
        }
    }

    @Override
    public List<Group> getGroups() throws StorageBackendException {
        List<Group> groups = new ArrayList<Group>();
        for (Entry<String, Integer> entry : this.groups.entrySet()) {
            // TODO Flags are always zero
            groups.add(new Group(entry.getKey(), entry.getValue(), 0));
        }
        return groups;
    }

    /**
     * Not yet supported.
     *
     * @param listAddress
     * @return
     * @throws StorageBackendException
     */
    @Override
    public List<String> getGroupsForList(String listAddress)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    /**
     * @return Index of the newest article in group, 0 if non existing
     */
    @Override
    public int getLastArticleNumber(Group group) throws StorageBackendException {
        int lastArtNum = 0;
        Map<Integer, String> idxs = this.groupArtIdxMsgID.get(group.getName());
        if(idxs != null) {
            for(int idx : idxs.keySet()) {
                if(idx > lastArtNum) {
                    lastArtNum = idx;
                }
            }
        }
        return lastArtNum;
    }

    /**
     * throw new StorageBackendException("Not implemented!"); Not yet supported.
     *
     * @param groupname
     * @return
     * @throws StorageBackendException
     */
    @Override
    public List<String> getListsForGroup(String groupname)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    /**
     * Not yet supported.
     *
     * @return
     * @throws StorageBackendException
     */
    @Override
    public String getOldestArticle() throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public int getPostingsCount(String groupname)
            throws StorageBackendException {
        Map<Integer, String> idxs = this.groupArtIdxMsgID.get(groupname);
        if(idxs != null) {
            return idxs.keySet().size();
        } else {
            return 0;
        }
    }

    /**
     * Not yet supported.
     *
     * @param type
     * @return
     * @throws StorageBackendException
     */
    @Override
    public List<Subscription> getSubscriptions(int type)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
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
     *
     * @param group
     * @throws StorageBackendException
     */
    @Override
    public void purgeGroup(Group group) throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public void setConfigValue(String key, String value)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public boolean update(Article article) throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    @Override
    public boolean update(Group group) throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

    /**
     * Not yet supported.
     *
     * @param username
     * @param password
     * @return
     * @throws StorageBackendException
     */
    @Override
    public boolean authenticateUser(String username, char[] password)
            throws StorageBackendException {
        throw new StorageBackendException("Not implemented!");
    }

}
