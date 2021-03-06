/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file Copyright (C) 2008 Jeremy D. Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.message;

import java.util.StringTokenizer;

/**
 * This message from server informs the client that a game they're playing
 * has been "reset" to a new game (with same name and players, new layout),
 * and they should join at the given position.
 *<P>
 * For human players, this message replaces the {@link JoinGameAuth} seen when joining a brand-new game; the reset message will be followed
 * with others which will fill in the game state.
 *<P>
 * For robots, they must discard game state and ask to re-join.
 * Treat as a {@link JoinGameRequest}: ask server for us to join the new game.
 *<P>
 * Follows {@link ResetBoardRequest} and {@link ResetBoardVote} messages.
 * For details of messages sent, see 
 * {@link soc.server.SOCServer#resetBoardAndNotify(String, int)}.
 *
 * @see ResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class ResetBoardAuth extends MessageTemplate2i
{
    private static final long serialVersionUID = 3731181743864325767L;

    /**
     * Create a ResetBoardAuth message.
     *
     * @param ga  the name of the game
     * @param joinpn  the player position number at which to join
     * @param reqpn  player number who requested the reset
     */
    public ResetBoardAuth(String ga, int joinpn, int reqpn)
    {
        super (RESETBOARDAUTH, ga, joinpn, reqpn);
    }

    /**
     * @return the player position number at which to rejoin
     */
    public int getRejoinPlayerNumber()
    {
        return p1;
    }

    /**
     * @return the number of the player who requested the board reset
     */
    public int getRequestingPlayerNumber()
    {
        return p2;
    }

    /**
     * Parse the command String into a ResetBoardAuth message
     *
     * @param s   the String to parse: RESETBOARDAUTH sep game sep2 playernumber sep2 requester
     * @return    a ResetBoardAuth message, or null if the data is garbled
     */
    public static ResetBoardAuth parseDataStr(String s)
    {
        String ga;   // the game name
        int joinpn;  // the player number to join at
        int reqpn;   // the requester player number

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            joinpn = Integer.parseInt(st.nextToken());
            reqpn = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new ResetBoardAuth(ga, joinpn, reqpn);
    }

    /**
     * Minimum version where this message type is used.
     * RESETBOARDAUTH introduced in 1.1.00 for reset-board feature.
     * @return Version number, 1100 for JSettlers 1.1.00.
     */
    public int getMinimumVersion()
    {
        return 1100;
    }

}
