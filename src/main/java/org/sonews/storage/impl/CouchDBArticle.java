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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;

import org.json.JSONObject;
import org.sonews.storage.Article;

public class CouchDBArticle {

    private Article article;
    
    public CouchDBArticle(Article article) {
        this.article = article;
    }
    
    public String toString() {
        JSONObject json = new JSONObject();
        Map<String, String> headerMap = new HashMap<String, String>();
        
        Enumeration<?> headers = article.getAllHeaders();
        while(headers.hasMoreElements()) {
            Header header = (Header)headers.nextElement();
            headerMap.put(header.getName(), header.getValue());
        }
        
        json.put("headers", headerMap);
        json.put("body", article.getBody());
        
        return json.toString();
    }
}
