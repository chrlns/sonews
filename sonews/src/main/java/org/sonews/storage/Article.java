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

import java.util.Enumeration;
import java.util.List;

import javax.mail.internet.InternetHeaders;

/**
 * A news article.
 * 
 * @author Christian Lins
 */
public interface Article {

    Enumeration<?> getAllHeaders();
    
    /**
     * Returns the body string.
     * 
     * @return Body string. May be null if only headers were fetched from backend.
     */
    byte[] getBody();

    /**
     * @return List of newsgroups this ArticleImpl belongs to.
     */
    List<Group> getGroups();
 
    /**
     * Returns the header field with given name.
     *
     * @param name
     *            Name of the header field(s).
     * @param returnNull
     *            If set to true, this method will return null instead of an
     *            empty array if there is no header field found.
     * @return Header values or empty string.
     */
    String[] getHeader(String name, boolean returnNull);
    
    /**
     * Convenience method for getHeader(name, false).
     * @param name
     * @return 
     */
    String[] getHeader(String name);
    
    String getHeaderSource();
    
    String getMessageID();
    
    /**
     * 
     * @return false if the body was not fetched from the backend 
     */
    boolean hasBody();
            
    /**
     * Removes the header identified by the given key.
     * @param headerKey
     */
    void removeHeader(String headerKey);
    
    void setBody(byte[] body);
    
    void setGroup(String groupname);
    
    void setHeader(String name, String value);
    
    void setHeaders(InternetHeaders headers);
}
