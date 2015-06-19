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

import java.util.logging.Level;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;
import org.sonews.util.Log;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Christian Lins
 */
@Component
public class CouchDBStorageProvider implements StorageProvider {
    
    private final EntityManagerFactory emf;
    private final String ddocUrl;
    private final String db;
    
    public CouchDBStorageProvider() {
        this.emf     = Persistence.createEntityManagerFactory("sonews");
        // TODO Get this from config
        this.ddocUrl = "http://localhost:5984/{db}/_design/{name}";
        this.db      = "hibernate-sonews";
        
        // Load design document
        JSONObject lddoc = new JSONObject(new JSONTokener(
                getClass().getResourceAsStream("/couchdb/design-access.json")));
        
        // Check if the design/access document is in the most recent version
        try {
            DesignDoc rddoc = getDesignDocument("access");
            if (rddoc.getVersion() < lddoc.getInt("version")) {
                Log.get().log(Level.INFO, 
                        "Updating CouchDB _design/access document to version {0}", 
                        lddoc.getInt("version"));
                DesignDoc nddoc = new DesignDoc(lddoc, rddoc.getRev());
                updateDesignDocument("access", nddoc);
            }
        } catch(HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                updateDesignDocument("access", new DesignDoc(lddoc, null));
            }
        }
    }

    private DesignDoc getDesignDocument(String name) {       
        RestTemplate rt = new RestTemplate();
        DesignDoc doc = rt.getForObject(ddocUrl, DesignDoc.class, db, name); 
        return doc;
    }
    
    private void updateDesignDocument(String name, DesignDoc ddoc) {
        RestTemplate rt = new RestTemplate();
        rt.put(ddocUrl, ddoc, db, name); 
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
