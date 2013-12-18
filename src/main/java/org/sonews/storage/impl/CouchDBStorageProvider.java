package org.sonews.storage.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lightcouch.CouchDbException;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;

public class CouchDBStorageProvider implements StorageProvider {
    
    protected static final Map<Thread, CouchDBDatabase> instances = new ConcurrentHashMap<Thread, CouchDBDatabase>();

    @Override
    public boolean isSupported(String uri) {
        return uri.startsWith("http");
    }

    @Override
    public Storage storage(Thread thread) throws StorageBackendException {
        try {
            if (!instances.containsKey(Thread.currentThread())) {
                CouchDBDatabase db = new CouchDBDatabase();
                instances.put(Thread.currentThread(), db);
                return db;
            } else {
                return instances.get(Thread.currentThread());
            }
        } catch (CouchDbException ex) {
            throw new StorageBackendException(ex);
        }
    }
    
}
