package org.sonews.storage.impl;

import org.sonews.storage.Storage;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageProvider;

public class LocalStorageProvider implements StorageProvider {

	@Override
	public boolean isSupported(String uri) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Storage storage(Thread thread) throws StorageBackendException {
		// TODO Auto-generated method stub
		return null;
	}

}
