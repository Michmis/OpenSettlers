package soc.gwtClient.game.abstractWidgets;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import soc.common.actions.gameAction.turnActions.AbstractTurnAction;
import soc.common.board.pieces.PiecesChangedEventHandler;
import soc.common.board.resources.ResourcesChangedEvent;
import soc.common.board.resources.ResourcesChangedEventHandler;
import soc.common.game.Game;
import soc.common.game.GamePhaseChangedEventHandler;
import soc.common.game.GamePlayer;

public abstract class AbstractActionWidget implements 
        IActionWidget
{
    protected GamePlayer player;
    protected IGamePanel gamePanel;
    protected ComplexPanel rootPanel;
    protected boolean onTurn;
    protected boolean enabled;
    protected abstract void updateEnabled();

    public AbstractActionWidget(IGamePanel gamePanel, GamePlayer player)
    {
        this.player=player;
        this.gamePanel = gamePanel;
    }

    @Override
    public GamePlayer getPlayer()
    {
        return player;
    }
    
    @Override
    public IActionWidget setEnabled(boolean enabled)
    {
        this.enabled=enabled;
        
        updateEnabled();
        
        return null;
    }
}
