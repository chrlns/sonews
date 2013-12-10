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

package org.sonews.feed;

import org.sonews.storage.Article;

/**
 * Controlls push and pull feeder.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class FeedManager {

    public static final int TYPE_PULL = 0;
    public static final int TYPE_PUSH = 1;
    private static PullFeeder pullFeeder = new PullFeeder();
    private static PushFeeder pushFeeder = new PushFeeder();

    /**
     * Reads the peer subscriptions from database and starts the appropriate
     * PullFeeder or PushFeeder.
     */
    public static synchronized void startFeeding() {
        pullFeeder.start();
        pushFeeder.start();
    }

    public static void queueForPush(Article article) {
        pushFeeder.queueForPush(article);
    }

    private FeedManager() {
    }
}
