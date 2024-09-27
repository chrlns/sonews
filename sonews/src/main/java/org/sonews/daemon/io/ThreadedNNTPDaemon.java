/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
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

package org.sonews.daemon.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import org.sonews.daemon.DaemonRunner;
import org.sonews.daemon.NNTPDaemonRunnable;
import org.sonews.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Base class for PlatformThreadedNNTPDaemon and VirtualThreadedNNTPDaemon.
 * @author Christian Lins
 */
@Component
abstract class ThreadedNNTPDaemon extends DaemonRunner implements NNTPDaemonRunnable {

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected Log logger;

    protected int port;
    protected ServerSocket serverSocket = null;
    protected ExecutorService threadPool;

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Close the server socket and shutdown the thread pool.
     */
    @Override
    public void dispose() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }
}
