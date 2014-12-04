/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
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

package org.sonews.daemon.sync;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import org.sonews.daemon.AbstractDaemon;
import org.sonews.daemon.ChannelLineBuffers;
import org.sonews.daemon.Connections;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.SocketChannelWrapperFactory;
import org.sonews.util.Log;

/**
 * Does most of the work: parsing input, talking to client and Database.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class ConnectionWorker extends AbstractDaemon {

    // 256 pending events should be enough
    private static final ArrayBlockingQueue<SocketChannel> pendingChannels =
            new ArrayBlockingQueue<>(256, true);

    /**
     * Registers the given channel for further event processing.
     *
     * @param channel
     */
    public static void addChannel(SocketChannel channel)
            throws InterruptedException {
        pendingChannels.put(channel);
    }

    /**
     * Processing loop.
     */
    @Override
    public void run() {
        while (isRunning()) {
            try {
                // Retrieve and remove if available, otherwise wait.
                SocketChannel channel = pendingChannels.take();

                if (channel != null) {
                    // Connections.getInstance().get() MAY return null
                    NNTPConnection conn = Connections.getInstance().get(
                            new SocketChannelWrapperFactory(channel).create());

                    if (conn == null) {
                        Log.get().log(Level.FINEST, "conn is null");
                        addChannel(channel);
                        continue;
                    }

                    // Try to lock the connection object
                    if (conn.tryReadLock()) {
                        ByteBuffer buf = conn.getBuffers().nextInputLine();
                        while (buf != null) // Complete line was received
                        {
                            final byte[] line = new byte[buf.limit()];
                            buf.get(line);
                            ChannelLineBuffers.recycleBuffer(buf);

                            // Here is the actual work done
                            conn.lineReceived(line);

                            // Read next line as we could have already received
                            // the next line
                            buf = conn.getBuffers().nextInputLine();
                        }
                        conn.unlockReadLock();
                    } else {
                        addChannel(channel);
                    }
                }
            } catch (InterruptedException ex) {
                Log.get().log(Level.INFO, "ConnectionWorker interrupted: {0}", ex);
            } catch (Exception ex) {
                Log.get().log(Level.SEVERE, "Exception in ConnectionWorker: {0}", ex);
                ex.printStackTrace();
            }
        } // end while(isRunning())
    }
}
