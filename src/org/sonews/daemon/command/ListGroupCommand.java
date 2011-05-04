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
import java.util.List;
import org.sonews.daemon.NNTPConnection;
import org.sonews.storage.Channel;
import org.sonews.storage.StorageBackendException;

/**
 * Class handling the LISTGROUP command.
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ListGroupCommand implements Command
{

	@Override
	public String[] getSupportedCommandStrings()
	{
		return new String[] {"LISTGROUP"};
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

	@Override
	public void processLine(NNTPConnection conn, final String commandName, byte[] raw)
		throws IOException, StorageBackendException
	{
		final String[] command = commandName.split(" ");

		Channel group;
		if (command.length >= 2) {
			group = Channel.getByName(command[1]);
		} else {
			group = conn.getCurrentChannel();
		}

		if (group == null) {
			conn.println("412 no group selected; use GROUP <group> command");
			return;
		}

		List<Long> ids = group.getArticleNumbers();
		conn.println("211 " + ids.size() + " "
			+ group.getFirstArticleNumber() + " "
			+ group.getLastArticleNumber() + " list of article numbers follow");
		for (long id : ids) {
			// One index number per line
			conn.println(Long.toString(id));
		}
		conn.println(".");
	}
}
