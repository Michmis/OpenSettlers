package soc.gwtClient.game.widgets.bitmap;

import soc.common.game.Game;
import soc.common.game.GamePlayer;
import soc.common.game.TurnChangedEvent;
import soc.gwtClient.game.abstractWidgets.PlayerTurnStatusWidget;
import soc.gwtClient.images.Resources;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class PlayerTurnStatusBitmapWidget implements PlayerTurnStatusWidget
{
    private AbsolutePanel rootPanel = new AbsolutePanel();
    private GamePlayer player;
    private Game game;
    private Image turnImage = new Image(Resources.icons().endTurn());

    public PlayerTurnStatusBitmapWidget(GamePlayer player, Game game)
    {
        super();
        this.game = game;
        this.player = player;

        rootPanel.setStyleName("playerStatusWidget");
        rootPanel.add(turnImage, 0, 0);
        turnImage.setVisible(false);

        game.addTurnchangedeventHandler(this);
    }

    @Override
    public Widget asWidget()
    {
        return rootPanel;
    }

    @Override
    public void onTurnChanged(TurnChangedEvent event)
    {
        if (player.isOnTurn())
        {
            turnImage.setVisible(true);
        }
        else
        {
            turnImage.setVisible(false);
        }
    }

}
