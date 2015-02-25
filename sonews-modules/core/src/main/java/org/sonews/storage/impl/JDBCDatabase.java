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

package org.sonews.storage.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.mail.Header;
import javax.mail.internet.MimeUtility;

import org.sonews.config.Config;
import org.sonews.util.Log;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;

/**
 * Storage backend facade class for a relational SQL database using JDBC.
 * The statements used should work for at least PostgreSQL and MySQL.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class JDBCDatabase implements Storage {
    public static final int MAX_RESTARTS = 2;

    protected Connection conn = null;
    protected PreparedStatement pstmtAddArticle1 = null;
    protected PreparedStatement pstmtAddArticle2 = null;
    protected PreparedStatement pstmtAddArticle3 = null;
    protected PreparedStatement pstmtAddArticle4 = null;
    protected PreparedStatement pstmtCountArticles = null;
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
    protected PreparedStatement pstmtGetLastArticleNumber = null;
    protected PreparedStatement pstmtGetMaxArticleID = null;
    protected PreparedStatement pstmtGetMaxArticleIndex = null;
    protected PreparedStatement pstmtGetOldestArticle = null;
    protected PreparedStatement pstmtGetPostingsCount = null;
    protected PreparedStatement pstmtIsArticleExisting = null;
    protected PreparedStatement pstmtPurgeGroup0 = null;
    protected PreparedStatement pstmtPurgeGroup1 = null;
    /** How many times the database connection was reinitialized */
    protected int restarts = 0;

    protected void prepareGetPostingsCountStatement() throws SQLException {
        this.pstmtGetPostingsCount = conn
                .prepareStatement("SELECT Count(*) FROM postings WHERE group_id = ?");
    }

    /**
     * Rises the database: reconnect and recreate all prepared statements.
     *
     * @throws java.sql.SQLException
     */
    protected void arise() throws SQLException {
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

            this.conn
                    .setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            if (this.conn.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE) {
                Log.get().warning("Database is NOT fully serializable!");
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
                    .prepareStatement("SELECT Max(article_index) FROM postings WHERE group_id = ?");

            // Prepare statement for method getOldestArticle()
            this.pstmtGetOldestArticle = conn
                    .prepareStatement("SELECT message_id FROM article_ids WHERE article_id = "
                            + "(SELECT Min(article_id) FROM article_ids)");

            // Prepare statement for method getFirstArticleNumber()
            this.pstmtGetFirstArticleNumber = conn
                    .prepareStatement("SELECT Min(article_index) FROM postings WHERE group_id = ?");

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
        } catch (Exception ex) {
            throw new Error("JDBC Driver not found!", ex);
        }
    }

    /**
     * Adds an article to the database.
     *
     * @param article
     * @throws StorageBackendException
     */
    @Override
    public void addArticle(final Article article)
            throws StorageBackendException {
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

    /**
     * Adds an article to the database.
     *
     * @param article
     * @return
     * @throws java.sql.SQLException
     */
    void addArticle(final Article article, final int newArticleID)
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
            pstmtAddArticle3.setLong(1, group.getInternalID());
            pstmtAddArticle3.setInt(2, newArticleID);
            pstmtAddArticle3.setLong(3,
                    getMaxArticleIndex(group.getInternalID()) + 1);
            pstmtAddArticle3.execute();
        }

        // Write message-id to article_ids table
        this.pstmtAddArticle4.setInt(1, newArticleID);
        this.pstmtAddArticle4.setString(2, article.getMessageID());
        this.pstmtAddArticle4.execute();
    }

    @Override
    public int countArticles() throws StorageBackendException {
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                restarts = 0;
            }
        }
    }

    @Override
    public void delete(final String messageID) throws StorageBackendException {
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
    public Article getArticle(String messageID) throws StorageBackendException {
        ResultSet rs = null;
        try {
            pstmtGetArticle0.setString(1, messageID);
            rs = pstmtGetArticle0.executeQuery();

            if (!rs.next()) {
                return null;
            } else {
                byte[] body = rs.getBytes("body");
                String headers = getArticleHeaders(rs.getInt("article_id"));
                return new Article(headers, body);
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticle(messageID);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                restarts = 0; // Reset error count
            }
        }
    }

    /**
     * Retrieves an article by its ID.
     *
     * @param articleID
     * @return
     * @throws StorageBackendException
     */
    @Override
    public Article getArticle(long articleIndex, long gid)
            throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticle1.setLong(1, articleIndex);
            this.pstmtGetArticle1.setLong(2, gid);

            rs = this.pstmtGetArticle1.executeQuery();

            if (rs.next()) {
                byte[] body = rs.getBytes("body");
                String headers = getArticleHeaders(rs.getInt("article_id"));
                return new Article(headers, body);
            } else {
                return null;
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticle(articleIndex, gid);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                restarts = 0;
            }
        }
    }

    /**
     * Searches for fitting header values using the given regular expression.
     *
     * @param group
     * @param start
     * @param end
     * @param headerKey
     * @param pattern
     * @return
     * @throws StorageBackendException
     */
    @Override
    public List<Pair<Long, String>> getArticleHeaders(Group group, long start,
            long end, String headerKey, String patStr)
            throws StorageBackendException, PatternSyntaxException {
        ResultSet rs = null;
        List<Pair<Long, String>> heads = new ArrayList<Pair<Long, String>>();

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
                        heads.add(new Pair<Long, String>(articleIndex,
                                headerValue));
                    }
                }
            }
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleHeaders(group, start, end, headerKey, patStr);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return heads;
    }

    private String getArticleHeaders(long articleID)
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public long getArticleIndex(Article article, Group group)
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a list of Long/Article Pairs.
     *
     * @throws java.sql.SQLException
     */
    @Override
    public List<Pair<Long, ArticleHead>> getArticleHeads(Group group,
            long first, long last) throws StorageBackendException {
        ResultSet rs = null;

        try {
            this.pstmtGetArticleHeads.setLong(1, group.getInternalID());
            this.pstmtGetArticleHeads.setLong(2, first);
            this.pstmtGetArticleHeads.setLong(3, last);
            rs = pstmtGetArticleHeads.executeQuery();

            List<Pair<Long, ArticleHead>> articles = new ArrayList<Pair<Long, ArticleHead>>();

            while (rs.next()) {
                long aid = rs.getLong("article_id");
                long aidx = rs.getLong("article_index");
                String headers = getArticleHeaders(aid);
                articles.add(new Pair<Long, ArticleHead>(aidx, new ArticleHead(
                        headers)));
            }

            return articles;
        } catch (SQLException ex) {
            restartConnection(ex);
            return getArticleHeads(group, first, last);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<Long> getArticleNumbers(long gid)
            throws StorageBackendException {
        ResultSet rs = null;
        try {
            List<Long> ids = new ArrayList<Long>();
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
            if (rs != null) {
                try {
                    rs.close();
                    restarts = 0; // Clear the restart count after successful
                                  // request
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private int getMaxArticleIndex(long groupID) throws StorageBackendException {
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public int getLastArticleNumber(Group group) throws StorageBackendException {
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public int getFirstArticleNumber(Group group)
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getOldestArticle() throws StorageBackendException {
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public int getPostingsCount(String groupname)
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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
    public boolean isArticleExisting(String messageID)
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Closes the JDBCDatabase connection.
     */
    public void shutdown() throws StorageBackendException {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException ex) {
            throw new StorageBackendException(ex);
        }
    }

    @Override
    public void purgeGroup(Group group) throws StorageBackendException {
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

    protected void restartConnection(SQLException cause)
            throws StorageBackendException {
        restarts++;
        Log.get().log(
                Level.SEVERE,
                Thread.currentThread()
                        + ": Database connection was closed (restart "
                        + restarts + ").", cause);

        if (restarts >= MAX_RESTARTS) {
            // Delete the current, probably broken JDBCDatabase instance.
            // So no one can use the instance any more.
            JDBCStorageProvider.instances.remove(Thread.currentThread());

            // Throw the exception upwards
            throw new StorageBackendException(cause);
        }

        try {
            Thread.sleep(1500L * restarts);
        } catch (InterruptedException ex) {
            Log.get().warning("Interrupted: " + ex.getMessage());
        }

        // Try to properly close the old database connection
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException ex) {
            Log.get().warning(ex.getMessage());
        }

        try {
            // Try to reinitialize database connection
            arise();
        } catch (SQLException ex) {
            Log.get().warning(ex.getMessage());
            restartConnection(ex);
        }
    }

    @Override
    public boolean update(Article article) throws StorageBackendException {
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
                Log.get().severe("Rollback failed: " + ex2.getMessage());
            }
            restartConnection(ex);
            return update(article);
        }
    }

    @Override
    public boolean authenticateUser(String username, char[] password)
            throws StorageBackendException {
        throw new StorageBackendException("Not supported yet.");
    }
}
