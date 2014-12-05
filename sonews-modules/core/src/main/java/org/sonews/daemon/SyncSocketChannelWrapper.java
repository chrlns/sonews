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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Wrapper for a SocketChannel.
 * @author Christian Lins
 */
public class SyncSocketChannelWrapper
    extends AbstractSocketChannelWrapper implements SocketChannelWrapper
{
    private SocketChannel channel;

    public SyncSocketChannelWrapper(SocketChannel socketChannel) {
        this.channel = socketChannel;
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return this.channel.getRemoteAddress();
    }

    @Override
    public Object getWrapier() {
        return this.channel;
    }

}
