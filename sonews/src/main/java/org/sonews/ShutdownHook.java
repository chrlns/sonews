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

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sonews.daemon.DaemonThread;

/**
 * Will force all other threads to shutdown cleanly.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class ShutdownHook implements Runnable {

    private final Logger logger;
    
    public ShutdownHook() {
        this.logger= Logger.getLogger("org.sonews");
    }
    /**
     * Called when the JVM exits.
     */
    @Override
    public void run() {
        logger.log(Level.INFO, "Clean shutdown of daemon threads initiated");

        Map<Thread, StackTraceElement[]> threadsMap = Thread.getAllStackTraces();
        
        threadsMap.keySet().parallelStream().forEach((thread) -> {
            // Interrupt the thread if it's a DaemonThread
            DaemonThread daemon;
            if (thread instanceof DaemonThread && thread.isAlive()) {
                daemon = (DaemonThread) thread;
                daemon.requestShutdown();
            }
        });

        threadsMap.keySet().stream().forEach((thread) -> {
            DaemonThread daemon;
            if (thread instanceof DaemonThread && thread.isAlive()) {
                daemon = (DaemonThread) thread;
                logger.log(Level.INFO, "Waiting for {0} to exit...", daemon);
                try {
                    daemon.join(500);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getLocalizedMessage());
                }
            }
        });

        // We have notified all not-sleeping AbstractDaemons of the shutdown;
        // all other threads can be simply purged on VM shutdown

        logger.log(Level.INFO, "Clean shutdown of daemon threads completed");
    }
}
