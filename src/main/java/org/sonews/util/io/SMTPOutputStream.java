/*
GNU-Classpath Extensions: javamail
Copyright (C) 2002 Chris Burdess

For more information on the classpathx please mail:
nferrier@tapsellferrier.co.uk

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.sonews.util.io; // original package: gnu.mail.providers.smtp

import java.io.FilterOutputStream;
import java.io.IOException;

/**
 * An output stream implementing the SMTP specification for dot escaping.
 * Objects of this class are intended to be chained with CRLFOutputStream
 * instances as all SMTP output is intended to be CRLF-escaped.
 * 
 * @author dog@gnu.org
 * @version 0.1
 */
public class SMTPOutputStream extends FilterOutputStream {

    /**
     * The LF octet.
     */
    public static final int LF = 0x0a;
    /**
     * The dot octet.
     */
    public static final int DOT = 0x2e;
    /**
     * The last octet read.
     */
    protected int last;

    /**
     * Constructs an SMTP output stream connected to the specified output
     * stream. The underlying output stream should coordinate proper CRLF pairs
     * at line ends.
     * 
     * @param out
     *            a CRLFOutputStream
     */
    public SMTPOutputStream(CRLFOutputStream out) {
        super(out);
    }

    /**
     * Writes a character to the underlying stream.
     * 
     * @exception IOException
     *                if an I/O error occurred
     */
    @Override
    public void write(int ch) throws IOException {
        if (ch == DOT) {
            if (last == LF) {
                out.write(DOT);
            }
        }
        out.write(ch);
        last = ch;
    }

    /**
     * Writes a byte array to the underlying stream.
     * 
     * @exception IOException
     *                if an I/O error occurred
     */
    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes a portion of a byte array to the underlying stream.
     * 
     * @exception IOException
     *                if an I/O error occurred
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        int d = off;
        len += off;
        for (int i = off; i < len; i++) {
            switch (b[i]) {
            case DOT:
                int l = (i - d);
                if (l > 0) {
                    out.write(b, d, l);
                }
                d = i;
                if (last == LF) {
                    out.write(DOT);
                }
                break;
            }
            last = b[i];
        }
        if (len - d > 0) {
            out.write(b, d, len - d);
        }
    }
}
