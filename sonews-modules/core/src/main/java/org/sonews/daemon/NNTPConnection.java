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

package org.sonews.daemon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.sonews.acl.User;
import org.sonews.storage.Article;
import org.sonews.storage.Group;

/**
 * Interface for an NNTP connection. This interface is implemented by
 * SynchronousNNTPConnection and AsynchronousNNTPConnection which use similar
 * but not compatible NIO APIs.
 *
 * @author Christian Lins
 */
public interface NNTPConnection {

    void close() throws IOException;

    ChannelLineBuffers getBuffers();

    Article getCurrentArticle();

    Charset getCurrentCharset();

    Group getCurrentGroup();

    ByteBuffer getInputBuffer();

    long getLastActivity();

    ByteBuffer getOutputBuffer();

    SocketChannelWrapper getSocketChannel();

    User getUser();

    void lineReceived(byte[] line);

    void println(byte[] line) throws IOException;

    void println(CharSequence line) throws IOException;

    void setCurrentArticle(Article art);

    void setCurrentGroup(Group group);

    void setLastActivity(long time);

    void setUser(User user);

    boolean tryReadLock();

    void unlockReadLock();
}
