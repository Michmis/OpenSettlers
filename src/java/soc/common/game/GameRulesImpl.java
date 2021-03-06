package soc.common.game;

import java.util.ArrayList;
import java.util.List;

import soc.common.actions.gameAction.turnActions.AbstractTurnAction;
import soc.common.actions.gameAction.turnActions.TurnAction;
import soc.common.board.hexes.Hex;
import soc.common.board.pieces.City;
import soc.common.board.pieces.LargestArmy;
import soc.common.board.pieces.Piece;
import soc.common.board.pieces.PlayerPiece;
import soc.common.board.pieces.PlayerPieceList;
import soc.common.board.pieces.Road;
import soc.common.board.pieces.Town;
import soc.common.board.resources.Resource;
import soc.common.game.developmentCards.DevelopmentCardList;
import soc.common.game.dices.Dice;
import soc.common.game.variants.Standard;
import soc.common.game.variants.Variant;

public class GameRulesImpl implements GameRules
{
    private Game game;
    private List<Variant> variants = new ArrayList<Variant>();

    private List<TurnAction> possibleActions = new ArrayList<TurnAction>();
    private List<Resource> playableResources = new ArrayList<Resource>();
    private List<Piece> playablePieces = new ArrayList<Piece>();
    private List<Hex> playableHexTypes = new ArrayList<Hex>();
    private List<PlayerPiece> stockPieceTypes = new ArrayList<PlayerPiece>();
    private List<Resource> tradeableResources = new ArrayList<Resource>();
    private DevelopmentCardList devCards;

    private int stockTowns;
    private int stockCities;
    private int stockRoads;
    private int stockShips;
    private int stockWalls;
    private int stockBridges;

    private int bankAmountPerResource = 19;
    private boolean isSeaFarers = false;
    private boolean isSa3D = false;
    private boolean isCitiesKnights = false;
    private boolean isExtended = false;
    private boolean isPioneers = false;
    private boolean isTeamGame = false;
    private LargestArmy largestArmy;

    // State of last rolled dice
    private Dice diceType;

    public GameRulesImpl(Game game)
    {
        this.game = game;

        // Add standard rule set per default
        variants.add(new Standard(game));
    }

    /**
     * @return the isSeaFarers
     */
    public boolean isSeaFarers()
    {
        return isSeaFarers;
    }

    /**
     * @param isSeaFarers
     *            the isSeaFarers to set
     */
    public GameRulesImpl setSeaFarers(boolean isSeaFarers)
    {
        this.isSeaFarers = isSeaFarers;

        return this;
    }

    /**
     * @return the isSa3D
     */
    public boolean isSa3D()
    {
        return isSa3D;
    }

    /**
     * @param isSa3D
     *            the isSa3D to set
     */
    public GameRulesImpl setSa3D(boolean isSa3D)
    {
        this.isSa3D = isSa3D;

        return this;
    }

    /**
     * @return the isCitiesKnights
     */
    public boolean isCitiesKnights()
    {
        return isCitiesKnights;
    }

    /**
     * @param isCitiesKnights
     *            the isCitiesKnights to set
     */
    public GameRulesImpl setCitiesKnights(boolean isCitiesKnights)
    {
        this.isCitiesKnights = isCitiesKnights;

        return this;
    }

    /**
     * @return the isExtended
     */
    public boolean isExtended()
    {
        return isExtended;
    }

    /**
     * @param isExtended
     *            the isExtended to set
     */
    public GameRulesImpl setExtended(boolean isExtended)
    {
        this.isExtended = isExtended;

        return this;
    }

    /**
     * @return the isPioneers
     */
    public boolean isPioneers()
    {
        return isPioneers;
    }

    /**
     * @param isPioneers
     *            the isPioneers to set
     */
    public GameRulesImpl setPioneers(boolean isPioneers)
    {
        this.isPioneers = isPioneers;

        return this;
    }

    /**
     * @return the isTeamGame
     */
    public boolean isTeamGame()
    {
        return isTeamGame;
    }

    /**
     * @param isTeamGame
     *            the isTeamGame to set
     */
    public GameRulesImpl setTeamGame(boolean isTeamGame)
    {
        this.isTeamGame = isTeamGame;

        return this;
    }

    @Override
    public AbstractTurnAction createPlaceRobberPirateAction()
    {
        return null;
    }

    /**
     * @return the possibleActions
     */
    public List<TurnAction> getPossibleActions()
    {
        return possibleActions;
    }

    /**
     * @return the playableResources
     */
    public List<Resource> getSupportedResources()
    {
        return playableResources;
    }

    public List<Resource> getTradeableResources()
    {
        return tradeableResources;
    }

    /**
     * @return the stockPieces
     */
    public List<PlayerPiece> getStockPieces()
    {
        return stockPieceTypes;
    }

    /**
     * @return the bankAmountPerResource
     */
    public int getBankAmountPerResource()
    {
        return bankAmountPerResource;
    }

    @Override
    public Dice getDiceType()
    {
        return diceType;
    }

    @Override
    public GameRulesImpl setDiceType(Dice diceType)
    {
        this.diceType = diceType;

        return this;
    }

    @Override
    public List<Hex> getHexTypes()
    {
        return playableHexTypes;
    }

    @Override
    public LargestArmy getLArgestArmy()
    {
        return largestArmy;
    }

    @Override
    public List<Piece> getPlayablePieces()
    {
        return playablePieces;
    }

    @Override
    public GameRules setLargestArmy(LargestArmy largestArmy)
    {
        this.largestArmy = largestArmy;

        return this;
    }

    @Override
    public void setRules(Game game)
    {
        // Assuming standard is always present and always first
        for (Variant variant : variants)
        {
            variant.setRules(this);
        }

        createBank();
        createTradeableResources();
        createPlayerStocks();
        createStockPieces();
        game.setCurrentDice(getDiceType());
    }

    private void createStockPieces()
    {
        for (Piece piece : playablePieces)
        {
            if (piece instanceof PlayerPiece)
            {
                PlayerPiece playerPiece = (PlayerPiece) piece;
                if (playerPiece.isStockPiece())
                {
                    stockPieceTypes.add(playerPiece);
                }
            }
        }
    }

    private void createPlayerStocks()
    {
        for (GamePlayer player : game.getPlayers())
        {
            PlayerPieceList stock = new PlayerPieceList();

            for (int i = 0; i < stockCities; i++)
                stock.add((City) new City().setPlayer(player));
            for (int i = 0; i < stockTowns; i++)
                stock.add((Town) new Town().setPlayer(player));
            for (int i = 0; i < stockRoads; i++)
                stock.add((Road) new Road().setPlayer(player));

            player.setStock(stock);
        }
    }

    private void createTradeableResources()
    {
        for (Resource resource : getSupportedResources())
        {
            if (resource.isTradeable())
                tradeableResources.add(resource);
        }
    }

    /*
     * Creates a bank. Adds X amount of resources per resource type found in the
     * list of playable resources, where X is amount per resource
     */
    private void createBank()
    {
        for (Resource resource : getSupportedResources())
        {
            for (int i = 0; i < getBankAmountPerResource(); i++)
            {
                game.getBank().add(resource.copy());
            }
        }
    }

    @Override
    public int getStockBridgeAmount()
    {
        return stockBridges;
    }

    @Override
    public int getStockCityAmount()
    {
        return stockCities;
    }

    @Override
    public int getStockRoadAmount()
    {
        return stockRoads;
    }

    @Override
    public int getStockShipAmount()
    {
        return stockShips;
    }

    @Override
    public int getStockTownAmount()
    {
        return stockTowns;
    }

    @Override
    public int getStockWallAmount()
    {
        return stockWalls;
    }

    @Override
    public GameRules setStockBridgeAmount(int stockBridges)
    {
        this.stockBridges = stockBridges;
        return this;
    }

    @Override
    public GameRules setStockCityAmount(int stockCities)
    {
        this.stockCities = stockCities;
        return this;
    }

    @Override
    public GameRules setStockRoadAmount(int stockRoads)
    {
        this.stockRoads = stockRoads;
        return this;
    }

    @Override
    public GameRules setStockShipAmount(int stockShips)
    {
        this.stockShips = stockShips;
        return this;
    }

    @Override
    public GameRules setStockTownAmount(int stockTowns)
    {
        this.stockTowns = stockTowns;
        return this;
    }

    @Override
    public GameRules setStockWallAmount(int stockWalls)
    {
        this.stockWalls = stockWalls;
        return this;
    }

    @Override
    public GameRules setDevelopmentCardStack(DevelopmentCardList devCards)
    {
        this.devCards = devCards;
        return this;
    }

}
