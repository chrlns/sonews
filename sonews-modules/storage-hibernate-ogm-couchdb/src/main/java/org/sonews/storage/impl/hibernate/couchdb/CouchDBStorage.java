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

package org.sonews.storage.impl.hibernate.couchdb;

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Christian Lins
 */
public class CouchDBStorage implements Storage {

    private final EntityManager em;
    
    public CouchDBStorage(EntityManager em) {
        this.em = em;
    }
    
    @Override
    public void addArticle(Article art) throws StorageBackendException {
        addArticle(new CouchDBArticle(art));
    }
    
    @Transactional
    public void addArticle(CouchDBArticle art) {
        this.em.getTransaction().begin();
        this.em.persist(art);
        this.em.getTransaction().commit();
    }

    @Override
    public int countArticles() throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(String messageID) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Article getArticle(String messageID) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Article getArticle(long articleIndex, long groupID) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Pair<Long, Article>> getArticleHeads(Group group, long first, long last) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Pair<Long, String>> getArticleHeaders(Group group, long start, long end, String header, String pattern) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getArticleIndex(Article art, Group group) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Long> getArticleNumbers(long groupID) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getFirstArticleNumber(Group group) throws StorageBackendException {
        try {
            return getMaxMinArticleIndex("min", group.getName());
        } catch(HttpStatusCodeException ex) {
            throw new StorageBackendException(ex);
        }
    }

    protected int getMaxMinArticleIndex(String what, String groupName) {
        // FIXME Get Server address from config
        String url = "http://localhost:5984/{db}/_design/access/_view/{what}_article_index?group=true&key=\"{group}\"";
        RestTemplate rt = new RestTemplate();
        ViewResult res = rt.getForObject(url,
                ViewResult.class,
                "hibernate-sonews", // TODO Get this from config
                what, // max or min
                groupName);

        if (res.getRows().length == 0) {
            return 0;
        }

        return Integer.parseInt(res.getRows()[0].getValue(), 10);
    }
    
    @Override
    public int getLastArticleNumber(Group group) throws StorageBackendException {
        try {
            return getMaxMinArticleIndex("max", group.getName());
        } catch(HttpStatusCodeException ex) {
            throw new StorageBackendException(ex);
        }
    }

    @Override
    public String getOldestArticle() throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getPostingsCount(String groupname) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isArticleExisting(String messageID) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void purgeGroup(Group group) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean update(Article article) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean authenticateUser(String username, char[] password) throws StorageBackendException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
