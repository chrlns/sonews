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
import org.json.JSONObject;
import org.json.JSONTokener;

import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Christian Lins
 */
@Component
public class CouchDBStorageProvider implements StorageProvider {
    
    private final EntityManagerFactory emf;
    
    public CouchDBStorageProvider() {
        this.emf = Persistence.createEntityManagerFactory("sonews");
        
        // Load design document
        JSONObject lddoc = new JSONObject(new JSONTokener(
                getClass().getResourceAsStream("/couchdb/design-access.json")));
        
        // Check if the design/access document is in the most recent version
        DesignDoc rddoc = getDesignDocument("access");
        if (rddoc.getVersion() < lddoc.getInt("version")) {
            
        }
    }

    private DesignDoc getDesignDocument(String name) {
        String url = "http://localhost:5984/{db}/_design/{name}";
        RestTemplate rt = new RestTemplate();
        DesignDoc doc = rt.getForObject(url,
                DesignDoc.class,
                "hibernate-sonews", // TODO Get this from config
                name); 

        return doc;
    }
    
    @Override
    public boolean isSupported(String uri) {
        return uri.startsWith("http://");
    }

    @Override
    public Storage storage(Thread thread) throws StorageBackendException {
        return new CouchDBStorage(emf.createEntityManager());
    }
}
