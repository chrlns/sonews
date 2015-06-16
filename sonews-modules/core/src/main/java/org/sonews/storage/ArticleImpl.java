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

package org.sonews.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.sonews.acl.User;
import org.sonews.config.Config;
import org.sonews.util.Log;

/**
 * Represents a newsgroup article.
 * 
 * @author Christian Lins
 * @author Dennis Schwerdel
 * @since n3tpd/0.1
 */
public class ArticleImpl extends ArticleHead implements Article {

    private byte[] body = new byte[0];
    private User sender;

    /**
     * Default constructor.
     */
    public ArticleImpl() {
    }

    /**
     * Creates a new Article object using the date from the given raw data.
     * @param headers
     * @param body
     */
    public ArticleImpl(String headers, byte[] body) {
        try {
            this.body = body;

            // Parse the header
            this.headers = new InternetHeaders(new ByteArrayInputStream(
                    headers.getBytes("UTF-8")));

            this.headerSrc = headers;
        } catch (MessagingException ex) {
            Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
        } catch (UnsupportedEncodingException ex) {
            Log.get().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates an Article instance using the data from the javax.mail.Message
     * object. This constructor is called by the Mailinglist gateway.
     *
     * @see javax.mail.Message
     * @param msg
     * @throws IOException
     * @throws MessagingException
     */
    public ArticleImpl(final Message msg) throws IOException, MessagingException {
        this.headers = new InternetHeaders();

        for (Enumeration<?> e = msg.getAllHeaders(); e.hasMoreElements();) {
            final Header header = (Header) e.nextElement();
            this.headers.addHeader(header.getName(), header.getValue());
        }

        // Reads the raw byte body using Message.writeTo(OutputStream out)
        this.body = readContent(msg);

        // Validate headers
        validateHeaders();
    }

    /**
     * Reads from the given Message into a byte array.
     *
     * @param in
     * @return
     * @throws IOException
     */
    private byte[] readContent(Message in) throws IOException,
            MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.writeTo(out);
        return out.toByteArray();
    }

    /**
     * Removes the header identified by the given key.
     *
     * @param headerKey
     */
    public void removeHeader(final String headerKey) {
        this.headers.removeHeader(headerKey);
        this.headerSrc = null;
    }

    /**
     * Generates a message id for this article and sets it into the header
     * object. You have to update the JDBCDatabase manually to make this change
     * persistent. Note: a Message-ID should never be changed and only generated
     * once.
     */
    private String generateMessageID() throws UnsupportedEncodingException {
        String randomString;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(getBody());
            md5.update(getHeader(Headers.SUBJECT)[0].getBytes("UTF-8"));
            md5.update(getHeader(Headers.FROM)[0].getBytes("UTF-8"));
            byte[] result = md5.digest();
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < result.length; i++) {
                hexString.append(Integer.toHexString(0xFF & result[i]));
            }
            randomString = hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            Log.get().log(Level.WARNING, ex.getLocalizedMessage(), ex);
            randomString = UUID.randomUUID().toString();
        }
        String msgID = "<" + randomString + "@"
                + Config.inst().get(Config.HOSTNAME, "localhost") + ">";

        this.headers.setHeader(Headers.MESSAGE_ID, msgID);

        return msgID;
    }

    /**
     * Returns the body string.
     * @return 
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * @return List of newsgroups this ArticleImpl belongs to.
     */
    public List<Group> getGroups() {
        String[] groupnames = getHeader(Headers.NEWSGROUPS)[0].split(",");
        List<Group> groups = new ArrayList<>(groupnames.length);

        for (String newsgroup : groupnames) {
            newsgroup = newsgroup.trim();
            Group group = Group.get(newsgroup);
            if (group != null && // If the server does not provide the group, ignore it
               !groups.contains(group)) // Yes, there may be duplicates
            {
                groups.add(group);
            }
        }

        return groups;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     *
     * @param groupname
     *            Name(s) of newsgroups
     */
    public void setGroup(String groupname) {
        this.headers.setHeader(Headers.NEWSGROUPS, groupname);
    }

    /**
     * Returns the Message-ID of this ArticleImpl. If the appropriate header is
     * empty, a new Message-ID is created.
     *
     * @return Message-ID of this ArticleImpl.
     */
    public String getMessageID() {
        String msgID;

        try {
            String[] msgIDHeader = getHeader(Headers.MESSAGE_ID);
            if (msgIDHeader[0].equals("")) {
                msgID = generateMessageID();
            } else {
                msgID = msgIDHeader[0];
            }
        } catch(UnsupportedEncodingException ex) {
            Log.get().log(Level.SEVERE, "UTF-8 not supported by VM", ex);
            msgID = UUID.randomUUID().toString();
        }

        return msgID;
    }

    /**
     * @return String containing the Message-ID.
     */
    @Override
    public String toString() {
        return getMessageID();
    }

    /**
     * @return sender – currently logged user – or null, if user is not
     *         authenticated.
     */
    public User getUser() {
        return sender;
    }

    /**
     * This method is to be called from POST Command implementation.
     *
     * @param sender
     *            current username – or null, if user is not authenticated.
     */
    public void setUser(User sender) {
        this.sender = sender;
    }
}
