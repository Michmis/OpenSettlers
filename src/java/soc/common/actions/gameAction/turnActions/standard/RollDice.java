package soc.common.actions.gameAction.turnActions.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soc.common.actions.gameAction.turnActions.AbstractTurnAction;
import soc.common.board.HexLocation;
import soc.common.board.hexes.Hex;
import soc.common.board.hexes.ResourceHex;
import soc.common.board.pieces.City;
import soc.common.board.pieces.PlayerPiece;
import soc.common.board.pieces.Town;
import soc.common.board.resources.ResourceList;
import soc.common.game.Game;
import soc.common.game.GamePlayer;
import soc.common.game.dices.StandardDice;
import soc.common.game.gamePhase.DetermineFirstPlayerGamePhase;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.PlayTurnsGamePhase;
import soc.common.game.gamePhase.turnPhase.RollDiceTurnPhase;
import soc.common.game.gamePhase.turnPhase.TurnPhase;
import soc.common.internationalization.I18n;

public class RollDice extends AbstractTurnAction
{
    private static final long serialVersionUID = -7274119531224635888L;
    private StandardDice dice;
    private List<Integer> looserPlayers = new ArrayList<Integer>();
    private List<HexLocation> hexesAffected = new ArrayList<HexLocation>();
    private boolean robberBlockingProduction;

    /**
     * @return the robberBlockingProduction
     */
    public boolean isRobberBlockingProduction()
    {
        return robberBlockingProduction;
    }

    /**
     * @return the looserPlayers
     */
    public List<Integer> getLooserPlayers()
    {
        return looserPlayers;
    }

    /**
     * @return the dice
     */
    public StandardDice getDice()
    {
        return dice;
    }

    /**
     * @return the hexesAffected
     */
    public List<HexLocation> getHexesAffected()
    {
        return hexesAffected;
    }

    /**
     * @param dice
     *            the dice to set
     */
    public RollDice setDice(StandardDice dice)
    {
        this.dice = dice;

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.actions.gameAction.GameAction#isValid(soc.common.game.Game)
     */
    @Override
    public boolean isValid(Game game)
    {
        if (!super.isValid(game))
            return false;

        if (!game.getCurrentTurn().getPlayer().equals(player))
        {
            invalidMessage = "player not on turn";
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.actions.gameAction.GameAction#perform(soc.common.game.Game)
     */
    @Override
    public void perform(Game game)
    {
        game.setCurrentDice(dice);

        if (game.getCurrentPhase() instanceof PlayTurnsGamePhase)
        {
            performPlayTurns(game);
        }
        if (game.getCurrentPhase() instanceof DetermineFirstPlayerGamePhase)
        {
            performDetermineGameStarter(game);
        }

        super.perform(game);
    }

    private void performDetermineGameStarter(Game game)
    {
        message = I18n.get().actions().rollDice(player.getUser().getName(),
                dice.getDiceTotal());
    }

    private void performPlayTurns(Game game)
    {
        HashMap<GamePlayer, ResourceList> playersResources = new HashMap<GamePlayer, ResourceList>();

        for (GamePlayer p : game.getPlayers())
        {
            playersResources.put(p, new ResourceList());
        }

        GamePlayer player = game.getPlayerByID(sender);
        HexLocation robber = game.getRobber().getLocation();

        if (dice.getDiceTotal() != 7)
        {
            // gather all resource hexes without the robber
            List<ResourceHex> rolledHexes = new ArrayList<ResourceHex>();
            for (Hex hex : game.getBoard().getHexes())
            {
                if (hex instanceof ResourceHex)
                {
                    ResourceHex resourceHex = (ResourceHex) hex;
                    if (resourceHex.getChit() != null)
                    {
                        if (resourceHex.getChit().getNumber() == dice
                                .getDiceTotal())
                        {
                            rolledHexes.add(resourceHex);
                        }
                    }
                }
            }
            // Iterate over all hexes with resources
            for (ResourceHex hex : rolledHexes)
            {
                boolean hexIsAffected = false;
                // For normal resources, the location of the robber is omitted.
                if (!hex.getLocation().equals(robber))
                {
                    for (GamePlayer player1 : game.getPlayers())
                    {
                        List<City> cities = new ArrayList<City>();
                        for (PlayerPiece piece : player1.getBuildPieces()
                                .ofType(City.CITY))
                        {
                            cities.add((City) piece);
                        }

                        List<Town> towns = new ArrayList<Town>();
                        for (PlayerPiece piece1 : player1.getBuildPieces()
                                .ofType(Town.TOWN))
                        {
                            towns.add((Town) piece1);
                        }

                        ResourceList gainedResources = new ResourceList();
                        for (Town town : towns)
                        {
                            gainedResources.add(hex.getResource().copy());
                        }
                        for (City zity : cities)
                        {
                            gainedResources.add(hex.getResource().copy());
                            gainedResources.add(hex.getResource().copy());
                        }

                        if (gainedResources.size() > 0)
                        {
                            hexIsAffected = true;
                        }
                        playersResources.get(player1).add(gainedResources);
                    } // For players
                } // If robber

                hexesAffected.add(hex.getLocation());
            } // For rolledHexes

            message = player.getUser().getName() + " rolled " + dice.toString();

            // Add th resources to each player and remove them from the bank
            for (GamePlayer resourcePlayer : playersResources.keySet())
            {
                ResourceList gainedResources = playersResources
                        .get(resourcePlayer);
                if (gainedResources.size() > 0)
                {
                    resourcePlayer.addResources(gainedResources);
                    game.getBank().remove(gainedResources, false);
                    message += resourcePlayer.getUser().getName() + " gained "
                            + gainedResources.toString();
                }
            }

        }
        else
        {
            // Rolled a 7, create list of players to loose cards
            String playerList = "";

            for (GamePlayer p1 : game.getPlayers())
            {
                if (p1.getResources().size() > p1
                        .getMaximumCardsInHandWhenSeven())
                {
                    looserPlayers.add(p1.getUser().getId());
                    // Add comma and playername to message
                    playerList += looserPlayers.size() > 0 ? ", "
                            + p1.getUser().getName() : p1.getUser().getName();
                }
            }

            message = player.getUser().getName() + " rolled a 7. ";

            if (looserPlayers.size() > 0)
            {
                message = message + playerList
                        + " loose half of their resources";
            }
            else
            {
                message += "No players lost any cards.";
            }
        }
    }

    @Override
    public boolean isAllowed(TurnPhase turnPhase)
    {
        return turnPhase instanceof RollDiceTurnPhase;
    }

    @Override
    public boolean isAllowed(GamePhase gamePhase)
    {
        return gamePhase instanceof PlayTurnsGamePhase
                || gamePhase instanceof DetermineFirstPlayerGamePhase;
    }

    @Override
    public String getToDoMessage()
    {
        return I18n.get().actions().rollDiceToDo(player.getUser().getName());
    }
}
