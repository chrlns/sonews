/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.sonews.daemon.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import org.sonews.acl.User;
import org.sonews.config.Config;
import org.sonews.daemon.ChannelLineBuffers;
import org.sonews.daemon.CommandSelector;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.command.Command;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.StorageBackendException;
import org.sonews.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ThreadedNNTPConnection implements NNTPConnection, Runnable {

    public static final String NEWLINE = "\r\n"; // RFC defines this as newline
    public static final String MESSAGE_ID_PATTERN = "<[^>]+>";
    private static final Timer cancelTimer = new Timer(true); // Thread-safe?
                                                              // True for run as
                                                              // daemon
    /** SocketChannel is generally thread-safe */
    private SocketChannel channel;
    
    private Charset charset = Charset.forName("UTF-8");
    private Command command = null;
    
    @Autowired
    private ApplicationContext context;
    
    private Article currentArticle = null;
    private Group currentGroup = null;
    private volatile long lastActivity = System.currentTimeMillis();
    private final ChannelLineBuffers lineBuffers = new ChannelLineBuffers();
    private int readLock = 0;
    private final Object readLockGate = new Object();
    private SelectionKey writeSelKey = null;
    private User user;
    
    private final Socket clientSocket;

    public ThreadedNNTPConnection(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Handle the connection here
            // You'll need to implement the actual NNTP protocol handling
            // This is a placeholder for the connection handling logic
            String hello = "200 " + Config.inst().get(Config.HOSTNAME, InetAddress.getLocalHost().getCanonicalHostName())
                    + " sonews news server ready, posting allowed";
            clientSocket.getOutputStream().write(hello.getBytes());
            clientSocket.getOutputStream().flush();
            
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                lineReceived(line.getBytes());
            }
        } catch (IOException e) {
            Log.get().log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            try {
                clientSocket.close();
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
     * @throws IllegalArgumentException
     *             if raw is null.
     * @throws IllegalStateException
     *             if calling thread does not own the readLock.
     */
    public void lineReceived(byte[] raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw is null");
        }

       // if (readLock == 0 || readLock != Thread.currentThread().hashCode()) {
       //     throw new IllegalStateException("readLock not properly set");
       // }

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
                strBuf.append(channel.socket().getRemoteSocketAddress());
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
            shutdownInput();
            shutdownOutput();
        }

        if (command == null || command.hasFinished()) {
            command = null;
            charset = Charset.forName("UTF-8"); // Reset to default
        }
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ChannelLineBuffers getBuffers() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Article getCurrentArticle() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Charset getCurrentCharset() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Group getCurrentGroup() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ByteBuffer getInputBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long getLastActivity() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ByteBuffer getOutputBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public SocketChannel getSocketChannel() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public User getUser() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void println(byte[] line) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void println(CharSequence line) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setCurrentArticle(Article art) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setCurrentGroup(Group group) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setLastActivity(long time) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setUser(User user) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean tryReadLock() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void unlockReadLock() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    private void shutdownInput() {
        try {
            // Closes the input line of the channel's socket, so no new data
            // will be received and a timeout can be triggered.
            if (!channel.socket().isInputShutdown()) {
                channel.socket().shutdownInput();
            }
        } catch (IOException ex) {
            Log.get().log(Level.WARNING,
                    "Exception in NNTPConnection.shutdownInput()", ex);
        }
    }

    private void shutdownOutput() {
        cancelTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Closes the output line of the channel's socket.
                    if (!channel.socket().isOutputShutdown()) {
                        channel.socket().shutdownOutput();
                    }
                    if (channel.isConnected()) {
                        channel.close();
                    }
                } catch (SocketException ex) {
                    // Socket was already disconnected
                    Log.get().log(Level.INFO,
                            "SynchronousNNTPConnection.shutdownOutput()", ex);
                } catch (IOException ex) {
                    Log.get().log(Level.WARNING,
                            "SynchronousNNTPConnection.shutdownOutput()", ex);
                }
            }
        }, 3000);
    }

}
