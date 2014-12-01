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
package org.sonews.mlgw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.InternetAddress;
import org.sonews.config.Config;
import org.sonews.storage.Article;
import org.sonews.storage.Group;
import org.sonews.storage.Headers;
import org.sonews.storage.StorageBackendException;
import org.sonews.storage.StorageManager;
import org.sonews.util.Log;

/**
 * Dispatches messages from mailing list to newsserver or vice versa.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class Dispatcher {

	static class PasswordAuthenticator extends Authenticator {

		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			final String username =
					Config.inst().get(Config.MLSEND_USER, "user");
			final String password =
					Config.inst().get(Config.MLSEND_PASSWORD, "mysecret");

			return new PasswordAuthentication(username, password);
		}
	}

	/**
	 * Chunks out the email address of the full List-Post header field.
	 * @param listPostValue
	 * @return The matching email address or null
	 */
	private static String chunkListPost(String listPostValue) {
		// listPostValue is of form "<mailto:dev@openoffice.org>"
		Pattern mailPattern = Pattern.compile("(\\w+[-|.])*\\w+@(\\w+.)+\\w+");
		Matcher mailMatcher = mailPattern.matcher(listPostValue);
		if (mailMatcher.find()) {
			return listPostValue.substring(mailMatcher.start(), mailMatcher.end());
		} else {
			return null;
		}
	}

	/**
	 * This method inspects the header of the given message, trying
	 * to find the most appropriate recipient.
	 * @param msg
	 * @param fallback If this is false only List-Post and X-List-Post headers
	 *                 are examined.
	 * @return null or fitting group name for the given message.
	 */
	private static List<String> getGroupFor(final Message msg, final boolean fallback)
			throws MessagingException, StorageBackendException {
		List<String> groups = null;

		// Is there a List-Post header?
		String[] listPost = msg.getHeader(Headers.LIST_POST);
		InternetAddress listPostAddr;

		if (listPost == null || listPost.length == 0 || "".equals(listPost[0])) {
			// Is there a X-List-Post header?
			listPost = msg.getHeader(Headers.X_LIST_POST);
		}

		if (listPost != null && listPost.length > 0
				&& !"".equals(listPost[0]) && chunkListPost(listPost[0]) != null) {
			// listPost[0] is of form "<mailto:dev@openoffice.org>"
			listPost[0] = chunkListPost(listPost[0]);
			listPostAddr = new InternetAddress(listPost[0], false);
                        //FIXME
			//groups = StorageManager.current().getGroupsForList(listPostAddr.getAddress());
		} else if (fallback) {
			StringBuilder strBuf = new StringBuilder();
			strBuf.append("Using fallback recipient discovery for: ");
			strBuf.append(msg.getSubject());
			Log.get().info(strBuf.toString());
			groups = new ArrayList<String>();
			// Fallback to TO/CC/BCC addresses
			Address[] to = msg.getAllRecipients();
			for (Address toa : to) // Address can have '<' '>' around
			{
                            //FIXME
				if (toa instanceof InternetAddress) {
				//	List<String> g = StorageManager.current().getGroupsForList(((InternetAddress) toa).getAddress());
				//	groups.addAll(g);
				}
			}
		}

		return groups;
	}

	/**
	 * Posts a message that was received from a mailing list to the
	 * appropriate newsgroup.
	 * If the message already exists in the storage, this message checks
	 * if it must be posted in an additional group. This can happen for
	 * crosspostings in different mailing lists.
	 * @param msg
	 */
	public static boolean toGroup(final Message msg) {
		if (msg == null) {
			throw new IllegalArgumentException("Argument 'msg' must not be null!");
		}

		try {
			// Create new Article object
			Article article = new Article(msg);
			boolean posted = false;

			// Check if this mail is already existing the storage
			boolean updateReq =
					StorageManager.current().isArticleExisting(article.getMessageID());

			List<String> newsgroups = getGroupFor(msg, !updateReq);
			List<String> oldgroups = new ArrayList<String>();
			if (updateReq) {
				// Check for duplicate entries of the same group
				Article oldArticle = StorageManager.current().getArticle(article.getMessageID());
				if (oldArticle != null) {
					List<Group> oldGroups = oldArticle.getGroups();
					for (Group oldGroup : oldGroups) {
						if (!newsgroups.contains(oldGroup.getName())) {
							oldgroups.add(oldGroup.getName());
						}
					}
				}
			}

			if (newsgroups.size() > 0) {
				newsgroups.addAll(oldgroups);
				StringBuilder groups = new StringBuilder();
				for (int n = 0; n < newsgroups.size(); n++) {
					groups.append(newsgroups.get(n));
					if (n + 1 != newsgroups.size()) {
						groups.append(',');
					}
				}

				StringBuilder strBuf = new StringBuilder();
				strBuf.append("Posting to group ");
				strBuf.append(groups.toString());
				Log.get().info(strBuf.toString());

				article.setGroup(groups.toString());
				//article.removeHeader(Headers.REPLY_TO);
				//article.removeHeader(Headers.TO);

				// Write article to database
				if (updateReq) {
					Log.get().info("Updating " + article.getMessageID()
							+ " with additional groups");
					StorageManager.current().delete(article.getMessageID());
					StorageManager.current().addArticle(article);
				} else {
					Log.get().info("Gatewaying " + article.getMessageID() + " to "
							+ article.getHeader(Headers.NEWSGROUPS)[0]);
					StorageManager.current().addArticle(article);
				}
				posted = true;
			} else {
				StringBuilder buf = new StringBuilder();
				for (Address toa : msg.getAllRecipients()) {
					buf.append(' ');
					buf.append(toa.toString());
				}
				buf.append(" ");
				buf.append(article.getHeader(Headers.LIST_POST)[0]);
				Log.get().warning("No group for" + buf.toString());
			}
			return posted;
		} catch (Exception ex) {
			Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
			return false;
		}
	}

	/**
	 * Mails a message received through NNTP to the appropriate mailing list.
	 * This method MAY be called several times by PostCommand for the same
	 * article.
	 */
	public static boolean toList(Article article, String group)
			throws IOException, MessagingException, StorageBackendException {
		// Get mailing lists for the group of this article
            //FIXME
		List<String> rcptAddresses = null; //StorageManager.current().getListsForGroup(group);

		if (rcptAddresses == null || rcptAddresses.isEmpty()) {
			StringBuilder strBuf = new StringBuilder();
			strBuf.append("No ML address found for group ");
			strBuf.append(group);
			Log.get().warning(strBuf.toString());
			return false;
		}

		for (String rcptAddress : rcptAddresses) {
			// Compose message and send it via given SMTP-Host
			String smtpHost = Config.inst().get(Config.MLSEND_HOST, "localhost");
			int smtpPort = Config.inst().get(Config.MLSEND_PORT, 25);
			//String smtpUser = Config.inst().get(Config.MLSEND_USER, "user");
			//String smtpPw = Config.inst().get(Config.MLSEND_PASSWORD, "mysecret");
			String smtpFrom = Config.inst().get(
					Config.MLSEND_ADDRESS, article.getHeader(Headers.FROM)[0]);

			// TODO: Make Article cloneable()
			article.getMessageID(); // Make sure an ID is existing
			article.removeHeader(Headers.NEWSGROUPS);
			article.removeHeader(Headers.PATH);
			article.removeHeader(Headers.LINES);
			article.removeHeader(Headers.BYTES);

			article.setHeader("To", rcptAddress);
			//article.setHeader("Reply-To", listAddress);

			if (Config.inst().get(Config.MLSEND_RW_SENDER, false)) {
				rewriteSenderAddress(article); // Set the SENDER address
			}

			SMTPTransport smtpTransport = new SMTPTransport(smtpHost, smtpPort);
			smtpTransport.login();
			smtpTransport.send(article, smtpFrom, rcptAddress);
			smtpTransport.close();

			Log.get().info("MLGateway: Mail " + article.getHeader("Subject")[0]
					+ " was delivered to " + rcptAddress + ".");
		}
		return true;
	}

	/**
	 * Sets the SENDER header of the given MimeMessage. This might be necessary
	 * for moderated groups that does not allow the "normal" FROM sender.
	 * @param msg
	 * @throws javax.mail.MessagingException
	 */
	private static void rewriteSenderAddress(Article msg)
			throws MessagingException {
		String mlAddress = Config.inst().get(Config.MLSEND_ADDRESS, null);

		if (mlAddress != null) {
			msg.setHeader(Headers.SENDER, mlAddress);
		} else {
			throw new MessagingException("Cannot rewrite SENDER header!");
		}
	}
}
