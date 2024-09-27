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

package org.sonews.daemon.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import org.sonews.auth.User;
import org.sonews.config.Config;
import org.sonews.daemon.CommandSelector;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.command.Command;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ThreadedNNTPConnection implements NNTPConnection, Runnable {

    public static final String NEWLINE = "\r\n"; // RFC defines this as newline
    public static final String MESSAGE_ID_PATTERN = "<[^>]+>";

    private Charset charset = Charset.forName("UTF-8");
    private Command command = null;

    @Autowired
    private ApplicationContext context;

    private Article currentArticle = null;
    private Group currentGroup = null;
    private volatile long lastActivity = System.currentTimeMillis();
    private User user;

    private final Socket socket;
    private PrintWriter out;

    @Autowired
    public ThreadedNNTPConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            String hello = "200 "
                    + Config.inst().get(Config.HOSTNAME, InetAddress.getLocalHost().getCanonicalHostName())
                    + " sonews news server ready, posting allowed";

            out = new PrintWriter(socket.getOutputStream());
            println(hello);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                lineReceived(line.getBytes());
            }
        } catch (SocketException | SocketTimeoutException ex) {
            Log.get().log(Level.INFO, "Connection to {0} closed.", socket.getRemoteSocketAddress());
        } catch (IOException ex) {
            Log.get().log(Level.SEVERE, "Error handling client connection", ex);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.get().log(Level.WARNING, "Error closing client socket", e);
            }
        }
    }

    /**
     * This method determines the fitting command processing class.
     *
     * @param line
     * @return
     */
    private Command parseCommandLine(String line) {
        String cmdStr = line.trim().split("\\s+")[0];
        CommandSelector csel = context.getBean(CommandSelector.class);
        return csel.get(cmdStr);
    }

    /**
     * Due to the readLockGate there is no need to synchronize this method.
     *
     * @param raw
     * @throws java.io.IOException
     * @throws IllegalArgumentException
     *             if raw is null.
     * @throws IllegalStateException
     *             if calling thread does not own the readLock.
     */
    public void lineReceived(byte[] raw) throws IOException {
        if (raw == null) {
            throw new IllegalArgumentException("raw is null");
        }

        this.lastActivity = System.currentTimeMillis();

        String line = new String(raw, this.charset);

        // There might be a trailing \r, but trim() is a bad idea
        // as it removes also leading spaces from long header lines.
        if (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
            raw = Arrays.copyOf(raw, raw.length - 1);
        }

        Log.get().log(Level.FINE, "<< {0}", line);

        if (command == null) {
            command = parseCommandLine(line);
            assert command != null;
        }

        try {
            // The command object will process the line we just received
            try {
                command.processLine(this, line, raw);
            } catch (StorageBackendException ex) {
                Log.get().info("Retry command processing after StorageBackendException");

                // Try it a second time, so that the backend has time to recover
                command.processLine(this, line, raw);
            }
        } catch (ClosedChannelException ex0) {
            try {
                StringBuilder strBuf = new StringBuilder();
                strBuf.append("Connection to ");
                strBuf.append(socket.getRemoteSocketAddress());
                strBuf.append(" closed: ");
                strBuf.append(ex0);
                Log.get().info(strBuf.toString());
            } catch (Exception ex0a) {
                Log.get().log(Level.INFO, ex0a.getLocalizedMessage(), ex0a);
            }
        } catch (IOException ex1) {
            // This will catch a second StorageBackendException
            command = null;
            Log.get().log(Level.WARNING, ex1.getLocalizedMessage(), ex1);
            println("403 Internal server error");

            // Should we end the connection here?
            // RFC says we MUST return 400 before closing the connection
            close();
        }

        if (command == null || command.hasFinished()) {
            command = null;
            charset = Charset.forName("UTF-8"); // Reset to default
        }
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        out.close();
        socket.close();
    }

    @Override
    public Article getCurrentArticle() {
        return currentArticle;
    }

    @Override
    public Charset getCurrentCharset() {
        return charset;
    }

    @Override
    public Group getCurrentGroup() {
        return currentGroup;
    }

    @Override
    public long getLastActivity() {
        return lastActivity;
    }

    @Override
    public SocketChannel getSocketChannel() {
        return null; // TODO remove from interface
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void println(byte[] line) {
        println(new String(line, charset));
    }

    @Override
    public void println(CharSequence line) {
        out.append(line);
        out.append(NEWLINE);
        out.flush();

        Log.get().log(Level.FINE, ">> {0}", line);
    }

    @Override
    public void setCurrentArticle(Article art) {
        currentArticle = art;
    }

    @Override
    public void setCurrentGroup(Group group) {
        currentGroup = group;
    }

    @Override
    public void setLastActivity(long time) {
        lastActivity = time;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

}
