/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
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
package org.sonews.storage.impl.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.Header;
import javax.mail.internet.MimeUtility;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;
import org.sonews.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Storage backend facade class for a relational SQL database using JDBC. The
 * statements used should work for at least PostgreSQL and MySQL.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
@Scope("prototype")
public class JDBCDatabase implements Storage {

    public static final int MAX_RESTARTS = 2;

    @Autowired
    private Log log;

    protected Connection conn = null;
    protected PreparedStatement pstmtAddArticle1 = null;
    protected PreparedStatement pstmtAddArticle2 = null;
    protected PreparedStatement pstmtAddArticle3 = null;
    protected PreparedStatement pstmtAddArticle4 = null;
    protected PreparedStatement pstmtCountArticles = null;
    protected PreparedStatement pstmtCreateOrUpdateGroup0 = null;
    protected PreparedStatement pstmtCreateOrUpdateGroup1 = null;
    protected PreparedStatement pstmtCreateOrUpdateGroup2 = null;
    protected PreparedStatement pstmtDeleteArticle0 = null;
    protected PreparedStatement pstmtDeleteArticle1 = null;
    protected PreparedStatement pstmtDeleteArticle2 = null;
    protected PreparedStatement pstmtDeleteArticle3 = null;
    protected PreparedStatement pstmtGetArticle0 = null;
    protected PreparedStatement pstmtGetArticle1 = null;
    protected PreparedStatement pstmtGetArticleHeaders0 = null;
    protected PreparedStatement pstmtGetArticleHeaders1 = null;
    protected PreparedStatement pstmtGetArticleHeads = null;
    protected PreparedStatement pstmtGetArticleIDs = null;
    protected PreparedStatement pstmtGetArticleIndex = null;
    protected PreparedStatement pstmtGetFirstArticleNumber = null;
    protected PreparedStatement pstmtGetGroups = null;
    protected PreparedStatement pstmtGetLastArticleNumber = null;
    protected PreparedStatement pstmtGetMaxArticleID = null;
    protected PreparedStatement pstmtGetMaxArticleIndex = null;
    protected PreparedStatement pstmtGetOldestArticle = null;
    protected PreparedStatement pstmtGetPostingsCount = null;
    protected PreparedStatement pstmtIsArticleExisting = null;
    protected PreparedStatement pstmtPurgeGroup0 = null;
    protected PreparedStatement pstmtPurgeGroup1 = null;
    protected PreparedStatement pstmtUpdateWatermark = null;

    /**
     * How many times the database connection was reinitialized
     */
    protected int restarts = 0;

    protected synchronized void prepareGetPostingsCountStatement() throws SQLException {
        this.pstmtGetPostingsCount = conn
                .prepareStatement("SELECT Count(*) FROM postings WHERE group_id = ?");
    }

    /**
     * Rises the database: reconnect and recreate all prepared statements.
     *
     * @throws java.sql.SQLException
     */
    @PostConstruct
    protected synchronized void arise() throws SQLException {
        try {
            // Load database driver
            //Class.forName(Config.inst().get(Config.LEVEL_FILE,
            //        Config.STORAGE_DBMSDRIVER, "java.lang.Object"));

            // Establish database connection
            this.conn = DriverManager.getConnection(
                    Config.inst().get(Config.LEVEL_FILE,
                            Config.STORAGE_DATABASE, "<not specified>"),
                    Config.inst().get(Config.LEVEL_FILE, Config.STORAGE_USER,
                            "root"),
                    Config.inst().get(Config.LEVEL_FILE,
                            Config.STORAGE_PASSWORD, ""));

            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            if (conn.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE) {
                log.warning("Database is NOT fully serializable!");
            }

            // Prepare statements for method addArticle()
            this.pstmtAddArticle1 = conn
                    .prepareStatement("INSERT INTO articles (article_id, body) VALUES(?, ?)");
            this.pstmtAddArticle2 = conn
                    .prepareStatement("INSERT INTO headers (article_id, header_key, header_value, header_index) "
                            + "VALUES (?, ?, ?, ?)");
            this.pstmtAddArticle3 = conn
                    .prepareStatement("INSERT INTO postings (group_id, article_id, article_index)"
                            + "VALUES (?, ?, ?)");
            this.pstmtAddArticle4 = conn
                    .prepareStatement("INSERT INTO article_ids (article_id, message_id) VALUES (?, ?)");

            // Prepare statement for method countArticles()
            this.pstmtCountArticles = conn
                    .prepareStatement("SELECT Count(article_id) FROM article_ids");

            // Prepare statements for method createOrUpdateGroup(group)
            this.pstmtCreateOrUpdateGroup0 = conn
                    .prepareStatement("SELECT group_id FROM groups WHERE group_id = ?");
            this.pstmtCreateOrUpdateGroup1 = conn
                    .prepareStatement("INSERT INTO groups (group_id, name, flags, watermark) "
                            + "VALUES (?, ?, ?, 0)");
            this.pstmtCreateOrUpdateGroup2 = conn
                    .prepareStatement("UPDATE groups SET name = ?, flags = ? WHERE group_id = ?");

            // Prepare statements for method delete(article)
            this.pstmtDeleteArticle0 = conn
                    .prepareStatement("DELETE FROM articles WHERE article_id = "
                            + "(SELECT article_id FROM article_ids WHERE message_id = ?)");
            this.pstmtDeleteArticle1 = conn
                    .prepareStatement("DELETE FROM headers WHERE article_id = "
                            + "(SELECT article_id FROM article_ids WHERE message_id = ?)");
            this.pstmtDeleteArticle2 = conn
                    .prepareStatement("DELETE FROM postings WHERE article_id = "
                            + "(SELECT article_id FROM article_ids WHERE message_id = ?)");
            this.pstmtDeleteArticle3 = conn
                    .prepareStatement("DELETE FROM article_ids WHERE message_id = ?");

            // Prepare statements for methods getArticle()
            this.pstmtGetArticle0 = conn
                    .prepareStatement("SELECT * FROM articles  WHERE article_id = "
                            + "(SELECT article_id FROM article_ids WHERE message_id = ?)");
            this.pstmtGetArticle1 = conn
                    .prepareStatement("SELECT * FROM articles WHERE article_id = "
                            + "(SELECT article_id FROM postings WHERE "
                            + "article_index = ? AND group_id = ?)");

            // Prepare statement for method getArticleHeaders()
            this.pstmtGetArticleHeaders0 = conn
                    .prepareStatement("SELECT header_key, header_value FROM headers WHERE article_id = ? "
                            + "ORDER BY header_index ASC");

            // Prepare statement for method getArticleHeaders(regular expr
            // pattern)
            this.pstmtGetArticleHeaders1 = conn
                    .prepareStatement("SELECT p.article_index, h.header_value FROM headers h "
                            + "INNER JOIN postings p ON h.article_id = p.article_id "
                            + "INNER JOIN groups g ON p.group_id = g.group_id "
                            + "WHERE g.name          =  ? AND "
                            + "h.header_key    =  ? AND "
                            + "p.article_index >= ? "
                            + "ORDER BY p.article_index ASC");

            this.pstmtGetArticleIDs = conn
                    .prepareStatement("SELECT article_index FROM postings WHERE group_id = ?");

            // Prepare statement for method getArticleIndex
            this.pstmtGetArticleIndex = conn
                    .prepareStatement("SELECT article_index FROM postings WHERE "
                            + "article_id = (SELECT article_id FROM article_ids "
                            + "WHERE message_id = ?) " + " AND group_id = ?");

            // Prepare statements for method getArticleHeads()
            this.pstmtGetArticleHeads = conn
                    .prepareStatement("SELECT article_id, article_index FROM postings WHERE "
                            + "postings.group_id = ? AND article_index >= ? AND "
                            + "article_index <= ?");

            // Prepare statement for method getLastArticleNumber()
            this.pstmtGetLastArticleNumber = conn
                    .prepareStatement("SELECT Max(article_index) FROM postings WHERE group_id = ?");

            // Prepare statement for method getMaxArticleID()
            this.pstmtGetMaxArticleID = conn
                    .prepareStatement("SELECT Max(article_id) FROM articles");

            // Prepare statement for method getMaxArticleIndex()
            this.pstmtGetMaxArticleIndex = conn
                    .prepareStatement("SELECT watermark FROM groups WHERE group_id = ?");

            // Prepare statement for method getOldestArticle()
            this.pstmtGetOldestArticle = conn
                    .prepareStatement("SELECT message_id FROM article_ids WHERE article_id = "
                            + "(SELECT Min(article_id) FROM article_ids)");

            // Prepare statement for method getFirstArticleNumber()
            this.pstmtGetFirstArticleNumber = conn
                    .prepareStatement("SELECT Min(article_index) FROM postings WHERE group_id = ?");

            pstmtGetGroups = conn.prepareStatement("SELECT * FROM groups");

            // Prepare statement for method getPostingsCount()
            prepareGetPostingsCountStatement();

            // Prepare statement for method isArticleExisting()
            this.pstmtIsArticleExisting = conn
                    .prepareStatement("SELECT Count(article_id) FROM article_ids WHERE message_id = ?");

            // Prepare statements for method purgeGroup()
            this.pstmtPurgeGroup0 = conn
                    .prepareStatement("DELETE FROM peer_subscriptions WHERE group_id = ?");
            this.pstmtPurgeGroup1 = conn
                    .prepareStatement("DELETE FROM groups WHERE group_id = ?");

            // Prepare statement for method updateWatermark()
            pstmtUpdateWatermark = conn.prepareStatement(
                    "UPDATE groups SET watermark = ? WHERE group_id = ?");
        } catch (Exception ex) {
            throw new Error("JDBC Driver not found!", ex);
        }
    }

    protected synchronized void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                // Ignore exception
            }
            restarts = 0; // Reset error count
        }
    }

    /**
     * Adds an article to the database.
     *
     * @param article
     * @throws StorageBackendException
     */
    @Override
    @SuppressWarnings("InfiniteRecursion")
    public void addArticle(final Article article) throws StorageBackendException {
        // It is necessary to synchronize this over all connections otherwise
        // several threads would update the article_id that is database-unique.
        synchronized (JDBCDatabase.class) {
            try {
                this.conn.setAutoCommit(false);

                int newArticleID = getMaxArticleID() + 1;
                addArticle(article, newArticleID);
                this.conn.commit();
                this.conn.setAutoCommit(true);

                this.restarts = 0; // Reset error count
            } catch (SQLException ex) {
                try {
                    this.conn.rollback(); // Rollback changes
                } catch (SQLException ex2) {
                    Log.get().log(Level.SEVERE, "Rollback of addArticle() failed: {0}", ex2);
                }

                try {
                    this.conn.setAutoCommit(true); // and release locks
                } catch (SQLException ex2) {
                    Log.get().log(
                            Level.SEVERE, "setAutoCommit(true) of addArticle() failed: {0}", ex2);
                }

                restartConnection(ex);
                addArticle(article);
            }
        }
    }

    /**
     * Adds an article to the database.
     *
     * @param article
     * @param newArticleID
     * @throws java.sql.SQLException
     * @throws org.sonews.storage.StorageBackendException
     */
    protected synchronized void addArticle(final Article article, final int newArticleID)
            throws SQLException, StorageBackendException {
        // Fill prepared statement with values;
        // writes body to article table
        pstmtAddArticle1.setInt(1, newArticleID);
        pstmtAddArticle1.setBytes(2, article.getBody());
        pstmtAddArticle1.execute();

        // Add headers
        Enumeration<?> headers = article.getAllHeaders();
        for (int n = 0; headers.hasMoreElements(); n++) {
            Header header = (Header) headers.nextElement();
            pstmtAddArticle2.setInt(1, newArticleID);
            pstmtAddArticle2.setString(2, header.getName().toLowerCase());
            pstmtAddArticle2.setString(3,
                    header.getValue().replaceAll("[\r\n]", ""));
            pstmtAddArticle2.setInt(4, n);
            pstmtAddArticle2.execute();
        }

        // For each newsgroup add a reference
        List<Group> groups = article.getGroups();
        for (Group group : groups) {
            long newWatermark = getMaxArticleIndex(group.getInternalID()) + 1;
            pstmtAddArticle3.setLong(1, group.getInternalID());
            pstmtAddArticle3.setInt(2, newArticleID);
            pstmtAddArticle3.setLong(3, newWatermark);
            pstmtAddArticle3.execute();

            updateWatermark(group, newWatermark);
        }

        // Write message-id to article_ids table
        this.pstmtAddArticle4.setInt(1, newArticleID);
        this.pstmtAddArticle4.setString(2, article.getMessageID());
        this.pstmtAddArticle4.execute();
    }

    @Override
    public synchronized int countArticles() throws StorageBackendException {
        ResultSet rs = null;

        try {
            rs = this.pstmtCountArticles.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return countArticles();
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public void createOrUpdateGroup(Group group) throws StorageBackendException {
        ResultSet rs = null;
        try {
            pstmtCreateOrUpdateGroup0.setInt(1, (int) group.getInternalID());
            rs = pstmtCreateOrUpdateGroup0.executeQuery();

            if (!rs.next()) {
                // INSERT group_id, name, flags
                pstmtCreateOrUpdateGroup1.setInt(1, (int) group.getInternalID());
                pstmtCreateOrUpdateGroup1.setString(2, group.getName());
                pstmtCreateOrUpdateGroup1.setInt(3, group.getFlags());
                pstmtCreateOrUpdateGroup1.execute();
            } else {
                // UPDATE name, flags, group_id
                pstmtCreateOrUpdateGroup2.setInt(3, (int) group.getInternalID());
                pstmtCreateOrUpdateGroup2.setString(1, group.getName());
                pstmtCreateOrUpdateGroup2.setInt(2, group.getFlags());
                pstmtCreateOrUpdateGroup2.execute();
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            createOrUpdateGroup(group);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized void delete(final String messageID) throws StorageBackendException {
        try {
            this.conn.setAutoCommit(false);

            this.pstmtDeleteArticle0.setString(1, messageID);
            int rs = this.pstmtDeleteArticle0.executeUpdate();
            if (rs != 1) {
                throw new StorageBackendException("Could not delete message "
                        + messageID);
            }

            // We do not trust the ON DELETE CASCADE functionality to delete
            // orphaned references...
            this.pstmtDeleteArticle1.setString(1, messageID);
            this.pstmtDeleteArticle1.executeUpdate();

            this.pstmtDeleteArticle2.setString(1, messageID);
            this.pstmtDeleteArticle2.executeUpdate();

            this.pstmtDeleteArticle3.setString(1, messageID);
            this.pstmtDeleteArticle3.executeUpdate();

            this.conn.commit();
            this.conn.setAutoCommit(true);
        } catch (SQLException ex) {
            throw new StorageBackendException(ex);
        }
    }

    @Override
    public synchronized Article getArticle(String messageID) throws StorageBackendException {
        ResultSet rs = null;
        try {
            pstmtGetArticle0.setString(1, messageID);
            rs = pstmtGetArticle0.executeQuery();

            if (!rs.next()) {
                return null;
            } else {
                byte[] body = rs.getBytes("body");
                String headers = getArticleHeaders(rs.getInt("article_id"));
                return StorageManager.createArticle(headers, body);
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticle(messageID);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Retrieves an article by its ID.
     *
     * @param articleIndex
     * @param gid
     * @return
     * @throws StorageBackendException
     */
    @Override
    public synchronized Article getArticle(long articleIndex, long gid)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticle1.setLong(1, articleIndex);
            this.pstmtGetArticle1.setLong(2, gid);

            rs = this.pstmtGetArticle1.executeQuery();

            if (rs.next()) {
                byte[] body = rs.getBytes("body");
                String headers = getArticleHeaders(rs.getInt("article_id"));
                return StorageManager.createArticle(headers, body);
            } else {
                return null;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticle(articleIndex, gid);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Searches for fitting header values using the given regular expression.
     *
     * @param group
     * @param start
     * @param end
     * @param headerKey
     * @param patStr
     * @return
     * @throws StorageBackendException
     */
    @Override
    public synchronized List<Pair<Long, String>> getArticleHeaders(Group group, long start,
            long end, String headerKey, String patStr)
            throws StorageBackendException, PatternSyntaxException {
        ResultSet rs = null;
        List<Pair<Long, String>> heads = new ArrayList<>();

        try {
            this.pstmtGetArticleHeaders1.setString(1, group.getName());
            this.pstmtGetArticleHeaders1.setString(2, headerKey);
            this.pstmtGetArticleHeaders1.setLong(3, start);

            rs = this.pstmtGetArticleHeaders1.executeQuery();

            // Convert the "NNTP" regex to Java regex
            patStr = patStr.replace("*", ".*");
            Pattern pattern = Pattern.compile(patStr);

            while (rs.next()) {
                Long articleIndex = rs.getLong(1);
                if (end < 0 || articleIndex <= end) // Match start is done via
                // SQL
                {
                    String headerValue = rs.getString(2);
                    Matcher matcher = pattern.matcher(headerValue);
                    if (matcher.matches()) {
                        heads.add(new Pair<>(articleIndex,
                                headerValue));
                    }
                }
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleHeaders(group, start, end, headerKey, patStr);
        } finally {
            closeResultSet(rs);
        }

        return heads;
    }

    private synchronized String getArticleHeaders(long articleID)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticleHeaders0.setLong(1, articleID);
            rs = this.pstmtGetArticleHeaders0.executeQuery();

            StringBuilder buf = new StringBuilder();
            if (rs.next()) {
                for (;;) {
                    buf.append(rs.getString(1)); // key
                    buf.append(": ");
                    String foldedValue = MimeUtility.fold(0, rs.getString(2));
                    buf.append(foldedValue); // value
                    if (rs.next()) {
                        buf.append("\r\n");
                    } else {
                        break;
                    }
                }
            }

            return buf.toString();
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleHeaders(articleID);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized long getArticleIndex(Article article, Group group)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticleIndex.setString(1, article.getMessageID());
            this.pstmtGetArticleIndex.setLong(2, group.getInternalID());

            rs = this.pstmtGetArticleIndex.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return -1;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleIndex(article, group);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Returns a list of Long/ArticleImpl Pairs.
     *
     * @param group
     * @param first
     * @param last
     * @return
     * @throws org.sonews.storage.StorageBackendException
     */
    @Override
    public synchronized List<Pair<Long, Article>> getArticleHeads(Group group,
            long first, long last) throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticleHeads.setLong(1, group.getInternalID());
            this.pstmtGetArticleHeads.setLong(2, first);
            this.pstmtGetArticleHeads.setLong(3, last);
            rs = pstmtGetArticleHeads.executeQuery();

            List<Pair<Long, Article>> articles = new ArrayList<>(rs.getFetchSize());

            while (rs.next()) {
                long aid = rs.getLong("article_id");
                long aidx = rs.getLong("article_index");
                String headers = getArticleHeaders(aid);
                articles.add(new Pair<>(aidx, StorageManager.createArticle(headers, null)));
            }

            return articles;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleHeads(group, first, last);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized List<Long> getArticleNumbers(long gid)
            throws StorageBackendException {
        ResultSet rs = null;
        try {
            List<Long> ids = new ArrayList<>();
            this.pstmtGetArticleIDs.setLong(1, gid);
            rs = this.pstmtGetArticleIDs.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong(1));
            }
            return ids;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleNumbers(gid);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Returns the largest article index (watermark) in this very group.
     * Currently this method returns an int but this is probably not enough for
     * VERY large (or old) groups. The SQL template was changed to BIGINT but
     * some servers may still use INTEGER here.
     *
     * @param groupID
     * @return
     * @throws StorageBackendException
     */
    private synchronized int getMaxArticleIndex(long groupID) throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetMaxArticleIndex.setLong(1, groupID);
            rs = this.pstmtGetMaxArticleIndex.executeQuery();

            int maxIndex = 0;
            if (rs.next()) {
                maxIndex = rs.getInt(1);
            }

            return maxIndex;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getMaxArticleIndex(groupID);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public List<Group> getGroups() throws StorageBackendException {
        ResultSet rs = null;

        try {
            rs = this.pstmtGetMaxArticleID.executeQuery();

            List<Group> groups = new LinkedList<>();
            while (rs.next()) {
                var id    = rs.getInt("group_id");
                var flags = rs.getInt("flags");
                var name  = rs.getString("name");
                var group = new Group(name, id, flags);
                groups.add(group);
            }

            return groups;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getGroups();
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * This method is only called by addArticle which is already synchronized,
     * so we do not need a synchronization here.
     *
     * @return
     * @throws StorageBackendException
     */
    private int getMaxArticleID() throws StorageBackendException {
        ResultSet rs = null;

        try {
            rs = this.pstmtGetMaxArticleID.executeQuery();

            int maxIndex = 0;
            if (rs.next()) {
                maxIndex = rs.getInt(1);
            }

            return maxIndex;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getMaxArticleID();
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized int getLastArticleNumber(Group group) throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetLastArticleNumber.setLong(1, group.getInternalID());
            rs = this.pstmtGetLastArticleNumber.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getLastArticleNumber(group);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized int getFirstArticleNumber(Group group)
            throws StorageBackendException {
        ResultSet rs = null;
        try {
            this.pstmtGetFirstArticleNumber.setLong(1, group.getInternalID());
            rs = this.pstmtGetFirstArticleNumber.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getFirstArticleNumber(group);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized String getOldestArticle() throws StorageBackendException {
        ResultSet rs = null;

        try {
            rs = this.pstmtGetOldestArticle.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getOldestArticle();
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public synchronized int getPostingsCount(String groupname)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            Group group = Group.get(groupname);
            if (group == null) {
                Log.get().log(Level.WARNING, "Group {0} does not exist!", groupname);
                return 0;
            }

            this.pstmtGetPostingsCount.setLong(1, group.getInternalID());
            rs = this.pstmtGetPostingsCount.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                Log.get().warning("Count on postings return nothing!");
                return 0;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getPostingsCount(groupname);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Checks if there is an article with the given messageid in the
     * JDBCDatabase.
     *
     * @param messageID
     * @return
     * @throws StorageBackendException
     */
    @Override
    public synchronized boolean isArticleExisting(String messageID)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtIsArticleExisting.setString(1, messageID);
            rs = this.pstmtIsArticleExisting.executeQuery();
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException ex) {
            restartConnection(ex);
            return isArticleExisting(messageID);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Closes the JDBCDatabase connection.
     */
    @PreDestroy
    public synchronized void close() {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException ex) {
            log.warning(ex.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void purgeGroup(Group group) throws StorageBackendException {
        try {
            this.pstmtPurgeGroup0.setLong(1, group.getInternalID());
            this.pstmtPurgeGroup0.executeUpdate();

            this.pstmtPurgeGroup1.setLong(1, group.getInternalID());
            this.pstmtPurgeGroup1.executeUpdate();
        } catch (SQLException ex) {
            restartConnection(ex);
            purgeGroup(group);
        }
    }

    /**
     * Restart the JDBC connection to the Database server.
     *
     * @param cause
     * @throws StorageBackendException
     */
    protected synchronized void restartConnection(SQLException cause)
            throws StorageBackendException {
        Log.get().log(Level.SEVERE, Thread.currentThread()
                + ": Database connection was closed (restart "
                + restarts + ").", cause);

        if (++restarts >= MAX_RESTARTS) {
            // Throw the exception upwards
            throw new StorageBackendException(cause);
        }

        try {
            Thread.sleep(1500L * restarts);
        } catch (InterruptedException ex) {
            // Sleep was interrupted. Ignore the InterruptedException.
        }

        // Try to properly close the old database connection
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException ex) {
            Log.get().warning(ex.getMessage());
        }

        this.conn = null;

        try {
            // Try to reinitialize database connection
            arise();
        } catch (SQLException ex) {
            Log.get().warning(ex.getMessage());
            restartConnection(ex);
        }
    }

    @Override
    public synchronized boolean update(Article article) throws StorageBackendException {
        ResultSet rs = null;
        try {
            // Retrieve internal article_id
            this.pstmtGetArticle0.setString(1, article.getMessageID());
            rs = this.pstmtGetArticle0.executeQuery();
            int articleID = rs.getInt("article_id");

            delete(article.getMessageID());

            this.conn.setAutoCommit(false);
            addArticle(article, articleID);
            this.conn.commit();
            this.conn.setAutoCommit(true);
            return true;
        } catch (SQLException ex) {
            try {
                this.conn.rollback();
            } catch (SQLException ex2) {
                Log.get().log(Level.SEVERE, "Rollback failed: {0}", ex2.getMessage());
            }
            restartConnection(ex);
            return update(article);
        }
    }

    protected void updateWatermark(Group group, long watermark) throws StorageBackendException {
        try {
            pstmtUpdateWatermark.setLong(1, watermark);
            pstmtUpdateWatermark.setInt(2, (int) group.getInternalID());
            pstmtUpdateWatermark.execute();
        } catch (SQLException ex) {
            restartConnection(ex);
        }
    }

    @Override
    public synchronized boolean authenticateUser(String username, char[] password)
            throws StorageBackendException {
        throw new StorageBackendException("Not supported yet.");
    }
}
