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

package org.sonews.daemon.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.sonews.acl.User;
import org.sonews.daemon.ChannelLineBuffers;
import org.sonews.daemon.LineEncoder;
import org.sonews.daemon.NNTPConnection;
import org.sonews.daemon.SocketChannelWrapper;
import org.sonews.daemon.SocketChannelWrapperFactory;
import static org.sonews.daemon.sync.SynchronousNNTPConnection.NEWLINE;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.util.Log;

/**
 *
 * @author Christian Lins
 */
public class AsynchronousNNTPConnection implements NNTPConnection {

    private final AsynchronousSocketChannel channel;
    private final SocketChannelWrapper channelWrapper;
    private final ChannelLineBuffers lineBuffers = new ChannelLineBuffers();

    public AsynchronousNNTPConnection(AsynchronousSocketChannel channel) {
        this.channel = channel;
        this.channelWrapper = new SocketChannelWrapperFactory(channel).create();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelLineBuffers getBuffers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getLastActivity() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SocketChannelWrapper getSocketChannel() {
        return channelWrapper;
    }

    @Override
    public boolean tryReadLock() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void lineReceived(byte[] line) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlockReadLock() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ByteBuffer getOutputBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLastActivity(long time) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ByteBuffer getInputBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Article getCurrentArticle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Charset getCurrentCharset() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getCurrentGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public User getUser() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Puts the given line into the output buffer, adds a newline character and
     * returns. The method returns immediately and does not block until the line
     * was sent. If line is longer than 510 octets it is split up in several
     * lines. Each line is terminated by \r\n (NNTPConnection.NEWLINE).
     *
     * @param line
     * @param charset
     * @throws java.io.IOException
     */
    public void println(final CharSequence line, final Charset charset)
            throws IOException {
        writeToChannel(CharBuffer.wrap(line), charset, line);
        writeToChannel(CharBuffer.wrap(NEWLINE), charset, null);
    }

    /**
     * Writes the given raw lines to the output buffers and finishes with a
     * newline character (\r\n).
     *
     * @param rawLines
     * @throws java.io.IOException
     */
    @Override
    public void println(final byte[] rawLines) throws IOException {
        this.lineBuffers.addOutputBuffer(ByteBuffer.wrap(rawLines));
        writeToChannel(CharBuffer.wrap(NEWLINE), StandardCharsets.UTF_8, null);
    }

    /**
     * Encodes the given CharBuffer using the given Charset to a bunch of
     * ByteBuffers (each 512 bytes large) and enqueues them for writing at the
     * connected SocketChannel.
     *
     * @throws java.io.IOException
     */
    private void writeToChannel(CharBuffer characters, final Charset charset,
            CharSequence debugLine) throws IOException {
        if (!charset.canEncode()) {
            Log.get().log(Level.SEVERE, "FATAL: Charset {0} cannot encode!",
                    charset);
            return;
        }

        // Write characters to output buffers
        LineEncoder lenc = new LineEncoder(characters, charset);
        lenc.encode(lineBuffers);

        //enableWriteEvents(debugLine);
    }

    @Override
    public void setCurrentArticle(Article art) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCurrentGroup(Group group) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setUser(User user) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(CharSequence line) {
       
    }


}
