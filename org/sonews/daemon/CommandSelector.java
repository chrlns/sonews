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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sonews.daemon.command.Command;
import org.sonews.daemon.command.UnsupportedCommand;
import org.sonews.util.Log;
import org.sonews.util.io.Resource;

/**
 * Selects the correct command processing class.
 * @author Christian Lins
 * @since sonews/1.0
 */
public class CommandSelector
{

  private static Map<Thread, CommandSelector> instances
    = new ConcurrentHashMap<Thread, CommandSelector>();
  private static Map<String, Class<?>> commandClassesMapping
    = new ConcurrentHashMap<String, Class<?>>();

  static
  {
    String[] classes = Resource.getAsString("helpers/commands.list", true).split("\n");
    for(String className : classes)
    {
      if(className.charAt(0) == '#')
      {
        // Skip comments
        continue;
      }

      try
      {
        addCommandHandler(className);
      }
      catch(ClassNotFoundException ex)
      {
        Log.get().warning("Could not load command class: " + ex);
      }
      catch(InstantiationException ex)
      {
        Log.get().severe("Could not instantiate command class: " + ex);
      }
      catch(IllegalAccessException ex)
      {
        Log.get().severe("Could not access command class: " + ex);
      }
    }
  }

  public static void addCommandHandler(String className)
    throws ClassNotFoundException, InstantiationException, IllegalAccessException
  {
    Class<?> clazz = Class.forName(className);
    Command cmd = (Command)clazz.newInstance();
    String[] cmdStrs = cmd.getSupportedCommandStrings();
    for (String cmdStr : cmdStrs)
    {
      commandClassesMapping.put(cmdStr, clazz);
    }
  }

  public static CommandSelector getInstance()
  {
    CommandSelector csel = instances.get(Thread.currentThread());
    if(csel == null)
    {
      csel = new CommandSelector();
      instances.put(Thread.currentThread(), csel);
    }
    return csel;
  }

  private Map<String, Command> commandMapping = new HashMap<String, Command>();
  private Command              unsupportedCmd = new UnsupportedCommand();

  private CommandSelector()
  {}

  public Command get(String commandName)
  {
    try
    {
      commandName = commandName.toUpperCase();
      Command cmd = this.commandMapping.get(commandName);

      if(cmd == null)
      {
        Class<?> clazz = commandClassesMapping.get(commandName);
        if(clazz == null)
        {
          cmd = this.unsupportedCmd;
        }
        else
        {
          cmd = (Command)clazz.newInstance();
          this.commandMapping.put(commandName, cmd);
        }
      }
      else if(cmd.isStateful())
      {
        cmd = cmd.getClass().newInstance();
      }

      return cmd;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return this.unsupportedCmd;
    }
  }

}
