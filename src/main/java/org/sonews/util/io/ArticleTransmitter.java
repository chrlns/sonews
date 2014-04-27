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

package org.sonews.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;

import org.sonews.util.Log;

/**
 * The ArticleTransmitter class encapsulates the functionality to transmit
 * a news article from one newsserver to another.
 * It uses the standart ARTICLE, GROUP and POST command to transmit the news
 * article.
 * There is no conversation done, the raw header and body parts are simply
 * read from one stream and written to the other.
 *
 * @author Christian Lins
 * @since sonews/2.0
 */
public class ArticleTransmitter {

    private static class Endpoint {
        public PrintWriter out;
        public BufferedReader in;
        public Socket socket;
    }

    private final String group;
    private final String messageID;

    public ArticleTransmitter(String group, String messageID) {
        this.messageID = messageID;
        this.group = group;
    }

    private static void changeGroup(String group, Endpoint ep)
            throws IOException {
        ep.out.print("GROUP " + group + "\r\n");
        ep.out.flush();

        String line = ep.in.readLine();
        if (null == line || !line.startsWith("211 ")) {
            throw new IOException("Unexpected reply to GROUP change: " + line);
        }
    }

    /**
     * Connects to the NNTP server identified by host and port and
     * reads the first server HELLO message of the server.
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    private Endpoint connect(String host, int port) throws IOException {
        Endpoint ep = new Endpoint();

        // Connect to NNTP server
        ep.socket = new Socket(host, port);
        ep.out = new PrintWriter(new OutputStreamWriter(ep.socket.getOutputStream(), "UTF-8"));
        ep.in = new BufferedReader(new InputStreamReader(ep.socket.getInputStream(), "UTF-8"));

        String line = ep.in.readLine();
        if (line == null || !line.startsWith("200 ")) {
            throw new IOException("Invalid hello from server: " + line);
        }

        return ep;
    }

    public void transfer(String srcHost, int srcPort, String dstHost, int dstPort)
            throws IOException {
        Endpoint src = connect(srcHost, srcPort);
        Endpoint dst = connect(dstHost, dstPort);
        String line;

        changeGroup(group, dst);

        src.out.print("ARTICLE " + this.messageID + "\r\n");
        src.out.flush();
        line = src.in.readLine();
        if (line == null) {
            Log.get().warning("Unexpected null reply from remote host");
            return;
        }
        if (line.startsWith("430 ")) {
            Log.get().log(Level.WARNING, "Message {0} not available at {1}",
                    new Object[]{this.messageID, srcHost});
            return;
        }
        if (!line.startsWith("220 ")) {
            throw new IOException("Unexpected reply to ARTICLE");
        }

        dst.out.print("POST\r\n");
        dst.out.flush();
        line = dst.in.readLine();
        if (!line.startsWith("340 ")) {
            throw new IOException("Unexpected reply to POST");
        }

        for(;;) {
            line = src.in.readLine();
            if (line == null) {
                Log.get().warning("Hmm, to early disconnect?");
                break;
            }

            dst.out.print(line);
            dst.out.print("\r\n");

            if (".".equals(line.trim())) {
                // End of article stream reached
                break;
            }
        }

        dst.out.flush();

        src.out.println("QUIT");
        src.out.flush();
        src.out.close();

        line = dst.in.readLine();
        if (line.startsWith("240 ")) {
            Log.get().log(Level.INFO, "Message {0} successfully transmitted", messageID);
        } else {
            Log.get().log(Level.WARNING, "POST: {0}", line);
        }

        dst.socket.close();
        src.socket.close();
    }
}
