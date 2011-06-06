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
package org.sonews.storage.impl;

import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;

/**
 *
 * @author Christian Lins
 * @since sonews/1.1
 */
public class HSQLDBProvider implements StorageProvider {

	public boolean isSupported(String uri) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Storage storage(Thread thread) throws StorageBackendException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
