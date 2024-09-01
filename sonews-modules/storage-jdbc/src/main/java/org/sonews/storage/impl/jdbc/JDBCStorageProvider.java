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

import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * StorageProvider for JDBC databases.
 * @author Christian Lins
 * @since sonews/1.0
 */
@Component
public class JDBCStorageProvider implements StorageProvider {

    @Autowired
    private JDBCDatabase database;

    @Override
    public boolean isSupported(String uri) {
        return uri.startsWith("jdbc:mysql")
                || uri.startsWith("jdbc:postgresql");
    }

    @Override
    public synchronized Storage storage(Thread thread) throws StorageBackendException {
        // TODO May return a connection object per Thread but currently this
        // fills up all available connections when we use the ThreadedNNTPDaemon
        return database;
    }
}
