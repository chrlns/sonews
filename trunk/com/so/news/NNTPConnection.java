/*
 *   StarOffice News Server
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

package com.so.news;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import com.so.news.command.ArticleCommand;
import com.so.news.command.GroupCommand;
import com.so.news.command.ListCommand;
import com.so.news.command.PostCommand;
import com.so.news.command.OverCommand;
import com.so.news.storage.Article;
import com.so.news.storage.Group;

/**
 * Represents the connection between the server and one client.
 * @author Christian Lins (christian.lins@web.de)
 */
public class NNTPConnection extends Thread
{
  public static final String NEWLINE            = "\r\n";
  public static final String MESSAGE_ID_PATTERN = "<[^>]+>";
  
  private boolean            debug              
    = Boolean.parseBoolean(Config.getInstance().get("n3tpd.debug", "false"));
  private Socket             socket;
  private boolean            exit               = false;
  private BufferedWriter     out;
  private BufferedReader     in;
  private Article            currentArticle     = null;
  private Group              currentGroup       = null;

  /**
   * Creates a new NNTPConnection instance using the given connected Socket.
   * @param socket
   * @throws java.io.IOException
   */
  public NNTPConnection(Socket socket) 
    throws IOException
  {
    this.socket = socket;
    this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out    = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    // TODO: The output stream should be of type PrintStream so that many
    // of the printX() methods of this class can go to trash
    
    setDaemon(true); // Exits if the main thread is killed
  }

  /**
   * Closes the associated socket end exits the Thread.
   */
  public void exit()
  {
    try
    {
      exit = true;
      socket.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Prints a CharSequence to the sockets output stream.
   */
  public void print(CharSequence s) throws IOException
  {
    out.append(s);
  }

  public void println(CharSequence s) throws IOException
  {
    print(s);
    print(NEWLINE);
    if (debug)
      System.out.println("<<< " + s);
  }

  public void printStatus(int code, String description) throws IOException
  {
    println("" + code + " " + description);
    flush();
  }

  public void printTextLine(CharSequence line) throws IOException
  {
    if (line.length() > 0 && line.charAt(0) == '.')
      print("..");
    println(line);
  }

  public void printTextPart(CharSequence text) throws IOException
  {
    String[] lines = text.toString().split(NEWLINE);
    for (String line : lines)
      printTextLine(line);
  }

  public void printText(CharSequence text) throws IOException
  {
    printTextPart(text);
    println(".");
    flush();
  }

  public void flush() throws IOException
  {
    out.flush();
  }

  public String readln() throws IOException
  {
    String s = in.readLine();
    if (s == null)
      throw new IOException("Socket closed");
    if (debug)
      System.out.println(">>> " + s);
    return s;
  }

  public String[] readCommand() throws IOException
  {
    return readln().split("[ ]+");
  }

  public List<String> readText() throws IOException
  {
    List<String> l = new LinkedList<String>();
    String s;
    do
    {
      s = readln();
      if (!s.equals("."))
      {
        if (s.startsWith(".."))
          s = s.substring(1);
        l.add(s);
      }
    }
    while (!s.equals("."));
    return l;
  }

  public String readTextLine() throws IOException
  {
    String s = null;
    do
    {
      s = readln();
    }
    while (s == null);
    if (s.equals("."))
      return null;
    if (s.startsWith(".."))
      s = s.substring(1);
    return s;
  }

  public void setCurrentArticle(Article current)
  {
    currentArticle = current;
  }

  public Article getCurrentArticle()
  {
    return currentArticle;
  }

  public void setCurrentGroup(Group current)
  {
    currentGroup = current;
  }

  public Group getCurrentGroup()
  {
    return currentGroup;
  }

  private void processCommand(String[] command) 
    throws Exception
  {
    if (command.length == 0)
      return; // TODO Error

    String commandName = command[0];

    // RFC977
    // TODO HELP command
    // TODO NEWGROUPS command
    // TODO NEWNEWS command

    // RFC2980
    // TODO LIST ACTIVE command
    // TODO LIST ACTIVE.TIMES command
    // TODO LIST DISTRIBUTIONS command
    // TODO LIST DISTRIB.PATS command
    // TODO XGTITLE command
    // TODO XHDR command
    // TODO XPAT command
    // TODO XPATH command
    // TODO XROVER command
    // TODO XTHREAD command
    // TODO AUTHINFO command

    // STANDARD COMMANDS
    if (commandName.equalsIgnoreCase("ARTICLE")
        || commandName.equalsIgnoreCase("STAT")
        || commandName.equalsIgnoreCase("HEAD") 
        || commandName.equalsIgnoreCase("BODY"))
    {
      ArticleCommand cmd = new ArticleCommand(this);
      cmd.process(command);
    }
    
    else if (commandName.equalsIgnoreCase("LIST"))
    {
      ListCommand cmd = new ListCommand(this);
      cmd.process(command);
    }

    else if (commandName.equalsIgnoreCase("GROUP"))
    {
      GroupCommand cmd = new GroupCommand(this);
      cmd.process(command);
    }
    
    else if(commandName.equalsIgnoreCase("POST"))
    {
      PostCommand cmd = new PostCommand(this);
      cmd.process(command);
    }

    else if (commandName.equalsIgnoreCase("CHECK")
        || commandName.equalsIgnoreCase("TAKETHIS"))
    {
      // untested, RFC2980 compliant
      printStatus(400, "not accepting articles");
      return;
    }

    else if (commandName.equalsIgnoreCase("IHAVE")
        || commandName.equalsIgnoreCase("XREPLIC"))
    {
      // untested, RFC977 compliant
      printStatus(435, "article not wanted - do not send it");
      return;
    }

    else if (commandName.equalsIgnoreCase("XCREATEGROUP"))
    {
      return;
    }

    else if (commandName.equalsIgnoreCase("SLAVE"))
    {
      // untested, RFC977 compliant
      printStatus(202, "slave status noted");
      return;
    }

    else if (commandName.equalsIgnoreCase("XINDEX"))
    {
      // untested, RFC2980 compliant
      printStatus(418, "no tin-style index is available for this news group");
      return;
    }

    else if (commandName.equalsIgnoreCase("DATE"))
    {
      printStatus(111, new SimpleDateFormat("yyyyMMddHHmmss")
          .format(new Date()));
      return;
    }

    else if (commandName.equalsIgnoreCase("MODE"))
    {
      if (command[1].equalsIgnoreCase("READER"))
      {
        // untested, RFC2980 compliant
        printStatus(200, "Hello, you can post");
      }
      else if (command[1].equalsIgnoreCase("STREAM"))
      {
        printStatus(203, "Streaming is OK");
      }
      else
        printStatus(501, "Command not supported");
    }

    else if (commandName.equalsIgnoreCase("QUIT"))
    {
      // untested, RFC977 compliant
      printStatus(205, "closing connection - goodbye!");
      exit();
      return;
    }  
    
    else if (commandName.equalsIgnoreCase("XSHUTDOWN"))
    {
      printStatus(205, "closing connection - goodbye!");
      exit();
      return;
    }

    // X COMMANDS
    else if(commandName.equalsIgnoreCase("XOVER")
       || commandName.equalsIgnoreCase("OVER"))
    {
      OverCommand cmd = new OverCommand(this);
      cmd.process(command);
    }

    else
      printStatus(501, "Command not supported");
  }

  /**
   * Runloop of this Thread.
   * @throws RuntimeException if this method is called directly.
   */
  @Override
  public void run()
  {
    assert !this.equals(Thread.currentThread());

    try
    {
      printStatus(200, Config.getInstance().get("n3tpd.hostname", "localhost")
          + " " + Main.VERSION + " news server ready - (posting ok).");
    }
    catch (IOException e1)
    {
      exit();
    }
    
    while (!exit)
    {
      try
      {
        processCommand(readCommand());
      }
      catch (SocketException e)
      {
        if (exit)
          return;
        exit();
        e.printStackTrace();
      }
      catch (IOException e)
      {
        if (exit)
          return;
        exit();
        e.printStackTrace();
      }
      catch (Throwable e)
      {
        if (exit)
          return;
        e.printStackTrace();
        // silently ignore
      }
    }
  }

}
