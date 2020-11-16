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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Encodes a line to buffers using the correct charset.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class LineEncoder {

    private final CharBuffer characters;
    private final Charset charset;

    /**
     * Constructs new LineEncoder.
     *
     * @param characters
     * @param charset
     */
    public LineEncoder(CharBuffer characters, Charset charset) {
        this.characters = characters;
        this.charset = charset;
    }

    /**
     * Encodes the characters of this instance to the given ChannelLineBuffers
     * using the Charset of this instance.
     *
     * @param buffer
     * @throws java.nio.channels.ClosedChannelException
     */
    public void encode(ChannelLineBuffers buffer) throws ClosedChannelException {
        CharsetEncoder encoder = charset.newEncoder();
        while (characters.hasRemaining()) {
            ByteBuffer buf = ChannelLineBuffers.newLineBuffer();
            assert buf.position() == 0;
            assert buf.capacity() >= 512;

            CoderResult res = encoder.encode(characters, buf, true);

            // Set limit to current position and current position to 0;
            // means make ready for read from buffer
            buf.flip();
            buffer.addOutputBuffer(buf);

            if (res.isUnderflow()) // All input processed
            {
                break;
            }
        }
    }
}
