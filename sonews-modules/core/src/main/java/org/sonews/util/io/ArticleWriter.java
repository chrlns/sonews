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

package org.sonews.util.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.sonews.storage.Article;

/**
 * Posts an Article to a NNTP server using the POST command.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleWriter {

    private final BufferedOutputStream out;
    private final BufferedReader inr;
    private final Socket socket;

    public ArticleWriter(String host, int port) throws IOException,
            UnknownHostException {
        // Connect to NNTP server
        this.socket = new Socket(host, port);
        this.out = new BufferedOutputStream(socket.getOutputStream());
        this.inr = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), "UTF-8"));
        String line = inr.readLine();
        if (line == null || !line.startsWith("200 ")) {
            throw new IOException("Invalid hello from server: " + line);
        }
    }

    public void close() throws IOException, UnsupportedEncodingException {
        this.out.write("QUIT\r\n".getBytes("UTF-8"));
        this.out.flush();

        this.socket.close();
    }

    protected void finishPOST() throws IOException {
        this.out.write("\r\n.\r\n".getBytes("UTF-8"));
        this.out.flush();
        String line = inr.readLine();
        if (line == null || (!line.startsWith("240 ") && !line.startsWith("441 "))) {
            throw new IOException(line);
        }
    }

    protected void preparePOST() throws IOException {
        this.out.write("POST\r\n".getBytes("UTF-8"));
        this.out.flush();

        String line = this.inr.readLine();
        if (line == null || !line.startsWith("340 ")) {
            throw new IOException(line);
        }
    }

    public void writeArticle(Article article) throws IOException,
            UnsupportedEncodingException {
        byte[] buf = new byte[512];
        ArticleInputStream in = new ArticleInputStream(article);

        preparePOST();

        int len = in.read(buf);
        while (len != -1) {
            writeLine(buf, len);
            len = in.read(buf);
        }

        finishPOST();
    }

    /**
     * Writes the raw content of an article to the remote server. This method
     * does no charset conversion/handling of any kind so its the preferred
     * method for sending an article to remote peers.
     *
     * @param rawArticle
     * @throws IOException
     */
    public void writeArticle(byte[] rawArticle) throws IOException {
        preparePOST();
        writeLine(rawArticle, rawArticle.length);
        finishPOST();
    }

    /**
     * Writes the given buffer to the connect remote server.
     *
     * @param buffer
     * @param len
     * @throws IOException
     */
    protected void writeLine(byte[] buffer, int len) throws IOException {
        this.out.write(buffer, 0, len);
        this.out.flush();
    }
}
