/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
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
package soc.game;

import java.io.Serializable;


/**
 * This class represents a trade offer in Settlers of Catan
 */
public class TradeOffer implements Serializable, Cloneable
{
    private static final long serialVersionUID = 7700011674791746160L;
    String game;
    ResourceSet give;
    ResourceSet get;
    int from;
    boolean[] to;

    /**
     * The constructor for a TradeOffer
     *
     * @param  game  the name of the game in which this offer was made
     * @param  from  the number of the player making the offer
     * @param  to    a boolean array where 'true' means that the offer
     *               is being made to the player with the same number as
     *               the index of the 'true'
     * @param  give  the set of resources being given
     * @param  get   the set of resources being asked for
     */
    public TradeOffer(String game, int from, boolean[] to, ResourceSet give, ResourceSet get)
    {
        this.game = game;
        this.from = from;
        this.to = to;
        this.give = give;
        this.get = get;
    }

    /**
     * make a copy of this offer
     *
     * @param offer   the trade offer to copy
     */
    public TradeOffer(TradeOffer offer)
    {
        game = offer.game;
        from = offer.from;
        final int maxPlayers = offer.to.length;
        to = new boolean[maxPlayers];

        for (int i = 0; i < maxPlayers; i++)
        {
            to[i] = offer.to[i];
        }

        give = offer.give.copy();
        get = offer.get.copy();
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the number of the player that made the offer
     */
    public int getFrom()
    {
        return from;
    }

    /**
     * @return the boolean array representing to whom this offer was made
     */
    public boolean[] getTo()
    {
        return to;
    }

    /**
     * @return the set of resources offered
     */
    public ResourceSet getGiveSet()
    {
        return give;
    }

    /**
     * @return the set of resources wanted in exchange
     */
    public ResourceSet getGetSet()
    {
        return get;
    }

    /**
     * @return a human readable string of data
     */
    public String toString()
    {
        String str = "game=" + game + "|from=" + from + "|to=" + to[0];

        for (int i = 1; i < to.length; i++)
        {
            str += ("," + to[i]);
        }

        str += ("|give=" + give + "|get=" + get);

        return str;
    }
}
