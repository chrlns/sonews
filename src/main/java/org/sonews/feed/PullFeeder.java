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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sonews.config.Config;
import org.sonews.daemon.AbstractDaemon;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;
import org.sonews.util.io.ArticleReader;
import org.sonews.util.io.ArticleWriter;

/**
 * The PullFeeder class regularily checks another Newsserver for new messages.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class PullFeeder extends AbstractDaemon {

    private final Map<Subscription, Integer> highMarks = new HashMap<>();
    private BufferedReader in;
    private PrintWriter out;
    private final Set<Subscription> subscriptions = new HashSet<>();

    private void addSubscription(final Subscription sub) {
        subscriptions.add(sub);

        if (!highMarks.containsKey(sub)) {
            // Set a initial highMark
            this.highMarks.put(sub, 0);
        }
    }

    /**
     * Changes to the given group and returns its high mark.
     *
     * @param groupName
     * @return
     */
    private int changeGroup(String groupName) throws IOException {
        this.out.print("GROUP " + groupName + "\r\n");
        this.out.flush();

        String line = this.in.readLine();
        if (line.startsWith("211 ")) {
            int highmark = Integer.parseInt(line.split(" ")[3]);
            return highmark;
        } else {
            throw new IOException("GROUP " + groupName + " returned: " + line);
        }
    }

    private void connectTo(final String host, final int port)
            throws IOException, UnknownHostException {
        Socket socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String line = in.readLine();
        if (!(line.charAt(0) == '2')) { // Could be 200 or 2xx if posting is not allowed
            throw new IOException(line);
        }

        // Send MODE READER to peer, some newsservers are friendlier then
        this.out.print("MODE READER\r\n");
        this.out.flush();
        line = this.in.readLine();
        if (!(line.charAt(0) == '2')) {
            throw new IOException(line);
        }
    }

    private void disconnect() throws IOException {
        this.out.print("QUIT\r\n");
        this.out.flush();
        this.out.close();
        this.in.close();

        this.out = null;
        this.in = null;
    }
    
    private void getAndRepostArticle(Subscription sub, String messageID) {
        try {
            // Post the message via common socket connection
            ArticleReader aread = new ArticleReader(sub.getHost(), sub.getPort(), messageID);
            byte[] abuf = aread.getArticleData();
            if (abuf == null) {
                Log.get().log(
                        Level.WARNING, "Could not feed {0} from {1}",
                        new Object[]{messageID, sub.getHost()});
            } else {
                Log.get().log(
                        Level.INFO, "Feeding {0}", messageID);
                ArticleWriter awrite = new ArticleWriter(
                        "localhost", Config.inst().get(Config.PORT, 119));
                awrite.writeArticle(abuf);
                awrite.close();
            }
        } catch (IOException ex) {
            // There may be a temporary network failure
            ex.printStackTrace();
            Log.get().log(
                    Level.WARNING, "Skipping mail {0} due to exception.", messageID);
        }
    }

    /**
     * Uses the OVER or XOVER command to get a list of message overviews that
     * may be unknown to this feeder and are about to be peered.
     *
     * @param start
     * @param end
     * @return A list of message ids with potentially interesting messages.
     */
    private List<String> over(int start, int end) throws IOException {
        this.out.print("OVER " + start + "-" + end + "\r\n");
        this.out.flush();

        String line = this.in.readLine();
        if (line.startsWith("500 ")) // OVER not supported
        {
            this.out.print("XOVER " + start + "-" + end + "\r\n");
            this.out.flush();

            line = this.in.readLine();
        }

        if (line.startsWith("224 ")) {
            List<String> messages = new ArrayList<>();
            line = this.in.readLine();
            while (!".".equals(line)) {
                String mid = line.split("\t")[4]; // 5th should be the
                                                  // Message-ID
                messages.add(mid);
                line = this.in.readLine();
            }
            return messages;
        } else {
            throw new IOException("Server return for OVER/XOVER: " + line);
        }
    }

    @Override
    public void run() {
        while (isRunning()) {
            int pullInterval = 1000 * Config.inst().get(
                    Config.FEED_PULLINTERVAL, 3600);

            Log.get().info("Start PullFeeder run...");
            this.subscriptions.clear();
            for (Subscription sub : Subscription.getAll()) {
                if (sub.getFeedtype() == FeedManager.TYPE_PULL) {
                    addSubscription(sub);
                }
            }

            try {
                for (Subscription sub : this.subscriptions) {
                    String host = sub.getHost();
                    int port = sub.getPort();

                    try {
                        Log.get().log(
                                Level.INFO, "Feeding {0} from {1}", new Object[]{sub.getGroup(), sub.getHost()});
                        try {
                            connectTo(host, port);
                        } catch (SocketException ex) {
                            Log.get().log(
                                    Level.INFO, "Skipping {0}: {1}", new Object[]{sub.getHost(), ex});
                            continue;
                        }

                        int oldMark = this.highMarks.get(sub);
                        int newMark = changeGroup(sub.getGroup());

                        if (oldMark != newMark) {
                            List<String> messageIDs = over(oldMark, newMark);

                            for (String messageID : messageIDs) {
                                if (!StorageManager.current().isArticleExisting(messageID)) {
                                    getAndRepostArticle(sub, messageID);
                                }
                            } // for(;;)
                            this.highMarks.put(sub, newMark);
                        }

                        disconnect();
                    } catch (StorageBackendException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Log.get().severe(
                                "PullFeeder run stopped due to exception.");
                    }
                } // for(Subscription sub : subscriptions)

                Log.get().log(
                        Level.INFO, "PullFeeder run ended. Waiting {0}s", pullInterval / 1000);
                Thread.sleep(pullInterval);
            } catch (InterruptedException ex) {
                Log.get().warning(ex.getMessage());
            }
        }
    }
}
