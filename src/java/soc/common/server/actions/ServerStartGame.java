package soc.common.server.actions;

import soc.common.actions.gameAction.AbstractGameAction;
import soc.common.actions.gameAction.HostStartsGame;
import soc.common.board.Board;
import soc.common.board.hexes.Hex;
import soc.common.game.Game;
import soc.common.game.GamePlayer;
import soc.common.game.GamePlayerImpl;
import soc.common.game.developmentCards.DevelopmentCardList;
import soc.common.server.GameServer;
import soc.common.server.data.Player;
import soc.common.server.data.UnregisteredUser;
import soc.common.server.data.User;

public class ServerStartGame implements ServerAction
{
    HostStartsGame hostStartsGame;
    GameServer gameServer;

    public ServerStartGame(HostStartsGame hostStartsGame, GameServer gameServer)
    {
        super();
        this.hostStartsGame = hostStartsGame;
        this.gameServer = gameServer;
    }

    @Override
    public AbstractGameAction getAction()
    {
        return hostStartsGame;
    }

    @Override
    public void execute()
    {
        createNewGame();

        gameServer.getGame().performAction(hostStartsGame);
    }

    private void createNewGame()
    {
        gameServer.getGame().getPlayers().add(
                (GamePlayerImpl) new GamePlayerImpl().setUser(
                        new UnregisteredUser().setId(1).setName("Piet"))
                        .setColor("yellow"));
        gameServer.getGame().getPlayers().add(
                (GamePlayerImpl) new GamePlayerImpl().setUser(
                        new UnregisteredUser().setId(1).setName("Kees"))
                        .setColor("white"));
        gameServer.getGame().getPlayers().add(
                (GamePlayerImpl) new GamePlayerImpl().setUser(
                        new UnregisteredUser().setId(1).setName("Truus"))
                        .setColor("green"));
        gameServer.getGame().getPlayers().add(
                (GamePlayerImpl) new GamePlayerImpl().setUser(
                        new UnregisteredUser().setId(1).setName("Klaas"))
                        .setColor("red"));
        gameServer.getGame().getPlayers().add(
                (GamePlayerImpl) new GamePlayerImpl().setUser(
                        new UnregisteredUser().setId(1).setName("Henk"))
                        .setColor("blue"));

        gameServer.getGame().setBoard(new Board());

        gameServer.getGame().start();

        hostStartsGame.setGame(gameServer.getGame());
    }

    private Board copyBoard(Board board)
    {
        Board result = new Board(board.getWidth(), board.getHeight());

        for (Hex hex : board.getHexes())
            result.getHexes().set(hex.getLocation(), hex.copy());

        return result;
    }

    private Game copyGame(Game game)
    {
        Game result = new Game();

        for (GamePlayer player : game.getPlayers())
            result.getPlayers().add(copyPlayer(player));

        result.setBoard(copyBoard(game.getBoard()));

        return result;
    }

    private GamePlayer copyPlayer(GamePlayer player)
    {
        User otherUser = player.getUser();

        User user = new Player().setId(otherUser.getId()).setName(
                otherUser.getName());

        return new GamePlayerImpl().setUser(user).setColor(player.getColor());
    }

    private DevelopmentCardList shuffleDevcardsDeck(DevelopmentCardList devcards)
    {
        DevelopmentCardList result = new DevelopmentCardList();

        // TODO: reimplement without GWT unsupported Hashtable
        // Create a list to associate random value to each development card
        // Map<Integer, DevelopmentCard> list = new Hashtable<Integer,
        // DevelopmentCard>();

        // Associate the random int value to each development card, put them
        // into sortable treemap
        // /for (DevelopmentCard dev : devcards)
        // list.put(gameServer.getRandom().nextInt(2100000000), dev);

        // Populate result with randomly ordered devcards
        // for (DevelopmentCard dev : list.values())
        // result.add(dev);

        return null;
    }

    @Override
    public AbstractGameAction getOpponentAction()
    {
        return hostStartsGame;
    }
}
