package org.sonews.storage.impl;

import java.util.List;

import org.sonews.feed.Subscription;
import org.sonews.storage.Article;
import org.sonews.storage.ArticleHead;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Pair;

public class CouchDBDatabase implements Storage {

    @Override
    public void addArticle(Article art) throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addEvent(long timestamp, int type, long groupID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addGroup(String groupname, int flags)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int countArticles() throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int countGroups() throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(String messageID) throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Article getArticle(String messageID) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Article getArticle(long articleIndex, long groupID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Pair<Long, ArticleHead>> getArticleHeads(Group group,
            long first, long last) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Pair<Long, String>> getArticleHeaders(Group group, long start,
            long end, String header, String pattern)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getArticleIndex(Article art, Group group)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Long> getArticleNumbers(long groupID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getConfigValue(String key) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getEventsCount(int eventType, long startTimestamp,
            long endTimestamp, Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getEventsPerHour(int key, long gid)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFirstArticleNumber(Group group)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Group getGroup(String name) throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Group> getGroups() throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public List<String> getGroupsForList(String listAddress)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLastArticleNumber(Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    @Deprecated
    public List<String> getListsForGroup(String groupname)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOldestArticle() throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPostingsCount(String groupname)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Subscription> getSubscriptions(int type)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isArticleExisting(String messageID)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isGroupExisting(String groupname)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void purgeGroup(Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setConfigValue(String key, String value)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean update(Article article) throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean update(Group group) throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean authenticateUser(String username, char[] password)
            throws StorageBackendException {
        // TODO Auto-generated method stub
        return false;
    }

}
