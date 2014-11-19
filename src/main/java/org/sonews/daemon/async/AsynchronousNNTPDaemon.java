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

package org.sonews.daemon.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sonews.daemon.AbstractDaemon;
import org.sonews.util.Log;

/**
 * Daemon listening for incoming connections using Java 7 Asynchronous
 * Socket NIO API.
 *
 * @author Christian Lins
 */
public class AsynchronousNNTPDaemon extends AbstractDaemon {

    private final int port;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private AsynchronousChannelGroup channelGroup;

    public AsynchronousNNTPDaemon(final int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            final int workerThreads = Math.max(4, 2 *
                    Runtime.getRuntime().availableProcessors());
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    workerThreads, Executors.defaultThreadFactory());

            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(port));

            serverSocketChannel.accept(null,
                    new AcceptCompletionHandler(serverSocketChannel));

            channelGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch(IOException | InterruptedException ex) {
            Log.get().log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }
}
