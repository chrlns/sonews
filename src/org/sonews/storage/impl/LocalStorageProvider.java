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

import java.io.File;

import org.sonews.config.Config;
import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;

/**
 * Provider for @see{LocalStorage}.
 * LocalStorage is a simple method for storing news in the local filesystem of
 * the server running SONEWS.
 * The news saved one file per news and are properly indexed for faster access.
 * Although it is not recommended to use LocalStorage for large installations
 * as the performance will decrease with growing numbers of news stored.
 * Additionally, there are hard limits dependending on the underlying OS and
 * filesystem.
 *
 * Directory structure:
 * $BASE$: Base directory of the LocalStorage, e.g. /var/share/sonews/stor0
 * $BASE$/news/: contains the news mails, one file per news named by its Message-ID
 * $BASE$/index/: contains index files referencing the files in ../news
 * 
 * @since sonews/1.1
 * @author Christian Lins
 */
public class LocalStorageProvider implements StorageProvider {

	private LocalStorage storage;
	
	public LocalStorageProvider() {
		String storageBase = Config.inst().
			get(Config.LEVEL_FILE, Config.STORAGE_DATABASE,  "sonews/stor0");
		
		// If the directory for the local storage does not exist yet,
		// create it!
		File dir = new File(storageBase);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		this.storage = new LocalStorage(storageBase);
	}
	
	@Override
	public boolean isSupported(String uri) {
		return uri.startsWith("sonews:local");
	}

	@Override
	public Storage storage(Thread thread) throws StorageBackendException {
		return storage;
	}

}
