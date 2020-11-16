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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.sonews.storage.Article;

/**
 * Capsulates an ArticleImpl to provide a raw InputStream.
 *
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleInputStream extends InputStream {

    private final byte[] buf;
    private int pos = 0;

    public ArticleInputStream(final Article art) throws IOException,
            UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(art.getHeaderSource().getBytes("UTF-8"));
        out.write("\r\n\r\n".getBytes("UTF-8"));
        out.write(art.getBody()); // Without CRLF
        out.flush();
        this.buf = out.toByteArray();
    }

    /**
     * This method reads one byte from the stream. The <code>pos</code> counter
     * is advanced to the next byte to be read. The byte read is returned as an
     * int in the range of 0-255. If the stream position is already at the end
     * of the buffer, no byte is read and a -1 is returned in order to indicate
     * the end of the stream.
     *
     * @return The byte read, or -1 if end of stream
     */
    @Override
    public synchronized int read() {
        if (pos < buf.length) {
            return ((int) buf[pos++]) & 0xFF;
        } else {
            return -1;
        }
    }
}
