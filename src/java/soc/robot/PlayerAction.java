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
package soc.robot;

import soc.game.Player;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
public class PlayerAction
{
    /**
     * possible actions
     */
    public static final int PLACE_ROAD = 0;
    public static final int PLACE_SETTLEMENT = 1;
    public static final int PLACE_CITY = 2;
    public static final int PLAY_KNIGHT = 3;
    public static final int DRAW_VP = 4;
    public static final int REMOVE_KNIGHT = 5;
    public static final int REMOVE_VP = 6;

    /**
     * The type of action
     */
    protected int actionType;

    /**
     * The player who owns this piece
     */
    protected Player player;

    /**
     * Where this piece is on the board
     */
    protected int coord;

    /**
     * constructor
     */
    public PlayerAction(int type, Player pl, int co)
    {
        actionType = type;
        player = pl;
        coord = co;
    }

    /**
     * @return  the type of piece
     */
    public int getType()
    {
        return actionType;
    }

    /**
     * @return the owner of the piece
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return the coordinates for this piece
     */
    public int getCoordinates()
    {
        return coord;
    }
}
