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

package org.sonews.daemon.async;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;

import org.sonews.daemon.Connections;
import org.sonews.util.Log;

/**
 *
 * @author Christian Lins
 */
class AcceptCompletionHandler
    implements CompletionHandler<AsynchronousSocketChannel,Void> {

    private final AsynchronousServerSocketChannel serverChannel;

    public AcceptCompletionHandler(AsynchronousServerSocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    public void completed(AsynchronousSocketChannel channel, Void v) {
        Log.get().log(Level.INFO, "Accepted: {0}", channel);
        serverChannel.accept(null, this);

        AsynchronousNNTPConnection conn = new AsynchronousNNTPConnection(channel);
        Connections.getInstance().add(conn);
    }

    @Override
    public void failed(Throwable thrwbl, Void a) {
        Log.get().log(Level.WARNING, thrwbl.getLocalizedMessage(), thrwbl);
    }

}
