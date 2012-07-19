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
package org.sonews.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.sonews.daemon.AbstractDaemon;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.storage.Headers;
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;

/**
 * The purger is started in configurable intervals to search for messages that
 * can be purged. A message must be deleted if its lifetime has exceeded, if it
 * was marked as deleted or if the maximum number of articles in the database is
 * reached.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Purger extends AbstractDaemon {

    /**
     * Loops through all messages and deletes them if their time has come.
     */
    @Override
    public void run() {
        try {
            while (isRunning()) {
                purgeDeleted();
                purgeOutdated();

                Thread.sleep(120000); // Sleep for two minutes
            }
        } catch (StorageBackendException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            Log.get().warning("Purger interrupted: " + ex);
        }
    }

    /**
     * Purge messages from storage backend that have been marked as deleted.
     * 
     * @throws StorageBackendException
     */
    private void purgeDeleted() throws StorageBackendException {
        List<Group> groups = StorageManager.current().getGroups();
        for (Group channel : groups) {
            if (!(channel instanceof Group)) {
                continue;
            }

            Group group = (Group) channel;
            // Look for groups that are marked as deleted
            if (group.isDeleted()) {
                List<Long> ids = StorageManager.current().getArticleNumbers(
                        group.getInternalID());
                if (ids.size() == 0) {
                    StorageManager.current().purgeGroup(group);
                    Log.get().info("Group " + group.getName() + " purged.");
                }

                for (int n = 0; n < ids.size() && n < 10; n++) {
                    Article art = StorageManager.current().getArticle(
                            ids.get(n), group.getInternalID());
                    StorageManager.current().delete(art.getMessageID());
                    Log.get()
                            .info("Article " + art.getMessageID() + " purged.");
                }
            }
        }
    }

    /**
     * Purge messages that are older then the given treshold.
     * 
     * @throws InterruptedException
     * @throws StorageBackendException
     */
    private void purgeOutdated() throws InterruptedException,
            StorageBackendException {
        long articleMaximum = Config.inst().get("sonews.article.maxnum",
                Long.MAX_VALUE);
        long lifetime = Config.inst().get("sonews.article.lifetime", -1);

        if (lifetime > 0
                || articleMaximum < Stats.getInstance().getNumberOfNews()) {
            Log.get().info("Purging old messages...");
            String mid = StorageManager.current().getOldestArticle();
            if (mid == null) { // No articles in the database
                return;
            }

            Article art = StorageManager.current().getArticle(mid);
            long artDate = 0;
            String dateStr = art.getHeader(Headers.DATE)[0];
            try {
                DateFormat dateFormat = DateFormat.getDateInstance(
                        DateFormat.LONG, Locale.US);
                artDate = dateFormat.parse(dateStr).getTime() / 1000 / 3600 / 24;
            } catch (ParseException ex) {
                Log.get().warning(
                        "Could not parse date string: " + dateStr + " " + ex);
            }

            // Should we delete the message because of its age or because the
            // article maximum was reached?
            if (lifetime < 0 || artDate < (new Date().getTime() + lifetime)) {
                StorageManager.current().delete(mid);
                System.out.println("Deleted: " + mid);
            } else {
                Thread.sleep(1000 * 60); // Wait 60 seconds
                return;
            }
        } else {
            Log.get().info("Lifetime purger is disabled");
            Thread.sleep(1000 * 60 * 30); // Wait 30 minutes
        }
    }
}
