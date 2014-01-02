package org.sonews.storage.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Response;
import org.sonews.config.Config;
import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Log;
import org.sonews.util.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 * @author Christian Lins
 * @since sonews/2.0.0
 */
public class CouchDBDatabase implements Storage {

    private final CouchDbClient client;

    public CouchDBDatabase() {
        final String db = Config.inst().get(Config.STORAGE_DATABASE, "sonews");
        final String host = Config.inst().get(Config.STORAGE_HOST, "localhost");
        final String port = Config.inst().get(Config.STORAGE_PORT, "5984");

        String user = Config.inst().get(Config.STORAGE_USER, null);
        String password = Config.inst().get(Config.STORAGE_PASSWORD, null);
        if("".equals(user) || "".equals(password)) {
            user = null;
            password = null;
        }

        this.client = new CouchDbClient(db, true, "http", host, Integer.parseInt(port), user, password);
    }

    @Override
    public void addArticle(final Article art) throws StorageBackendException {
        final CouchDBArticle cart = new CouchDBArticle(art);
        final Response resp = this.client.save(cart.toString());
        Log.get().log(Level.INFO, resp.toString());
    }

    @Override
    public int countArticles() throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(final String messageID) throws StorageBackendException {
        // TODO Auto-generated method stub

    }

    @Override
    public Article getArticle(final String messageID) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Article getArticle(final long articleIndex, final long groupID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Pair<Long, ArticleHead>> getArticleHeads(final Group group,
            final long first, final long last) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Pair<Long, String>> getArticleHeaders(final Group group, final long start,
            final long end, final String header, final String pattern)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getArticleIndex(final Article art, final Group group)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Long> getArticleNumbers(final long groupID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFirstArticleNumber(final Group group)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getLastArticleNumber(final Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getOldestArticle() throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPostingsCount(final String groupname)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Subscription> getSubscriptions(final int type)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isArticleExisting(final String messageID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean update(final Article article) throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean update(final Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean authenticateUser(final String username, final char[] password)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

}
