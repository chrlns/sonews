/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
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

package org.sonews.storage.impl;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Christian Lins
 */
public class CouchDBClient {
    
    private static final String GROUP_DB_SEPARATOR = "$";
    
    private String host;
    private int port;
    private String user;
    private String password;
    private String baseURL;
    
    public CouchDBClient(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        
        this.baseURL = "http://" + host + ":" + port + "/";
    }
    
    /**
     * Retrieves the document with the given doc ID.
     * @param doc
     * @throws IOException
     */
    public String get(final String group, final String doc) throws IOException {
        String uri = baseURL + group.replace(".", GROUP_DB_SEPARATOR) + "/" + doc;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(uri);
            CloseableHttpResponse resp = httpClient.execute(get);
            return EntityUtils.toString(resp.getEntity());
        }
    }
    
    public void login(String user, String password) {
        
    }
    
    /**
     * Stores the given document in the database.
     * @param doc JSON-formatted document to be stored
     * @throws IOException
     * @return HTTP status code of the CouchDB's response
     */
    public int put(final String doc, final String id) throws IOException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut put = new HttpPut(this.baseURL + id);
            put.setEntity(new StringEntity(doc, ContentType.APPLICATION_JSON));
            CloseableHttpResponse resp = httpClient.execute(put);
            
            return resp.getStatusLine().getStatusCode();
        }
    }
    
    /**
     * Calls a view of a design document.
     * @param ddoc
     * @param view 
     */
    public void view(String ddoc, String view) {
        
    }
}
