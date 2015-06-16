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

import java.util.List;

import org.sonews.util.Pair;

/**
 * A generic storage backend interface.
 *
 * @author Christian Lins
 * @since sonews/1.0
 */
public interface Storage {

    /**
     * Stores the given Article in the storage.
     *
     * @param art
     * @throws StorageBackendException
     */
    void addArticle(Article art) throws StorageBackendException;

    int countArticles() throws StorageBackendException;

    void delete(String messageID) throws StorageBackendException;

    Article getArticle(String messageID) throws StorageBackendException;

    Article getArticle(long articleIndex, long groupID)
            throws StorageBackendException;

    List<Pair<Long, Article>> getArticleHeads(Group group, long first,
            long last) throws StorageBackendException;

    List<Pair<Long, String>> getArticleHeaders(Group group, long start,
            long end, String header, String pattern)
            throws StorageBackendException;

    long getArticleIndex(Article art, Group group)
            throws StorageBackendException;

    List<Long> getArticleNumbers(long groupID) throws StorageBackendException;

    int getFirstArticleNumber(Group group) throws StorageBackendException;

    int getLastArticleNumber(Group group) throws StorageBackendException;

    String getOldestArticle() throws StorageBackendException;

    int getPostingsCount(String groupname) throws StorageBackendException;

    boolean isArticleExisting(String messageID) throws StorageBackendException;

    /**
     * Performes a purge operation in the storage backend, e.g. to
     * delete old messages or release allocated resources.
     * @param group
     * @throws StorageBackendException
     */
    void purgeGroup(Group group) throws StorageBackendException;
    
    /**
     * Updates headers and group references of the given article.
     *
     * @param article
     * @return
     * @throws StorageBackendException
     */
    boolean update(Article article) throws StorageBackendException;

    /**
     * TODO Move to separate Authentication Backend
     * @param username
     * @param password
     * @return
     * @throws StorageBackendException
     * @deprecated
     */
    @Deprecated
    public boolean authenticateUser(String username, char[] password)
            throws StorageBackendException;
}
