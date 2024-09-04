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
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sonews.daemon.DaemonRunner;
import org.sonews.daemon.NNTPDaemonRunnable;
import org.sonews.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ThreadedNNTPDaemon extends DaemonRunner implements NNTPDaemonRunnable {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Log logger;

    private int port;
    private ServerSocket serverSocket = null;
    private ThreadPoolExecutor threadPool;
    private final int numThreads = 2 * Runtime.getRuntime().availableProcessors();
    private final BlockingQueue<Runnable> connQueue = new LinkedBlockingDeque<>();

    public ThreadedNNTPDaemon() {
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void run() {
        try {
            logger.log(Level.INFO, "Server listening on port {0}", port);

            // Create a thread pool for handling connections. A cached thread
            // pool will use as many threads as required (up to the system's
            // maximum) to process the connectes. Idle threads are removed from
            // the pool after 60 seconds.
            threadPool = new ThreadPoolExecutor(
                    numThreads, // min. (core) thread number
                    numThreads, // max. thread number (ignored with unbounded queue)
                    1, TimeUnit.MINUTES, // thread idle lifetime
                    connQueue);

            // Create and bind the server socket
            serverSocket = new ServerSocket(this.port);

            while (daemon.isRunning()) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();

                    // It is important to set the timeout as early as possible
                    // as in overload situations the connection times out earlier
                    // to relieve the server from load.
                    clientSocket.setSoTimeout(15 * 1000); // Timeout of 15 seconds

                    logger.log(Level.INFO, "Connected: {0}", clientSocket.getRemoteSocketAddress());

                    // Create a new thread to handle the connection...
                    var thread = context.getBean(ThreadedNNTPConnection.class, clientSocket);

                    // ...and execute it some time in the future.
                    threadPool.execute(thread);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "IOException while accepting connection: {0}", ex.getMessage());
                    logger.info("Connection accepting sleeping for a second...");
                    Thread.sleep(1000);
                } catch (OutOfMemoryError err) {
                    // This may happen when we handle to many connections
                    logger.log(Level.SEVERE, "OutOfMemoryError, we'll try to continue.", err);
                    connQueue.clear(); // Give us some space to breathe
                    logger.warning("Removed all waiting connections.");
                }
            }
        } catch (BindException ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage() + " -> shutdown sonews", ex);
            daemon.requestShutdown();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected exception, trying to continue", ex);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
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
                Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }

}
