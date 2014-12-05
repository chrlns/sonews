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

package org.sonews.storage.impl.hibernate.couchdb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;

/**
 *
 * @author Christian Lins
 */
@Entity
@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
public class CouchDBArticle implements Serializable {
    
    @Id
    private String messageID;
    
    @Version
    @Column(name="_rev")
    private String revision;
    
    /**
     * List of header objects. We cannot store header entries as map as 1) the
     * order must be preserved and 2) header names (keys) can occur multiple
     * times, e.g. "Path".
     */
    @Embedded
    @ElementCollection
    private List<Header> headers;
    
    private String bodyEncoding;
    
    private byte[] body;
    
    /**
     * Fills the instance with random test data. Used for testing purposes.
     */
    void fillWithRandom() {
        Random rnd = new Random();
        
        this.messageID = "<" + rnd.nextLong() + "@nirvana.test>";
        this.headers = new ArrayList<>();
        this.headers.add(new Header("From", "nobody@nobody.nobody"));
        this.headers.add(new Header("Subject", "Random message for testing purposes #" + rnd.nextInt()));
        this.headers.add(new Header("Newsgroups", "local.test, local.debug"));
        this.bodyEncoding = "base64";
        this.body = "This is a test message. Thank you!".getBytes();
    }
}

@Embeddable
class Header implements Serializable {
    private String key, value;
    
    public Header() {
    
    }
    
    public Header(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
