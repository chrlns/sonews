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

package org.sonews.util.io;

/**
 * The ArticleTransmitter class encapsulates the functionality to transmit
 * a news article from one newsserver to another.
 * It uses the standart ARTICLE, GROUP and POST command to transmit the news
 * article.
 * There is no conversation done, the raw header and body parts are simply
 * read from one stream and written to the other.
 * 
 * @author Christian Lins
 * @since sonews/2.0
 */
public class ArticleTransmitter {
    
    private String group; 
    private String messageID;
    
    public ArticleTransmitter(String messageID, String group) {
        this.messageID = messageID;
        this.group = group;
    }
    
    public void transfer(String srcHost, int srcPort, String dstHost, int dstPort) {
        
    }
}
