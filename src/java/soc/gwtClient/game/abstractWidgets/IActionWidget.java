package soc.gwtClient.game.abstractWidgets;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import soc.common.actions.gameAction.turnActions.AbstractTurnAction;
import soc.common.game.GamePlayer;

/*
 * Interface for a widget that shows UI elements representing actions
 * in a turn, such as building a town or ending turn
 */
public interface IActionWidget extends IsWidget
{
    public GamePlayer getPlayer();
    public IActionWidget setEnabled(boolean enabled);
}
