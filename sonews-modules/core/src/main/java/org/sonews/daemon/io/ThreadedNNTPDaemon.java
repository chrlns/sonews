/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.sonews.daemon.io;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.sonews.daemon.DaemonRunner;
import org.sonews.daemon.DaemonThread;
import org.sonews.daemon.NNTPDaemonRunnable;
import org.sonews.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ThreadedNNTPDaemon extends DaemonRunner implements NNTPDaemonRunnable {

    @Autowired
    private ApplicationContext context;
    private int port;
    private ServerSocket serverSocket = null;
    private ExecutorService threadPool;

    public ThreadedNNTPDaemon() {
    }
    
    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            Log.get().log(Level.INFO, "Server listening on port {0}", port);
            
            // Create a thread pool for handling connections
            threadPool = Executors.newCachedThreadPool();

            // Create and bind the server socket
            serverSocket = new ServerSocket(this.port);

            while (daemon.isRunning()) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    
                    Log.get().log(Level.INFO, "Connected: {0}", 
                            clientSocket.getRemoteSocketAddress());

                    // Create a new thread to handle the connection
                    var thread = context.getBean(ThreadedNNTPConnection.class, clientSocket);
                    threadPool.execute(thread);

                } catch (IOException ex) {
                    Log.get().log(Level.SEVERE, "IOException while accepting connection: {0}", ex.getMessage());
                    Log.get().info("Connection accepting sleeping for 5 seconds...");
                    Thread.sleep(5000);
                }
            }
        } catch (BindException ex) {
            Log.get().log(Level.SEVERE, ex.getLocalizedMessage() + " -> shutdown sonews", ex);
            daemon.requestShutdown();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    @Override
    public void dispose() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException ex) {
                Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }

    
    
}
