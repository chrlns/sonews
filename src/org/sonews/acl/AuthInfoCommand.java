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

package org.sonews.acl;

import java.io.IOException;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.command.Command;
import org.sonews.storage.StorageBackendException;

/**
 *
 * @author Christian Lins
 * @since sonews/1.1
 */
public class AuthInfoCommand implements Command
{

	@Override
	public String[] getSupportedCommandStrings()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasFinished()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String impliedCapability()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isStateful()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void processLine(NNTPConnection conn, String line, byte[] rawLine)
		throws IOException, StorageBackendException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
