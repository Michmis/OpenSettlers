/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2008 Jeremy D Monin <jeremy@nand.net>
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
package soc.client;

import java.awt.Frame;


/**
 * This is the dialog to confirm when someone closes the client.
 * The quit action is to call client.putLeaveAll() and System.exit(0).
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
class QuitAllConfirmDialog extends AskDialog
{
    private static final long serialVersionUID = 2429965525477307743L;
    protected boolean hostedServerActive;

    /**
     * Creates and shows a new QuitAllConfirmDialog.
     * "Continue" is default.
     *
     * @param cli      Player client interface
     * @param gamePIOrSelf   An active game's player interface, or the client's Frame
     *                 if we're hosting a local server but not actively playing
     * @throws IllegalArgumentException If cli or gameOrSelf is null
     */
    public static void createAndShow(PlayerClient cli, Frame gamePIOrSelf)
        throws IllegalArgumentException
    {
        if ((cli == null) || (gamePIOrSelf == null))
            throw new IllegalArgumentException("no nulls");

        boolean hasAny = cli.anyHostedActiveGames();
        QuitAllConfirmDialog qcd = new QuitAllConfirmDialog(cli, gamePIOrSelf, hasAny);
        qcd.setVisible(true);      
    }
    

    /**
     * Creates a new QuitAllConfirmDialog.
     *
     * @param cli      Player client interface
     * @param gamePIOrSelf   An active game's player interface, or the client's Frame
     *                 if we're hosting a local server but not actively playing
     * @param hostedServerActive Is client hosting a local server with games active?
     *                 Call {@link PlayerClient#anyHostedActiveGames()} to determine.
     */
    protected QuitAllConfirmDialog(PlayerClient cli, Frame gamePIOrSelf, boolean hostedServerActive)
    {
        super(cli, gamePIOrSelf,
            (hostedServerActive ? "Shut down game server?" : "Really quit all games?"),
            "One or more games are still active.",
            (hostedServerActive ? "Shut down server anyway" : "Quit all games"),
            (hostedServerActive ? "Continue serving" : "Continue playing"),
            false, true);
        this.hostedServerActive = hostedServerActive;
    }

    /**
     * React to the Quit button. Just as PlayerClient does,
     * call client.putLeaveAll() and System.exit(0).
     */
    public void button1Chosen()
    {
        pcli.putLeaveAll();
        System.exit(0);
    }

    /**
     * React to the Continue button. (Nothing to do, continue playing)
     */
    public void button2Chosen()
    {
        // Nothing to do (continue playing)
    }

    /**
     * React to the dialog window closed by user. (Nothing to do, continue playing)
     */
    public void windowCloseChosen()
    {
        // Nothing to do (continue playing)
    }

}
