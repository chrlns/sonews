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

package org.sonews;

import java.sql.Driver;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sonews.config.Config;
import org.sonews.daemon.ChannelLineBuffers;
import org.sonews.daemon.Connections;
import org.sonews.daemon.NNTPDaemon;
import org.sonews.feed.FeedManager;
import org.sonews.storage.StorageManager;
import org.sonews.storage.StorageProvider;
import org.sonews.util.Purger;
import org.sonews.util.io.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Startup class of the daemon.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
@Configuration
@ComponentScan
public class Application {

    /** Version information of the sonews daemon */
    public static final String VERSION = "sonews/2.1.0";

    /** The server's startup date */
    public static final LocalDateTime STARTDATE = LocalDateTime.now();

    /**
     * The main entrypoint.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.sonews").log(Level.INFO, VERSION);

        // Command line arguments
        boolean async = false;
        boolean feed = false; // Enable feeding?
        boolean purger = false; // Enable message purging?
        int port = -1;

        for (int n = 0; n < args.length; n++) {
            switch (args[n]) {
                case "-async": {
                    async = true;
                    break;
                }
                case "-c":
                case "-config": {
                    Config.inst().set(Config.LEVEL_CLI, Config.CONFIGFILE,
                            args[++n]);
                    System.out.println("Using config file " + args[n]);
                    break;
                }
                case "-C":
                case "-context": {
                    // FIXME: Additional context files
                    n++;
                    break;
                }
                case "-dumpjdbcdriver": {
                    System.out.println("Available JDBC drivers:");
                    Enumeration<Driver> drvs = DriverManager.getDrivers();
                    while (drvs.hasMoreElements()) {
                        System.out.println(drvs.nextElement());
                    }
                    return;
                }
                case "-feed": {
                    feed = true;
                    break;
                }
                case "-h":
                case "-help": {
                    printArguments();
                    return;
                }
                case "-p": {
                    port = Integer.parseInt(args[++n]);
                    break;
                }
                case "-plugin-storage": {
                    System.out
                            .println("Warning: -plugin-storage is not implemented!");
                    break;
                }
                case "-purger": {
                    purger = true;
                    break;
                }
                case "-v":
                case "-version":
                    // Simply return as the version info is already printed above
                    return;
            }
        }
        
        ApplicationContext context = 
                new AnnotationConfigApplicationContext(Application.class);
        context = new FileSystemXmlApplicationContext(
                new String[]{"sonews.xml"}, context);

        // Enable storage backend
        StorageProvider sprov = context.getBean("storageProvider", StorageProvider.class);
        StorageManager.enableProvider(sprov);

        ChannelLineBuffers.allocateDirect();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        // Start the listening daemon
        if (port <= 0) {
            port = Config.inst().get(Config.PORT, 119);
        }
        
        NNTPDaemon daemon = context.getBean(NNTPDaemon.class);
        daemon.setPort(port);
        daemon.start();

        // Start Connections purger thread...
        Connections.getInstance().start();

        // Start feeds
        if (feed) {
            FeedManager.startFeeding();
        }

        if (purger) {
            Purger purgerDaemon = new Purger();
            purgerDaemon.start();
        }

        // Wait for main thread to exit (setDaemon(false))
        daemon.join();
    }

    private static void printArguments() {
        String usage = Resource.getAsString("usage", true);
        System.out.println(usage);
    }

    public Application() {
    }
}
