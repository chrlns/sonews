/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.sonews.daemon.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import org.sonews.daemon.Connections;
import org.sonews.daemon.DaemonRunner;
import org.sonews.daemon.NNTPConnection;
import org.sonews.util.Log;

/**
 *
 * @author chris
 */
public class ChannelIO extends DaemonRunner {

    private static final ChannelIO instance = new ChannelIO();

    /**
     * @return Active ChannelReader instance.
     */
    public static ChannelIO getInstance() {
        return instance;
    }

    private Selector selector = null;

    protected ChannelIO() {
    }

    /**
     * Sets the selector which is used by this reader to determine the channel
     * to read from.
     *
     * @param selector
     */
    public void setSelector(final Selector selector) {
        this.selector = selector;
    }
    
    public Selector getSelector() {
        return this.selector;
    }

    /**
     * Run loop. Blocks until some data is available in a channel.
     */
    @Override
    public void run() {
        assert selector != null;

        while (daemon.isRunning()) {
            try {
                // select() blocks until some SelectableChannels are ready for
                // processing. There is no need to synchronize the selector as
                // we have only one thread per selector.
                while (0 == selector.select()) {
                    // Eventually wait for a register operation
                    synchronized (SynchronousNNTPDaemon.RegisterGate) {
                        /* do nothing */
                    }
                }

                // Get list of selection keys with pending events.
                // Note: the selected key set is not thread-safe
                SocketChannel channel = null;
                NNTPConnection conn = null;
                final Set<SelectionKey> selKeys = selector.selectedKeys();
                SelectionKey selKey = null;

                synchronized (selKeys) {
                    Iterator<SelectionKey> it = selKeys.iterator();

                    // Process the first pending event
                    while (it.hasNext()) {
                        selKey = it.next();

                        channel = (SocketChannel) selKey.channel();
                        conn = (SynchronousNNTPConnection)selKey.attachment();

                        if (selKey.isWritable() || selKey.isReadable()) {
                            it.remove();
                        }
                        
                        if (selKey.isWritable()) {
                            processWrite(conn, channel, selKey);
                        }

                        if (selKey.isReadable()) {
                            processRead(conn, channel, selKey);
                        }
                    }

                    // Because we cannot lock the selKey as that would cause
                    // a deadlock we lock the connection. To preserve the 
                    // order of the received byte blocks a selection key for 
                    // a connection that has pending read events is skipped.
                }

            } catch (CancelledKeyException ex) {
                Log.get().log(Level.WARNING, "ChannelReader.run(): {0}", ex);
                Log.get().log(Level.INFO, "", ex);
            } catch (IOException | InterruptedException ex) {
                Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        } // while(isRunning())
    }

    private void processRead(final NNTPConnection connection,
            final SocketChannel socketChannel, final SelectionKey selKey)
            throws InterruptedException, IOException {
        assert selKey != null;
        assert selKey.isReadable();

        // Some bytes are available for reading
        if (selKey.isValid()) {
            // Lock the channel
            synchronized (socketChannel) { // TODO is synchronization necessary as socketchannel is thread-safe?
                // Read the data into the appropriate buffer
                ByteBuffer buf = connection.getInputBuffer();
                int read = -1;
                try {
                    read = socketChannel.read(buf);
                } catch (IOException ex) {
                    // The connection was probably closed by the remote host
                    // in a non-clean fashion
                    Log.get().log(Level.INFO,
                            "ChannelReader.processSelectionKey(): Connection reset");
                } catch (Exception ex) {
                    Log.get().log(Level.WARNING,
                            "ChannelReader.processSelectionKey()", ex);
                }

                System.err.println("<< " + new String(buf.array()));

                if (read == -1) { // End of stream
                    selKey.cancel();
                } else if (read > 0) { // If some data was read
                    ConnectionWorker.addChannel(socketChannel);
                }
            }
        } else {
            // Should not happen
            Log.get().log(Level.SEVERE, "Should not happen: {0}", selKey.toString());
        }
    }
    
    private void processWrite(final NNTPConnection connection,
            final SocketChannel socketChannel, final SelectionKey selKey)
            throws InterruptedException, IOException 
    {
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
                    // writeable events until we have something to write to the 
                    // socket channel.
                    // selKey.cancel();
                    selKey.interestOps(SelectionKey.OP_READ);
                    // Update activity timestamp to prevent too early
                    // disconnects on slow client connections
                    connection.setLastActivity(System.currentTimeMillis());
                    return;
                }

                while (buf != null) { // There is data to be send
                    // Write buffer to socket channel; this method does not block.
                    try {
                        System.err.println(">> " + new String(buf.array()));
                        if (socketChannel.write(buf) <= 0) {
                            // Perhaps there is data to be written, but the
                            // SocketChannel's buffer is full, so we stop writing
                            // to until the next event.
                            break;
                        } else {
                            // Retrieve next buffer if available; method may return
                            // the same buffer instance if it still have some bytes 
                            // remaining.
                            buf = connection.getOutputBuffer();
                        }
                    } catch(IOException ex) {
                        Log.get().log(Level.INFO, "ChannelWriter.run(): Error writing to channel", ex);
                        throw ex;
                    }
                }
            }
        } else {
            Log.get().log(Level.WARNING, "Invalid OP_WRITE key: {0}", selKey);

            if (socketChannel.socket().isClosed()) {
                connection.close();
                socketChannel.close();
                Log.get().info("Connection closed.");
            }
        }
    }
}
