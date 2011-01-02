/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file copyright (C) 2008 Eli McGowan <http://sourceforge.net/users/emcgowan>
 * Portions of this file copyright (C) 2003-2004 Robert S. Thomas
 * Portions of this file copyright (C) 2008 Christopher McNeil <http://sourceforge.net/users/cmcneil>
 * Portions of this file copyright (C) 2009-2010 Jeremy D Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package soc.robot;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import soc.disableDebug.D;
import soc.game.Board;
import soc.game.Game;
import soc.game.Player;
import soc.game.PlayerNumbers;
import soc.game.ResourceSet;
import soc.game.Settlement;
import soc.util.CutoffExceededException;

/**
 * This class is a temporary class put in place to slowly pull tasks out of
 * RobotBrain and start replacing them with classes that implement strategy
 * interfaces. (Strategy Pattern)
 * 
 * @author Eli
 * 
 */
public class OpeningBuildStrategy
{

    /**
     * used in planning where to put our first and second settlements
     */
    protected int firstSettlement;

    /**
     * used in planning where to put our first and second settlements
     */
    protected int secondSettlement;

    /**
     * used to cache resource estimates for the board
     */
    protected int[] resourceEstimates;

    /**
     * figure out where to place the two settlements
     */
    public int planInitialSettlements(Game game, Player ourPlayerData)
    {
        D.debug("--- planInitialSettlements");

        int[] rolls;
        Enumeration hexes; // Integers
        int speed;
        boolean allTheWay;
        firstSettlement = 0;
        secondSettlement = 0;

        int bestSpeed = 4 * BuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        Board board = game.getBoard();
        ResourceSet emptySet = new ResourceSet();
        PlayerNumbers playerNumbers = new PlayerNumbers(board
                .getBoardEncodingFormat());
        int probTotal;
        int bestProbTotal;
        boolean[] ports = new boolean[Board.WOOD_PORT + 1];
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate();
        int[] prob = NumberProbabilities.INT_VALUES;

        bestProbTotal = 0;

        for (int firstNode = board.getMinNode(); firstNode <= Board.MAXNODE; firstNode++)
        {
            if (ourPlayerData.isPotentialSettlement(firstNode))
            {
                Integer firstNodeInt = new Integer(firstNode);

                //
                // this is just for testing purposes
                //
                D.debug("FIRST NODE -----------");
                D.debug("firstNode = " + board.nodeCoordToString(firstNode));

                StringBuffer sb = new StringBuffer();
                sb.append("numbers:[");
                playerNumbers.clear();
                probTotal = 0;
                hexes = Board.getAdjacentHexesToNode(firstNode).elements();

                while (hexes.hasMoreElements())
                {
                    Integer hex = (Integer) hexes.nextElement();
                    int number = board.getNumberOnHexFromCoord(hex.intValue());
                    int resource = board.getHexTypeFromCoord(hex.intValue());
                    playerNumbers.addNumberForResource(number, resource, hex
                            .intValue());
                    probTotal += prob[number];
                    sb.append(number + " ");
                }

                sb.append("]");
                D.debug(sb.toString());
                sb = new StringBuffer();
                sb.append("ports: ");

                for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT; portType++)
                {
                    if (board.getPortCoordinates(portType).contains(
                            firstNodeInt))
                    {
                        ports[portType] = true;
                    }
                    else
                    {
                        ports[portType] = false;
                    }

                    sb.append(ports[portType] + "  ");
                }

                D.debug(sb.toString());
                D.debug("probTotal = " + probTotal);
                estimate.recalculateEstimates(playerNumbers);
                speed = 0;
                allTheWay = false;

                try
                {
                    speed += estimate.calculateRollsFast(emptySet,
                            Game.SETTLEMENT_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet,
                            Game.CITY_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet,
                            Game.CARD_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet,
                            Game.ROAD_SET, 300, ports).getRolls();
                }
                catch (CutoffExceededException e)
                {
                }

                rolls = estimate.getEstimatesFromNothingFast(ports, 300);
                sb = new StringBuffer();
                sb.append(" road: " + rolls[BuildingSpeedEstimate.ROAD]);
                sb.append(" stlmt: " + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                sb.append(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                sb.append(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                D.debug(sb.toString());
                D.debug("speed = " + speed);

                //
                // end test
                //
                for (int secondNode = firstNode + 1; secondNode <= Board.MAXNODE; secondNode++)
                {
                    if ((ourPlayerData.isPotentialSettlement(secondNode))
                            && (!board.getAdjacentNodesToNode(secondNode)
                                    .contains(firstNodeInt)))
                    {
                        D.debug("firstNode = "
                                + board.nodeCoordToString(firstNode));
                        D.debug("secondNode = "
                                + board.nodeCoordToString(secondNode));

                        Integer secondNodeInt = new Integer(secondNode);

                        /**
                         * get the numbers for these settlements
                         */
                        sb = new StringBuffer();
                        sb.append("numbers:[");
                        playerNumbers.clear();
                        probTotal = 0;
                        hexes = Board.getAdjacentHexesToNode(firstNode)
                                .elements();

                        while (hexes.hasMoreElements())
                        {
                            Integer hex = (Integer) hexes.nextElement();
                            int number = board.getNumberOnHexFromCoord(hex
                                    .intValue());
                            int resource = board.getHexTypeFromCoord(hex
                                    .intValue());
                            playerNumbers.addNumberForResource(number,
                                    resource, hex.intValue());
                            probTotal += prob[number];
                            sb.append(number + " ");
                        }

                        sb.append("] [");
                        hexes = Board.getAdjacentHexesToNode(secondNode)
                                .elements();

                        while (hexes.hasMoreElements())
                        {
                            Integer hex = (Integer) hexes.nextElement();
                            int number = board.getNumberOnHexFromCoord(hex
                                    .intValue());
                            int resource = board.getHexTypeFromCoord(hex
                                    .intValue());
                            playerNumbers.addNumberForResource(number,
                                    resource, hex.intValue());
                            probTotal += prob[number];
                            sb.append(number + " ");
                        }

                        sb.append("]");
                        D.debug(sb.toString());

                        /**
                         * see if the settlements are on any ports
                         */
                        sb = new StringBuffer();
                        sb.append("ports: ");

                        for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT; portType++)
                        {
                            if ((board.getPortCoordinates(portType)
                                    .contains(firstNodeInt))
                                    || (board.getPortCoordinates(portType)
                                            .contains(secondNodeInt)))
                            {
                                ports[portType] = true;
                            }
                            else
                            {
                                ports[portType] = false;
                            }

                            sb.append(ports[portType] + "  ");
                        }

                        D.debug(sb.toString());
                        D.debug("probTotal = " + probTotal);

                        /**
                         * estimate the building speed for this pair
                         */
                        estimate.recalculateEstimates(playerNumbers);
                        speed = 0;
                        allTheWay = false;

                        try
                        {
                            speed += estimate.calculateRollsFast(emptySet,
                                    Game.SETTLEMENT_SET, bestSpeed, ports)
                                    .getRolls();

                            if (speed < bestSpeed)
                            {
                                speed += estimate.calculateRollsFast(emptySet,
                                        Game.CITY_SET, bestSpeed, ports)
                                        .getRolls();

                                if (speed < bestSpeed)
                                {
                                    speed += estimate.calculateRollsFast(
                                            emptySet, Game.CARD_SET, bestSpeed,
                                            ports).getRolls();

                                    if (speed < bestSpeed)
                                    {
                                        speed += estimate.calculateRollsFast(
                                                emptySet, Game.ROAD_SET,
                                                bestSpeed, ports).getRolls();
                                        allTheWay = true;
                                    }
                                }
                            }
                        }
                        catch (CutoffExceededException e)
                        {
                            speed = bestSpeed;
                        }

                        rolls = estimate.getEstimatesFromNothingFast(ports,
                                bestSpeed);
                        sb = new StringBuffer();
                        sb
                                .append(" road: "
                                        + rolls[BuildingSpeedEstimate.ROAD]);
                        sb.append(" stlmt: "
                                + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                        sb
                                .append(" city: "
                                        + rolls[BuildingSpeedEstimate.CITY]);
                        sb
                                .append(" card: "
                                        + rolls[BuildingSpeedEstimate.CARD]);
                        D.debug(sb.toString());
                        D.debug("allTheWay = " + allTheWay);
                        D.debug("speed = " + speed);

                        /**
                         * keep the settlements with the best speed
                         */
                        if (speed < bestSpeed)
                        {
                            firstSettlement = firstNode;
                            secondSettlement = secondNode;
                            bestSpeed = speed;
                            bestProbTotal = probTotal;
                            D.debug("bestSpeed = " + bestSpeed);
                            D.debug("bestProbTotal = " + bestProbTotal);
                        }
                        else if ((speed == bestSpeed) && allTheWay)
                        {
                            if (probTotal > bestProbTotal)
                            {
                                D.debug("Equal speed, better prob");
                                firstSettlement = firstNode;
                                secondSettlement = secondNode;
                                bestSpeed = speed;
                                bestProbTotal = probTotal;
                                D.debug("firstSettlement = "
                                        + Integer.toHexString(firstSettlement));
                                D
                                        .debug("secondSettlement = "
                                                + Integer
                                                        .toHexString(secondSettlement));
                                D.debug("bestSpeed = " + bestSpeed);
                                D.debug("bestProbTotal = " + bestProbTotal);
                            }
                        }
                    }
                }
            }
        }

        /**
         * choose which settlement to place first
         */
        playerNumbers.clear();
        hexes = Board.getAdjacentHexesToNode(firstSettlement).elements();

        while (hexes.hasMoreElements())
        {
            int hex = ((Integer) hexes.nextElement()).intValue();
            int number = board.getNumberOnHexFromCoord(hex);
            int resource = board.getHexTypeFromCoord(hex);
            playerNumbers.addNumberForResource(number, resource, hex);
        }

        Integer firstSettlementInt = new Integer(firstSettlement);

        for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT; portType++)
        {
            if (board.getPortCoordinates(portType).contains(firstSettlementInt))
            {
                ports[portType] = true;
            }
            else
            {
                ports[portType] = false;
            }
        }

        estimate.recalculateEstimates(playerNumbers);

        int firstSpeed = 0;
        int cutoff = 100;

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet,
                    Game.SETTLEMENT_SET, cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.CITY_SET,
                    cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.CARD_SET,
                    cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET,
                    cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        playerNumbers.clear();
        hexes = Board.getAdjacentHexesToNode(secondSettlement).elements();

        while (hexes.hasMoreElements())
        {
            int hex = ((Integer) hexes.nextElement()).intValue();
            int number = board.getNumberOnHexFromCoord(hex);
            int resource = board.getHexTypeFromCoord(hex);
            playerNumbers.addNumberForResource(number, resource, hex);
        }

        Integer secondSettlementInt = new Integer(secondSettlement);

        for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT; portType++)
        {
            if (board.getPortCoordinates(portType)
                    .contains(secondSettlementInt))
            {
                ports[portType] = true;
            }
            else
            {
                ports[portType] = false;
            }
        }

        estimate.recalculateEstimates(playerNumbers);

        int secondSpeed = 0;

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet,
                    Game.SETTLEMENT_SET, bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.CITY_SET,
                    bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.CARD_SET,
                    bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET,
                    bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        if (firstSpeed > secondSpeed)
        {
            int tmp = firstSettlement;
            firstSettlement = secondSettlement;
            secondSettlement = tmp;
        }

        D.debug(board.nodeCoordToString(firstSettlement) + ":" + firstSpeed
                + ", " + board.nodeCoordToString(secondSettlement) + ":"
                + secondSpeed);
        return firstSettlement;
    }

    /**
     * figure out where to place the second settlement
     */
    public int planSecondSettlement(Game game, Player ourPlayerData)
    {
        D.debug("--- planSecondSettlement");

        int bestSpeed = 4 * BuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        Board board = game.getBoard();
        ResourceSet emptySet = new ResourceSet();
        PlayerNumbers playerNumbers = new PlayerNumbers(board
                .getBoardEncodingFormat());
        boolean[] ports = new boolean[Board.WOOD_PORT + 1];
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate();
        int probTotal;
        int bestProbTotal;
        int[] prob = NumberProbabilities.INT_VALUES;
        int firstNode = firstSettlement;
        Integer firstNodeInt = new Integer(firstNode);

        bestProbTotal = 0;
        secondSettlement = -1;

        for (int secondNode = board.getMinNode(); secondNode <= Board.MAXNODE; secondNode++)
        {
            if ((ourPlayerData.isPotentialSettlement(secondNode))
                    && (!board.getAdjacentNodesToNode(secondNode).contains(
                            firstNodeInt)))
            {
                Integer secondNodeInt = new Integer(secondNode);

                /**
                 * get the numbers for these settlements
                 */
                StringBuffer sb = new StringBuffer();
                sb.append("numbers: ");
                playerNumbers.clear();
                probTotal = 0;

                Enumeration hexes = Board.getAdjacentHexesToNode(firstNode)
                        .elements(); // Integers

                while (hexes.hasMoreElements())
                {
                    final int hex = ((Integer) hexes.nextElement()).intValue();
                    int number = board.getNumberOnHexFromCoord(hex);
                    int resource = board.getHexTypeFromCoord(hex);
                    playerNumbers.addNumberForResource(number, resource, hex);
                    probTotal += prob[number];
                    sb.append(number + " ");
                }

                hexes = Board.getAdjacentHexesToNode(secondNode).elements();

                while (hexes.hasMoreElements())
                {
                    final int hex = ((Integer) hexes.nextElement()).intValue();
                    int number = board.getNumberOnHexFromCoord(hex);
                    int resource = board.getHexTypeFromCoord(hex);
                    playerNumbers.addNumberForResource(number, resource, hex);
                    probTotal += prob[number];
                    sb.append(number + " ");
                }

                /**
                 * see if the settlements are on any ports
                 */
                sb.append("ports: ");

                for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT; portType++)
                {
                    if ((board.getPortCoordinates(portType)
                            .contains(firstNodeInt))
                            || (board.getPortCoordinates(portType)
                                    .contains(secondNodeInt)))
                    {
                        ports[portType] = true;
                    }
                    else
                    {
                        ports[portType] = false;
                    }

                    sb.append(ports[portType] + "  ");
                }

                D.debug(sb.toString());
                D.debug("probTotal = " + probTotal);

                /**
                 * estimate the building speed for this pair
                 */
                estimate.recalculateEstimates(playerNumbers);

                int speed = 0;

                try
                {
                    speed += estimate.calculateRollsFast(emptySet,
                            Game.SETTLEMENT_SET, bestSpeed, ports).getRolls();

                    if (speed < bestSpeed)
                    {
                        speed += estimate.calculateRollsFast(emptySet,
                                Game.CITY_SET, bestSpeed, ports).getRolls();

                        if (speed < bestSpeed)
                        {
                            speed += estimate.calculateRollsFast(emptySet,
                                    Game.CARD_SET, bestSpeed, ports).getRolls();

                            if (speed < bestSpeed)
                            {
                                speed += estimate.calculateRollsFast(emptySet,
                                        Game.ROAD_SET, bestSpeed, ports)
                                        .getRolls();
                            }
                        }
                    }
                }
                catch (CutoffExceededException e)
                {
                    speed = bestSpeed;
                }

                D.debug(Integer.toHexString(firstNode) + ", "
                        + Integer.toHexString(secondNode) + ":" + speed);

                /**
                 * keep the settlements with the best speed
                 */
                if ((speed < bestSpeed) || (secondSettlement < 0))
                {
                    firstSettlement = firstNode;
                    secondSettlement = secondNode;
                    bestSpeed = speed;
                    bestProbTotal = probTotal;
                    D.debug("firstSettlement = "
                            + Integer.toHexString(firstSettlement));
                    D.debug("secondSettlement = "
                            + Integer.toHexString(secondSettlement));

                    int[] rolls = estimate.getEstimatesFromNothingFast(ports);
                    sb = new StringBuffer();
                    sb.append("road: " + rolls[BuildingSpeedEstimate.ROAD]);
                    sb.append(" stlmt: "
                            + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                    sb.append(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                    sb.append(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                    D.debug(sb.toString());
                    D.debug("bestSpeed = " + bestSpeed);
                }
                else if (speed == bestSpeed)
                {
                    if (probTotal > bestProbTotal)
                    {
                        firstSettlement = firstNode;
                        secondSettlement = secondNode;
                        bestSpeed = speed;
                        bestProbTotal = probTotal;
                        D.debug("firstSettlement = "
                                + Integer.toHexString(firstSettlement));
                        D.debug("secondSettlement = "
                                + Integer.toHexString(secondSettlement));

                        int[] rolls = estimate
                                .getEstimatesFromNothingFast(ports);
                        sb = new StringBuffer();
                        sb.append("road: " + rolls[BuildingSpeedEstimate.ROAD]);
                        sb.append(" stlmt: "
                                + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                        sb
                                .append(" city: "
                                        + rolls[BuildingSpeedEstimate.CITY]);
                        sb
                                .append(" card: "
                                        + rolls[BuildingSpeedEstimate.CARD]);
                        D.debug(sb.toString());
                        D.debug("bestSpeed = " + bestSpeed);
                    }
                }
            }
        }
        return secondSettlement;
    }

    /**
     * place a road attached to the last initial settlement
     */
    public int planInitRoad(Game game, Player ourPlayerData, RobotClient client)
    {
        int settlementNode = ourPlayerData.getLastSettlementCoord();
        Hashtable twoAway = new Hashtable(); // <Integer,Integer>

        D.debug("--- placeInitRoad");

        /**
         * look at all of the nodes that are 2 away from the last settlement and
         * pick the best one
         */
        Board board = game.getBoard();
        int tmp;

        tmp = settlementNode - 0x20;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x02;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x22;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x20;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode - 0x02;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode - 0x22;

        if (board.isNodeOnBoard(tmp)
                && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        scoreNodesForSettlements(twoAway, 3, 5, 10, game, ourPlayerData);

        D.debug("Init Road for " + client.getNickname());

        /**
         * create a dummy player to calculate possible places to build taking
         * into account where other players will build before we can.
         */
        Player dummy = new Player(ourPlayerData.getPlayerNumber(), game);

        if (game.getGameState() == Game.START1B)
        {
            /**
             * do a look ahead so we don't build toward a place where someone
             * else will build first.
             */
            int numberOfBuilds = numberOfEnemyBuilds(game);
            D.debug("Other players will build " + numberOfBuilds
                    + " settlements before I get to build again.");

            if (numberOfBuilds > 0)
            {
                /**
                 * rule out where other players are going to build
                 */
                Hashtable allNodes = new Hashtable(); // <Integer.Integer>
                final int minNode = board.getMinNode();

                for (int i = minNode; i <= Board.MAXNODE; i++)
                {
                    if (ourPlayerData.isPotentialSettlement(i))
                    {
                        D.debug("-- potential settlement at "
                                + Integer.toHexString(i));
                        allNodes.put(new Integer(i), new Integer(0));
                    }
                }

                /**
                 * favor spots with the most high numbers
                 */
                bestSpotForNumbers(allNodes, 100, game);

                /**
                 * favor spots near good ports
                 */
                /**
                 * check 3:1 ports
                 */
                Vector miscPortNodes = game.getBoard().getPortCoordinates(
                        Board.MISC_PORT);
                bestSpot2AwayFromANodeSet(allNodes, miscPortNodes, 5);

                /**
                 * check out good 2:1 ports
                 */
                for (int portType = Board.CLAY_PORT; portType <= Board.WOOD_PORT; portType++)
                {
                    /**
                     * if the chances of rolling a number on the resource is
                     * better than 1/3, then it's worth looking at the port
                     */
                    if (resourceEstimates[portType] > 33)
                    {
                        Vector portNodes = game.getBoard().getPortCoordinates(
                                portType);
                        int portWeight = (resourceEstimates[portType] * 10) / 56;
                        bestSpot2AwayFromANodeSet(allNodes, portNodes,
                                portWeight);
                    }
                }

                /*
                 * create a list of potential settlements that takes into
                 * account where other players will build
                 */
                Vector psList = new Vector(); // <Integer>

                for (int j = minNode; j <= Board.MAXNODE; j++)
                {
                    if (ourPlayerData.isPotentialSettlement(j))
                    {
                        D.debug("- potential settlement at "
                                + Integer.toHexString(j));
                        psList.addElement(new Integer(j));
                    }
                }

                dummy.setPotentialSettlements(psList);

                for (int builds = 0; builds < numberOfBuilds; builds++)
                {
                    BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0,
                            0);
                    Enumeration nodesEnum = allNodes.keys(); // <Integer>

                    while (nodesEnum.hasMoreElements())
                    {
                        Integer nodeCoord = (Integer) nodesEnum.nextElement();
                        final int score = ((Integer) allNodes.get(nodeCoord))
                                .intValue();
                        D.debug("NODE = "
                                + Integer.toHexString(nodeCoord.intValue())
                                + " SCORE = " + score);

                        if (bestNodePair.getScore() < score)
                        {
                            bestNodePair.setScore(score);
                            bestNodePair.setNode(nodeCoord.intValue());
                        }
                    }

                    /**
                     * pretend that someone has built a settlement on the best
                     * spot
                     */
                    dummy.updatePotentials(new Settlement(ourPlayerData,
                            bestNodePair.getNode(), null));

                    /**
                     * remove this spot from the list of best spots
                     */
                    allNodes.remove(new Integer(bestNodePair.getNode()));
                }
            }
        }

        /**
         * Find the best scoring node
         */
        BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, 0);
        Enumeration cenum = twoAway.keys(); // <Integer>

        while (cenum.hasMoreElements())
        {
            Integer coord = (Integer) cenum.nextElement();
            final int score = ((Integer) twoAway.get(coord)).intValue();

            D.debug("Considering " + Integer.toHexString(coord.intValue())
                    + " with a score of " + score);

            if (dummy.isPotentialSettlement(coord.intValue()))
            {
                if (bestNodePair.getScore() < score)
                {
                    bestNodePair.setScore(score);
                    bestNodePair.setNode(coord.intValue());
                }
            }
            else
            {
                D.debug("Someone is bound to ruin that spot.");
            }
        }

        int roadEdge = 0;
        int destination = bestNodePair.getNode();

        /**
         * if the coords are (even, odd), then the node is 'Y'.
         */
        if (((settlementNode >> 4) % 2) == 0)
        {
            if ((destination == (settlementNode - 0x02))
                    || (destination == (settlementNode + 0x20)))
            {
                roadEdge = settlementNode - 0x01;
            }
            else if (destination < settlementNode)
            {
                roadEdge = settlementNode - 0x11;
            }
            else
            {
                roadEdge = settlementNode;
            }
        }
        else
        {
            if ((destination == (settlementNode - 0x20))
                    || (destination == (settlementNode + 0x02)))
            {
                roadEdge = settlementNode - 0x10;
            }
            else if (destination > settlementNode)
            {
                roadEdge = settlementNode;
            }
            else
            {
                roadEdge = settlementNode - 0x11;
            }
        }
        dummy.destroyPlayer();

        return roadEdge;
    }

    /**
     * this is a function more for convience given a set of nodes, run a bunch
     * of metrics across them to find which one is best for building a
     * settlement
     * 
     * @param nodes
     *            a hashtable of nodes, the scores in the table will be
     *            modified. Hashtable<Integer,Integer>.
     * @param numberWeight
     *            the weight given to nodes on good numbers
     * @param miscPortWeight
     *            the weight given to nodes on 3:1 ports
     * @param portWeight
     *            the weight given to nodes on good 2:1 ports
     */
    protected void scoreNodesForSettlements(Hashtable nodes, int numberWeight,
            int miscPortWeight, int portWeight, Game game, Player ourPlayerData)
    {
        /**
         * favor spots with the most high numbers
         */
        bestSpotForNumbers(nodes, ourPlayerData, numberWeight, game);

        /**
         * favor spots on good ports
         */
        /**
         * check if this is on a 3:1 ports, only if we don't have one
         */
        if (!ourPlayerData.getPortFlag(Board.MISC_PORT))
        {
            Vector miscPortNodes = game.getBoard().getPortCoordinates(
                    Board.MISC_PORT);
            bestSpotInANodeSet(nodes, miscPortNodes, miscPortWeight);
        }

        /**
         * check out good 2:1 ports that we don't have
         */
        // TO-DO: this is extremely dangerous as there already exists a variable
        // called resourceEstimates
        int[] resourceEstimates = null;
        resourceEstimates = estimateResourceRarity(game);

        for (int portType = Board.CLAY_PORT; portType <= Board.WOOD_PORT; portType++)
        {
            /**
             * if the chances of rolling a number on the resource is better than
             * 1/3, then it's worth looking at the port
             */
            if ((resourceEstimates[portType] > 33)
                    && (!ourPlayerData.getPortFlag(portType)))
            {
                Vector portNodes = game.getBoard().getPortCoordinates(portType);
                int estimatedPortWeight = (resourceEstimates[portType] * portWeight) / 56;
                bestSpotInANodeSet(nodes, portNodes, estimatedPortWeight);
            }
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to each node score in
     * the table. A vector of nodes that we want to be on is also taken as an
     * argument. Here are the rules for scoring: If a node is in the desired set
     * of nodes it gets 100. Otherwise it gets 0.
     * 
     * @param nodesIn
     *            the table of nodes to evaluate: Hashtable<Integer,Integer>
     * @param nodeSet
     *            the set of desired nodes
     * @param weight
     *            the score multiplier
     */
    protected void bestSpotInANodeSet(Hashtable nodesIn, Vector nodeSet,
            int weight)
    {
        Enumeration nodesInEnum = nodesIn.keys(); // <Integer>

        while (nodesInEnum.hasMoreElements())
        {
            Integer nodeCoord = (Integer) nodesInEnum.nextElement();
            int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = ((Integer) nodesIn.get(nodeCoord)).intValue();

            Enumeration nodeSetEnum = nodeSet.elements();

            while (nodeSetEnum.hasMoreElements())
            {
                int target = ((Integer) nodeSetEnum.nextElement()).intValue();

                if (node == target)
                {
                    score = 100;

                    break;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, new Integer(oldScore + score));

            // log.debug("BSIANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to each node score in
     * the table. A vector of nodes that we want to be near is also taken as an
     * argument. Here are the rules for scoring: If a node is two away from a
     * node in the desired set of nodes it gets 100. Otherwise it gets 0.
     * 
     * @param nodesIn
     *            the table of nodes to evaluate: Hashtable<Integer,Integer>
     * @param nodeSet
     *            the set of desired nodes
     * @param weight
     *            the score multiplier
     */
    protected void bestSpot2AwayFromANodeSet(Hashtable nodesIn, Vector nodeSet,
            int weight)
    {
        Enumeration nodesInEnum = nodesIn.keys(); // <Integer>

        while (nodesInEnum.hasMoreElements())
        {
            Integer nodeCoord = (Integer) nodesInEnum.nextElement();
            int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = ((Integer) nodesIn.get(nodeCoord)).intValue();

            Enumeration nodeSetEnum = nodeSet.elements();

            while (nodeSetEnum.hasMoreElements())
            {
                int target = ((Integer) nodeSetEnum.nextElement()).intValue();

                if (node == target)
                {
                    break;
                }
                else if (node == (target - 0x20))
                {
                    score = 100;
                }
                else if (node == (target + 0x02))
                {
                    score = 100;
                }
                else if (node == (target + 0x22))
                {
                    score = 100;
                }
                else if (node == (target + 0x20))
                {
                    score = 100;
                }
                else if (node == (target - 0x02))
                {
                    score = 100;
                }
                else if (node == (target - 0x22))
                {
                    score = 100;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, new Integer(oldScore + score));

            // log.debug("BS2AFANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * estimate the rarity of each resource
     * 
     * @return an array of rarity numbers where estimates[Board.CLAY_HEX] == the
     *         clay rarity
     */
    protected int[] estimateResourceRarity(Game game)
    {
        if (resourceEstimates == null)
        {
            Board board = game.getBoard();
            int[] numberWeights = NumberProbabilities.INT_VALUES;

            resourceEstimates = new int[6];

            resourceEstimates[0] = 0;

            // look at each hex
            final int L = board.getNumberLayout().length;
            for (int i = 0; i < L; i++)
            {
                int hexNumber = board.getNumberOnHexFromNumber(i);

                if (hexNumber > 0)
                {
                    resourceEstimates[board.getHexTypeFromNumber(i)] += numberWeights[hexNumber];
                }
            }
        }

        // D.ebugPrint("Resource Estimates = ");
        for (int i = 1; i < 6; i++)
        {
            // D.ebugPrint(i+":"+resourceEstimates[i]+" ");
        }

        // log.debug();
        return resourceEstimates;
    }

    /**
     * Takes a table of nodes and adds a weighted score to each node score in
     * the table. Nodes touching hexes with better numbers get better scores.
     * 
     * @param nodes
     *            the table of nodes with scores: Hashtable<Integer,Integer>
     * @param weight
     *            a number that is multiplied by the score
     */
    protected void bestSpotForNumbers(Hashtable nodes, int weight, Game game)
    {
        int[] numRating = NumberProbabilities.INT_VALUES;
        Board board = game.getBoard();
        int oldScore;
        Enumeration nodesEnum = nodes.keys(); // <Integer>

        while (nodesEnum.hasMoreElements())
        {
            Integer node = (Integer) nodesEnum.nextElement();

            // log.debug("BSN - looking at node "+Integer.toHexString(node.intValue()));
            oldScore = ((Integer) nodes.get(node)).intValue();

            int score = 0;
            Enumeration hexesEnum = Board.getAdjacentHexesToNode(
                    node.intValue()).elements(); // <Integer>

            while (hexesEnum.hasMoreElements())
            {
                int hex = ((Integer) hexesEnum.nextElement()).intValue();
                score += numRating[board.getNumberOnHexFromCoord(hex)];

                // log.debug(" -- -- Adding "+numRating[board.getNumberOnHexFromCoord(hex)]);
            }

            /*
             * normalize score and multiply by weight 40 is highest practical
             * score lowest score is 0
             */
            int nScore = ((score * 100) / 40) * weight;
            Integer finalScore = new Integer(nScore + oldScore);
            nodes.put(node, finalScore);

            // log.debug("BSN -- put node "+Integer.toHexString(node.intValue())+" with old score "+oldScore+" + new score "+nScore);
        }
    }

    /**
     * calculate the number of builds before the next turn during init placement
     * 
     */
    protected int numberOfEnemyBuilds(Game game)
    {
        int numberOfBuilds = 0;
        int pNum = game.getCurrentPlayerNumber();

        /**
         * This is the clockwise direction
         */
        if ((game.getGameState() == Game.START1A)
                || (game.getGameState() == Game.START1B))
        {
            do
            {
                /**
                 * look at the next player
                 */
                pNum++;

                if (pNum >= game.maxPlayers)
                {
                    pNum = 0;
                }

                if ((pNum != game.getFirstPlayer()) && !game.isSeatVacant(pNum))
                {
                    numberOfBuilds++;
                }
            } while (pNum != game.getFirstPlayer());
        }

        /**
         * This is the counter-clockwise direction
         */
        do
        {
            /**
             * look at the next player
             */
            pNum--;

            if (pNum < 0)
            {
                pNum = game.maxPlayers - 1;
            }

            if ((pNum != game.getCurrentPlayerNumber())
                    && !game.isSeatVacant(pNum))
            {
                numberOfBuilds++;
            }
        } while (pNum != game.getCurrentPlayerNumber());

        return numberOfBuilds;
    }

    /**
     * Takes a table of nodes and adds a weighted score to each node score in
     * the table. Nodes touching hexes with better numbers get better scores.
     * Also numbers that the player isn't touching yet are better than ones that
     * the player is already touching.
     * 
     * @param nodes
     *            the table of nodes with scores: Hashtable<Integer,Integer>
     * @param player
     *            the player that we are doing the rating for
     * @param weight
     *            a number that is multiplied by the score
     */
    protected void bestSpotForNumbers(Hashtable nodes, Player player,
            int weight, Game game)
    {
        int[] numRating = NumberProbabilities.INT_VALUES;
        Board board = game.getBoard();
        int oldScore;
        Enumeration nodesEnum = nodes.keys(); // <Integer>

        while (nodesEnum.hasMoreElements())
        {
            Integer node = (Integer) nodesEnum.nextElement();

            // log.debug("BSN - looking at node "+Integer.toHexString(node.intValue()));
            oldScore = ((Integer) nodes.get(node)).intValue();

            int score = 0;
            Enumeration hexesEnum = Board.getAdjacentHexesToNode(
                    node.intValue()).elements(); // <Integer>

            while (hexesEnum.hasMoreElements())
            {
                final int hex = ((Integer) hexesEnum.nextElement()).intValue();
                final int number = board.getNumberOnHexFromCoord(hex);
                score += numRating[number];

                if ((number != 0) && (!player.getNumbers().hasNumber(number)))
                {
                    /**
                     * add a bonus for numbers that the player doesn't already
                     * have
                     */

                    // log.debug("ADDING BONUS FOR NOT HAVING "+number);
                    score += numRating[number];
                }

                // log.debug(" -- -- Adding "+numRating[board.getNumberOnHexFromCoord(hex)]);
            }

            /*
             * normalize score and multiply by weight 80 is highest practical
             * score lowest score is 0
             */
            int nScore = ((score * 100) / 80) * weight;
            Integer finalScore = new Integer(nScore + oldScore);
            nodes.put(node, finalScore);

            // log.debug("BSN -- put node "+Integer.toHexString(node.intValue())+" with old score "+oldScore+" + new score "+nScore);
        }
    }
}
