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

package org.sonews.config;

import java.util.logging.Level;

import org.sonews.util.Log;

/**
 * Configuration facade class.
 *
 * @author Christian Lins
 * @since sonews/1.0
 */
public class Config extends AbstractConfig {

    public static final int LEVEL_CLI = 1;
    public static final int LEVEL_FILE = 2;

    public static final String CONFIGFILE = "sonews.configfile";
    /**
     * BackendConfig key constant. Value is the maximum article size in
     * kilobytes.
     */
    public static final String ARTICLE_MAXSIZE = "sonews.article.maxsize";
    /**
     * BackendConfig key constant. Value: Amount of news that are feeded per
     * run.
     */
    public static final String EVENTLOG = "sonews.eventlog";

    public static final String FEED_NEWSPERRUN = "sonews.feed.newsperrun";
    public static final String FEED_PULLINTERVAL = "sonews.feed.pullinterval";

    public static final String HOSTNAME = "sonews.hostname";
    public static final String PORT = "sonews.port";
    public static final String TIMEOUT = "sonews.timeout";
    public static final String LOGLEVEL = "sonews.loglevel";

    public static final String MLPOLL_DELETEUNKNOWN = "sonews.mlpoll.deleteunknown";
    public static final String MLPOLL_HOST = "sonews.mlpoll.host";
    public static final String MLPOLL_PASSWORD = "sonews.mlpoll.password";
    public static final String MLPOLL_USER = "sonews.mlpoll.user";

    public static final String MLSEND_ADDRESS = "sonews.mlsend.address";
    public static final String MLSEND_RW_FROM = "sonews.mlsend.rewrite.from";
    public static final String MLSEND_RW_SENDER = "sonews.mlsend.rewrite.sender";
    public static final String MLSEND_HOST = "sonews.mlsend.host";
    public static final String MLSEND_PASSWORD = "sonews.mlsend.password";
    public static final String MLSEND_PORT = "sonews.mlsend.port";
    public static final String MLSEND_USER = "sonews.mlsend.user";
    public static final String MLSEND_AUTH = "sonews.mlsend.auth";

    /**
     * Key constant. If value is "true" every I/O is written to logfile (which
     * is a lot!)
     */
    public static final String DEBUG = "sonews.debug";

    /** Key constant. Value is classname of the JDBC driver */
    public static final String STORAGE_DBMSDRIVER = "sonews.storage.dbmsdriver";
    /** Key constant. Value is JDBC connect String to the database. */
    public static final String STORAGE_DATABASE = "sonews.storage.database";
    public static final String STORAGE_HOST = "sonews.storage.host";
    /** Key constant. Value is the username for the DBMS. */
    public static final String STORAGE_USER = "sonews.storage.user";
    /** Key constant. Value is the password for the DBMS. */
    public static final String STORAGE_PASSWORD = "sonews.storage.password";
    public static final String STORAGE_PORT     = "sonews.storage.port";
    public static final String STORAGE_PROVIDER = "sonews.storage.provider";

    /**
     * Key constant. Value is the name of the host which is allowed to use the
     * XDAEMON command; default: "localhost"
     */
    public static final String XDAEMON_HOST = "sonews.xdaemon.host";

    /** The config key for the filename of the logfile */
    public static final String LOGFILE = "sonews.log";
    public static final String[] AVAILABLE_KEYS = { ARTICLE_MAXSIZE, EVENTLOG,
            FEED_NEWSPERRUN, FEED_PULLINTERVAL, HOSTNAME, MLPOLL_DELETEUNKNOWN,
            MLPOLL_HOST, MLPOLL_PASSWORD, MLPOLL_USER, MLSEND_ADDRESS,
            MLSEND_HOST, MLSEND_PASSWORD, MLSEND_PORT, MLSEND_RW_FROM,
            MLSEND_RW_SENDER, MLSEND_USER, PORT, TIMEOUT, XDAEMON_HOST };
    private static final Config instance = new Config();

    public static Config inst() {
        return instance;
    }

    private Config() {
    }

    @Override
    public String get(final String key, final String def) {
        String val = CommandLineConfig.getInstance().get(key, null);

        if (val == null) {
            val = FileConfig.getInstance().get(key, def);
        }

        if (val == null) {
            Log.get().log(Level.WARNING, "Returning default value for {0}", key);
        }
        return val;
    }

    public String get(final int maxLevel, final String key, final String def) {
        String val = CommandLineConfig.getInstance().get(key, null);

        if (val == null && maxLevel >= LEVEL_FILE) {
            val = FileConfig.getInstance().get(key, null);
        }

        return val != null ? val : def;
    }

    @Override
    public void set(final String key, final String val) {
        set(LEVEL_FILE, key, val);
    }

    public void set(final int level, final String key, final String val) {
        switch (level) {
            case LEVEL_CLI: {
                CommandLineConfig.getInstance().set(key, val);
                break;
            }
            case LEVEL_FILE: {
                FileConfig.getInstance().set(key, val);
                break;
            }
        }
    }
}
