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

package org.sonews.daemon;

import org.sonews.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import org.sonews.daemon.command.ArticleCommand;
import org.sonews.daemon.command.CapabilitiesCommand;
import org.sonews.daemon.command.AbstractCommand;
import org.sonews.daemon.command.GroupCommand;
import org.sonews.daemon.command.HelpCommand;
import org.sonews.daemon.command.ListCommand;
import org.sonews.daemon.command.ListGroupCommand;
import org.sonews.daemon.command.ModeReaderCommand;
import org.sonews.daemon.command.NewGroupsCommand;
import org.sonews.daemon.command.NextPrevCommand;
import org.sonews.daemon.command.OverCommand;
import org.sonews.daemon.command.PostCommand;
import org.sonews.daemon.command.QuitCommand;
import org.sonews.daemon.command.StatCommand;
import org.sonews.daemon.command.UnsupportedCommand;
import org.sonews.daemon.command.XDaemonCommand;
import org.sonews.daemon.command.XPatCommand;
import org.sonews.daemon.storage.Article;
import org.sonews.daemon.storage.Group;
import org.sonews.util.Stats;

/**
 * For every SocketChannel (so TCP/IP connection) there is an instance of
 * this class.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class NNTPConnection
{

  public static final String NEWLINE            = "\r\n";    // RFC defines this as newline
  public static final String MESSAGE_ID_PATTERN = "<[^>]+>";
  
  private static final Timer cancelTimer = new Timer(true); // Thread-safe? True for run as daemon
  
  /** SocketChannel is generally thread-safe */
  private SocketChannel   channel        = null;
  private Charset         charset        = Charset.forName("UTF-8");
  private AbstractCommand command        = null;
  private Article         currentArticle = null;
  private Group           currentGroup   = null;
  private volatile long   lastActivity   = System.currentTimeMillis();
  private ChannelLineBuffers lineBuffers = new ChannelLineBuffers();
  private int             readLock       = 0;
  private final Object    readLockGate   = new Object();
  private SelectionKey    writeSelKey    = null;
  
  public NNTPConnection(final SocketChannel channel)
    throws IOException
  {
    if(channel == null)
    {
      throw new IllegalArgumentException("channel is null");
    }

    this.channel = channel;
    Stats.getInstance().clientConnect();
  }
  
  /**
   * Tries to get the read lock for this NNTPConnection. This method is Thread-
   * safe and returns true of the read lock was successfully set. If the lock
   * is still hold by another Thread the method returns false.
   */
  boolean tryReadLock()
  {
    // As synchronizing simple types may cause deadlocks,
    // we use a gate object.
    synchronized(readLockGate)
    {
      if(readLock != 0)
      {
        return false;
      }
      else
      {
        readLock = Thread.currentThread().hashCode();
        return true;
      }
    }
  }
  
  /**
   * Releases the read lock in a Thread-safe way.
   * @throws IllegalMonitorStateException if a Thread not holding the lock
   * tries to release it.
   */
  void unlockReadLock()
  {
    synchronized(readLockGate)
    {
      if(readLock == Thread.currentThread().hashCode())
      {
        readLock = 0;
      }
      else
      {
        throw new IllegalMonitorStateException();
      }
    }
  }
  
  /**
   * @return Current input buffer of this NNTPConnection instance.
   */
  public ByteBuffer getInputBuffer()
  {
    return this.lineBuffers.getInputBuffer();
  }
  
  /**
   * @return Output buffer of this NNTPConnection which has at least one byte
   * free storage.
   */
  public ByteBuffer getOutputBuffer()
  {
    return this.lineBuffers.getOutputBuffer();
  }
  
  /**
   * @return ChannelLineBuffers instance associated with this NNTPConnection.
   */
  public ChannelLineBuffers getBuffers()
  {
    return this.lineBuffers;
  }
  
  /**
   * @return true if this connection comes from a local remote address.
   */
  public boolean isLocalConnection()
  {
    return ((InetSocketAddress)this.channel.socket().getRemoteSocketAddress())
      .getHostName().equalsIgnoreCase("localhost");
  }

  void setWriteSelectionKey(SelectionKey selKey)
  {
    this.writeSelKey = selKey;
  }

  public void shutdownInput()
  {
    try
    {
      // Closes the input line of the channel's socket, so no new data
      // will be received and a timeout can be triggered.
      this.channel.socket().shutdownInput();
    }
    catch(IOException ex)
    {
      Log.msg("Exception in NNTPConnection.shutdownInput(): " + ex, false);
      if(Log.isDebug())
      {
        ex.printStackTrace();
      }
    }
  }
  
  public void shutdownOutput()
  {
    cancelTimer.schedule(new TimerTask() 
    {
      @Override
      public void run()
      {
        try
        {
          // Closes the output line of the channel's socket.
          channel.socket().shutdownOutput();
          channel.close();
        }
        catch(Exception ex)
        {
          Log.msg("NNTPConnection.shutdownOutput(): " + ex, false);
          if(Log.isDebug())
          {
            ex.printStackTrace();
          }
        }
      }
    }, 3000);
  }
  
  public SocketChannel getChannel()
  {
    return this.channel;
  }
  
  public Article getCurrentArticle()
  {
    return this.currentArticle;
  }
  
  public Charset getCurrentCharset()
  {
    return this.charset;
  }
  
  public Group getCurrentGroup()
  {
    return this.currentGroup;
  }
  
  public void setCurrentArticle(final Article article)
  {
    this.currentArticle = article;
  }
  
  public void setCurrentGroup(final Group group)
  {
    this.currentGroup = group;
  }
  
  public long getLastActivity()
  {
    return this.lastActivity;
  }
  
  /**
   * Due to the readLockGate there is no need to synchronize this method.
   * @param raw
   * @throws IllegalArgumentException if raw is null.
   * @throws IllegalStateException if calling thread does not own the readLock.
   */
  void lineReceived(byte[] raw)
  {
    if(raw == null)
    {
      throw new IllegalArgumentException("raw is null");
    }
    
    if(readLock == 0 || readLock != Thread.currentThread().hashCode())
    {
      throw new IllegalStateException("readLock not properly set");
    }

    this.lastActivity = System.currentTimeMillis();
    
    String line = new String(raw, this.charset);
    
    // There might be a trailing \r, but trim() is a bad idea
    // as it removes also leading spaces from long header lines.
    if(line.endsWith("\r"))
    {
      line = line.substring(0, line.length() - 1);
    }
    
    Log.msg("<< " + line, true);
    
    if(command == null)
    {
      command = parseCommandLine(line);
      assert command != null;
    }

    try
    {
      // The command object will process the line we just received
      command.processLine(line);
    }
    catch(ClosedChannelException ex0)
    {
      try
      {
        Log.msg("Connection to " + channel.socket().getRemoteSocketAddress() 
            + " closed: " + ex0, true);
      }
      catch(Exception ex0a)
      {
        ex0a.printStackTrace();
      }
    }
    catch(Exception ex1)
    {
      try
      {
        command = null;
        ex1.printStackTrace();
        println("500 Internal server error");
      }
      catch(Exception ex2)
      {
        ex2.printStackTrace();
      }
    }

    if(command == null || command.hasFinished())
    {
      command = null;
      charset = Charset.forName("UTF-8"); // Reset to default
    }
  }
  
  /**
   * This method performes several if/elseif constructs to determine the
   * fitting command object. 
   * TODO: This string comparisons are probably slow!
   * @param line
   * @return
   */
  private AbstractCommand parseCommandLine(String line)
  {
    AbstractCommand  cmd    = new UnsupportedCommand(this);
    String   cmdStr = line.split(" ")[0];
    
    if(cmdStr.equalsIgnoreCase("ARTICLE") || 
      cmdStr.equalsIgnoreCase("BODY"))
    {
      cmd = new ArticleCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("CAPABILITIES"))
    {
      cmd = new CapabilitiesCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("GROUP"))
    {
      cmd = new GroupCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("HEAD"))
    {
      cmd = new ArticleCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("HELP"))
    {
      cmd = new HelpCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("LIST"))
    {
      cmd = new ListCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("LISTGROUP"))
    {
      cmd = new ListGroupCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("MODE"))
    {
      cmd = new ModeReaderCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("NEWGROUPS"))
    {
      cmd = new NewGroupsCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("NEXT") ||
      cmdStr.equalsIgnoreCase("PREV"))
    {
      cmd = new NextPrevCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("OVER") ||
      cmdStr.equalsIgnoreCase("XOVER")) // for compatibility with older RFCs
    {
      cmd = new OverCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("POST"))
    {
      cmd = new PostCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("QUIT"))
    {
      cmd = new QuitCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("STAT"))
    {
      cmd = new StatCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("XDAEMON"))
    {
      cmd = new XDaemonCommand(this);
    }
    else if(cmdStr.equalsIgnoreCase("XPAT"))
    {
      cmd = new XPatCommand(this);
    }
    
    return cmd;
  }
  
  /**
   * Puts the given line into the output buffer, adds a newline character
   * and returns. The method returns immediately and does not block until
   * the line was sent. If line is longer than 510 octets it is split up in
   * several lines. Each line is terminated by \r\n (NNTPConnection.NEWLINE).
   * @param line
   */
  public void println(final CharSequence line, final Charset charset)
    throws IOException
  {    
    writeToChannel(CharBuffer.wrap(line), charset, line);
    writeToChannel(CharBuffer.wrap(NEWLINE), charset, null);
  }
  
  /**
   * Encodes the given CharBuffer using the given Charset to a bunch of
   * ByteBuffers (each 512 bytes large) and enqueues them for writing at the
   * connected SocketChannel.
   * @throws java.io.IOException
   */
  private void writeToChannel(CharBuffer characters, final Charset charset,
    CharSequence debugLine)
    throws IOException
  {
    if(!charset.canEncode())
    {
      Log.msg("FATAL: Charset " + charset + " cannot encode!", false);
      return;
    }
    
    // Write characters to output buffers
    LineEncoder lenc = new LineEncoder(characters, charset);
    lenc.encode(lineBuffers);
    
    // Enable OP_WRITE events so that the buffers are processed
    try
    {
      this.writeSelKey.interestOps(SelectionKey.OP_WRITE);
      ChannelWriter.getInstance().getSelector().wakeup();
    }
    catch (Exception ex) // CancelledKeyException and ChannelCloseException
    {
      Log.msg("NNTPConnection.writeToChannel(): " + ex, false);
      return;
    }

    // Update last activity timestamp
    this.lastActivity = System.currentTimeMillis();
    if(debugLine != null)
    {
      Log.msg(">> " + debugLine, true);
    }
  }
  
  public void println(final CharSequence line)
    throws IOException
  {
    println(line, charset);
  }
  
  public void print(final String line)
    throws IOException
  {
    writeToChannel(CharBuffer.wrap(line), charset, line);
  }
  
  public void setCurrentCharset(final Charset charset)
  {
    this.charset = charset;
  }
  
}
