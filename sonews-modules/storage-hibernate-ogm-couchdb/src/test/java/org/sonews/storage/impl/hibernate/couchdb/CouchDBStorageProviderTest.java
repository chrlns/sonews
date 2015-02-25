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


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.sonews.storage.StorageBackendException;

/**
 *
 * @author clins
 */
public class CouchDBStorageProviderTest {
    
    private CouchDBStorageProvider subject;
    
    public CouchDBStorageProviderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        subject = new CouchDBStorageProvider(); 
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testStoreArticle() {
        CouchDBArticle art = new CouchDBArticle();
        art.fillWithRandom();
        
        try {
            CouchDBStorage storage = (CouchDBStorage)subject.storage(null);
            storage.addArticle(art);

        } catch (StorageBackendException ex) {
            fail(ex.getLocalizedMessage());
        }
    }
    
}
