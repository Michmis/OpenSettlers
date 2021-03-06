/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007-2009 Jeremy D. Monin <jeremy@nand.net>
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
package soc.disableDebug;

/**
 * Debug output; the disabled class is always off.
 * {@link soc.debug.D} and soc.disableDebug.D have the same interface, to easily switch
 * debug on and off per class.
 */
public class D
{
    static public final boolean ebugOn = false;
    // static private boolean enabled = false;

    /**
     * Does nothing, since this is the disabled version.
     */
    public static final void ebug_enable() {}

    /**
     * Always disabled; does nothing, since this is the disabled version.
     */
    public static final void ebug_disable() {}

    /**
     * Is debug currently enabled?
     */
    public static final boolean ebugIsEnabled()
    {
        return false;
    }

   /**
     * DOCUMENT ME!
     *
     * @param text DOCUMENT ME!
     */
    public static final void ebugPrintln(String text) {}

    /**
     * DOCUMENT ME!
     */
    public static final void ebugPrintln() {}

    /**
     * If debug is enabled, print the stack trace of this exception
     * @param ex Exception or other Throwable
     * @param prefixMsg Message for {@link #ebugPrintln(String)} above the exception,
     *                  or null
     */
    public static final void ebugPrintStackTrace(Throwable ex, String prefixMsg) {}

    /**
     * DOCUMENT ME!
     *
     * @param text DOCUMENT ME!
     */
    public static final void ebugPrint(String text) {}

    /**
     * Debug-println this text; for compatability with log4j.
     * Calls {@link #ebugPrintln(String)}.
     * @param text Text to debug-print
     */
    public static final void debug(String text) { ebugPrintln(text); }

}
