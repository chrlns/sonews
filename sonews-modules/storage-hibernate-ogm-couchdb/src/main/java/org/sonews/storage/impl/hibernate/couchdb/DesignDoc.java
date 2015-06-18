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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
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
 * A CouchDB design document.
 * @author Christian Lins
 */
@Entity
@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
class DesignDoc implements Serializable {
    @Id // Fake Id
    private Long id;
    
    @Column(name="_id")
    private String couchId;
    
    @Column(name="_rev")
    private String couchRev;
    
    private String language;
    
    private int version;
    
    @Embedded
    private Map<String, ViewFunction> views;
    
    public String getLanguage() {
        return this.language;
    }
    
    public int getVersion() {
        return this.version;
    }

    public String getCouchId() {
        return this.couchId;
    }
    
    public String getRevision() {
        return this.couchRev;
    }
    
    public Map<String, ViewFunction> getViews() {
        return this.views;
    }
}

@Embeddable
class ViewFunction implements Serializable {
    private String map, reduce;
    
    public String getMap() {
        return map;
    }
    
    public String getReduce() {
        return reduce;
    }
}