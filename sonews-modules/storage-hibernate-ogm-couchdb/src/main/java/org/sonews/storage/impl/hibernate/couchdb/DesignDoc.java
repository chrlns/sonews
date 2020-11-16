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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.json.JSONObject;

/**
 * A CouchDB design document.
 * @author Christian Lins
 */
@Entity
@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
class DesignDoc implements Serializable {    
    @Id
    @Column(name="_id")
    private String _id;
    
    @Column(name="_rev")
    private String _rev;
    
    private String language;
    
    private int version;
    
    @Embedded
    private Map<String, ViewFunction> views;
    
    public DesignDoc() {
    }
    
    public DesignDoc(JSONObject obj, String rev) {
        this._id  = obj.getString("_id");
        this._rev = rev;
        this.language = obj.getString("language");
        this.version  = obj.getInt("version");
        
        this.views = new HashMap<>();
        
        JSONObject objv = obj.getJSONObject("views");
        objv.keySet().forEach(k -> addView(objv, (String)k));
    }
    
    private void addView(JSONObject obj, String key) {
        JSONObject v = obj.getJSONObject(key);
        
        String map = v.getString("map");
        String red = v.optString("reduce");
        
        this.views.put(key, new ViewFunction(map, red));
    }
    
    public String getLanguage() {
        return this.language;
    }
    
    public int getVersion() {
        return this.version;
    }
 
    public String getId() {
        return this._id;
    }
    
    public String getRev() {
        return this._rev;
    }
    
    public Map<String, ViewFunction> getViews() {
        return this.views;
    }
}

@Embeddable
class ViewFunction implements Serializable {
    private String map, reduce;
    
    public ViewFunction() {
    }
    
    public ViewFunction(String map, String reduce) {
        this.map = map;
        this.reduce = reduce;
    }
    
    public String getMap() {
        return map;
    }
    
    public String getReduce() {
        return reduce;
    }
}