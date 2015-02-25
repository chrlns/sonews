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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sonews.util.Log;
import org.sonews.util.io.Resource;

/**
 * For every group that is synchronized with or from a remote newsserver a
 * Subscription instance exists.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Subscription {

    private static List<Subscription> allSubs;

    /**
     * @return List of all groups this server handles.
     */
    public static List<Subscription> getAll() {
        if(allSubs == null) {
            String peersStr = Resource.getAsString("peers.conf", true);
            if(peersStr == null) {
                Log.get().log(Level.WARNING, "Could not read peers.conf");
                return new ArrayList<>(); // return empty list
            }

            String[] peersLines = peersStr.split("\n");
            List<Subscription> subs = new ArrayList<>(peersLines.length);
            for(String subLine : peersLines) {
                if(subLine.startsWith("#")) {
                    continue;
                }

                subLine = subLine.trim();
                String[] subLineChunks = subLine.split("\\s+");
                if(subLineChunks.length != 3) {
                    Log.get().log(Level.WARNING, "Malformed peers.conf line: {0}", subLine);
                } else {
                    int feedtype = FeedManager.PULL;
                    if (subLineChunks[0].contains("PUSH")) {
                        feedtype = FeedManager.PUSH;
                    }
                    Log.get().log(Level.INFO, "Found peer subscription {0}", feedtype);
                    Subscription sub = new Subscription(subLineChunks[2], 119, feedtype, subLineChunks[1]);
                    subs.add(sub);
                }
            }

            // The subscription loading is not synchronized so it is possible that
            // this method is called multiple times parallel.
            // Therefore we better set allSubs in a (more or less) atomic way...
            Subscription.allSubs = subs;
        }
        return allSubs;
    }

    private final String host;
    private final int port;
    private final int feedtype;
    private final String group;

    public Subscription(String host, int port, int feedtype, String group) {
        this.host = host;
        this.port = port;
        this.feedtype = feedtype;
        this.group = group;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Subscription) {
            Subscription sub = (Subscription) obj;
            return sub.host.equals(host) && sub.group.equals(group)
                    && sub.port == port && sub.feedtype == feedtype;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return host.hashCode() + port + feedtype + group.hashCode();
    }

    public int getFeedtype() {
        return feedtype;
    }

    public String getGroup() {
        return group;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
