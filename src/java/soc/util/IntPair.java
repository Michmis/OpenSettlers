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
package soc.util;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.5 $
 */
public class IntPair
{
    public int a;
    public int b;

    /**
     * Creates a new IntPair object initialized with 0 for both values.
     */
    public IntPair()
    {
        this(0, 0);
    }

    /**
     * Creates a new IntPair object.
     *
     * @param i DOCUMENT ME!
     * @param j DOCUMENT ME!
     */
    public IntPair(int i, int j)
    {
        a = i;
        b = j;
    }

    /**
     * returns a hash code for the object
     *
     * @return the hash code
     */
    public int hashCode()
    {
        return a ^ b;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param o DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean equals(Object o)
    {
        if (o instanceof IntPair)
        {
            IntPair ip = (IntPair)o;
            if (((ip.a == a) && (ip.b == b)) || ((ip.a == b) && (ip.b == a)))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getA()
    {
        return a;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getB()
    {
        return b;
    }

    /**
     * DOCUMENT ME!
     *
     * @param val DOCUMENT ME!
     */
    public void setA(int val)
    {
        a = val;
    }

    /**
     * DOCUMENT ME!
     *
     * @param val DOCUMENT ME!
     */
    public void setB(int val)
    {
        b = val;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String toString()
    {
        return "a:" + Integer.toHexString(a) + ", b:" + Integer.toHexString(b);
    }
}