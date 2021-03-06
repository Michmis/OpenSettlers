package soc.common.game;

import java.util.List;

import soc.common.actions.gameAction.turnActions.TurnAction;
import soc.common.board.hexes.Hex;
import soc.common.board.pieces.LargestArmy;
import soc.common.board.pieces.Piece;
import soc.common.board.pieces.PlayerPiece;
import soc.common.board.resources.Resource;
import soc.common.game.developmentCards.DevelopmentCardList;
import soc.common.game.dices.Dice;

/*
 * Abstracted properties for game {@see IRuleSet} to modify
 */
public interface GameRules
{
    public int getStockRoadAmount();

    public GameRules setStockRoadAmount(int stockRoads);

    public int getStockCityAmount();

    public GameRules setStockCityAmount(int stockCities);

    public int getStockTownAmount();

    public GameRules setStockTownAmount(int stockTowns);

    public int getStockWallAmount();

    public GameRules setStockWallAmount(int stockWalls);

    public int getStockBridgeAmount();

    public GameRules setStockBridgeAmount(int stockBridges);

    public int getStockShipAmount();

    public GameRules setStockShipAmount(int stockShips);

    /*
     * Creates an action for what to do when a 7 is rolled or a soldier
     * development card is played. Standard will return PlaceRobber, where
     * SeaFarers will return a PlaceRobberPirate
     */
    public TurnAction createPlaceRobberPirateAction();

    /*
     * Returns a list of piece types allowed in stock along with amount of items
     * maximum in stock for any player
     */
    public List<PlayerPiece> getStockPieces();

    /*
     * Returns a list of TurnAction types allowed to be played during a turn
     */
    public List<TurnAction> getPossibleActions();

    /*
     * Returns list of basic resource types
     */
    public List<Resource> getSupportedResources();

    /*
     * Returns a list of resources allowed to trade to players and bank
     */
    public List<Resource> getTradeableResources();

    /*
     * Returns a list of supported hex types
     */
    public List<Hex> getHexTypes();

    /*
     * Returns amount of resources in the bank at beginning of the game
     */
    public int getBankAmountPerResource();

    // Returns the type of the dice rolled by the player on turn in the
    // RollDiceTurnPhase
    public Dice getDiceType();

    // Sets the type of the dice
    public GameRules setDiceType(Dice diceType);

    public List<Piece> getPlayablePieces();

    public LargestArmy getLArgestArmy();

    public GameRules setLargestArmy(LargestArmy largestArmy);

    public void setRules(Game game);

    public GameRules setDevelopmentCardStack(DevelopmentCardList devCards);
}
