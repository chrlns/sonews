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

package org.sonews.daemon;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.sonews.config.Config;

/**
 * Class holding ByteBuffers for SocketChannels/NNTPConnection. Due to the
 * complex nature of AIO/NIO we must properly handle the line buffers for the
 * input and output of the SocketChannels.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ChannelLineBuffers {

    /**
     * Size of one small buffer; per default this is 512 bytes to fit one
     * standard line.
     */
    public static final int BUFFER_SIZE = 512;
    
    private static final int MAX_CACHED_BUFFERS_DEFAULT = 2048; // Cached buffers default maximum
    private static final List<ByteBuffer> freeSmallBuffers = new ArrayList<>(MAX_CACHED_BUFFERS_DEFAULT);
    
    /**
     * Allocates a predefined number of direct ByteBuffers (allocated via
     * ByteBuffer.allocateDirect()). This method is Thread-safe, but should only
     * called at startup.
     */
    public static void allocateDirect() {
        /*synchronized (freeSmallBuffers) {
            int maxCachedBuffers = Config.inst().get(
                    Config.PERF_MAX_CACHED_BUFFERS, MAX_CACHED_BUFFERS_DEFAULT);
            for (int n = 0; n < maxCachedBuffers; n++) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                freeSmallBuffers.add(buffer);
            }
        }*/
    }

    // Both input and output buffers should be final as we synchronize on them,
    // but the buffers are set somewhere to another object or null. 
    private final ByteBuffer inputBuffer = newLineBuffer();
    private final List<ByteBuffer> outputBuffers = new LinkedList<>();
    private boolean outputBuffersClosed = false;

    public ChannelLineBuffers() {
    }

    /**
     * Add the given ByteBuffer to the list of buffers to be send to the client.
     * This method is Thread-safe.
     *
     * @param buffer
     * @throws java.nio.channels.ClosedChannelException
     *             If the client channel was already closed.
     */
    public void addOutputBuffer(ByteBuffer buffer)
            throws ClosedChannelException {
        synchronized(outputBuffers) {
            if (outputBuffersClosed) {
                throw new ClosedChannelException();
            }
            outputBuffers.add(buffer);
        }
    }

    /**
     * Currently a channel has only one input buffer. This *may* be a bottleneck
     * and should investigated in the future.
     *
     * @return The input buffer associated with given channel.
     */
    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }

    /**
     * Returns the current output buffer for writing(!) to SocketChannel.
     *
     * @return The next input buffer that contains unprocessed data or null if
     *         the connection was closed or there are no more unprocessed
     *         buffers.
     */
    public ByteBuffer getOutputBuffer() {
        synchronized (outputBuffers) {
            if (outputBuffers.isEmpty()) {
                // Wake up ConnectionWorkers that are waiting for the output
                // to be flushed
                outputBuffers.notifyAll();
                return null;
            } else {
                ByteBuffer buffer = outputBuffers.get(0);
                if (buffer.remaining() == 0) {
                    outputBuffers.remove(0);
                    // Add old buffers to the list of free buffers
                    recycleBuffer(buffer);
                    buffer = getOutputBuffer();
                }
                return buffer;
            }
        }
    }

    /**
     * @return false if there are output buffers pending to be written to the
     *         client.
     */
    boolean isOutputBufferEmpty() {
        synchronized (outputBuffers) {
            return outputBuffers.isEmpty();
        }
    }
    
    /**
     * Blocks while output buffers are not empty.
     * @throws InterruptedException 
     */
    public void waitForOutput() throws InterruptedException {
        synchronized(outputBuffers) {
            while(!isOutputBufferEmpty()) {
                outputBuffers.wait();
            }
        }
    }

    /**
     * Goes through the input buffer and searches for next line terminator. 
     * If a '\n' is found, the bytes up to the line terminator
     * are returned as array of bytes (the line terminator is omitted). If none
     * is found the method returns null.
     *
     * @return A ByteBuffer wrapping the line.
     */
    public ByteBuffer nextInputLine() {
        if (inputBuffer == null) {
            return null;
        }
        
        synchronized(inputBuffer) {
            // Mark the current write position
            int mark = inputBuffer.position();

            // Set position to 0 and limit to current position
            inputBuffer.flip();

            ByteBuffer lineBuffer = newLineBuffer();

            while (inputBuffer.position() < inputBuffer.limit()) {
                byte b = inputBuffer.get();
                if (b == 10) // '\n'
                {
                    // The bytes between the buffer's current position and its
                    // limit, if any, are copied to the beginning of the buffer.
                    // That is, the byte at index p = position() is copied to
                    // index zero, the byte at index p + 1 is copied to index
                    // one, and so forth until the byte at index limit() - 1
                    // is copied to index n = limit() - 1 - p.
                    // The buffer's position is then set to n+1 and its limit is
                    // set to its capacity.
                    inputBuffer.compact();

                    lineBuffer.flip(); // limit to position, position to 0
                    return lineBuffer;
                } else {
                    lineBuffer.put(b);
                }
            }

            inputBuffer.limit(BUFFER_SIZE);
            inputBuffer.position(mark);

            if (inputBuffer.hasRemaining()) { // TODO Is this correct here?
                return null;
            } else {
                // In the first 512 was no newline found, so the input is not
                // standard compliant. We return the current buffer as new line
                // and add a space to the beginning of the next line which
                // corrects some overlong header lines.
                inputBuffer.clear();
                inputBuffer.put((byte) ' ');
            }

            lineBuffer.flip(); // limit to position, position to 0
            return lineBuffer;
        }
    }

    /**
     * Returns a at least 512 bytes long ByteBuffer ready for usage. The method
     * first try to reuse an already allocated (cached) buffer but if that fails
     * returns a newly allocated direct buffer. Use recycleBuffer() method when
     * you do not longer use the allocated buffer.
     */
    static ByteBuffer newLineBuffer() {
        ByteBuffer buf = null;
        synchronized (freeSmallBuffers) {
            if (!freeSmallBuffers.isEmpty()) {
                buf = freeSmallBuffers.remove(0);
            }
        }

        if (buf == null) {
            // Allocate a non-direct buffer
            buf = ByteBuffer.allocate(BUFFER_SIZE);
        }

        assert buf != null;
        assert buf.position() == 0;
        assert buf.limit() >= BUFFER_SIZE;

        return buf;
    }

    /**
     * Adds the given buffer to the list of free buffers if it is a valuable
     * direct allocated buffer.
     *
     * @param buffer
     */
    public static void recycleBuffer(ByteBuffer buffer) {
        assert buffer != null;

        if (buffer.isDirect()) {
            assert buffer.capacity() >= BUFFER_SIZE;

            // Add old buffers to the list of free buffers
            synchronized (freeSmallBuffers) {
                buffer.clear(); // Set position to 0 and limit to capacity
                freeSmallBuffers.add(buffer);
            }
        } // if(buffer.isDirect())
    }

    /**
     * Recycles all buffers of this ChannelLineBuffers object.
     */
    public void recycleBuffers() {
        synchronized (inputBuffer) {
            this.inputBuffer.clear();
        }

        synchronized (outputBuffers) {
            outputBuffers.forEach(ChannelLineBuffers::recycleBuffer);
            outputBuffers.clear();
            outputBuffersClosed = true;
        }
    }
}
