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

/**
 * Contains header constants. These header keys are no way complete but all
 * headers that are relevant for sonews.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public final class Headers
{

	public static final String BYTES = "bytes";
	public static final String CONTENT_TYPE = "content-type";
	public static final String CONTROL = "control";
	public static final String DATE = "date";
	public static final String FROM = "from";
	public static final String LINES = "lines";
	public static final String LIST_POST = "list-post";
	public static final String MESSAGE_ID = "message-id";
	public static final String NEWSGROUPS = "newsgroups";
	public static final String NNTP_POSTING_DATE = "nntp-posting-date";
	public static final String NNTP_POSTING_HOST = "nntp-posting-host";
	public static final String PATH = "path";
	public static final String REFERENCES = "references";
	public static final String REPLY_TO = "reply-to";
	public static final String SENDER = "sender";
	public static final String SUBJECT = "subject";
	public static final String SUPERSEDES = "subersedes";
	public static final String TO = "to";
	public static final String X_COMPLAINTS_TO = "x-complaints-to";
	public static final String X_LIST_POST = "x-list-post";
	public static final String X_TRACE = "x-trace";
	public static final String XREF = "xref";

	private Headers()
	{
	}
}
