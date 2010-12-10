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

import soc.game.DevCardConstants;
import soc.game.Player;
import soc.game.PlayingPiece;

import java.util.Vector;

/**
 * This represents a possible thing to build.
 * It includes what and where to build.  A score
 * that represents how many VP this build is worth,
 * and a list of other building possibilities that
 * result from building this thing.
 */
public class BuildPossibility
{
    PlayingPiece piece;
    boolean buyDevCard;
    int devCardType;
    int freeRoads;
    int score;
    int[] buildingSpeedup;
    int eta;
    int priority;
    Player player;
    BuildPossibility parent;
    Vector children;

    /**
     * this is a constructor
     *
     * @param pi  what and where to build
     * @param sc  how many VP this build is worth
     * @param bs  how our building speeds are affected
     * @param et  how many turns until we can build this
     * @param pr  the priority
     * @param pl  the player's state after the build
     */
    public BuildPossibility(PlayingPiece pi, int sc, int[] bs, int et, int pr, Player pl)
    {
        piece = pi;
        buyDevCard = false;
        devCardType = -1;
        freeRoads = 0;
        score = sc;
        buildingSpeedup = bs;
        this.eta = et;
        priority = pr;
        player = pl;
        parent = null;
        children = new Vector();
    }

    /**
     * this is a constructor for when you are using a road building card
     *
     * @param pi  what and where to build
     * @param sc  how many VP this build is worth
     * @param bs  how our building speeds are affected
     * @param et  how many turns until we can build this
     * @param pr  the priority
     * @param pl  the player's state after the build
     * @param fr  how many free roads are left
     */
    public BuildPossibility(PlayingPiece pi, int sc, int[] bs, int et, int pr, Player pl, int fr)
    {
        piece = pi;
        buyDevCard = false;
        devCardType = -1;
        freeRoads = fr;
        score = sc;
        buildingSpeedup = bs;
        this.eta = et;
        priority = pr;
        player = pl;
        parent = null;
        children = new Vector();
    }

    /**
     * this is a constructor for buying a dev card
     *
     * @param sc  how many VP this build is worth
     * @param bs  how our building speeds are affected
     * @param et  how many turns until we can build this
     * @param pr  the priority
     * @param pl  the player's state after the build
     */
    public BuildPossibility(int sc, int[] bs, int et, int pr, Player pl)
    {
        piece = null;
        buyDevCard = true;
        devCardType = DevCardConstants.KNIGHT;
        freeRoads = 0;
        score = sc;
        buildingSpeedup = bs;
        this.eta = et;
        priority = pr;
        player = pl;
        parent = null;
        children = new Vector();
    }

    /**
     * this is a constructor for PLAYING a dev card
     *
     * @param dt  which dev card to play
     * @param sc  how many VP this build is worth
     * @param bs  how our building speeds are affected
     * @param et  how many turns until we can build this
     * @param pr  the priority
     * @param pl  the player's state after the build
     */
    public BuildPossibility(int dt, int sc, int[] bs, int et, int pr, Player pl)
    {
        piece = null;
        buyDevCard = false;
        devCardType = dt;
        freeRoads = 0;
        score = sc;
        buildingSpeedup = bs;
        this.eta = et;
        priority = pr;
        player = pl;
        parent = null;
        children = new Vector();
    }

    /**
     * @return the piece
     */
    public PlayingPiece getPiece()
    {
        return piece;
    }

    /**
     * @return true if this is a request to buy a dev card
     */
    public boolean isBuyDevCard()
    {
        return buyDevCard;
    }

    /**
     * @return true if this is a request to play a dev card
     */
    public boolean isPlayDevCard()
    {
        return ((piece == null) && !buyDevCard);
    }

    /**
     * @return the type of dev card to play or buy
     */
    public int getDevCardType()
    {
        return devCardType;
    }

    /**
     * @return the number of free roads left
     */
    public int getFreeRoads()
    {
        return freeRoads;
    }

    /**
     * @return the score
     */
    public int getScore()
    {
        return score;
    }

    /**
     * @return the building speed differences
     */
    public int[] getBuildingSpeedup()
    {
        return buildingSpeedup;
    }

    /**
     * @return the number of turns it will take to do this
     */
    public int getETA()
    {
        return eta;
    }

    /**
     * @return the priority
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * @return the player's future state
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return the building children that this one makes
     */
    public Vector getChildren()
    {
        return children;
    }

    /**
     * @return the parent of this node
     */
    public BuildPossibility getParent()
    {
        return parent;
    }

    /**
     * set the parent for this node
     *
     * @param par  the parent
     */
    public void setParent(BuildPossibility par)
    {
        parent = par;
    }

    /**
     * add a building possibility to the list of children
     *
     * @param poss  the building possibility
     */
    public void addChild(BuildPossibility poss)
    {
        children.addElement(poss);
        poss.setParent(this);
    }

    /**
     * @return a human readable form of this object
     */
    public String toString()
    {
        String str = "SOCBP:player=" + player + "|piece=" + piece + "|score=" + score + "|speedup=";

        if (buildingSpeedup != null)
        {
            for (int i = BuildingSpeedEstimate.MIN;
                    i < BuildingSpeedEstimate.MAXPLUSONE; i++)
            {
                str += (" " + buildingSpeedup[i]);
            }
        }
        else
        {
            str += "null";
        }

        str += ("|eta=" + eta + "|priority=" + priority + "|children=" + children.size());

        return str;
    }
}
