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

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.mail.Header;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.sonews.storage.Article;
import org.sonews.util.Log;

/**
 * 
 * @author Christian Lins
 * @since sonews/2.0.0
 */
class CouchDBArticle {

    private final Article article;
    
    public CouchDBArticle(Article article) {
        this.article = article;
    }
    
    /**
     * Creates and returns a JSON string representation of the wrapped Article.
     */
    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        Map<String, String> headerMap = new HashMap<>();
        
        Enumeration<?> headers = article.getAllHeaders();
        while(headers.hasMoreElements()) {
            Header header = (Header)headers.nextElement();
            headerMap.put(header.getName(), header.getValue());
        }
        
        try {
            json.put("headers", headerMap);
            json.put("body-encoding", "base64");
            json.put("body", new String(Base64.encodeBase64(article.getBody()), "UTF-8"));
        } catch(UnsupportedEncodingException ex) {
            // This is unlikely to happen as UTF-8 MUST be supported by any
            // Java runtime environment, but say never no...
            Log.get().log(Level.SEVERE, ex.getLocalizedMessage());
        }
        
        return json.toString();
    }
}
