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
import org.sonews.util.Log;
import org.sonews.util.Stats;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Daemon thread collecting all NNTPConnection instances. The thread
 * checks periodically if there are stale/timed out connections and
 * removes and purges them properly.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Connections extends AbstractDaemon
{

	private static final Connections instance = new Connections();

	/**
	 * @return Active Connections instance.
	 */
	public static Connections getInstance()
	{
		return Connections.instance;
	}
	private final List<NNTPConnection> connections = new ArrayList<NNTPConnection>();
	private final Map<SocketChannel, NNTPConnection> connByChannel = new HashMap<SocketChannel, NNTPConnection>();

	private Connections()
	{
		setName("Connections");
	}

	/**
	 * Adds the given NNTPConnection to the Connections management.
	 * @param conn
	 * @see org.sonews.daemon.NNTPConnection
	 */
	public void add(final NNTPConnection conn)
	{
		synchronized (this.connections) {
			this.connections.add(conn);
			this.connByChannel.put(conn.getSocketChannel(), conn);
		}
	}

	/**
	 * @param channel
	 * @return NNTPConnection instance that is associated with the given
	 * SocketChannel.
	 */
	public NNTPConnection get(final SocketChannel channel)
	{
		synchronized (this.connections) {
			return this.connByChannel.get(channel);
		}
	}

	int getConnectionCount(String remote)
	{
		int cnt = 0;
		synchronized (this.connections) {
			for (NNTPConnection conn : this.connections) {
				assert conn != null;
				assert conn.getSocketChannel() != null;

				Socket socket = conn.getSocketChannel().socket();
				if (socket != null) {
					InetSocketAddress sockAddr = (InetSocketAddress) socket.getRemoteSocketAddress();
					if (sockAddr != null) {
						if (sockAddr.getHostName().equals(remote)) {
							cnt++;
						}
					}
				} // if(socket != null)
			}
		}
		return cnt;
	}

	/**
	 * Run loops. Checks periodically for timed out connections and purged them
	 * from the lists.
	 */
	@Override
	public void run()
	{
		while (isRunning()) {
			int timeoutMillis = 1000 * Config.inst().get(Config.TIMEOUT, 180);

			synchronized (this.connections) {
				final ListIterator<NNTPConnection> iter = this.connections.listIterator();
				NNTPConnection conn;

				while (iter.hasNext()) {
					conn = iter.next();
					if ((System.currentTimeMillis() - conn.getLastActivity()) > timeoutMillis
						&& conn.getBuffers().isOutputBufferEmpty()) {
						// A connection timeout has occurred so purge the connection
						iter.remove();

						// Close and remove the channel
						SocketChannel channel = conn.getSocketChannel();
						connByChannel.remove(channel);

						try {
							assert channel != null;
							assert channel.socket() != null;

							// Close the channel; implicitely cancels all selectionkeys
							channel.close();
							Log.get().info("Disconnected: " + channel.socket().getRemoteSocketAddress()
								+ " (timeout)");
						} catch (IOException ex) {
							Log.get().warning("Connections.run(): " + ex);
						}

						// Recycle the used buffers
						conn.getBuffers().recycleBuffers();

						Stats.getInstance().clientDisconnect();
					}
				}
			}

			try {
				Thread.sleep(10000); // Sleep ten seconds
			} catch (InterruptedException ex) {
				Log.get().warning("Connections Thread was interrupted: " + ex.getMessage());
			}
		}
	}
}
