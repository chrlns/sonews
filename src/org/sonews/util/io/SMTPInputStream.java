/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter input stream for reading from SMTP (or NNTP or similar) socket
 * where lines containing single dot have special meaning â end of message.
 * 
 * @author FrantiÅ¡ek KuÄera (frantovo.cz)
 */
public class SMTPInputStream extends FilterInputStream {

	public static final int CR = 0x0d;
	public static final int LF = 0x0a;
	public static final int DOT = 0x2e;
	protected int last;

	public SMTPInputStream(InputStream in) {
		super(in);
	}

	/**
	 * @return one byte as expected 
	 * or -2 if there was line with single dot (which means end of message)
	 * @throws IOException 
	 */
	@Override
	public int read() throws IOException {
		// read current character
		int ch = super.read();

		if (ch == DOT) {
			if (last == LF) {
				int next = super.read();

				if (next == CR || next == LF) { // There should be CRLF, but we may accept also just LF or CR with missing LF. Or should we be more strict?
					// <CRLF>.<CRLF> â end of current message
					ch = -2;
				} else {
					// <CRLF>.â¦ â eat one dot and return next character
					ch = next;
				}
			}
		}

		last = ch;
		return ch;
	}

	/**
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return See {@link FilterInputStream#read(byte[], int, int)} or -2 (then see {@link #read(byte[])})
	 * @throws IOException 
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (buffer == null) {
			throw new NullPointerException("Byte array should not be null.");
		} else if ((offset < 0) || (offset > buffer.length) || (length < 0) || ((offset + length) > buffer.length) || ((offset + length) < 0)) {
			throw new IndexOutOfBoundsException("Invalid offset or length.");
		} else if (length == 0) {
			return 0;
		}

		int ch = read();

		if (ch == -1 || ch == -2) {
			return ch;
		}

		buffer[offset] = (byte) ch;

		int readCounter = 1;

		for (; readCounter < length; readCounter++) {
			ch = read();

			if (ch == -1 || ch == -2) {
				break;
			}

			if (buffer != null) {
				buffer[offset + readCounter] = (byte) ch;
			}
		}

		return readCounter;
	}
}
