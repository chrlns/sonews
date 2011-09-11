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
package org.sonews;

import java.sql.SQLException;
import java.util.Map;
import org.sonews.daemon.AbstractDaemon;

/**
 * Will force all other threads to shutdown cleanly.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class ShutdownHook extends Thread {

	/**
	 * Called when the JVM exits.
	 */
	@Override
	public void run() {
		System.out.println("sonews: Trying to shutdown all threads...");

		Map<Thread, StackTraceElement[]> threadsMap = Thread.getAllStackTraces();
		for (Thread thread : threadsMap.keySet()) {
			// Interrupt the thread if it's a AbstractDaemon
			AbstractDaemon daemon;
			if (thread instanceof AbstractDaemon && thread.isAlive()) {
				try {
					daemon = (AbstractDaemon) thread;
					daemon.shutdownNow();
				} catch (SQLException ex) {
					System.out.println("sonews: " + ex);
				}
			}
		}

		for (Thread thread : threadsMap.keySet()) {
			AbstractDaemon daemon;
			if (thread instanceof AbstractDaemon && thread.isAlive()) {
				daemon = (AbstractDaemon) thread;
				System.out.println("sonews: Waiting for " + daemon + " to exit...");
				try {
					daemon.join(500);
				} catch (InterruptedException ex) {
					System.out.println(ex.getLocalizedMessage());
				}
			}
		}

		// We have notified all not-sleeping AbstractDaemons of the shutdown;
		// all other threads can be simply purged on VM shutdown

		System.out.println("sonews: Clean shutdown.");
	}
}
