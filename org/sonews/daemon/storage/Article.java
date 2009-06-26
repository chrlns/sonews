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

package org.sonews.daemon.storage;

import org.sonews.daemon.Config;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeUtility;
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
   * Loads the Article identified by the given ID from the Database.
   * @param messageID
   * @return null if Article is not found or if an error occurred.
   */
  public static Article getByMessageID(final String messageID)
  {
    try
    {
      return Database.getInstance().getArticle(messageID);
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
      return null;
    }
  }
  
  public static Article getByArticleNumber(long articleIndex, Group group)
    throws SQLException
  {
    return Database.getInstance().getArticle(articleIndex, group.getID()); 
  }
  
  private String              body       = "";
  private String              headerSrc  = null;
  
  /**
   * Default constructor.
   */
  public Article()
  {
  }
  
  /**
   * Creates a new Article object using the date from the given
   * raw data.
   * This construction has only package visibility.
   */
  Article(String headers, String body)
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
      this.body = (String)content;
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
   * Reads lines from the given InputString into a String object.
   * TODO: Move this generalized method to org.sonews.util.io.Resource.
   * @param in
   * @return
   * @throws IOException
   */
  private String readContent(InputStream in)
    throws IOException
  {
    StringBuilder buf = new StringBuilder();
    
    BufferedReader rin = new BufferedReader(new InputStreamReader(in));
    String line =  rin.readLine();
    while(line != null)
    {
      buf.append('\n');
      buf.append(line);
      line = rin.readLine();
    }
    
    return buf.toString();
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
   * the header object. You have to update the Database manually to make this
   * change persistent.
   * Note: a Message-ID should never be changed and only generated once.
   */
  private String generateMessageID()
  {
    String msgID = "<" + UUID.randomUUID() + "@"
        + Config.getInstance().get(Config.HOSTNAME, "localhost") + ">";
    
    this.headers.setHeader(Headers.MESSAGE_ID, msgID);
    
    return msgID;
  }

  /**
   * Returns the body string.
   */
  public String getBody()
  {
    return body;
  }

  /**
   * @return Charset of the body text
   */
  public Charset getBodyCharset()
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
  List<Group> getGroups()
  {
    String[]         groupnames = getHeader(Headers.NEWSGROUPS)[0].split(",");
    ArrayList<Group> groups     = new ArrayList<Group>();

    try
    {
      for(String newsgroup : groupnames)
      {
        newsgroup = newsgroup.trim();
        Group group = Database.getInstance().getGroup(newsgroup);
        if(group != null &&         // If the server does not provide the group, ignore it
          !groups.contains(group))  // Yes, there may be duplicates
        {
          groups.add(group);
        }
      }
    }
    catch (SQLException ex)
    {
      ex.printStackTrace();
      return null;
    }
    return groups;
  }

  public void setBody(String body)
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

  public String getMessageID()
  {
    String[] msgID = getHeader(Headers.MESSAGE_ID);
    return msgID[0];
  }

  public Enumeration getAllHeaders()
  {
    return this.headers.getAllHeaders();
  }
  
  /**
   * @return Header source code of this Article.
   */
  public String getHeaderSource()
  {
    if(this.headerSrc != null)
    {
      return this.headerSrc;
    }

    StringBuffer buf = new StringBuffer();
    
    for(Enumeration en = this.headers.getAllHeaders(); en.hasMoreElements();)
    {
      Header entry = (Header)en.nextElement();

      buf.append(entry.getName());
      buf.append(": ");
      buf.append(
        MimeUtility.fold(entry.getName().length() + 2, entry.getValue()));

      if(en.hasMoreElements())
      {
        buf.append("\r\n");
      }
    }
    
    this.headerSrc = buf.toString();
    return this.headerSrc;
  }
  
  public long getIndexInGroup(Group group)
    throws SQLException
  {
    return Database.getInstance().getArticleIndex(this, group);
  }
  
  /**
   * Sets the headers of this Article. If headers contain no
   * Message-Id a new one is created.
   * @param headers
   */
  public void setHeaders(InternetHeaders headers)
  {
    this.headers = headers;
    validateHeaders();
  }
  
  /**
   * @return String containing the Message-ID.
   */
  @Override
  public String toString()
  {
    return getMessageID();
  }
  
  /**
   * Checks some headers for their validity and generates an
   * appropriate Path-header for this host if not yet existing.
   * This method is called by some Article constructors and the
   * method setHeaders().
   * @return true if something on the headers was changed.
   */
  private void validateHeaders()
  {
    // Check for valid Path-header
    final String path = getHeader(Headers.PATH)[0];
    final String host = Config.getInstance().get(Config.HOSTNAME, "localhost");
    if(!path.startsWith(host))
    {
      StringBuffer pathBuf = new StringBuffer();
      pathBuf.append(host);
      pathBuf.append('!');
      pathBuf.append(path);
      this.headers.setHeader(Headers.PATH, pathBuf.toString());
    }
    
    // Generate a messageID if no one is existing
    if(getMessageID().equals(""))
    {
      generateMessageID();
    }
  }

}
