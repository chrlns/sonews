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

package org.sonews.plugin;

/**
 * A generic Plugin for sonews. Implementing classes do not really add new
 * functionality to sonews but can use this interface as convenient procedure
 * for installing functionality plugins, e.g. Command-Plugins or Storage-Plugins.
 * @author Christian Lins
 * @since sonews/1.1
 */
public interface Plugin
{

	/**
	 * Called when the Plugin is loaded by sonews. This method can be used
	 * by implementing classes to install additional or required plugins.
	 */
	void load();

	/**
	 * Called when the Plugin is unloaded by sonews.
	 */
	void unload();
}
