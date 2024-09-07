/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2024  Christian Lins <christian@lins.me>
 *   Copyright (C) 2011  František Kučera <informace@frantovo.cz>
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

package org.sonews.auth;

/**
 * Represents users (clients) accessing our service through NNTP protocol.
 *
 * This class can be extended by your plugin to describe additional information,
 * that was gained during login process.
 *
 * When User object is created, default authentication status is false.
 *
 * @author František Kučera
 */
public class User {

    private String userName;
    private boolean authenticated = false;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * In some configurations users don't have to use their password – they can
     * just tell us their name and we will trust them – in this case User object
     * will exist end user name will be filled, but this method will return
     * false.
     *
     * @return true if user was succesfully authenticated (has provided correct
     *         password).
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * This method is to be called from AUTHINFO PASS Command implementation.
     *
     * @param authenticated
     *            true if user has provided right password in AUTHINFO PASS
     *            password.
     * @see #isAuthenticated()
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public User() {
    }

    public User(String userName) {
        this.userName = userName;
    }

    public User(String userName, boolean authenticated) {
        this.userName = userName;
        this.authenticated = authenticated;
    }
}
