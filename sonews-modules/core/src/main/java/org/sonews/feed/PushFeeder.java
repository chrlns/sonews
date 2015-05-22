/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
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

package org.sonews.feed;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sonews.daemon.DaemonRunner;

import org.sonews.storage.Article;
import org.sonews.storage.Headers;
import org.sonews.util.Log;
import org.sonews.util.io.ArticleWriter;

/**
 * Pushes new articles to remote newsservers. This feeder sleeps until a new
 * message is posted to the sonews instance.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class PushFeeder extends DaemonRunner {

    // TODO Make configurable
    public static final int QUEUE_SIZE = 128;

    private final LinkedBlockingQueue<Article> articleQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    @Override
    public void run() {
        while (daemon.isRunning()) {
            try {
                Article article = this.articleQueue.take();
                String[] newsgroupsHeader = article.getHeader(Headers.NEWSGROUPS);
                if (newsgroupsHeader == null) {
                    Log.get().warning("Article has no newsgroups header(s). Skipping.");
                    continue;
                }

                String[] groups = newsgroupsHeader[0].split(",");

                Log.get().log(Level.INFO, "PushFeed: {0}", article.getMessageID());
                
                Subscription.getAll().stream()
                        .filter(sub -> (sub.getFeedtype() == FeedManager.PUSH))
                        .forEach(sub -> push(article, groups, sub));
                
            } catch (InterruptedException ex) {
                Log.get().log(Level.WARNING, "PushFeeder interrupted: {0}", ex);
            }
        }
    }
    
    protected void push(Article article, String[] groups, Subscription sub) {
        // Circle check
        if (article.getHeader(Headers.PATH)[0].contains(sub.getHost())) {
            Log.get().log(
                    Level.INFO, "{0} skipped for host {1}",
                    new Object[]{article.getMessageID(), sub.getHost()});
            return;
        }

        try {
            for (String group : groups) {
                if (sub.getGroup().equals(group)) {
                    // Delete headers that may cause problems
                    article.removeHeader(Headers.NNTP_POSTING_DATE);
                    article.removeHeader(Headers.NNTP_POSTING_HOST);
                    article.removeHeader(Headers.X_COMPLAINTS_TO);
                    article.removeHeader(Headers.X_TRACE);
                    article.removeHeader(Headers.XREF);

                    // POST the message to remote server
                    ArticleWriter awriter = new ArticleWriter(
                            sub.getHost(), sub.getPort());
                    awriter.writeArticle(article);
                    break;
                }
            }
        } catch (IOException ex) {
            Log.get().warning(ex.toString());
        }
    }

    public void queueForPush(Article article) {
        try {
            // If queue is full, this call blocks until the queue has free space;
            // This is probably a bottleneck for article posting
            this.articleQueue.put(article);
        } catch (InterruptedException ex) {
            Log.get().log(Level.WARNING, null, ex);
        }
    }
}
