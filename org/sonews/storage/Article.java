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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import org.sonews.config.Config;
import org.sonews.util.Log;

/**
 * Represents a newsgroup article.
 * @author Christian Lins
 * @author Denis Schwerdel
 * @since n3tpd/0.1
 */
public class Article extends ArticleHead
{
  
  /**
   * Loads the Article identified by the given ID from the JDBCDatabase.
   * @param messageID
   * @return null if Article is not found or if an error occurred.
   */
  public static Article getByMessageID(final String messageID)
  {
    try
    {
      return StorageManager.current().getArticle(messageID);
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return null;
    }
  }
  
  private byte[] body       = new byte[0];
  
  /**
   * Default constructor.
   */
  public Article()
  {
  }
  
  /**
   * Creates a new Article object using the date from the given
   * raw data.
   */
  public Article(String headers, byte[] body)
  {
    try
    {
      this.body  = body;

      // Parse the header
      this.headers = new InternetHeaders(
        new ByteArrayInputStream(headers.getBytes()));
      
      this.headerSrc = headers;
    }
    catch(MessagingException ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Creates an Article instance using the data from the javax.mail.Message
   * object.
   * @see javax.mail.Message
   * @param msg
   * @throws IOException
   * @throws MessagingException
   */
  public Article(final Message msg)
    throws IOException, MessagingException
  {
    this.headers = new InternetHeaders();

    for(Enumeration e = msg.getAllHeaders() ; e.hasMoreElements();) 
    {
      final Header header = (Header)e.nextElement();
      this.headers.addHeader(header.getName(), header.getValue());
    }
    
    // The "content" of the message can be a String if it's a simple text/plain
    // message, a Multipart object or an InputStream if the content is unknown.
    final Object content = msg.getContent();
    if(content instanceof String)
    {
      this.body = ((String)content).getBytes(getBodyCharset());
    }
    else if(content instanceof Multipart) // probably subclass MimeMultipart
    {
      // We're are not interested in the different parts of the MultipartMessage,
      // so we simply read in all data which *can* be huge.
      InputStream in = msg.getInputStream();
      this.body = readContent(in);
    }
    else if(content instanceof InputStream)
    {
      // The message format is unknown to the Message class, but we can
      // simply read in the whole message data.
      this.body = readContent((InputStream)content);
    }
    else
    {
      // Unknown content is probably a malformed mail we should skip.
      // On the other hand we produce an inconsistent mail mirror, but no
      // mail system must transport invalid content.
      Log.msg("Skipping message due to unknown content. Throwing exception...", true);
      throw new MessagingException("Unknown content: " + content);
    }
    
    // Validate headers
    validateHeaders();
  }

  /**
   * Reads from the given InputString into a byte array.
   * TODO: Move this generalized method to org.sonews.util.io.Resource.
   * @param in
   * @return
   * @throws IOException
   */
  private byte[] readContent(InputStream in)
    throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    int b = in.read();
    while(b >= 0)
    {
      out.write(b);
      b = in.read();
    }

    return out.toByteArray();
  }

  /**
   * Removes the header identified by the given key.
   * @param headerKey
   */
  public void removeHeader(final String headerKey)
  {
    this.headers.removeHeader(headerKey);
    this.headerSrc = null;
  }

  /**
   * Generates a message id for this article and sets it into
   * the header object. You have to update the JDBCDatabase manually to make this
   * change persistent.
   * Note: a Message-ID should never be changed and only generated once.
   */
  private String generateMessageID()
  {
    String randomString;
    MessageDigest md5;
    try
    {
      md5 = MessageDigest.getInstance("MD5");
      md5.reset();
      md5.update(getBody());
      md5.update(getHeader(Headers.SUBJECT)[0].getBytes());
      md5.update(getHeader(Headers.FROM)[0].getBytes());
      byte[] result = md5.digest();
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < result.length; i++)
      {
        hexString.append(Integer.toHexString(0xFF & result[i]));
      }
      randomString = hexString.toString();
    }
    catch (NoSuchAlgorithmException e)
    {
      e.printStackTrace();
      randomString = UUID.randomUUID().toString();
    }
    String msgID = "<" + randomString + "@"
        + Config.inst().get(Config.HOSTNAME, "localhost") + ">";
    
    this.headers.setHeader(Headers.MESSAGE_ID, msgID);
    
    return msgID;
  }

  /**
   * Returns the body string.
   */
  public byte[] getBody()
  {
    return body;
  }

  /**
   * @return Charset of the body text
   */
  private Charset getBodyCharset()
  {
    // We espect something like 
    // Content-Type: text/plain; charset=ISO-8859-15
    String contentType = getHeader(Headers.CONTENT_TYPE)[0];
    int idxCharsetStart = contentType.indexOf("charset=") + "charset=".length();
    int idxCharsetEnd   = contentType.indexOf(";", idxCharsetStart);
    
    String charsetName = "UTF-8";
    if(idxCharsetStart >= 0 && idxCharsetStart < contentType.length())
    {
      if(idxCharsetEnd < 0)
      {
        charsetName = contentType.substring(idxCharsetStart);
      }
      else
      {
        charsetName = contentType.substring(idxCharsetStart, idxCharsetEnd);
      }
    }
    
    // Sometimes there are '"' around the name
    if(charsetName.length() > 2 &&
      charsetName.charAt(0) == '"' && charsetName.endsWith("\""))
    {
      charsetName = charsetName.substring(1, charsetName.length() - 2);
    }
    
    // Create charset
    Charset charset = Charset.forName("UTF-8"); // This MUST be supported by JVM
    try
    {
      charset = Charset.forName(charsetName);
    }
    catch(Exception ex)
    {
      Log.msg(ex.getMessage(), false);
      Log.msg("Article.getBodyCharset(): Unknown charset: " + charsetName, false);
    }
    return charset;
  }
  
  /**
   * @return Numerical IDs of the newsgroups this Article belongs to.
   */
  public List<Group> getGroups()
  {
    String[]         groupnames = getHeader(Headers.NEWSGROUPS)[0].split(",");
    ArrayList<Group> groups     = new ArrayList<Group>();

    try
    {
      for(String newsgroup : groupnames)
      {
        newsgroup = newsgroup.trim();
        Group group = StorageManager.current().getGroup(newsgroup);
        if(group != null &&         // If the server does not provide the group, ignore it
          !groups.contains(group))  // Yes, there may be duplicates
        {
          groups.add(group);
        }
      }
    }
    catch(StorageBackendException ex)
    {
      ex.printStackTrace();
      return null;
    }
    return groups;
  }

  public void setBody(byte[] body)
  {
    this.body = body;
  }
  
  /**
   * 
   * @param groupname Name(s) of newsgroups
   */
  public void setGroup(String groupname)
  {
    this.headers.setHeader(Headers.NEWSGROUPS, groupname);
  }

  /**
   * Returns the Message-ID of this Article. If the appropriate header
   * is empty, a new Message-ID is created.
   * @return Message-ID of this Article.
   */
  public String getMessageID()
  {
    String[] msgID = getHeader(Headers.MESSAGE_ID);
    return msgID[0].equals("") ? generateMessageID() : msgID[0];
  }
  
  /**
   * @return String containing the Message-ID.
   */
  @Override
  public String toString()
  {
    return getMessageID();
  }

}
