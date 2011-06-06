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
 * Provides access to a storage backend.
 * @author Christian Lins
 * @since sonews/1.0
 */
public final class StorageManager {

	private static StorageProvider provider;

	public static Storage current()
			throws StorageBackendException {
		synchronized (StorageManager.class) {
			if (provider == null) {
				return null;
			} else {
				return provider.storage(Thread.currentThread());
			}
		}
	}

	public static StorageProvider loadProvider(String pluginClassName) {
		try {
			Class<?> clazz = Class.forName(pluginClassName);
			Object inst = clazz.newInstance();
			return (StorageProvider) inst;
		} catch (Exception ex) {
			System.err.println(ex);
			return null;
		}
	}

	/**
	 * Sets the current storage provider.
	 * @param provider
	 */
	public static void enableProvider(StorageProvider provider) {
		synchronized (StorageManager.class) {
			if (StorageManager.provider != null) {
				disableProvider();
			}
			StorageManager.provider = provider;
		}
	}

	/**
	 * Disables the current provider.
	 */
	public static void disableProvider() {
		synchronized (StorageManager.class) {
			provider = null;
		}
	}
}
