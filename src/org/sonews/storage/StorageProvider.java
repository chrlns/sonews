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

package org.sonews.storage;

/**
 * Provides access to storage backend instances.
 * @author Christian Lins
 * @since sonews/1.0
 */
public interface StorageProvider
{

	public boolean isSupported(String uri);

	/**
	 * This method returns the reference to the associated storage.
	 * The reference MAY be unique for each thread. In any case it MUST be
	 * thread-safe to use this method.
	 * @return The reference to the associated Storage.
	 */
	public Storage storage(Thread thread)
		throws StorageBackendException;
}
