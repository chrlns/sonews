package org.sonews.storage.impl;

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
 */
public class LocalStorageProvider implements StorageProvider {

	@Override
	public boolean isSupported(String uri) {
		return uri.startsWith("sonews:local");
	}

	@Override
	public Storage storage(Thread thread) throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

}
