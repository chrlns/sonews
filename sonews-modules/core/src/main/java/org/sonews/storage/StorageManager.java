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
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Provides access to a storage backend.
 * 
 * @author Christian Lins
 * @since sonews/1.0
 */
public class StorageManager {

    private static StorageProvider provider;

    // FIXME Is this the right place for factory methods?
    public static Article createArticle() {
        return new ArticleImpl();
    }
    
    public static Article createArticle(String headers, byte[] body) {
        return new ArticleImpl(headers, body);
    }
    
    public static Article createArticle(Message msg) throws IOException, MessagingException {
        return new ArticleImpl(msg);
    }
    
    public static Storage current() throws StorageBackendException {
        synchronized (StorageManager.class) {
            if (provider == null) {
                return null;
            } else {
                return provider.storage(Thread.currentThread());
            }
        }
    }

    public static StorageProvider loadProvider(String pluginClassName) {
        try {
            Class<?> clazz = Class.forName(pluginClassName);
            Object inst = clazz.newInstance();
            return (StorageProvider) inst;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            // Do not use logging here as the Log class requires a working
            // backend which is in most cases not available at this point
            // FIXME
            System.out.println("Could not instantiate StorageProvider: " + ex);
            return null;
        }
    }

    /**
     * Sets the current storage provider.
     * 
     * @param provider
     */
    public static void enableProvider(StorageProvider provider) {
        synchronized (StorageManager.class) {
            if (StorageManager.provider != null) {
                disableProvider();
            }
            StorageManager.provider = provider;
        }
    }

    /**
     * Disables the current provider.
     */
    public static void disableProvider() {
        synchronized (StorageManager.class) {
            provider = null;
        }
    }
}
