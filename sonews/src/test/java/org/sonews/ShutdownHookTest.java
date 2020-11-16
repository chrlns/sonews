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

package org.sonews;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.sonews.daemon.DaemonThread;

/**
 *
 * @author Christian Lins
 */
public class ShutdownHookTest {
    
    public ShutdownHookTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of run method, of class ShutdownHook.
     */
    @Test
    public void testRun() {
        try {
            System.out.println("run");
            Thread instance = new Thread(new ShutdownHook());
            instance.start();
            instance.join();
            
            instance = new Thread(new ShutdownHook());
            
            DaemonThread daemon;
            daemon = new DaemonThread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    System.out.println("Sleep interrupted");
                }
            });
            daemon.start();
            
            instance.start();
            instance.join();
        } catch(InterruptedException ex) {
            fail("Interrupted while shutting down all AbstractDaemon");
        }
    }
    
}
