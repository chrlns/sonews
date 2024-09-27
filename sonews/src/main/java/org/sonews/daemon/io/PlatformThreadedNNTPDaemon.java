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
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.springframework.stereotype.Component;

/**
 * An NNTP daemon listening for incoming connections. This implementation of
 * NNTPDaemonRunnable uses a dynamically growing and shrinking pool of platform
 * threads to handle the incoming connections. The number of threads is between
 * min(4, 2*CPU-Cores) to min(128, 20*CPU_Cores). Due to the hard upper limit
 * this effectively limits the maximum number of connections that can be
 * processed in parallel. If there are more than 128 (or 20*CPU_Cores) incoming
 * connections, new connections will be rejected.
 * Due to this limitations the VirtualThreadedNNTPDaemon is the better choice
 * in almost any situations.
 *
 * @author Christian Lins
 */
@Component
public class PlatformThreadedNNTPDaemon extends ThreadedNNTPDaemon {

    private final int numMinThreads;
    private final int numMaxThreads;
    private final BlockingQueue<Runnable> connQueue = new ArrayBlockingQueue<>(1024);

    public PlatformThreadedNNTPDaemon() {
        // At least four threads as minimum
        numMinThreads = Math.min(4, 2 * Runtime.getRuntime().availableProcessors());

        // Maximum of 128 Threads, could be increased for large machines
        numMaxThreads = Math.min(128, 20 * Runtime.getRuntime().availableProcessors());
    }

    /**
     * Opens a server socket on the configured port and waits for incoming
     * connections.
     */
    @Override
    @SuppressWarnings({"SleepWhileInLoop", "UseSpecificCatch"})
    public void run() {
        try {
            logger.log(Level.INFO, "Server listening on port {0}", port);

            // Create a thread pool for handling connections. A cached thread
            // pool will use as many threads as required (up to the system's
            // maximum) to process the connectes. Idle threads are removed from
            // the pool after 60 seconds.
            threadPool = new ThreadPoolExecutor(
                    numMinThreads, // min. (core) thread number
                    numMaxThreads, // max. thread number
                    1, TimeUnit.MINUTES, // thread idle lifetime
                    connQueue,
                    new ThreadPoolExecutor.AbortPolicy());

            // Create and bind the server socket
            serverSocket = new ServerSocket(this.port);

            while (daemon.isRunning()) {
                Socket clientSocket = null;
                try {
                    // Accept incoming connections
                    clientSocket = serverSocket.accept();

                    // It is important to set the timeout as early as possible
                    // as in overload situations the connection times out earlier
                    // to relieve the server from load.
                    clientSocket.setSoTimeout(15 * 1000); // Timeout of 15 seconds

                    logger.log(Level.INFO, "Connected: {0}", clientSocket.getRemoteSocketAddress());

                    // Create a new thread to handle the connection...
                    var thread = context.getBean(ThreadedNNTPConnection.class, clientSocket);

                    // ...and execute it some time in the future.
                    threadPool.execute(thread);
                } catch(RejectedExecutionException ex) {
                    logger.warning("Rejecting execution, queue full.");
                    if (clientSocket != null) {
                        try (var out = new PrintWriter(clientSocket.getOutputStream())) {
                            out.print("400 Temporary overload, please retry later");
                            out.print(ThreadedNNTPConnection.NEWLINE);
                        }
                    }
                    Thread.sleep(100);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "IOException while accepting connection: {0}", ex.getMessage());
                    logger.info("Connection accepting sleeping for a second...");
                    Thread.sleep(1000);
                } catch (OutOfMemoryError err) {
                    // This may happen when we handle to many connections
                    logger.log(Level.SEVERE, "OutOfMemoryError, we'll try to continue.", err);
                    connQueue.clear(); // Give us some space to breathe
                    logger.warning("Removed all waiting connections.");
                    Thread.sleep(5000);
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

}
