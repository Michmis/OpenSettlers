package soc.gwtClient.game.abstractWidgets;

import soc.common.actions.gameAction.turnActions.TurnAction;
import soc.common.board.Board;
import soc.common.board.pieces.PlayerPiece;
import soc.common.client.visuals.game.IGameBoardVisual;
import soc.common.game.Game;
import soc.common.game.Player;

public interface IGamePanel
{
    // The current game
    public Game getGame();
    
    // Called by ActionWidget to execute a TurnAction
    public void startAction(TurnAction turnAction);
    
    // Called by ActionWidgets to notify a BankTrade is needed 
    public void requestBankTrade(PlayerPiece piece, Player player);
    
    public IActionsWidget createActionsWidget();
    public IPlayersWidget createPlayersWidget();
    public IBankStockPanel createBankStockPanel();
    public IGameBoardVisual createGameBoard(int width, int height, Board board);
}
