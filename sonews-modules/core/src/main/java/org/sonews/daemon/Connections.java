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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;

import org.sonews.config.Config;
import org.sonews.util.Log;

/**
 * Daemon thread collecting all NNTPConnection instances. The thread checks
 * periodically if there are stale/timed out connections and removes and purges
 * them properly.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Connections extends DaemonRunner {

    private static final Connections instance = new Connections();

    /**
     * @return Active Connections instance.
     */
    public static Connections getInstance() {
        return Connections.instance;
    }

    private final List<NNTPConnection> connections = new ArrayList<>();
    private final Map<SocketChannel, NNTPConnection> connByChannel
            = new HashMap<>();

    private Connections() {
    }

    /**
     * Adds the given NNTPConnection to the Connections management.
     *
     * @param conn
     * @see org.sonews.daemon.SynchronousNNTPConnection
     */
    public void add(final NNTPConnection conn) {
        synchronized (this.connections) {
            this.connections.add(conn);
            this.connByChannel.put(conn.getSocketChannel(), conn);
            assert connections.size() == connByChannel.size();
        }
    }

    /**
     * @param channel
     * @return NNTPConnection instance that is associated with the given
     *         SocketChannel.
     */
    public NNTPConnection get(final SocketChannel channel) {
        synchronized (this.connections) {
            return this.connByChannel.get(channel);
        }
    }

    /**
     * Run loops. Checks periodically for timed out connections and purged them
     * from the lists.
     */
    @Override
    public void run() {
        this.daemon.setName("Connections");
        
        while (daemon.isRunning()) {
            int timeoutMillis = 1000 * Config.inst().get(Config.TIMEOUT, 180);

            synchronized (this.connections) {
                final ListIterator<NNTPConnection> iter = this.connections
                        .listIterator();
                NNTPConnection conn;

                while (iter.hasNext()) {
                    conn = iter.next();
                    if ((System.currentTimeMillis() - conn.getLastActivity()) > timeoutMillis
                            && conn.getBuffers().isOutputBufferEmpty()) {
                        // A connection timeout has occurred so purge the
                        // connection
                        iter.remove();

                        // Close and remove the channel
                        SocketChannel channel = conn.getSocketChannel();
                        connByChannel.remove(channel);

                        try {
                            assert channel != null;

                            var remoteAddress = channel.getRemoteAddress();

                            // Close the channel; implicitely cancels all
                            // selectionkeys
                            channel.close();
                            Log.get().log(
                                    Level.INFO,
                                    "Disconnected: {0} (timeout)",
                                    remoteAddress);
                        } catch (IOException ex) {
                            Log.get().log(Level.WARNING, "Connections.run(): {0}", ex);
                        }

                        // Recycle the used buffers
                        conn.getBuffers().recycleBuffers();
                    }
                }
            }

            try {
                Thread.sleep(10000); // Sleep ten seconds
            } catch (InterruptedException ex) {
                Log.get().log(Level.WARNING, "Connections Thread was interrupted: {0}", ex.getMessage());
            }
        }
    }
}
