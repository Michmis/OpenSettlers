package soc.gwtClient.game.abstractWidgets;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractStandardDiceWidget implements IStandardDiceWidget
{
    protected IGamePanel gamePanel;
    protected AbsolutePanel rootPanel;

    protected abstract ComplexPanel createRootPanel();

    public AbstractStandardDiceWidget(IGamePanel gamePanel)
    {
        this.gamePanel = gamePanel;
        rootPanel = new AbsolutePanel();
    }

    @Override
    public Widget asWidget()
    {
        return rootPanel;
    }

}
