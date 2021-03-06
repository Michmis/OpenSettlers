package soc.gwtClient.game.widgets;

import soc.common.actions.gameAction.GameChat;
import soc.common.game.logs.SaidEvent;
import soc.common.game.logs.SaidEventHandler;
import soc.gwtClient.game.abstractWidgets.IGamePanel;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class ChatPanel implements IsWidget, SaidEventHandler
{
    TextArea chats = new TextArea();
    TextBox saySomething = new TextBox();
    LayoutPanel rootPanel = new LayoutPanel();
    IGamePanel gamePanel;

    public ChatPanel()
    {
        rootPanel.add(chats);
        rootPanel.add(saySomething);
        // rootPanel.setWidgetTopBottom(saySomething, 0, Unit.PX, 0, Unit.PX);
        rootPanel.setWidgetVerticalPosition(saySomething, Alignment.END);
        rootPanel.setWidgetVerticalPosition(chats, Alignment.STRETCH);
        rootPanel.setWidgetRightWidth(chats, 0, Unit.PX, 100, Unit.PCT);
        rootPanel.setWidgetHorizontalPosition(chats, Alignment.STRETCH);
        rootPanel.setWidgetHorizontalPosition(saySomething, Alignment.STRETCH);

        saySomething.addKeyUpHandler(new KeyUpHandler()
        {
            @Override
            public void onKeyUp(KeyUpEvent event)
            {
                if (event.getNativeKeyCode() == 13)
                {
                    sendChat();
                }
            }
        });

    }

    public ChatPanel(IGamePanel gamePanel)
    {
        this();

        this.gamePanel = gamePanel;
        gamePanel.getGame().getChatLog().addSaidEventHandler(this);
    }

    private void sendChat()
    {
        GameChat chat = new GameChat();
        chat.setPlayer(gamePanel.getPlayingPlayer());
        chat.setChatMessage(saySomething.getText());

        gamePanel.startAction(chat);

        // clear chat textbox
        saySomething.setText("");
    }

    @Override
    public Widget asWidget()
    {
        return rootPanel;
    }

    @Override
    public void onSaid(SaidEvent event)
    {
        String text = event.getSaid().getPlayer().getUser().getName() + ": "
                + event.getSaid().getChatMessage();

        chats.setText(chats.getText() + "\r\n" + text);
    }

}
