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

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.springframework.stereotype.Component;

/**
 *
 * @author Christian Lins
 */
@Component
public class CouchDBStorageProvider implements org.sonews.storage.StorageProvider {
    
    private EntityManagerFactory emf;
    
    public CouchDBStorageProvider() {
        this.emf = Persistence.createEntityManagerFactory("sonews");
    }

    @Override
    public boolean isSupported(String uri) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Storage storage(Thread thread) throws StorageBackendException {
        return new CouchDBStorage(emf.createEntityManager());
    }
}
