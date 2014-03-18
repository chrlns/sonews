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

import org.sonews.config.Config;
import org.sonews.Main;
import org.sonews.util.Log;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * NNTP daemon using SelectableChannels.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class NNTPDaemon extends AbstractDaemon {

    public static final Object RegisterGate = new Object();
    private static NNTPDaemon instance = null;

    public static synchronized NNTPDaemon createInstance(int port) {
        if (instance == null) {
            instance = new NNTPDaemon(port);
            return instance;
        } else {
            throw new RuntimeException(
                    "NNTPDaemon.createInstance() called twice");
        }
    }

    private int port;

    private NNTPDaemon(final int port) {
        Log.get().info("Server listening on port " + port);
        this.port = port;
    }

    @Override
    public void run() {
        try {
            // Create a Selector that handles the SocketChannel multiplexing
            final Selector readSelector = Selector.open();
            final Selector writeSelector = Selector.open();

            // Start working threads
            final int workerThreads = Math.max(4, 2 * 
                    Runtime.getRuntime().availableProcessors());
            ConnectionWorker[] cworkers = new ConnectionWorker[workerThreads];
            for (int n = 0; n < workerThreads; n++) {
                cworkers[n] = new ConnectionWorker();
                cworkers[n].start();
            }
            Log.get().info(workerThreads + " worker threads started.");

            ChannelWriter.getInstance().setSelector(writeSelector);
            ChannelReader.getInstance().setSelector(readSelector);
            ChannelWriter.getInstance().start();
            ChannelReader.getInstance().start();

            final ServerSocketChannel serverSocketChannel = ServerSocketChannel
                    .open();
            serverSocketChannel.configureBlocking(true); // Set to blocking mode

            // Configure ServerSocket; bind to socket...
            final ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(this.port));

            while (isRunning()) {
                SocketChannel socketChannel;

                try {
                    // As we set the server socket channel to blocking mode the
                    // accept()
                    // method will block.
                    socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    assert socketChannel.isConnected();
                    assert socketChannel.finishConnect();
                } catch (IOException ex) {
                    // Under heavy load an IOException "Too many open files may
                    // be thrown. It most cases we should slow down the
                    // connection
                    // accepting, to give the worker threads some time to
                    // process work.
                    Log.get().severe(
                            "IOException while accepting connection: "
                                    + ex.getMessage());
                    Log.get().info(
                            "Connection accepting sleeping for seconds...");
                    Thread.sleep(5000); // 5 seconds
                    continue;
                }

                final NNTPConnection conn;
                try {
                    conn = new NNTPConnection(socketChannel);
                    Connections.getInstance().add(conn);
                } catch (IOException ex) {
                    Log.get().warning(ex.toString());
                    socketChannel.close();
                    continue;
                }

                try {
                    SelectionKey selKeyWrite = registerSelector(writeSelector,
                            socketChannel, SelectionKey.OP_WRITE);
                    registerSelector(readSelector, socketChannel,
                            SelectionKey.OP_READ);

                    Log.get().info(
                            "Connected: "
                                    + socketChannel.socket()
                                            .getRemoteSocketAddress());

                    // Set write selection key and send hello to client
                    conn.setWriteSelectionKey(selKeyWrite);
                    conn.println("200 "
                            + Config.inst().get(Config.HOSTNAME, "localhost")
                            + " " + Main.VERSION
                            + " news server ready - (posting ok).");
                } catch (CancelledKeyException cke) {
                    Log.get().warning(
                            "CancelledKeyException " + cke.getMessage()
                                    + " was thrown: " + socketChannel.socket());
                } catch (ClosedChannelException cce) {
                    Log.get().warning(
                            "ClosedChannelException " + cce.getMessage()
                                    + " was thrown: " + socketChannel.socket());
                }
            }
        } catch (BindException ex) {
            // Could not bind to socket; this is a fatal problem; so perform
            // shutdown
            ex.printStackTrace();
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static SelectionKey registerSelector(final Selector selector,
            final SocketChannel channel, final int op)
            throws CancelledKeyException, ClosedChannelException {
        // Register the selector at the channel, so that it will be notified
        // on the socket's events
        synchronized (RegisterGate) {
            // Wakeup the currently blocking reader/writer thread; we have
            // locked
            // the RegisterGate to prevent the awakened thread to block again
            selector.wakeup();

            // Lock the selector to prevent the waiting worker threads going
            // into
            // selector.select() which would block the selector.
            synchronized (selector) {
                return channel.register(selector, op, null);
            }
        }
    }
}
