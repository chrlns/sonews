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
package org.sonews.mlgw;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.util.io.ArticleInputStream;

/**
 * Connects to a SMTP server and sends a given Article to it.
 * @author Christian Lins
 * @since sonews/1.0
 */
class SMTPTransport {

	public static final String NEWLINE = "\r\n";

	protected BufferedReader in;
	protected BufferedOutputStream out;
	protected Socket socket;

	public SMTPTransport(String host, int port)
			throws IOException, UnknownHostException {
		this.socket = new Socket(host, port);
		this.in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		this.out = new BufferedOutputStream(socket.getOutputStream());

		// Read HELO from server
		String line = this.in.readLine();
		if (line == null || !line.startsWith("220 ")) {
			throw new IOException("Invalid HELO from server: " + line);
		}
	}

	public void close()
			throws IOException {
		this.out.write("QUIT".getBytes("UTF-8"));
		this.out.flush();
		this.in.readLine();

		this.socket.close();
	}

	private byte[] createCredentials() throws UnsupportedEncodingException {
		String user = Config.inst().get(Config.MLSEND_USER, "");
		String pass = Config.inst().get(Config.MLSEND_PASSWORD, "");
		StringBuilder credBuf = new StringBuilder();
		credBuf.append(user);
		credBuf.append("\u0000");
		credBuf.append(pass);
		return Base64.encodeBase64(credBuf.toString().getBytes("UTF-8"));
	}

	private void ehlo(String hostname) throws IOException {
		StringBuilder strBuf = new StringBuilder();
		strBuf.append("EHLO ");
		strBuf.append(hostname);
		strBuf.append(NEWLINE);

		// Send EHLO to server
		this.out.write(strBuf.toString().getBytes("UTF-8"));
		this.out.flush();

		/*List<String> ehloReplies = */readReply("250");

		// TODO: Check for supported methods

		// Do a PLAIN login
		strBuf = new StringBuilder();
		strBuf.append("AUTH PLAIN");
		strBuf.append(NEWLINE);

		// Send AUTH to server
		this.out.write(strBuf.toString().getBytes("UTF-8"));
		this.out.flush();

		readReply("334");

		// Send PLAIN credentials to server
		this.out.write(createCredentials());
		this.out.flush();

		// Read reply of successful login
		readReply("235");
	}

	private void helo(String hostname) throws IOException {
		StringBuilder heloStr = new StringBuilder();
		heloStr.append("HELO ");
		heloStr.append(hostname);
		heloStr.append(NEWLINE);

		// Send HELO to server
		this.out.write(heloStr.toString().getBytes("UTF-8"));
		this.out.flush();

		// Read reply
		readReply("250");
	}

	public void login() throws IOException {
		String hostname = Config.inst().get(Config.HOSTNAME, "localhost");
		String auth = Config.inst().get(Config.MLSEND_AUTH, "none");
		if(auth.equals("none")) {
			helo(hostname);
		} else {
			ehlo(hostname);
		}
	}

	/**
	 * Read one or more exspected reply lines.
	 * @param expectedReply
	 * @return
	 * @throws IOException If the reply of the server does not fit the exspected
	 * reply code.
	 */
	private List<String> readReply(String expectedReply) throws IOException {
		List<String> replyStrings = new ArrayList<String>();

		for(;;) {
			String line = this.in.readLine();
			if (line == null || !line.startsWith(expectedReply)) {
				throw new IOException("Unexpected reply: " + line);
			}

			replyStrings.add(line);

			if(line.charAt(3) == ' ') { // Last reply line
				break;
			}
		}

		return replyStrings;
	}

	public void send(Article article, String mailFrom, String rcptTo)
			throws IOException {
		assert (article != null);
		assert (mailFrom != null);
		assert (rcptTo != null);

		this.out.write(("MAIL FROM: " + mailFrom).getBytes("UTF-8"));
		this.out.flush();
		String line = this.in.readLine();
		if (line == null || !line.startsWith("250 ")) {
			throw new IOException("Unexpected reply: " + line);
		}

		this.out.write(("RCPT TO: " + rcptTo).getBytes("UTF-8"));
		this.out.flush();
		line = this.in.readLine();
		if (line == null || !line.startsWith("250 ")) {
			throw new IOException("Unexpected reply: " + line);
		}

		this.out.write("DATA".getBytes("UTF-8"));
		this.out.flush();
		line = this.in.readLine();
		if (line == null || !line.startsWith("354 ")) {
			throw new IOException("Unexpected reply: " + line);
		}

		ArticleInputStream artStream = new ArticleInputStream(article);
		for (int b = artStream.read(); b >= 0; b = artStream.read()) {
			this.out.write(b);
		}

		// Flush the binary stream; important because otherwise the output
		// will be mixed with the PrintWriter.
		this.out.flush();
		this.out.write("\r\n.\r\n".getBytes("UTF-8"));
		this.out.flush();
		line = this.in.readLine();
		if (line == null || !line.startsWith("250 ")) {
			throw new IOException("Unexpected reply: " + line);
		}
	}
}
