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
package soc.game;

import java.util.Vector;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.5 $
 */
public class LRPathData
{
    private int beginningCoord;
    private int endCoord;
    private int length;
    private Vector nodePairs;

    /**
     * Creates a new LRPathData object.
     *
     * @param start DOCUMENT ME!
     * @param end DOCUMENT ME!
     * @param len DOCUMENT ME!
     * @param pairs DOCUMENT ME!
     */
    public LRPathData(int start, int end, int len, Vector pairs)
    {
        beginningCoord = start;
        endCoord = end;
        length = len;
        nodePairs = pairs;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getBeginning()
    {
        return beginningCoord;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getEnd()
    {
        return endCoord;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Vector getNodePairs()
    {
        return nodePairs;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getLength()
    {
        return length;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String toString()
    {
        String s = "LRPathData:";
        s += ("bc=" + Integer.toHexString(beginningCoord) + "|");
        s += ("ec=" + Integer.toHexString(endCoord) + "|");
        s += ("len=" + length + "|");
        s += ("pairs=" + nodePairs);

        return s;
    }
}
