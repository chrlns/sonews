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

package org.sonews.daemon.command;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.sonews.config.Config;
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.feed.FeedManager;
import org.sonews.feed.Subscription;
import org.sonews.storage.Channel;
import org.sonews.storage.Group;
import org.sonews.util.Stats;

/**
 * The XDAEMON command allows a client to get/set properties of the
 * running server daemon. Only locally connected clients are allowed to
 * use this command.
 * The restriction to localhost connection can be suppressed by overriding
 * the sonews.xdaemon.host bootstrap config property.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class XDaemonCommand implements Command
{

	@Override
	public String[] getSupportedCommandStrings()
	{
		return new String[] {"XDAEMON"};
	}

	@Override
	public boolean hasFinished()
	{
		return true;
	}

	@Override
	public String impliedCapability()
	{
		return null;
	}

	@Override
	public boolean isStateful()
	{
		return false;
	}

	private void channelAdd(String[] commands, NNTPConnection conn)
		throws IOException, StorageBackendException
	{
		String groupName = commands[2];
		if (StorageManager.current().isGroupExisting(groupName)) {
			conn.println("400 group " + groupName + " already existing!");
		} else {
			StorageManager.current().addGroup(groupName, Integer.parseInt(commands[3]));
			conn.println("200 group " + groupName + " created");
		}
	}

	// TODO: Refactor this method to reduce complexity!
	@Override
	public void processLine(NNTPConnection conn, String line, byte[] raw)
		throws IOException, StorageBackendException
	{
		InetSocketAddress addr = (InetSocketAddress) conn.getSocketChannel().socket().getRemoteSocketAddress();
		if (addr.getHostName().equals(
			Config.inst().get(Config.XDAEMON_HOST, "localhost"))) {
			String[] commands = line.split(" ", 4);
			if (commands.length == 3 && commands[1].equalsIgnoreCase("LIST")) {
				if (commands[2].equalsIgnoreCase("CONFIGKEYS")) {
					conn.println("100 list of available config keys follows");
					for (String key : Config.AVAILABLE_KEYS) {
						conn.println(key);
					}
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("PEERINGRULES")) {
					List<Subscription> pull =
						StorageManager.current().getSubscriptions(FeedManager.TYPE_PULL);
					List<Subscription> push =
						StorageManager.current().getSubscriptions(FeedManager.TYPE_PUSH);
					conn.println("100 list of peering rules follows");
					for (Subscription sub : pull) {
						conn.println("PULL " + sub.getHost() + ":" + sub.getPort()
							+ " " + sub.getGroup());
					}
					for (Subscription sub : push) {
						conn.println("PUSH " + sub.getHost() + ":" + sub.getPort()
							+ " " + sub.getGroup());
					}
					conn.println(".");
				} else {
					conn.println("401 unknown sub command");
				}
			} else if (commands.length == 3 && commands[1].equalsIgnoreCase("DELETE")) {
				StorageManager.current().delete(commands[2]);
				conn.println("200 article " + commands[2] + " deleted");
			} else if (commands.length == 4 && commands[1].equalsIgnoreCase("GROUPADD")) {
				channelAdd(commands, conn);
			} else if (commands.length == 3 && commands[1].equalsIgnoreCase("GROUPDEL")) {
				Group group = StorageManager.current().getGroup(commands[2]);
				if (group == null) {
					conn.println("400 group not found");
				} else {
					group.setFlag(Group.DELETED);
					group.update();
					conn.println("200 group " + commands[2] + " marked as deleted");
				}
			} else if (commands.length == 4 && commands[1].equalsIgnoreCase("SET")) {
				String key = commands[2];
				String val = commands[3];
				Config.inst().set(key, val);
				conn.println("200 new config value set");
			} else if (commands.length == 3 && commands[1].equalsIgnoreCase("GET")) {
				String key = commands[2];
				String val = Config.inst().get(key, null);
				if (val != null) {
					conn.println("100 config value for " + key + " follows");
					conn.println(val);
					conn.println(".");
				} else {
					conn.println("400 config value not set");
				}
			} else if (commands.length >= 3 && commands[1].equalsIgnoreCase("LOG")) {
				Group group = null;
				if (commands.length > 3) {
					group = (Group) Channel.getByName(commands[3]);
				}

				if (commands[2].equalsIgnoreCase("CONNECTED_CLIENTS")) {
					conn.println("100 number of connections follow");
					conn.println(Integer.toString(Stats.getInstance().connectedClients()));
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("POSTED_NEWS")) {
					conn.println("100 hourly numbers of posted news yesterday");
					for (int n = 0; n < 24; n++) {
						conn.println(n + " " + Stats.getInstance().getYesterdaysEvents(Stats.POSTED_NEWS, n, group));
					}
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("GATEWAYED_NEWS")) {
					conn.println("100 hourly numbers of gatewayed news yesterday");
					for (int n = 0; n < 24; n++) {
						conn.println(n + " " + Stats.getInstance().getYesterdaysEvents(Stats.GATEWAYED_NEWS, n, group));
					}
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("TRANSMITTED_NEWS")) {
					conn.println("100 hourly numbers of news transmitted to peers yesterday");
					for (int n = 0; n < 24; n++) {
						conn.println(n + " " + Stats.getInstance().getYesterdaysEvents(Stats.FEEDED_NEWS, n, group));
					}
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("HOSTED_NEWS")) {
					conn.println("100 number of overall hosted news");
					conn.println(Integer.toString(Stats.getInstance().getNumberOfNews()));
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("HOSTED_GROUPS")) {
					conn.println("100 number of hosted groups");
					conn.println(Integer.toString(Stats.getInstance().getNumberOfGroups()));
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("POSTED_NEWS_PER_HOUR")) {
					conn.println("100 posted news per hour");
					conn.println(Double.toString(Stats.getInstance().postedPerHour(-1)));
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("FEEDED_NEWS_PER_HOUR")) {
					conn.println("100 feeded news per hour");
					conn.println(Double.toString(Stats.getInstance().feededPerHour(-1)));
					conn.println(".");
				} else if (commands[2].equalsIgnoreCase("GATEWAYED_NEWS_PER_HOUR")) {
					conn.println("100 gatewayed news per hour");
					conn.println(Double.toString(Stats.getInstance().gatewayedPerHour(-1)));
					conn.println(".");
				} else {
					conn.println("401 unknown sub command");
				}
			} else if (commands.length >= 3 && commands[1].equalsIgnoreCase("PLUGIN")) {
			} else {
				conn.println("400 invalid command usage");
			}
		} else {
			conn.println("501 not allowed");
		}
	}
}
