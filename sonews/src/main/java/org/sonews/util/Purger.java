/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
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
import java.util.logging.Level;
import org.sonews.config.Config;
import org.sonews.daemon.DaemonRunnable;
import org.sonews.daemon.DaemonRunner;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.Headers;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The purger is started in configurable intervals to search for messages that
 * can be purged. A message must be deleted if its lifetime has exceeded, if it
 * was marked as deleted or if the maximum number of articles in the database is
 * reached.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Component
public class Purger extends DaemonRunner implements DaemonRunnable {

    @Autowired
    private Log logger;

    /**
     * Loops through all messages and deletes them if their time has come.
     */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        try {
            while (daemon.isRunning()) {
                purgeDeleted();
                purgeOutdated();

                Thread.sleep(5 * 60 * 1000); // Sleep for 5 minutes
            }
        } catch (StorageBackendException ex) {
            logger.log(Level.WARNING, "Storage exception", ex);
        } catch (InterruptedException ex) {
            logger.info("Purger interrupted");
        }
    }

    /**
     * Purge messages from storage backend that have been marked as deleted.
     *
     * @throws StorageBackendException
     */
    private void purgeDeleted() throws StorageBackendException {
        List<Group> groups = Group.getAll();
        if (groups == null) {
            logger.warning("No groups?");
            return;
        }

        for (Group group : groups) {
            // Look for groups that are marked as deleted
            if (group.isDeleted()) {
                List<Long> ids = StorageManager.current().getArticleNumbers(group.getInternalID());
                if (ids.isEmpty()) {
                    StorageManager.current().purgeGroup(group);
                    logger.log(Level.INFO, "Group {0} purged.", group.getName());
                }

                for (int n = 0; n < ids.size() && n < 10; n++) {
                    Article art = StorageManager.current().getArticle(ids.get(n), group.getInternalID());
                    if (art != null) {
                        StorageManager.current().delete(art.getMessageID());
                        logger.log(Level.INFO, "Article {0} purged.", art.getMessageID());
                    }
                }
            }
        }
    }

    private int msAsDays(long time) {
        return (int) (time / 1000 / 3600 / 24);
    }

    private int currentUnixDays() {
        return msAsDays(new Date().getTime());
    }

    /**
     * Purge messages that are older then the given treshold.
     *
     * @throws InterruptedException
     * @throws StorageBackendException
     */
    private void purgeOutdated() throws InterruptedException, StorageBackendException {
        var smgr = StorageManager.current();
        var articleMaximum = Config.inst().get(Config.STORAGE_ARTICLE_MAXNUM, Long.MAX_VALUE);
        var lifetime = Config.inst().get(Config.STORAGE_ARTICLE_LIFETIME, -1);

        if (lifetime > 0 || articleMaximum < smgr.countArticles()) {
            boolean purged = false;
            do {
                logger.info("Purging old messages...");
                String mid = smgr.getOldestArticle();
                if (mid == null) { // No articles in the database
                    return;
                }

                Article art = smgr.getArticle(mid);
                if (art == null) {
                    logger.log(Level.WARNING, "Could not retrieve or delete article: {0}", mid);
                    return;
                }

                long artDate = 0; // Article age in UNIX Epoch days
                String dateStr = art.getHeader(Headers.DATE)[0];

                // FIXME Refactor using java.time classes
                try {
                    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
                    artDate = msAsDays(dateFormat.parse(dateStr).getTime());
                } catch (ParseException ex) {
                    logger.log(
                            Level.WARNING, "Could not parse date string: {0} {1}",
                            new Object[]{dateStr, ex});
                }

                // Should we delete the message because of its age or because the
                // article maximum was reached?
                if (lifetime < 0 || artDate < (currentUnixDays() - lifetime)) {
                    smgr.delete(mid);
                    logger.log(Level.INFO, "Deleted: {0}", mid);
                    purged = true;
                }
            } while (purged);
        }
    }
}
