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
 */
class CommandSelector
{

  private static Map<Thread, CommandSelector> instances
    = new ConcurrentHashMap<Thread, CommandSelector>();
  
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
        Class<?> clazz   = Class.forName(className);
        Command  cmd     = (Command)clazz.newInstance();
        String[] cmdStrs = cmd.getSupportedCommandStrings();
        for(String cmdStr : cmdStrs)
        {
          this.commandMapping.put(cmdStr, cmd);
        }
      }
      catch(ClassNotFoundException ex)
      {
        Log.msg("Could not load command class: " + ex, false);
      }
      catch(InstantiationException ex)
      {
        Log.msg("Could not instantiate command class: " + ex, false);
      }
      catch(IllegalAccessException ex)
      {
        Log.msg("Could not access command class: " + ex, false);
      }
    }
  }

  public Command get(String commandName)
  {
    try
    {
      commandName = commandName.toUpperCase();
      Command cmd = this.commandMapping.get(commandName);

      if(cmd == null)
      {
        return this.unsupportedCmd;
      }
      else if(cmd.isStateful())
      {
        return cmd.getClass().newInstance();
      }
      else
      {
        return cmd;
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return this.unsupportedCmd;
    }
  }

}
