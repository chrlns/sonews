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

package org.sonews.daemon;

import org.sonews.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * A Thread task that processes OP_WRITE events for SocketChannels.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class ChannelWriter extends AbstractDaemon {

    private static final ChannelWriter instance = new ChannelWriter();

    /**
     * @return Returns the active ChannelWriter instance.
     */
    public static ChannelWriter getInstance() {
        return instance;
    }

    private Selector selector = null;

    protected ChannelWriter() {
    }

    /**
     * @return Selector associated with this instance.
     */
    public Selector getSelector() {
        return this.selector;
    }

    /**
     * Sets the selector that is used by this ChannelWriter.
     * 
     * @param selector
     */
    public void setSelector(final Selector selector) {
        this.selector = selector;
    }

    /**
     * Run loop.
     */
    @Override
    public void run() {
        assert selector != null;

        while (isRunning()) {
            try {
                SelectionKey selKey = null;
                SocketChannel socketChannel = null;
                NNTPConnection connection = null;

                // select() blocks until some SelectableChannels are ready for
                // processing. There is no need to synchronize the selector as
                // we
                // have only one thread per selector.
                selector.select(); // The return value of select can be ignored

                // Get list of selection keys with pending OP_WRITE events.
                // The keySET is not thread-safe whereas the keys itself are.
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    // We remove the first event from the set and store it for
                    // later processing.
                    selKey = it.next();
                    socketChannel = (SocketChannel) selKey.channel();
                    connection = Connections.getInstance().get(socketChannel);

                    it.remove();
                    if (connection != null) {
                        break;
                    } else {
                        selKey = null;
                    }
                }

                if (selKey != null) {
                    try {
                        // Process the selected key.
                        // As there is only one OP_WRITE key for a given
                        // channel, we need
                        // not to synchronize this processing to retain the
                        // order.
                        processSelectionKey(connection, socketChannel, selKey);
                    } catch (IOException ex) {
                        Log.get().log(Level.WARNING, "Error writing to channel: {0}", ex);

                        // Cancel write events for this channel
                        selKey.cancel();
                        if (connection != null) {
                            connection.shutdownInput();
                            connection.shutdownOutput();
                        }
                    }
                }

                // Eventually wait for a register operation
                synchronized (NNTPDaemon.RegisterGate) { /* do nothing */
                }
            } catch (CancelledKeyException ex) {
                Log.get().log(Level.INFO, "ChannelWriter.run(): {0}", ex);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } // while(isRunning())
    }

    private void processSelectionKey(final NNTPConnection connection,
            final SocketChannel socketChannel, final SelectionKey selKey)
            throws InterruptedException, IOException {
        assert connection != null;
        assert socketChannel != null;
        assert selKey != null;
        assert selKey.isWritable();

        // SocketChannel is ready for writing
        if (selKey.isValid()) {
            // Lock the socket channel
            synchronized (socketChannel) {
                // Get next output buffer
                ByteBuffer buf = connection.getOutputBuffer();
                if (buf == null) {
                    // Currently we have nothing to write, so we stop the
                    // writeable
                    // events until we have something to write to the socket
                    // channel
                    // selKey.cancel();
                    selKey.interestOps(0);
                    // Update activity timestamp to prevent too early
                    // disconnects
                    // on slow client connections
                    connection.setLastActivity(System.currentTimeMillis());
                    return;
                }

                while (buf != null) // There is data to be send
                {
                    // Write buffer to socket channel; this method does not
                    // block
                    if (socketChannel.write(buf) <= 0) {
                        // Perhaps there is data to be written, but the
                        // SocketChannel's
                        // buffer is full, so we stop writing to until the next
                        // event.
                        break;
                    } else {
                        // Retrieve next buffer if available; method may return
                        // the same
                        // buffer instance if it still have some bytes remaining
                        buf = connection.getOutputBuffer();
                    }
                }
            }
        } else {
            Log.get().log(Level.WARNING, "Invalid OP_WRITE key: {0}", selKey);

            if (socketChannel.socket().isClosed()) {
                connection.shutdownInput();
                connection.shutdownOutput();
                socketChannel.close();
                Log.get().info("Connection closed.");
            }
        }
    }
}
