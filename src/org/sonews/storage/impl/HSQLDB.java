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
package org.sonews.storage.impl;

import java.sql.SQLException;
import org.sonews.storage.Group;
import org.sonews.storage.Storage;

/**
 * A specialized JDBCDatabase supporting HSQLDB.
 * @author Christian Lins
 * @since sonews/1.1
 */
public class HSQLDB extends JDBCDatabase implements Storage {

	@Override
	protected void prepareAddGroupStatement() throws SQLException {
		this.pstmtAddGroup0 = conn.prepareStatement(
				"INSERT INTO groups (name, flags, group_id) VALUES (?, ?, IDENTITY())");
	}

	@Override
	protected void prepareCountGroupsStatement() throws SQLException {
		this.pstmtCountGroups = conn.prepareStatement(
				"SELECT Count(group_id) FROM groups WHERE "
				+ "BITAND(flags, " + Group.DELETED + ") = 0");
	}

	@Override
	protected void prepareGetPostingsCountStatement() throws SQLException {
		this.pstmtGetPostingsCount = conn.prepareStatement(
				"SELECT Count(*) FROM postings JOIN groups "
				+ "ON groups.name = ? GROUP BY groups.name");
	}

	@Override
	protected void prepareGetSubscriptionsStatement() throws SQLException {
		this.pstmtGetSubscriptions = conn.prepareStatement(
				"SELECT * FROM (SELECT feedtype, host, port, peer_id FROM peers JOIN "
				+ "peer_subscriptions ON peers.peer_id = peer_subscriptions.peer_id) "
				+ "JOIN groups ON group_id = groups.group_id WHERE feedtype = ?");
	}
}
