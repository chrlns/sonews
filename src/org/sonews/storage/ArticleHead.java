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

package org.sonews.storage;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeUtility;
import org.sonews.config.Config;

/**
 * An article with no body only headers.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class ArticleHead {

	protected InternetHeaders headers = null;
	protected String headerSrc = null;

	protected ArticleHead() {
	}

	public ArticleHead(String headers) {
		try {
			// Parse the header
			this.headers = new InternetHeaders(new ByteArrayInputStream(
					headers.getBytes()));
		} catch (MessagingException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Returns the header field with given name.
	 * 
	 * @param name
	 *            Name of the header field(s).
	 * @param returnNull
	 *            If set to true, this method will return null instead of an
	 *            empty array if there is no header field found.
	 * @return Header values or empty string.
	 */
	public String[] getHeader(String name, boolean returnNull) {
		String[] ret = this.headers.getHeader(name);
		if (ret == null && !returnNull) {
			ret = new String[] { "" };
		}
		return ret;
	}

	public String[] getHeader(String name) {
		return getHeader(name, false);
	}

	/**
	 * Sets the header value identified through the header name.
	 * 
	 * @param name
	 * @param value
	 */
	public void setHeader(String name, String value) {
		this.headers.setHeader(name, value);
		this.headerSrc = null;
	}

	public Enumeration<?> getAllHeaders() {
		return this.headers.getAllHeaders();
	}

	/**
	 * @return Header source code of this Article.
	 */
	public String getHeaderSource() {
		if (this.headerSrc != null) {
			return this.headerSrc;
		}

		StringBuffer buf = new StringBuffer();

		for (Enumeration<?> en = this.headers.getAllHeaders(); en
				.hasMoreElements();) {
			Header entry = (Header) en.nextElement();

			String value = entry.getValue().replaceAll("[\r\n]", " ");
			buf.append(entry.getName());
			buf.append(": ");
			buf.append(MimeUtility.fold(entry.getName().length() + 2, value));

			if (en.hasMoreElements()) {
				buf.append("\r\n");
			}
		}

		this.headerSrc = buf.toString();
		return this.headerSrc;
	}

	/**
	 * Sets the headers of this Article. If headers contain no Message-Id a new
	 * one is created.
	 * 
	 * @param headers
	 */
	public void setHeaders(InternetHeaders headers) {
		this.headers = headers;
		this.headerSrc = null;
		validateHeaders();
	}

	/**
	 * Checks some headers for their validity and generates an appropriate
	 * Path-header for this host if not yet existing. This method is called by
	 * some Article constructors and the method setHeaders().
	 * 
	 * @return true if something on the headers was changed.
	 */
	protected void validateHeaders() {
		// Check for valid Path-header
		final String path = getHeader(Headers.PATH)[0];
		final String host = Config.inst().get(Config.HOSTNAME, "localhost");
		if (!path.startsWith(host)) {
			StringBuffer pathBuf = new StringBuffer();
			pathBuf.append(host);
			pathBuf.append('!');
			pathBuf.append(path);
			this.headers.setHeader(Headers.PATH, pathBuf.toString());
		}
	}
}
