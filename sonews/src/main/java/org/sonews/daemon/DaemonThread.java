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

package org.sonews.daemon;

import org.sonews.storage.StorageManager;

/**
 * Base class of all sonews threads. Instances of this class will be
 * automatically registered at the ShutdownHook to be cleanly exited when the
 * server is forced to exit.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class DaemonThread extends Thread {

    /** This variable is write synchronized through setRunning */
    private volatile boolean isRunning = false;
    
    private DaemonRunnable run = null;

    /**
     * Constructs a new DaemonThread with the given Runnable.
     * @param run
     */
    public DaemonThread(Runnable run) {
        super(run);
        
        if(run instanceof DaemonRunnable) {
            this.run = (DaemonRunnable)run;
        }
        
        setDaemon(true); // VM will exit when all threads are daemons
        setName(getClass().getSimpleName());
    }

    /**
     * @return true if shutdown() was not yet called.
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * Set the running state of this daemon.
     * @param running
     */
    protected void setRunning(boolean running) {
        synchronized(this) {
            this.isRunning = running;
        }
    }

    /**
     * Marks this thread to exit soon. Closes the associated JDBCDatabase
     * connection if available.
     */
    public void requestShutdown() {
        synchronized (this) {
            this.isRunning = false;
            StorageManager.disableProvider(); // TODO Check if this is correct here
            if (run != null) {
                run.dispose();
            }
        }
    }

    /**
     * Starts this daemon.
     */
    @Override
    public void start() {
        if (run != null) {
            run.setDaemon(this);
        }
        setRunning(true);
        super.start();
    }
}
