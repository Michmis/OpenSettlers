/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
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


/**
 * This message requests that the player rolls the dice
 * this client to join a game
 *
 * @author Robert S Thomas
 */
public class RollDiceRequest extends Message
{
    private static final long serialVersionUID = 1982481151988828756L;
    /**
     * Name of game
     */
    private String game;

    /**
     * Create a RollDiceRequest message.
     *
     * @param ga  name of game
     */
    public RollDiceRequest(String ga)
    {
        messageType = ROLLDICEREQUEST;
        game = ga;
    }

    /**
     * @return the game name
     */
    public String getGame()
    {
        return game;
    }

    /**
     * ROLLDICEREQUEST sep game
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(game);
    }

    /**
     * ROLLDICEREQUEST sep game
     *
     * @param ga  the game name
     * @return    the command string
     */
    public static String toCmd(String ga)
    {
        return ROLLDICEREQUEST + sep + ga;
    }

    /**
     * Parse the command String into a RollDiceRequest message
     *
     * @param s   the String to parse
     * @return    a RollDiceRequest message, or null of the data is garbled
     */
    public static RollDiceRequest parseDataStr(String s)
    {
        return new RollDiceRequest(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        String s = "RollDiceRequest:game=" + game;

        return s;
    }
}
