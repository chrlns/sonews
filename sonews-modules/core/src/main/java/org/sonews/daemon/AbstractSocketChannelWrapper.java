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

package org.sonews.daemon;

/**
 * Abstract base class for SocketChannelWrappers. This class implements
 * passes calls to equals() and hashCode() to the wrapped objects' methods.
 *
 * @author Christian Lins
 */
abstract class AbstractSocketChannelWrapper implements SocketChannelWrapper {

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof SocketChannelWrapper) {
            SocketChannelWrapper wrapper = (SocketChannelWrapper)obj;
            return getWrapier().equals(wrapper.getWrapier());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getWrapier().hashCode();
    }
}
