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
 * This message means that a new chat channel has been created.
 *
 * @author Robert S Thomas
 */
public class NewChannel extends Message
{
    private static final long serialVersionUID = -2445009994491461146L;
    /**
     * Name of the new channel.
     */
    private String channel;

    /**
     * Create a NewChannel message.
     *
     * @param ch  name of new channel
     */
    public NewChannel(String ch)
    {
        messageType = NEWCHANNEL;
        channel = ch;
    }

    /**
     * @return the name of the channel
     */
    public String getChannel()
    {
        return channel;
    }

    /**
     * NEWCHANNEL sep channel
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(channel);
    }

    /**
     * NEWCHANNEL sep channel
     *
     * @param ch  the new channel name
     * @return    the command string
     */
    public static String toCmd(String ch)
    {
        return NEWCHANNEL + sep + ch;
    }

    /**
     * Parse the command String into a NewChannel message
     *
     * @param s   the String to parse
     * @return    a NewChannel message
     */
    public static NewChannel parseDataStr(String s)
    {
        return new NewChannel(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "NewChannel:channel=" + channel;
    }
}
