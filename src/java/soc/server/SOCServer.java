/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2005 Chadwick A McHenry <mchenryc@acm.org>
 * Portions of this file Copyright (C) 2007-2010 Jeremy D. Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.server;

import java.io.FileWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import soc.debug.D;
import soc.game.Board;
import soc.game.City;
import soc.game.DevCardConstants;
import soc.game.DevCardSet;
import soc.game.ForceEndTurnResult;
import soc.game.Game;
import soc.game.GameOption;
import soc.game.GameOptionVersionException;
import soc.game.MoveRobberResult;
import soc.game.Player;
import soc.game.PlayingPiece;
import soc.game.ResourceConstants;
import soc.game.ResourceSet;
import soc.game.Road;
import soc.game.Settlement;
import soc.game.TradeOffer;
import soc.message.AcceptOffer;
import soc.message.AdminPing;
import soc.message.AdminReset;
import soc.message.BCastTextMsg;
import soc.message.BankTrade;
import soc.message.BoardLayout;
import soc.message.BoardLayout2;
import soc.message.BuildRequest;
import soc.message.BuyCardRequest;
import soc.message.CancelBuildRequest;
import soc.message.ChangeFace;
import soc.message.Channels;
import soc.message.ChoosePlayer;
import soc.message.ChoosePlayerRequest;
import soc.message.ClearOffer;
import soc.message.ClearTradeMsg;
import soc.message.CreateAccount;
import soc.message.DeleteChannel;
import soc.message.DeleteGame;
import soc.message.DevCard;
import soc.message.DevCardCount;
import soc.message.DiceResult;
import soc.message.Discard;
import soc.message.DiscardRequest;
import soc.message.DiscoveryPick;
import soc.message.EndTurn;
import soc.message.FirstPlayer;
import soc.message.GameMembers;
import soc.message.GameOptionGetDefaults;
import soc.message.GameOptionGetInfos;
import soc.message.GameOptionInfo;
import soc.message.GameState;
import soc.message.GameStats;
import soc.message.GameTextMsg;
import soc.message.Games;
import soc.message.GamesWithOptions;
import soc.message.GetStatistics;
import soc.message.ImARobot;
import soc.message.Join;
import soc.message.JoinAuth;
import soc.message.JoinGame;
import soc.message.JoinGameAuth;
import soc.message.JoinGameRequest;
import soc.message.LargestArmy;
import soc.message.LastSettlement;
import soc.message.Leave;
import soc.message.LeaveGame;
import soc.message.LongestRoad;
import soc.message.MakeOffer;
import soc.message.Members;
import soc.message.Message;
import soc.message.MonopolyPick;
import soc.message.MoveRobber;
import soc.message.NewChannel;
import soc.message.NewGame;
import soc.message.NewGameWithOptions;
import soc.message.NewGameWithOptionsRequest;
import soc.message.PlayDevCardRequest;
import soc.message.PlayerElement;
import soc.message.PlayerStats;
import soc.message.PotentialSettlements;
import soc.message.PutPiece;
import soc.message.RejectConnection;
import soc.message.RejectOffer;
import soc.message.ResetBoardAuth;
import soc.message.ResetBoardReject;
import soc.message.ResetBoardRequest;
import soc.message.ResetBoardVote;
import soc.message.ResetBoardVoteRequest;
import soc.message.ResetStatistics;
import soc.message.ResourceCount;
import soc.message.RobotDismiss;
import soc.message.RollDice;
import soc.message.RollDicePrompt;
import soc.message.SOCVersion;
import soc.message.ServerPing;
import soc.message.SetPlayedDevCard;
import soc.message.SetSeatLock;
import soc.message.SetTurn;
import soc.message.ShowStatistics;
import soc.message.SitDown;
import soc.message.StartGame;
import soc.message.StatusMessage;
import soc.message.TextMsg;
import soc.message.Turn;
import soc.message.UpdateRobotParams;
import soc.robot.RobotClient;
import soc.server.database.SOCDBHelper;
import soc.server.genericServer.LocalStringConnection;
import soc.server.genericServer.Server;
import soc.server.genericServer.StringConnection;
import soc.util.GameBoardReset;
import soc.util.GameList;
import soc.util.IntPair;
import soc.util.PlayerInfo;
import soc.util.RobotParameters;
import soc.util.Version;

/**
 * A server for Settlers of Catan
 * 
 * @author Robert S. Thomas
 * 
 *         Note: This is an attempt at being more modular. 5/13/99 RST Note:
 *         Hopefully fixed all of the deadlock problems. 12/27/01 RST
 *        <P>
 *         For server command line options, use the --help option.
 *        <P>
 *         If the database is used (see {@link SOCDBHelper}), users can be set
 *         up with a username & password in that database to log in and play.
 *         Users without accounts can connect by leaving the password blank, as
 *         long as they aren't using a nickname which has a password in the
 *         database.
 *        <P>
 *         <b>Network traffic:</b> The first message over the connection is the
 *         client's version and the second is the server's response: Either
 *         {@link RejectConnection}, or the lists of channels and games (
 *         {@link Channels}, {@link Games}). See {@link Message} for details of
 *         the client/server protocol. See {@link Server} for details of the
 *         server threading and processing.
 *        <P>
 *         The server supports several <b>debug commands</b> when enabled, and
 *         when sent as chat messages by a user named "debug". (Or, by the only
 *         user in a practice game.) See
 *         {@link #processDebugCommand(StringConnection, String, String)} for
 *         details.
 *        <P>
 *         The version check timer is set in
 *         {@link ClientData#setVersionTimer(SOCServer, StringConnection)}.
 *         Before 1.1.06, the server's response was first message, and client
 *         version was then sent in reply to server's version.
 *        <P>
 *         Java properties (starting with "jsettlers.") were added in 1.1.09,
 *         with constant names starting with PROP_JSETTLERS_, and listed in
 *         {@link #PROPS_LIST}.
 */
public class SOCServer extends Server
{
    private static final long serialVersionUID = -6123388779047708029L;

    /** Default port server listens on. */
    public static final int DEFAULT_PORT = 8880;

    /** Default maximum number of connections allowed. */
    public static final int DEFAULT_CONNECTIONS = 10;

    /** Default thread priority. */
    public static final int DEFAULT_NICE = 5;

    /** Property to specify the port the server listens on. */
    public static final String OPENSETTLERS_PORT = "osettlers.port";

    /** Property to specify the maximum number of connections allowed. */
    public static final String OPENSETTLERS_CONNECTIONS = "osettlers.connections";

    /** Property to turn on debug mode. */
    public static final String OPENSETTLERS_DEBUG = "osettlers.debug";

    /** Property to specify the server thread priority. */
    public static final String OPENSETTLERS_NICE = "osettlers.nice";

    /**
     * Default tcp port number 8880 to listen, and for client to connect to
     * remote server. Should match PlayerClient.SOC_PORT_DEFAULT.
     *<P>
     * 8880 is the default PlayerClient port since jsettlers 1.0.4, per cvs
     * history.
     * 
     * @since 1.1.09
     */
    public static final int SOC_PORT_DEFAULT = 8880;

    // If a new property is added, please add a PROP_JSETTLERS_ constant
    // and also add it to PROPS_LIST.

    /**
     * Property <tt>jsettlers.port</tt> to specify the port the server listens
     * on.
     * 
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_PORT = "osettlers.port";

    /**
     * Property <tt>jsettlers.connections</tt> to specify the maximum number of
     * connections allowed. Remember that robots count against this limit.
     * 
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_CONNECTIONS = "osettlers.connections";

    /**
     * Property <tt>jsettlers.startrobots</tt> to start some robots when the
     * server starts. (The default is 0, no robots are started by default.)
     *<P>
     * 30% will be "smart" robots, the other 70% will be "fast" robots. Remember
     * that robots count against the {@link #PROP_OPENSETTLERS_CONNECTIONS max
     * connections} limit.
     * 
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_STARTROBOTS = "osettlers.startrobots";

    /**
     * List of all available JSettlers {@link Properties properties}, such as
     * {@link #PROP_OPENSETTLERS_PORT} and
     * {@link SOCDBHelper#PROP_OPENSETTLERS_DB_URL}.
     * 
     * @since 1.1.09
     */
    public static final String[] PROPS_LIST =
        { PROP_OPENSETTLERS_PORT, PROP_OPENSETTLERS_CONNECTIONS,
                SOCDBHelper.PROP_OPENSETTLERS_DB_USER,
                SOCDBHelper.PROP_OPENSETTLERS_DB_PASS,
                SOCDBHelper.PROP_OPENSETTLERS_DB_URL,
                SOCDBHelper.PROP_OPENSETTLERS_DB_DRIVER,
                SOCDBHelper.PROP_OPENSETTLERS_DB_ENABLED };

    /**
     * Name used when sending messages from the server.
     */
    public static final String SERVERNAME = "Server";

    /**
     * Minimum required client version, to connect and play a game. Same format
     * as {@link soc.util.Version#versionNumber()}. Currently there is no
     * enforced minimum (0000).
     * 
     * @see #setClientVersSendGamesOrReject(StringConnection, int, boolean)
     */
    public static final int CLI_VERSION_MIN = 0000;

    /**
     * Minimum required client version, in "display" form, like "1.0.00".
     * Currently there is no minimum.
     * 
     * @see #setClientVersSendGamesOrReject(StringConnection, int, boolean)
     */
    public static final String CLI_VERSION_MIN_DISPLAY = "0.0.00";

    /**
     * If client never tells us their version, assume they are version 1.0.0
     * (1000).
     * 
     * @see #CLI_VERSION_TIMER_FIRE_MS
     * @see #handleJOINGAME(StringConnection, JoinGame)
     * @since 1.1.06
     */
    public static final int CLI_VERSION_ASSUMED_GUESS = 1000;

    /**
     * Client version is guessed after this many milliseconds (1200) if the
     * client hasn't yet sent it to us.
     * 
     * @see #CLI_VERSION_ASSUMED_GUESS
     * @since 1.1.06
     */
    public static final int CLI_VERSION_TIMER_FIRE_MS = 1200;

    /**
     * If game will expire in this or fewer minutes, warn the players. Default
     * 10. Must be at least twice the sleep-time in
     * {@link GameTimeoutChecker#run()}. The game expiry time is set at game
     * creation in {@link GameListAtServer#createGame(String, Hashtable)}.
     * 
     * @see #checkForExpiredGames()
     * @see GameTimeoutChecker#run()
     * @see GameListAtServer#createGame(String, Hashtable)
     */
    public static int GAME_EXPIRE_WARN_MINUTES = 10;

    /**
     * Maximum permitted game name length, default 20 characters.
     * 
     * @see #createOrJoinGameIfUserOK(StringConnection, String, String, String,
     *      Hashtable)
     * @since 1.1.07
     */
    public static int GAME_NAME_MAX_LENGTH = 20;

    /**
     * Maximum permitted player name length, default 20 characters. The client
     * already truncates to 20 characters in PlayerClient.getValidNickname.
     * 
     * @see #createOrJoinGameIfUserOK(StringConnection, String, String, String,
     *      Hashtable)
     * @since 1.1.07
     */
    public static int PLAYER_NAME_MAX_LENGTH = 20;

    /**
     * For local practice games (pipes, not TCP), the name of the pipe. Used to
     * distinguish practice vs "real" games.
     * 
     * @see soc.server.genericServer.LocalStringConnection
     */
    public static String PRACTICE_STRINGPORT = "SOCPRACTICE";

    /**
     * So we can get random numbers.
     */
    private Random rand = new Random();

    /**
     * Maximum number of connections allowed. Remember that robots count against
     * this limit.
     */
    protected int maxConnections;

    /**
     * Properties for the server, or empty if that constructor wasn't used.
     * Property names are held in PROP_* and SOCDBHelper.PROP_* constants.
     * 
     * @see #SOCServer(int, Properties)
     * @see #PROPS_LIST
     * @since 1.1.09
     */
    private Properties props;

    /**
     * A list of robots connected to this server
     */
    private Vector robots = new Vector();

    /**
     * Robot default parameters; copied for each newly connecting robot.
     * Changing this will not change parameters of any robots already connected.
     * 
     * @see #handleIMAROBOT(StringConnection, soc.message.ImARobot)
     * @see soc.robot.RobotDM
     */
    public static RobotParameters ROBOT_PARAMS_DEFAULT = new RobotParameters(
            120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 1, 1);
    // Formerly a literal in handleIMAROBOT.
    // Strategy type 1 == RobotDM.FAST_STRATEGY.
    // If you change values here, see PlayerClient.startPracticeGame
    // for assumptions which may also need to be changed.

    /**
     * Smarter robot default parameters. (For practice games; not referenced by
     * server) Same as ROBOT_PARAMS_DEFAULT but with SMART_STRATEGY, not
     * FAST_STRATEGY.
     * 
     * @see #ROBOT_PARAMS_DEFAULT
     * @see soc.robot.RobotDM
     */
    public static RobotParameters ROBOT_PARAMS_SMARTER = new RobotParameters(
            120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 0, 1);

    /**
     * Did the command line include --option / -o to set {@link GameOption game
     * option} values? Checked in constructors for possible stderr option-values
     * printout.
     * 
     * @since 1.1.07
     */
    public static boolean hasSetGameOptions = false;

    /** Status Message to send, nickname already logged into the system */
    public static final String MSG_NICKNAME_ALREADY_IN_USE = "Someone with that nickname is already logged into the system.";

    /**
     * Status Message to send, nickname already logged into the system. Prepend
     * to {@link #MSG_NICKNAME_ALREADY_IN_USE}. The "take over" option is used
     * for reconnect when a client loses connection, and server doesn't realize
     * it. A new connection can "take over" the name after a minute's timeout.
     * 
     * @since 1.1.08
     */
    public static final String MSG_NICKNAME_ALREADY_IN_USE_WAIT_TRY_AGAIN = " and try again. ";

    /**
     * Part 1 of Status Message to send, nickname already logged into the system
     * with a newer client version. Prepend to version number required. The
     * "take over" option is used for reconnect when a client loses connection,
     * and server doesn't realize it.
     * 
     * @see #MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P2
     * @since 1.1.08
     */
    public static final String MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P1 = "You need client version ";

    /**
     * Part 2 of Status Message to send, nickname already logged into the system
     * with a newer client version. Append to version number required.
     * 
     * @see #MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P1
     * @since 1.1.08
     */
    public static final String MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P2 = " or newer to take over this connection.";

    /**
     * Number of seconds before a connection is considered disconnected, and its
     * nickname can be "taken over" by a new connection with the right password.
     * Used only when a password is given by the new connection.
     * 
     * @see #checkNickname(String, StringConnection)
     * @since 1.1.08
     */
    public static final int NICKNAME_TAKEOVER_SECONDS_SAME_PASSWORD = 15;

    /**
     * Number of seconds before a connection is considered disconnected, and its
     * nickname can be "taken over" by a new connection from the same IP. Used
     * when no password is given by the new connection.
     * 
     * @see #checkNickname(String, StringConnection)
     * @since 1.1.08
     */
    public static final int NICKNAME_TAKEOVER_SECONDS_SAME_IP = 30;

    /**
     * Number of seconds before a connection is considered disconnected, and its
     * nickname can be "taken over" by a new connection from a different IP.
     * Used when no password is given by the new connection.
     * 
     * @see #checkNickname(String, StringConnection)
     * @since 1.1.08
     */
    public static final int NICKNAME_TAKEOVER_SECONDS_DIFFERENT_IP = 150;

    /**
     * list of chat channels
     */
    private ChannelList channelList = new ChannelList();

    /**
     * list of soc games
     */
    protected GameListAtServer gameList = new GameListAtServer();

    /**
     * table of requests for robots to join games. No need to use synchronized
     * Hashtable since access is only made when we have game lock anyway.
     */
    private Map robotJoinRequests = new HashMap();

    /**
     * table of requests for robots to leave games. No need to use synchronized
     * Hashtable since access is only made when we have game lock anyway.
     */
    private Map robotDismissals = new HashMap();

    /**
     * table of game data files
     */
    private Map gameDataFiles = new HashMap();

    /**
     * the current game event record
     */
    // protected SOCGameEventRecord currentGameEventRecord;

    /**
     * the time that this server was started
     */
    private long startTime;

    /**
     * the total number of games that have been started
     */
    private int numberOfGamesStarted;

    /**
     * the total number of games finished
     */
    private int numberOfGamesFinished;

    /**
     * total number of users
     */
    private int numberOfUsers;

    /**
     * server robot pinger
     */
    private ServerRobotPinger serverRobotPinger;

    /**
     * game timeout checker
     */
    private GameTimeoutChecker gameTimeoutChecker;
    String databaseUserName;
    String databasePassword;

    /**
     * Create a Settlers of Catan server listening on TCP port p. You must start
     * its thread yourself.
     *<P>
     * In 1.1.07 and later, will also print game options to stderr if any option
     * defaults require a minimum client version, or if
     * {@link #hasSetGameOptions} is set.
     * 
     * @param p
     *            the TCP port that the server listens on
     * @param mc
     *            the maximum number of connections allowed; remember that
     *            robots count against this limit.
     * @param databaseUserName
     *            the user name for accessing the database
     * @param databasePassword
     *            the password for the user
     */
    public SOCServer(int p, int mc, String databaseUserName,
            String databasePassword)
    {
        super(p);
        maxConnections = mc;
        initSocServer(databaseUserName, databasePassword, null);
    }

    /**
     * Create a Settlers of Catan server listening on TCP port p. You must start
     * its thread yourself.
     *<P>
     * The database properties are {@link SOCDBHelper#PROP_OPENSETTLERS_DB_USER}
     * and {@link SOCDBHelper#PROP_OPENSETTLERS_DB_PASS}.
     *<P>
     * Will also print game options to stderr if any option defaults require a
     * minimum client version, or if {@link #hasSetGameOptions} is set.
     * 
     * @param p
     *            the TCP port that the server listens on
     * @param props
     *            null, or properties containing
     *            {@link #PROP_OPENSETTLERS_CONNECTIONS} and any other desired
     *            properties.
     * @since 1.1.09
     * @see #PROPS_LIST
     */
    public SOCServer(final int p, Properties props)
    {
        super(p);
        try
        {
            String mcs = props.getProperty(PROP_OPENSETTLERS_CONNECTIONS, "15");
            if (mcs != null)
                maxConnections = Integer.parseInt(mcs);
            else
                maxConnections = 15;
        }
        catch (NumberFormatException e)
        {
            maxConnections = 15;
        }
        String dbuser = props.getProperty(
                SOCDBHelper.PROP_OPENSETTLERS_DB_USER, "dbuser");
        String dbpass = props.getProperty(
                SOCDBHelper.PROP_OPENSETTLERS_DB_PASS, "dbpass");
        initSocServer(dbuser, dbpass, props);
    }

    /**
     * Create a Settlers of Catan server listening on local stringport s. You
     * must start its thread yourself.
     *<P>
     * In 1.1.07 and later, will also print game options to stderr if any option
     * defaults require a minimum client version, or if
     * {@link #hasSetGameOptions} is set.
     * 
     * @param s
     *            the stringport that the server listens on. If this is a
     *            "practice game" server on the user's local computer, please
     *            use {@link #PRACTICE_STRINGPORT}.
     * @param mc
     *            the maximum number of connections allowed; remember that
     *            robots count against this limit.
     * @param databaseUserName
     *            the user name for accessing the database
     * @param databasePassword
     *            the password for the user
     */
    public SOCServer(String s, int mc, String databaseUserName,
            String databasePassword)
    {
        super(s);
        maxConnections = mc;
        initSocServer(databaseUserName, databasePassword, null);
    }

    /**
     * Common init for all constructors. Starts all server threads except the
     * main thread. If {@link #PROP_OPENSETTLERS_STARTROBOTS} is specified,
     * those aren't started until {@link #serverUp()}.
     * 
     * @param databaseUserName
     *            Used for DB connect - not retained
     * @param databasePassword
     *            Used for DB connect - not retained
     * @param props
     *            null, or properties containing
     *            {@link #PROP_OPENSETTLERS_CONNECTIONS} and any other desired
     *            properties. If <code>props</code> is null, the properties will
     *            be created empty.
     */
    private void initSocServer(String databaseUserName,
            String databasePassword, Properties props)
    {
        printVersionText();

        /* Check for problems during super setup (such as port already in use) */
        if (error != null)
        {
            System.err.println("* Exiting due to network setup problem: "
                    + error.toString());
            System.exit(1);
        }

        if (props == null)
        {
            this.props = new Properties();
        }
        else
        {
            this.props = props;
        }

        try
        {
            // false indicates no connection for valid reason (e.g. disabled)
            if (SOCDBHelper.initialize(props))
                System.err.println("User database initialized.");
            else
                System.err.println("User database disabled.");
        }
        catch (SQLException x) // just a warning
        {
            System.err.println("No user database available: " + x.getMessage());
            Throwable cause = x.getCause();
            while ((cause != null)
                    && !(cause instanceof ClassNotFoundException))
            {
                System.err.println("\t" + cause);
                cause = cause.getCause();
            }
            System.err.println("Users will not be authenticated.");
        }

        startTime = System.currentTimeMillis();
        numberOfGamesStarted = 0;
        numberOfGamesFinished = 0;
        numberOfUsers = 0;
        serverRobotPinger = new ServerRobotPinger(robots);
        serverRobotPinger.setDaemon(true);
        serverRobotPinger.start();
        gameTimeoutChecker = new GameTimeoutChecker(this);
        gameTimeoutChecker.setDaemon(true);
        gameTimeoutChecker.start();
        this.databaseUserName = databaseUserName;
        this.databasePassword = databasePassword;

        /**
         * Print game options if we've set them on commandline, or if any option
         * defaults require a minimum client version.
         */
        if (hasSetGameOptions
                || (GameOption.optionsMinimumVersion(GameOption
                        .getAllKnownOptions()) > -1))
        {
            Thread.yield(); // wait for other output to appear first
            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException ie)
            {
            }

            printGameOptions();
        }
    }

    /** Get the version number string. */
    public static String getVersion()
    {
        return "Java Settlers Server " + Version.version();
    }

    /**
     * Print the version and attribution text. Formerly inside constructors.
     * 
     * @since 1.1.07
     */
    public static void printVersionText()
    {
        System.err.println("Java Settlers Server " + Version.version()
                + ", build " + Version.buildnum() + ", " + Version.copyright());
        System.err
                .println("Network layer based on code by Cristian Bogdan; local network by Jeremy Monin.");
    }

    /**
     * Callback to take care of things when server comes up, after the server
     * socket is bound and listening, in the main thread. If
     * {@link #PROP_OPENSETTLERS_STARTROBOTS} is specified, start those
     * {@link RobotClient}s now.
     * 
     * @since 1.1.09
     */
    public void serverUp()
    {
        /**
         * If we have any STARTROBOTS, start them up now. Each bot will have its
         * own thread and {@link RobotClient}.
         */
        if ((props != null)
                && (props.containsKey(PROP_OPENSETTLERS_STARTROBOTS)))
        {
            try
            {
                int rcount = Integer.parseInt(props
                        .getProperty(PROP_OPENSETTLERS_STARTROBOTS));
                int fast30 = (int) (0.30f * rcount);
                setupLocalRobots(fast30, rcount - fast30); // each bot gets a
                                                           // thread
            }
            catch (NumberFormatException e)
            {
                System.err
                        .println("Not starting robots: Bad number format, ignoring property "
                                + PROP_OPENSETTLERS_STARTROBOTS);
            }
        }
    }

    /**
     * Adds a connection to a chat channel.
     * 
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch) before
     * calling this method
     * 
     * @param c
     *            the Connection to be added
     * @param ch
     *            the name of the channel
     * 
     */
    public void connectToChannel(StringConnection c, String ch)
    {
        if (c != null)
        {
            if (channelList.isChannel(ch))
            {
                if (!channelList.isMember(c, ch))
                {
                    c.put(Members.toCmd(ch, channelList.getMembers(ch)));
                    D.ebugPrintln("*** " + c.getData() + " joined the channel "
                            + ch);
                    channelList.addMember(c, ch);
                }
            }
        }
    }

    /**
     * the connection c leaves the channel ch
     * 
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch) before
     * calling this method
     * 
     * @param c
     *            the connection
     * @param ch
     *            the channel
     * @param channelListLock
     *            true if we have the channelList monitor
     * @return true if we destroyed the channel
     */
    public boolean leaveChannel(StringConnection c, String ch,
            boolean channelListLock)
    {
        D.ebugPrintln("leaveChannel: " + c.getData() + " " + ch + " "
                + channelListLock);

        boolean result = false;

        if (c != null)
        {
            if (channelList.isMember(c, ch))
            {
                channelList.removeMember(c, ch);

                Leave leaveMessage = new Leave((String) c.getData(), c.host(),
                        ch);
                messageToChannelWithMon(ch, leaveMessage);
                D.ebugPrintln("*** " + (String) c.getData()
                        + " left the channel " + ch);
            }

            if (channelList.isChannelEmpty(ch))
            {
                if (channelListLock)
                {
                    channelList.deleteChannel(ch);
                }
                else
                {
                    channelList.takeMonitor();

                    try
                    {
                        channelList.deleteChannel(ch);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintStackTrace(e, "Exception in leaveChannel");
                    }

                    channelList.releaseMonitor();
                }

                result = true;
            }
        }

        return result;
    }

    /**
     * Adds a connection to a game, unless they're already a member. If the game
     * doesn't yet exist, create it, and announce the new game to all clients.
     *<P>
     * After this, human players are free to join, until someone clicks
     * "Start Game". At that point, server will look for robots to fill empty
     * seats.
     * 
     * @param c
     *            the Connection to be added; its name and version should
     *            already be set.
     * @param gaName
     *            the name of the game
     * @param gaOpts
     *            if creating a game with options, hashtable of
     *            {@link GameOption}; otherwise null. Should already be
     *            validated, by calling
     *            {@link GameOption#adjustOptionsToKnown(Hashtable, Hashtable)}.
     * 
     * @return true if c was not a member of ch before, false if c was already
     *         in this game
     * 
     * @throws GameOptionVersionException
     *             if asking to create a game (gaOpts != null), but client's
     *             version is too low to join because of a requested game
     *             option's minimum version in gaOpts. Calculated via
     *             {@link GameOption#optionsNewerThanVersion(int, boolean, boolean, Hashtable)}
     *             . (this exception was added in 1.1.07)
     * @throws IllegalArgumentException
     *             if client's version is too low to join for any other reason.
     *             (this exception was added in 1.1.06)
     * @see #handleSTARTGAME(StringConnection, StartGame)
     */
    public boolean connectToGame(StringConnection c, final String gaName,
            Hashtable gaOpts) throws GameOptionVersionException,
            IllegalArgumentException
    {
        if (c == null)
        {
            return false; // shouldn't happen
        }

        boolean result = false;

        final int cliVers = c.getVersion();
        boolean gameExists = false;
        gameList.takeMonitor();

        try
        {
            gameExists = gameList.isGame(gaName);
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception in connectToGame");
        }

        gameList.releaseMonitor();

        if (gameExists)
        {
            boolean cliVersOld = false;
            gameList.takeMonitorForGame(gaName);
            Game ga = gameList.getGameData(gaName);

            try
            {
                if (gameList.isMember(c, gaName))
                {
                    result = false;
                }
                else
                {
                    if (ga.getClientVersionMinRequired() <= cliVers)
                    {
                        gameList.addMember(c, gaName);
                        result = true;
                    }
                    else
                    {
                        cliVersOld = true;
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e,
                        "Exception in connectToGame (isMember)");
            }

            gameList.releaseMonitorForGame(gaName);
            if (cliVersOld)
                throw new IllegalArgumentException("Client version");

            // <---- Exception: Early return ----
        }
        else
        {
            /**
             * the game did not exist, create it after checking options
             */
            final int gVers;
            if (gaOpts == null)
            {
                gVers = -1;
            }
            else
            {
                gVers = GameOption.optionsMinimumVersion(gaOpts);
                if (gVers > cliVers)
                {
                    // Which option(s) are too new for client?
                    Vector optsValuesTooNew = GameOption
                            .optionsNewerThanVersion(cliVers, true, false,
                                    gaOpts);
                    throw new GameOptionVersionException(gVers, cliVers,
                            optsValuesTooNew);

                    // <---- Exception: Early return ----
                }
            }

            gameList.takeMonitor();
            boolean monitorReleased = false;

            try
            {
                // Create new game, expiring in
                // GameListAtServer.GAME_EXPIRE_MINUTES .
                gameList.createGame(gaName, gaOpts);
                if ((strSocketName != null)
                        && (strSocketName.equals(PRACTICE_STRINGPORT)))
                {
                    gameList.getGameData(gaName).isLocal = true; // flag if
                                                                 // practice
                                                                 // game (set
                                                                 // since
                                                                 // 1.1.09)
                }

                // Add this (creating) player to the game
                gameList.addMember(c, gaName);

                // must release monitor before we broadcast
                gameList.releaseMonitor();
                monitorReleased = true;
                result = true;

                // check version before we broadcast
                final int cversMin = getMinConnectedCliVersion();

                if ((gVers <= cversMin) && (gaOpts == null))
                {
                    // All clients can join it, and no game options: use
                    // simplest message
                    broadcast(NewGame.toCmd(gaName));

                }
                else
                {
                    // Send messages, based on clients' version
                    // and whether there are game options.

                    if (cversMin >= NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS)
                    {
                        // All cli can understand msg with version/options
                        // included
                        broadcast(NewGameWithOptions.toCmd(gaName, gaOpts,
                                gVers));
                    }
                    else
                    {
                        // Only some can understand msg with version/options
                        // included;
                        // send at most 1 message to each connected client,
                        // split by client version.

                        final int cversMax = getMaxConnectedCliVersion();
                        int newgameMaxCliVers;
                        if ((gaOpts != null)
                                && (cversMax >= NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS))
                        {
                            broadcastToVers(
                                    NewGameWithOptions.toCmd(gaName, gaOpts,
                                            gVers),
                                    NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS,
                                    Integer.MAX_VALUE);
                            newgameMaxCliVers = NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS - 1;
                        }
                        else
                        {
                            newgameMaxCliVers = Integer.MAX_VALUE;
                        }

                        // To older clients who can join, announce game without
                        // its options/version
                        broadcastToVers(NewGame.toCmd(gaName), gVers,
                                newgameMaxCliVers);

                        // To older clients who can't join, announce game with
                        // cant-join prefix
                        StringBuffer sb = new StringBuffer();
                        sb.append(Games.MARKER_THIS_GAME_UNJOINABLE);
                        sb.append(gaName);
                        broadcastToVers(NewGame.toCmd(sb.toString()),
                                Games.VERSION_FOR_UNJOINABLE, gVers - 1);
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                if (!monitorReleased)
                    gameList.releaseMonitor();
                throw e; // caller handles it
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in connectToGame");
            }

            if (!monitorReleased)
            {
                gameList.releaseMonitor();
            }
        }

        return result;
    }

    /**
     * the connection c leaves the game gm. Clean up; if needed, call
     * {@link #forceEndGameTurn(Game, String)}.
     *<P>
     * WARNING: MUST HAVE THE gameList.takeMonitorForGame(gm) before calling
     * this method
     * 
     * @param c
     *            the connection; if c is being dropped because of an error,
     *            this method assumes that {@link StringConnection#disconnect()}
     *            has already been called. This method won't exclude c from any
     *            communication about leaving the game, in case they are still
     *            connected and in other games.
     * @param gm
     *            the game
     * @param gameListLock
     *            true if we have the gameList.takeMonitor() lock
     * @return true if the game was destroyed (because c was the last non-robot
     *         player, and no one was watching)
     */
    public boolean leaveGame(StringConnection c, String gm, boolean gameListLock)
    {
        System.err.println("L712: leaveGame(" + c + ", " + gm + ")"); // JM TEMP
        if (c == null)
        {
            return false; // <---- Early return: no connection ----
        }

        boolean gameDestroyed = false;

        gameList.removeMember(c, gm);

        boolean isPlayer = false;
        int playerNumber = 0; // removing this player number
        Game ga = gameList.getGameData(gm);
        if (ga == null)
        {
            return false; // <---- Early return: no game ----
        }

        boolean gameHasHumanPlayer = false;
        boolean gameHasObserver = true; // Changed by Monte Carlo
        boolean gameVotingActiveDuringStart = false;

        final int gameState = ga.getGameState();
        final String plName = (String) c.getData(); // Retain name, since will
                                                    // become null within game
                                                    // obj.

        for (playerNumber = 0; playerNumber < ga.maxPlayers; playerNumber++)
        {
            Player player = ga.getPlayer(playerNumber);

            if ((player != null) && (player.getName() != null)
                    && (player.getName().equals(plName)))
            {
                isPlayer = true;

                /**
                 * About to remove this player from the game. Before doing so:
                 * If a board-reset vote is in progress, they cannot vote once
                 * they have left. So to keep the game moving, fabricate their
                 * response: vote No.
                 */
                if (ga.getResetVoteActive())
                {
                    if (gameState <= Game.START2B)
                        gameVotingActiveDuringStart = true;

                    if (ga.getResetPlayerVote(playerNumber) == Game.VOTE_NONE)
                    {
                        gameList.releaseMonitorForGame(gm);
                        ga.takeMonitor();
                        resetBoardVoteNotifyOne(ga, playerNumber, plName, false);
                        ga.releaseMonitor();
                        gameList.takeMonitorForGame(gm);
                    }
                }

                /**
                 * Remove the player.
                 */
                ga.removePlayer(plName); // player obj name becomes null

                // broadcastGameStats(cg);
                break;
            }
        }

        LeaveGame leaveMessage = new LeaveGame(plName, c.host(), gm);
        messageToGameWithMon(gm, leaveMessage);
        recordGameEvent(gm, leaveMessage.toCmd()); // Commented out in Monte
                                                   // Carlo

        D.ebugPrintln("*** " + plName + " left the game " + gm);
        messageToGameWithMon(gm, new GameTextMsg(gm, SERVERNAME, plName
                + " left the game"));

        /**
         * check if there is at least one person playing the game
         */
        for (int pn = 0; pn < ga.maxPlayers; pn++)
        {
            Player player = ga.getPlayer(pn);

            if ((player != null) && (player.getName() != null)
                    && (!ga.isSeatVacant(pn)) && (!player.isRobot()))
            {
                gameHasHumanPlayer = true;
                break;
            }
        }

        // D.ebugPrintln("*** gameHasHumanPlayer = "+gameHasHumanPlayer+" for "+gm);

        /**
         * if no human players, check if there is at least one person watching
         * the game
         */
        if (!gameHasHumanPlayer && !gameList.isGameEmpty(gm))
        {
            Enumeration membersEnum = gameList.getMembers(gm).elements();

            while (membersEnum.hasMoreElements())
            {
                StringConnection member = (StringConnection) membersEnum
                        .nextElement();

                // D.ebugPrintln("*** "+member.data+" is a member of "+gm);
                boolean nameMatch = false;

                for (int pn = 0; pn < ga.maxPlayers; pn++)
                {
                    Player player = ga.getPlayer(pn);

                    if ((player != null)
                            && (player.getName() != null)
                            && (player.getName().equals((String) member
                                    .getData())))
                    {
                        nameMatch = true;
                        break;
                    }
                }

                if (!nameMatch)
                {
                    gameHasObserver = true;
                    break;
                }
            }
        }
        // D.ebugPrintln("*** gameHasObserver = "+gameHasObserver+" for "+gm);

        /**
         * if the leaving member was playing the game, and the game isn't over,
         * then decide: - Do we need to force-end the current turn? - Do we need
         * to cancel their initial settlement placement? - Should we replace the
         * leaving player with a robot?
         */
        if (isPlayer
                && (gameHasHumanPlayer || gameHasObserver)
                && ((ga.getPlayer(playerNumber).getPublicVP() > 0)
                        || (gameState == Game.START1A) || (gameState == Game.START1B))
                && (gameState < Game.OVER) && !(gameState < Game.START1A))
        {
            boolean foundNoRobots;

            if (ga.getPlayer(playerNumber).isRobot())
            {
                /**
                 * don't replace bot with bot; force end-turn instead.
                 */
                foundNoRobots = true;
            }
            else
            {
                /**
                 * get a robot to replace this human player; just in case, check
                 * game-version vs robots-version, like at new-game
                 * (readyGameAskRobotsJoin).
                 */
                foundNoRobots = false;

                messageToGameWithMon(gm, new GameTextMsg(gm, SERVERNAME,
                        "Fetching a robot player..."));

                if (robots.isEmpty())
                {
                    messageToGameWithMon(gm, new GameTextMsg(gm, SERVERNAME,
                            "Sorry, no robots on this server."));
                    foundNoRobots = true;
                }
                else if (ga.getClientVersionMinRequired() > Version
                        .versionNumber())
                {
                    messageToGameWithMon(
                            gm,
                            new GameTextMsg(
                                    gm,
                                    SERVERNAME,
                                    "Sorry, the robots can't join this game; its version is somehow newer than server and robots, it's "
                                            + ga.getClientVersionMinRequired()));
                    foundNoRobots = true;
                }
                else
                {
                    /**
                     * request a robot that isn't already playing this game or
                     * is not already requested to play in this game
                     */
                    boolean nameMatch = false;
                    StringConnection robotConn = null;

                    final int[] robotIndexes = robotShuffleForJoin(); // Shuffle
                                                                      // to
                                                                      // distribute
                                                                      // load

                    Vector requests = (Vector) robotJoinRequests.get(gm);

                    for (int idx = 0; idx < robots.size(); idx++)
                    {
                        robotConn = (StringConnection) robots
                                .get(robotIndexes[idx]);
                        nameMatch = false;

                        for (int i = 0; i < ga.maxPlayers; i++)
                        {
                            Player pl = ga.getPlayer(i);

                            if (pl != null)
                            {
                                String pname = pl.getName();

                                // D.ebugPrintln("CHECKING " + (String)
                                // robotConn.getData() + " == " + pname);

                                if ((pname != null)
                                        && (pname.equals((String) robotConn
                                                .getData())))
                                {
                                    nameMatch = true;

                                    break;
                                }
                            }
                        }

                        if ((!nameMatch) && (requests != null))
                        {
                            Enumeration requestsEnum = requests.elements();

                            while (requestsEnum.hasMoreElements())
                            {
                                StringConnection tempCon = (StringConnection) requestsEnum
                                        .nextElement();

                                // D.ebugPrintln("CHECKING " + robotConn +
                                // " == " + tempCon);

                                if (tempCon == robotConn)
                                {
                                    nameMatch = true;
                                }

                                break;
                            }
                        }

                        if (!nameMatch)
                        {
                            break;
                        }
                    }

                    if (!nameMatch)
                    {
                        /**
                         * make the request
                         */
                        D.ebugPrintln("@@@ JOIN GAME REQUEST for "
                                + (String) robotConn.getData());

                        robotConn.put(JoinGameRequest.toCmd(gm, playerNumber,
                                ga.getGameOptions()));

                        /**
                         * record the request
                         */
                        if (requests == null)
                        {
                            requests = new Vector();
                            requests.addElement(robotConn);
                            robotJoinRequests.put(gm, requests);
                        }
                        else
                        {
                            requests.addElement(robotConn);
                        }
                    }
                    else
                    {
                        messageToGameWithMon(gm, new GameTextMsg(gm,
                                SERVERNAME, "*** Can't find a robot! ***"));
                        foundNoRobots = true;
                    }
                }
            } // if (should try to find a robot)

            /**
             * What to do if no robot was found to fill their spot? Must keep
             * the game going, might need to force-end current turn.
             */
            if (foundNoRobots)
            {
                final int cpn = ga.getCurrentPlayerNumber();

                if (playerNumber == cpn)
                {
                    /**
                     * Rare condition: No robot was found, but it was this
                     * player's turn. End their turn just to keep the game
                     * limping along. To prevent deadlock, we must release
                     * gamelist's monitor for this game before calling
                     * endGameTurn.
                     */

                    if ((gameState == Game.START1B)
                            || (gameState == Game.START2B))
                    {
                        /**
                         * Leaving during 1st or 2nd initial road placement.
                         * Cancel the settlement they just placed, and send that
                         * cancel to the other players. Don't change gameState
                         * yet. Note that their 2nd settlement is removed in
                         * START2B, but not their 1st settlement. (This would
                         * impact the robots much more.)
                         */
                        Player pl = ga.getPlayer(playerNumber);
                        Settlement pp = new Settlement(pl, pl
                                .getLastSettlementCoord(), null);
                        ga.undoPutInitSettlement(pp);
                        ga.setGameState(gameState); // state was changed by
                                                    // undoPutInitSettlement
                        messageToGameWithMon(gm, new CancelBuildRequest(gm,
                                Settlement.SETTLEMENT));
                    }

                    if (ga.canEndTurn(playerNumber))
                    {
                        gameList.releaseMonitorForGame(gm);
                        ga.takeMonitor();
                        endGameTurn(ga, null);
                        ga.releaseMonitor();
                        gameList.takeMonitorForGame(gm);
                    }
                    else
                    {
                        /**
                         * Cannot easily end turn. Must back out something in
                         * progress. May or may not end turn; see javadocs of
                         * forceEndGameTurn and game.forceEndTurn. All start
                         * phases are covered here (START1A..START2B) because
                         * canEndTurn returns false in those gameStates.
                         */
                        gameList.releaseMonitorForGame(gm);
                        ga.takeMonitor();
                        if (gameVotingActiveDuringStart)
                        {
                            /**
                             * If anyone has requested a board-reset vote during
                             * game-start phases, we have to tell clients to
                             * cancel the vote request, because
                             * {@link soc.message.Turn} isn't always sent during
                             * start phases. (Voting must end when the turn
                             * ends.)
                             */
                            messageToGame(gm, new ResetBoardReject(gm));
                            ga.resetVoteClear();
                        }

                        /**
                         * Force turn to end
                         */
                        forceEndGameTurn(ga, plName);
                        ga.releaseMonitor();
                        gameList.takeMonitorForGame(gm);
                    }
                }
                else
                {
                    /**
                     * Check if game is waiting for input from the player who is
                     * leaving, but who isn't current player. To keep the game
                     * moving, fabricate their response. - Board-reset voting:
                     * Handled above. - Waiting for discard: Handle here.
                     */
                    if ((gameState == Game.WAITING_FOR_DISCARDS)
                            && (ga.getPlayer(playerNumber).getNeedToDiscard()))
                    {
                        /**
                         * For discard, tell the discarding player's client that
                         * they discarded the resources, tell everyone else that
                         * the player discarded unknown resources.
                         */
                        gameList.releaseMonitorForGame(gm);
                        ga.takeMonitor();
                        forceGamePlayerDiscard(ga, cpn, c, plName, playerNumber);
                        sendGameState(ga, false); // WAITING_FOR_DISCARDS or
                                                  // MOVING_ROBBER
                        ga.releaseMonitor();
                        gameList.takeMonitorForGame(gm);
                    }
                } // current player?
            }
        }

        /**
         * if the game has no players, or if they're all robots, then end the
         * game and write the data to disk.
         */
        boolean emptyGame = false;
        emptyGame = gameList.isGameEmpty(gm);

        if (emptyGame || (!gameHasHumanPlayer && !gameHasObserver))
        {
            if (gameListLock)
            {
                destroyGame(gm);
            }
            else
            {
                gameList.takeMonitor();

                try
                {
                    destroyGame(gm);
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e,
                            "Exception in leaveGame (destroyGame)");
                }

                gameList.releaseMonitor();
            }

            gameDestroyed = true;
        }

        // D.ebugPrintln("*** gameDestroyed = "+gameDestroyed+" for "+gm);
        return gameDestroyed;
    }

    /**
     * shuffle the indexes to distribute load among {@link #robots}
     * 
     * @return a shuffled array of robot indexes, from 0 to ({#link
     *         {@link #robots} .size() - 1
     * @since 1.1.06
     */
    private int[] robotShuffleForJoin()
    {
        int[] robotIndexes = new int[robots.size()];

        for (int i = 0; i < robots.size(); i++)
        {
            robotIndexes[i] = i;
        }

        for (int j = 0; j < 3; j++)
        {
            for (int i = 0; i < robotIndexes.length; i++)
            {
                // Swap a random robot, below the ith robot, with the ith robot
                int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                int tmp = robotIndexes[idx];
                robotIndexes[idx] = robotIndexes[i];
                robotIndexes[i] = tmp;
            }
        }
        return robotIndexes;
    }

    /**
     * Set up some robot opponents, running in our JVM for operator convenience.
     * Set up more than needed; when a game is started, game setup will
     * randomize whether its humans will play against smart or fast ones. (Some
     * will be RobotDM.FAST_STRATEGY, some SMART_STRATEGY).
     *<P>
     * Before 1.1.09, this method was part of PlayerClient.
     * 
     * @param numFast
     *            number of fast robots, with
     *            {@link soc.robot.RobotDM#FAST_STRATEGY FAST_STRATEGY}
     * @param numSmart
     *            number of smart robots, with
     *            {@link soc.robot.RobotDM#SMART_STRATEGY SMART_STRATEGY}
     * @see #startPracticeGame()
     * @see #startLocalTCPServer(int)
     * @since 1.1.00
     */
    public void setupLocalRobots(final int numFast, final int numSmart)
    {
        RobotClient[] robo_fast = new RobotClient[numFast];
        RobotClient[] robo_smrt = new RobotClient[numSmart];

        // ASSUMPTION: Server ROBOT_PARAMS_DEFAULT uses RobotDM.FAST_STRATEGY.

        // Make some faster ones first.
        for (int i = 0; i < numFast; ++i)
        {
            String rname = "droid " + (i + 1);
            SOCPlayerLocalRobotRunner.createAndStartRobotClientThread(rname,
                    strSocketName, port);
            // includes yield() and sleep(75 ms) this thread.
        }

        try
        {
            Thread.sleep(150);
            // Wait for these robots' accept and UPDATEROBOTPARAMS,
            // before we change the default params.
        }
        catch (InterruptedException ie)
        {
        }

        // Make a few smarter ones now:

        // Switch params to SMARTER for future new robots.
        RobotParameters prevSetting = SOCServer.ROBOT_PARAMS_DEFAULT;
        SOCServer.ROBOT_PARAMS_DEFAULT = SOCServer.ROBOT_PARAMS_SMARTER; // RobotDM.SMART_STRATEGY

        for (int i = 0; i < numSmart; ++i)
        {
            String rname = "robot " + (i + 1 + robo_fast.length);
            SOCPlayerLocalRobotRunner.createAndStartRobotClientThread(rname,
                    strSocketName, port);
            // includes yield() and sleep(75 ms) this thread.
        }

        SOCServer.ROBOT_PARAMS_DEFAULT = prevSetting;
    }

    /**
     * Force this player (not current player) to discard, and report resources
     * to all players. Does not send gameState, which may have changed; see
     * {@link Game#discardPickRandom(ResourceSet, int, ResourceSet, Random)}.
     *<P>
     * Assumes, as {@link #endGameTurn(Game, Player)} does:
     * <UL>
     * <LI>ga.takeMonitor already called (not the same as
     * {@link GameList#takeMonitorForGame(String)})
     * <LI>gamelist.takeMonitorForGame is NOT called, we do NOT have that
     * monitor
     * </UL>
     * 
     * @param cg
     *            Game object
     * @param cpn
     *            Game's current player number
     * @param c
     *            Connection of discarding player
     * @param plName
     *            Discarding player's name, for GameTextMsg
     * @param pn
     *            Player number who must discard
     */
    private void forceGamePlayerDiscard(Game cg, final int cpn,
            StringConnection c, String plName, final int pn)
    {
        ResourceSet discard = cg.playerDiscardRandom(pn);
        final String gaName = cg.getName();
        if ((c != null) && c.isConnected())
            reportRsrcGainLoss(gaName, discard, true, cpn, -1, null, c);
        int totalRes = discard.getTotal();
        messageToGameExcept(gaName, c, new PlayerElement(gaName, cpn,
                PlayerElement.LOSE, PlayerElement.UNKNOWN, totalRes), true);
        messageToGame(gaName, plName + " discarded " + totalRes + " resources.");
    }

    /**
     * destroy the game
     * 
     * WARNING: MUST HAVE THE gameList.takeMonitor() before calling this method
     * 
     * @param gm
     *            the name of the game
     */
    private void destroyGame(String gm)
    {
        // D.ebugPrintln("***** destroyGame("+gm+")");
        Game game = gameList.getGameData(gm);

        if (game != null)
        {
            if (game.getGameState() == Game.OVER)
            {
                numberOfGamesFinished++;
            }

            // /
            // / write out game data
            // /

            /*
             * saveCurrentGameEventRecord(game); SOCGameRecord gr =
             * (SOCGameRecord)gameRecords.get(game); writeGameRecord(gm, gr);
             */

            // storeGameScores(game);
            // /
            // / tell all robots to leave
            // /
            Vector members = null;
            members = gameList.getMembers(gm);

            if (members != null)
            {
                Enumeration conEnum = members.elements();

                while (conEnum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) conEnum
                            .nextElement();
                    con.put(RobotDismiss.toCmd(gm));
                }
            }

            gameList.deleteGame(gm);
        }
    }

    /**
     * Used when PlayerClient is also hosting games.
     * 
     * @return The names (Strings) of games on this server
     */
    public Enumeration getGameNames()
    {
        return gameList.getGames();
    }

    /**
     * Given a game name on this server, return its state.
     * 
     * @param gm
     *            Game name
     * @return Game's state, or -1 if no game with that name on this server
     * @since 1.1.00
     */
    public int getGameState(String gm)
    {
        Game g = gameList.getGameData(gm);
        if (g != null)
            return g.getGameState();
        else
            return -1;
    }

    /**
     * Given a game name on this server, return its game options.
     * 
     * @param gm
     *            Game name
     * @return the game options (hashtable of {@link GameOption}), or null if
     *         the game doesn't exist or has no options
     * @since 1.1.07
     */
    public Hashtable getGameOptions(String gm)
    {
        return gameList.getGameOptions(gm);
    }

    /**
     * the connection c leaves all channels it was in
     * 
     * @param c
     *            the connection
     * @return the channels it was in
     */
    public Vector leaveAllChannels(StringConnection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            channelList.takeMonitor();

            try
            {
                for (Enumeration k = channelList.getChannels(); k
                        .hasMoreElements();)
                {
                    String ch = (String) k.nextElement();

                    if (channelList.isMember(c, ch))
                    {
                        boolean thisChannelDestroyed = false;
                        channelList.takeMonitorForChannel(ch);

                        try
                        {
                            thisChannelDestroyed = leaveChannel(c, ch, true);
                        }
                        catch (Exception e)
                        {
                            D
                                    .ebugPrintStackTrace(e,
                                            "Exception in leaveAllChannels (leaveChannel)");
                        }

                        channelList.releaseMonitorForChannel(ch);

                        if (thisChannelDestroyed)
                        {
                            destroyed.addElement(ch);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in leaveAllChannels");
            }

            channelList.releaseMonitor();

            /**
             * let everyone know about the destroyed channels
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                broadcast(DeleteChannel.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * the connection c leaves all games it was in
     * 
     * @param c
     *            the connection
     * @return the games it was in
     */
    public Vector leaveAllGames(StringConnection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            gameList.takeMonitor();

            try
            {
                for (Enumeration k = gameList.getGames(); k.hasMoreElements();)
                {
                    String ga = (String) k.nextElement();
                    Vector v = (Vector) gameList.getMembers(ga);

                    if (v.contains(c))
                    {
                        boolean thisGameDestroyed = false;
                        gameList.takeMonitorForGame(ga);

                        try
                        {
                            thisGameDestroyed = leaveGame(c, ga, true);
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintStackTrace(e,
                                    "Exception in leaveAllGames (leaveGame)");
                        }

                        gameList.releaseMonitorForGame(ga);

                        if (thisGameDestroyed)
                        {
                            destroyed.addElement(ga);
                        }

                        ret.addElement(ga);
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in leaveAllGames");
            }

            gameList.releaseMonitor();

            /**
             * let everyone know about the destroyed games
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                D.ebugPrintln("** Broadcasting DeleteGame " + ga);
                broadcast(DeleteGame.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * Send a message to the given channel
     * 
     * @param ch
     *            the name of the channel
     * @param mes
     *            the message to send
     */
    private void messageToChannel(String ch, Message mes)
    {
        channelList.takeMonitorForChannel(ch);

        try
        {
            Vector v = channelList.getMembers(ch);

            if (v != null)
            {
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection c = (StringConnection) menum.nextElement();

                    if (c != null)
                    {
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception in messageToChannel");
        }

        channelList.releaseMonitorForChannel(ch);
    }

    /**
     * Send a message to the given channel
     * 
     * WARNING: MUST HAVE THE gameList.takeMonitorForChannel(ch) before calling
     * this method
     * 
     * @param ch
     *            the name of the channel
     * @param mes
     *            the message to send
     */
    private void messageToChannelWithMon(String ch, Message mes)
    {
        Vector v = channelList.getMembers(ch);

        if (v != null)
        {
            Enumeration menum = v.elements();

            while (menum.hasMoreElements())
            {
                StringConnection c = (StringConnection) menum.nextElement();

                if (c != null)
                {
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to a player and record it
     * 
     * @param c
     *            the player connection
     * @param mes
     *            the message to send
     */
    public void messageToPlayer(StringConnection c, Message mes)
    {
        if ((c != null) && (mes != null))
        {
            // currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes,
            // "SERVER", c.getData()));
            c.put(mes.toCmd());
        }
    }

    /**
     * Send a {@link GameTextMsg} game text message to a player. Equivalent to:
     * messageToPlayer(conn, new {@link GameTextMsg}(ga, {@link #SERVERNAME},
     * txt));
     * 
     * @param c
     *            the player connection
     * @param ga
     *            game name
     * @param txt
     *            the message text to send
     * @since 1.1.08
     */
    public void messageToPlayer(StringConnection c, final String ga,
            final String txt)
    {
        if (c == null)
            return;
        c.put(GameTextMsg.toCmd(ga, SERVERNAME, txt));
    }

    /**
     * Send a message to the given game. <b>Locks:</b> Takes, releases
     * {@link GameList#takeMonitorForGame(String)}.
     * 
     * @param ga
     *            the name of the game
     * @param mes
     *            the message to send. If mes is a GameTextMsg whose text begins
     *            with ">>>", the client should consider this an urgent message,
     *            and draw the user's attention in some way. (See
     *            {@link #messageToGameUrgent(String, String)})
     * @see #messageToGame(String, String)
     * @see #messageToGameWithMon(String, Message)
     */
    private void messageToGame(String ga, Message mes)
    {
        gameList.takeMonitorForGame(ga);

        try
        {
            Vector v = gameList.getMembers(ga);

            if (v != null)
            {
                // D.ebugPrintln("M2G - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection c = (StringConnection) menum.nextElement();

                    if (c != null)
                    {
                        // currentGameEventRecord.addMessageOut(new
                        // SOCMessageRecord(mes, "SERVER", c.getData()));
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception in messageToGame");
        }

        gameList.releaseMonitorForGame(ga);
    }

    /**
     * Send a server text message to the given game. Equivalent to:
     * messageToGame(ga, new GameTextMsg(ga, {@link #SERVERNAME}, txt));
     *<P>
     * Do not pass SOCSomeMessage.toCmd() into this method; the message type
     * number will be GAMETEXTMSG, not the desired SOMEMESSAGE.
     *<P>
     * <b>Locks:</b> Takes, releases {@link GameList#takeMonitorForGame(String)}.
     * 
     * @param ga
     *            the name of the game
     * @param txt
     *            the message text to send. If text begins with ">>>", the
     *            client should consider this an urgent message, and draw the
     *            user's attention in some way. (See
     *            {@link #messageToGameUrgent(String, String)})
     * @see #messageToGame(String, Message)
     * @see #messageToGameWithMon(String, Message)
     * @since 1.1.08
     */
    public void messageToGame(final String ga, final String txt)
    {
        gameList.takeMonitorForGame(ga);

        try
        {
            Vector v = gameList.getMembers(ga);

            if (v != null)
            {
                final String gameTextMsg = GameTextMsg.toCmd(ga, SERVERNAME,
                        txt);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection c = (StringConnection) menum.nextElement();
                    if (c != null)
                        c.put(gameTextMsg);
                }
            }
        }
        catch (Throwable e)
        {
            D.ebugPrintStackTrace(e, "Exception in messageToGame");
        }

        gameList.releaseMonitorForGame(ga);
    }

    /**
     * Send a message to the given game.
     *<P>
     * <b>Locks:</b> MUST HAVE THE {@link GameList#takeMonitorForGame(String)
     * gameList.takeMonitorForGame(ga)} before calling this method.
     * 
     * @param ga
     *            the name of the game
     * @param mes
     *            the message to send
     * @see #messageToGame(String, Message)
     */
    private void messageToGameWithMon(String ga, Message mes)
    {
        Vector v = gameList.getMembers(ga);

        if (v != null)
        {
            // D.ebugPrintln("M2G - "+mes);
            Enumeration menum = v.elements();

            while (menum.hasMoreElements())
            {
                StringConnection c = (StringConnection) menum.nextElement();

                if (c != null)
                {
                    // currentGameEventRecord.addMessageOut(new
                    // SOCMessageRecord(mes, "SERVER", c.getData()));
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to all the connections in a game excluding some.
     * 
     * @param gn
     *            the name of the game
     * @param ex
     *            the list of exceptions
     * @param mes
     *            the message
     * @param takeMon
     *            Should this method take and release game's monitor via
     *            {@link GameList#takeMonitorForGame(String)} ?
     */
    public void messageToGameExcept(String gn, Vector ex, Message mes,
            boolean takeMon)
    {
        if (takeMon)
            gameList.takeMonitorForGame(gn);

        try
        {
            Vector v = gameList.getMembers(gn);

            if (v != null)
            {
                // D.ebugPrintln("M2GE - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) menum
                            .nextElement();

                    if ((con != null) && (!ex.contains(con)))
                    {
                        // currentGameEventRecord.addMessageOut(new
                        // SOCMessageRecord(mes, "SERVER", con.getData()));
                        con.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception in messageToGameExcept");
        }

        if (takeMon)
            gameList.releaseMonitorForGame(gn);
    }

    /**
     * Send a message to all the connections in a game excluding one.
     * 
     * @param gn
     *            the name of the game
     * @param ex
     *            the excluded connection, or null
     * @param mes
     *            the message
     * @param takeMon
     *            Should this method take and release game's monitor via
     *            {@link GameList#takeMonitorForGame(String)} ?
     */
    public void messageToGameExcept(String gn, StringConnection ex,
            Message mes, boolean takeMon)
    {
        if (takeMon)
            gameList.takeMonitorForGame(gn);

        try
        {
            Vector v = gameList.getMembers(gn);

            if (v != null)
            {
                // D.ebugPrintln("M2GE - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) menum
                            .nextElement();
                    if ((con != null) && (con != ex))
                    {
                        // currentGameEventRecord.addMessageOut(new
                        // SOCMessageRecord(mes, "SERVER", con.getData()));
                        con.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception in messageToGameExcept");
        }

        if (takeMon)
            gameList.releaseMonitorForGame(gn);
    }

    /**
     * Send an urgent GameTextMsg to the given game. An "urgent" message is a
     * GameTextMsg whose text begins with ">>>"; the client should draw the
     * user's attention in some way.
     *<P>
     * Like messageToGame, will take and release the game's monitor.
     * 
     * @param ga
     *            the name of the game
     * @param mes
     *            the message to send. If mes does not begin with ">>>", will
     *            prepend ">>> " before sending mes.
     */
    public void messageToGameUrgent(String ga, String mes)
    {
        if (!mes.startsWith(">>>"))
            mes = ">>> " + mes;
        messageToGame(ga, mes);
    }

    /**
     * things to do when the connection c leaves
     *<P>
     * This method is called within a per-client thread, after connection is
     * removed from conns collection and version collection, and after
     * c.disconnect() has been called.
     * 
     * @param c
     *            the connection
     */
    @Override
    public void leaveConnection(StringConnection c)
    {
        if ((c != null) && (c.getData() != null))
        {
            leaveAllChannels(c);
            leaveAllGames(c);

            /**
             * if it is a robot, remove it from the list
             */
            robots.removeElement(c);
        }
    }

    /**
     * Things to do when a new connection comes. Called only by superclass. If
     * the connection is accepted, it's added to {@link #unnamedConns} until the
     * player "names" it by joining or creating a game under their player name.
     * Other communication is then done, in
     * {@link #newConnection2(StringConnection)}.
     *<P>
     * Also set client's "assumed version" to -1, until we have sent and
     * received a VERSION message.
     *<P>
     * This method is called within a per-client thread. You can send to client,
     * but can't yet receive messages from them.
     *<P>
     * SYNCHRONIZATION NOTE: During the call to newConnection1, the monitor lock
     * of {@link #unnamedConns} is held. Thus, defer as much as possible until
     * {@link #newConnection2(StringConnection)} (after the connection is
     * accepted).
     * 
     * @param c
     *            the new Connection
     * @return true to accept and continue, false if you have rejected this
     *         connection; if false, addConnection will call
     *         {@link StringConnection#disconnectSoft()}.
     * 
     * @see #addConnection(StringConnection)
     * @see #newConnection2(StringConnection)
     * @see #nameConnection(StringConnection, boolean)
     */
    public boolean newConnection1(StringConnection c)
    {
        if (c != null)
        {
            /**
             * see if we are under the connection limit
             */
            try
            {
                // see if we are under the connection limit
                if (this.connectionCount() >= maxConnections)
                {
                    RejectConnection rcCommand = new RejectConnection(
                            "Too many connections, please try another server.");
                    c.put(rcCommand.toCmd());
                }
            }
            catch (Exception e)
            {
                D
                        .ebugPrintStackTrace(e,
                                "Caught exception in SOCServer.newConnection(Connection)");
            }

            try
            {
                /**
                 * prevent someone from connecting twice from the same machine
                 * (Commented out: This is a bad idea due to proxies, NAT, etc.)
                 */
                boolean hostMatch = false;
                /*
                 * Enumeration allConnections = getConnections();
                 * 
                 * while(allConnections.hasMoreElements()) { StringConnection
                 * tempCon = (StringConnection)allConnections.nextElement(); if
                 * (!(c.host().equals("pippen")) &&
                 * (tempCon.host().equals(c.host()))) { hostMatch = true; break;
                 * } }
                 */
                if (hostMatch)
                {
                    RejectConnection rcCommand = new RejectConnection(
                            "Can't connect to the server more than once from one machine.");
                    c.put(rcCommand.toCmd());
                }
                else
                {
                    /**
                     * Accept this connection. Once it's added to the list,
                     * {@link #newConnection2(StringConnection)} will try to
                     * wait for client version, and will send the list of
                     * channels and games.
                     */
                    c.setVersion(-1);
                    return true;
                }
            }
            catch (Exception e)
            {
                D
                        .ebugPrintStackTrace(e,
                                "Caught exception in SOCServer.newConnection(Connection)");
            }
        }

        return false; // Not accepted
    }

    /**
     * Send welcome messages (server version, and the lists of channels and
     * games ({@link Channels}, {@link Games})) when a new connection comes,
     * part 2 - c has been accepted and added to a connection list. Unlike
     * {@link #newConnection1(StringConnection)}, no connection-list locks are
     * held when this method is called.
     *<P>
     * Client's {@link ClientData} appdata is set here.
     *<P>
     * This method is called within a per-client thread. You can send to client,
     * but can't yet receive messages from them.
     */
    protected void newConnection2(StringConnection c)
    {
        ClientData cdata = new ClientData();
        c.setAppData(cdata);

        // VERSION of server
        c.put(SOCVersion.toCmd(Version.versionNumber(), Version.version(),
                Version.buildnum()));

        // CHANNELS
        Vector cl = new Vector();
        channelList.takeMonitor();

        try
        {
            Enumeration clEnum = channelList.getChannels();

            while (clEnum.hasMoreElements())
            {
                cl.addElement(clEnum.nextElement());
            }
        }
        catch (Exception e)
        {
            D
                    .ebugPrintStackTrace(e,
                            "Exception in newConnection (channelList)");
        }

        channelList.releaseMonitor();

        c.put(Channels.toCmd(cl));

        // GAMES

        /**
         * Has the client sent us its VERSION message, as the first inbound
         * message? Games will be sent once we know the client's version, or
         * have guessed that it's too old (if the client doesn't tell us soon
         * enough). So: Check if input is waiting for us. If it turns out the
         * waiting message is something other than VERSION, server callback
         * {@link #processFirstCommand} will set up the version TimerTask using
         * {@link ClientData#setVersionTimer}. The version timer will call
         * {@link #sendGameList} when it expires. If no input awaits us right
         * now, set up the timer here.
         */
        if (!c.isInputAvailable())
        {
            cdata.setVersionTimer(this, c);
        }

    } // newConnection2

    /**
     * Name a current connection to the system, which may replace an older
     * connection. Call c.setData(name) just before calling this method. Calls
     * {@link Server#nameConnection(StringConnection)}. Will then adjust game
     * list/channel list if <tt>isReplacing</tt>.
     * 
     * @param c
     *            Connected client; its key data (
     *            {@link StringConnection#getData()}) must not be null
     * @param isReplacing
     *            Are we replacing / taking over a current connection?
     * @throws IllegalArgumentException
     *             If c isn't already connected, if c.getData() returns null, or
     *             if nameConnection has previously been called for this
     *             connection.
     * @since 1.1.08
     */
    private void nameConnection(StringConnection c, boolean isReplacing)
            throws IllegalArgumentException
    {
        System.err.println("L1819: nameConn(" + c + ", " + isReplacing + ")"); // JM
                                                                               // TEMP
        StringConnection oldConn = null;
        if (isReplacing)
        {
            Object cKey = c.getData();
            if (cKey == null)
                throw new IllegalArgumentException("null c.getData");
            oldConn = (StringConnection) conns.get(cKey);
            if (oldConn == null)
                isReplacing = false; // shouldn't happen, but fail gracefully
        }

        super.nameConnection(c);

        if (isReplacing)
        {
            gameList.replaceMemberAllGames(oldConn, c);
            channelList.replaceMemberAllChannels(oldConn, c);

            ClientData scdNew = (ClientData) (c.getAppData());
            ClientData scdOld = (ClientData) (oldConn.getAppData());
            if ((scdNew != null) && (scdOld != null))
                scdNew.copyClientPlayerStats(scdOld);

            // Let the old one know it's disconnected now,
            // in case it ever does get its connection back.
            if (oldConn.getVersion() >= 1108)
                oldConn.put(ServerPing.toCmd(-1));
        }
    }

    /**
     * Send the entire list of games to this client; this is sent once per
     * connecting client. Or, send the set of changed games, if the client's
     * guessed version was wrong. The list includes a flag on games which can't
     * be joined by this client version (
     * {@link Games#MARKER_THIS_GAME_UNJOINABLE}).
     *<P>
     * If <b>entire list</b>, then depending on client's version, the message
     * sent will be either {@link Games GAMES} or {@link GamesWithOptions
     * GAMESWITHOPTIONS}. If <b>set of changed games</b>, sent as matching pairs
     * of {@link DeleteGame DELETEGAME} and either {@link NewGame NEWGAME} or
     * {@link NewGameWithOptions NEWGAMEWITHOPTIONS}.
     *<P>
     * There are 2 possible scenarios for when this method is called:
     *<P>
     * - (A) Sending game list to client, for the first time: Iterate through
     * all games, looking for ones the client's version is capable of joining.
     * If not capable, mark the game name as such before sending it to the
     * client. (As a special case, very old client versions "can't know" about
     * the game they can't join, because they don't recognize the marker.) Also
     * set the client data's hasSentGameList flag.
     *<P>
     * - (B) The client didn't give its version, and was thus identified as an
     * old version. Now we know its newer true version, so we must tell it about
     * games that it can now join, which couldn't have been joined by the older
     * assumed version. So: Look for games with those criteria.
     *<P>
     * Sending the list is done here, and not in newConnection2, because we must
     * first know the client's version.
     *<P>
     * The minimum version which recognizes the "can't join" marker is 1.1.06 (
     * {@link Games#VERSION_FOR_UNJOINABLE}). Older clients won't be sent the
     * game names they can't join.
     *<P>
     * <b>Locks:</b> Calls {@link GameListAtServer#takeMonitor()} /
     * releaseMonitor
     * 
     * @param c
     *            Client's connection; will call getVersion() on it
     * @param prevVers
     *            Previously assumed version of this client; if re-sending the
     *            list, should be less than c.getVersion.
     * @since 1.1.06
     */
    public void sendGameList(StringConnection c, int prevVers)
    {
        final int cliVers = c.getVersion(); // Need to know this before sending

        // Before send list of games, try for a client version.
        // Give client 1.2 seconds to send it, before we assume it's old
        // (too old to know VERSION).
        // This waiting is done from ClientData.setVersionTimer;
        // time to wait is SOCServer.CLI_VERSION_TIMER_FIRE_MS.

        // GAMES / GAMESWITHOPTIONS

        // Based on version:
        // If client is too old (< 1.1.06), it can't be told names of games
        // that it isn't capable of joining.

        boolean cliCanKnow = (cliVers >= Games.VERSION_FOR_UNJOINABLE);
        final boolean cliCouldKnow = (prevVers >= Games.VERSION_FOR_UNJOINABLE);

        Vector gl = new Vector(); // contains Strings and/or Games;
        // strings are names of unjoinable games,
        // with the UNJOINABLE prefix.
        gameList.takeMonitor();
        final boolean alreadySent = ((ClientData) c.getAppData())
                .hasSentGameList(); // Check while gamelist monitor is held
        boolean cliVersionChange = alreadySent && (cliVers > prevVers);

        if (alreadySent && !cliVersionChange)
        {
            gameList.releaseMonitor();

            return; // <---- Early return: Nothing to do ----
        }

        if (!alreadySent)
        {
            ((ClientData) c.getAppData()).setSentGameList(); // Set while
                                                             // gamelist monitor
                                                             // is held
        }

        /**
         * We release the monitor as soon as we can, even though we haven't yet
         * sent the list to the client. It's theoretically possible the client
         * will get a NEWGAME message, which is OK, or a DELETEGAME message,
         * before it receives the list we're building. NEWGAME is OK because the
         * GAMES message won't clear the list contents at client. DELETEGAME is
         * less OK, but it's not very likely. If the game is deleted, and then
         * they see it in the list, trying to join that game will create a new
         * empty game with that name.
         */
        Enumeration gaEnum = gameList.getGamesData();
        gameList.releaseMonitor();

        if (cliVersionChange && cliCouldKnow)
        {
            // If they already have the names of games they can't join,
            // no need to re-send those names.
            cliCanKnow = false;
        }

        try
        {
            Game g;

            // Build the list of game names. This loop is used for the
            // initial list, or for sending just the delta after the version
            // fix.

            while (gaEnum.hasMoreElements())
            {
                g = (Game) gaEnum.nextElement();
                int gameVers = g.getClientVersionMinRequired();

                if (cliVersionChange && (prevVers >= gameVers))
                {
                    continue; // No need to re-announce, they already
                    // could join it with lower (prev-assumed) version
                }

                if (cliVers >= gameVers)
                {
                    gl.addElement(g); // Can join
                }
                else if (cliCanKnow)
                {
                    // Cannot join, but can see it
                    StringBuffer sb = new StringBuffer();
                    sb.append(Games.MARKER_THIS_GAME_UNJOINABLE);
                    sb.append(g.getName());
                    gl.addElement(sb.toString());
                }
                // else
                // can't join, and won't see it

            }

            // We now have the list of game names / socgame objs.

            if (!alreadySent)
            {
                // send the full list as 1 message
                if (cliVers >= NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS)
                    c.put(GamesWithOptions.toCmd(gl));
                else
                    c.put(Games.toCmd(gl));
            }
            else
            {
                // send deltas only
                for (int i = 0; i < gl.size(); ++i)
                {
                    Object ob = gl.elementAt(i);
                    String gaName;
                    if (ob instanceof Game)
                        gaName = ((Game) ob).getName();
                    else
                        gaName = (String) ob;

                    if (cliCouldKnow)
                    {
                        // first send delete, if it's on their list already
                        c.put(DeleteGame.toCmd(gaName));
                    }
                    // announce as 'new game' to client
                    if ((ob instanceof Game)
                            && (cliVers >= NewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS))
                        c.put(NewGameWithOptions.toCmd((Game) ob));
                    else
                        c.put(NewGame.toCmd(gaName));
                }
            }
        }
        catch (Exception e)
        {
            D
                    .ebugPrintStackTrace(e,
                            "Exception in newConnection(sendgamelist)");
        }

        /*
         * gaEnum = gameList.getGames(); int scores[] = new
         * int[Game.MAXPLAYERS]; boolean robots[] = new
         * boolean[Game.MAXPLAYERS]; while (gaEnum.hasMoreElements()) { String
         * gameName = (String)gaEnum.nextElement(); Game theGame =
         * gameList.getGameData(gameName); for (int i = 0; i < Game.MAXPLAYERS;
         * i++) { Player player = theGame.getPlayer(i); if (player != null) { if
         * (theGame.isSeatVacant(i)) { scores[i] = -1; robots[i] = false; } else
         * { scores[i] = player.getPublicVP(); robots[i] = player.isRobot(); } }
         * else { scores[i] = 0; } } c.put(GameStats.toCmd(gameName, scores,
         * robots)); }
         */

    } // sendGameList

    /**
     * Check if a nickname is okay, and, if they're already logged in, whether a
     * new replacement connection can "take over" the existing one.
     *<P>
     * a name is ok if it hasn't been used yet, isn't {@link #SERVERNAME the
     * server's name}, and (since 1.1.07) passes
     * {@link Message#isSingleLineAndSafe(String)}.
     *<P>
     * The "take over" option is used for reconnect when a client loses
     * connection, and server doesn't realize it. A new connection can
     * "take over" the name after a timeout; check the return value. (After
     * {@link #NICKNAME_TAKEOVER_SECONDS_SAME_IP} or
     * {@link #NICKNAME_TAKEOVER_SECONDS_DIFFERENT_IP} seconds) When taking
     * over, the new connection's client version must be able to join all games
     * that the old connection is playing, as returned by
     * {@link GameListAtServer#playerGamesMinVersion(StringConnection)
     * gameList.playerGamesMinVersion}.
     * 
     * @param n
     *            the name
     * @param newc
     *            A new incoming connection, asking for this name
     * @param withPassword
     *            Did the connection supply a password?
     * @return 0 if the name is okay; <BR>
     *         -1 if OK <strong>and you are taking over a connection;</strong> <BR>
     *         -2 if not OK by rules (fails isSingleLineAndSafe); <BR>
     *         -vers if not OK by version (for takeover; will be -1000 lower); <BR>
     *         or, the number of seconds after which <tt>newc</tt> can take over
     *         this name's games.
     * @see #checkNickname_getRetryText(int)
     */
    private int checkNickname(String n, StringConnection newc,
            final boolean withPassword)
    {
        if (n.equalsIgnoreCase(SERVERNAME))
        {
            return -2;
        }

        if (!Message.isSingleLineAndSafe(n))
        {
            return -2;
        }

        // check conns hashtable
        StringConnection oldc = getConnection(n);
        if (oldc == null)
        {
            return 0; // OK: no player by that name already
        }

        // Can we take over this one?
        ClientData scd = (ClientData) oldc.getAppData();
        if (scd == null)
        {
            return -2; // Shouldn't happen; name and SCD are assigned at same
                       // time
        }
        final int timeoutNeeded;
        if (withPassword)
            timeoutNeeded = NICKNAME_TAKEOVER_SECONDS_SAME_PASSWORD;
        else if (newc.host().equals(oldc.host()))
            // same IP address or hostname
            timeoutNeeded = NICKNAME_TAKEOVER_SECONDS_SAME_IP;
        else
            timeoutNeeded = NICKNAME_TAKEOVER_SECONDS_DIFFERENT_IP;

        final long now = System.currentTimeMillis();
        if (scd.disconnectLastPingMillis != 0)
        {
            int secondsSincePing = (int) (((now - scd.disconnectLastPingMillis)) / 1000L);
            if (secondsSincePing >= timeoutNeeded)
            {
                // Already sent ping, timeout has expired.
                // Re-check version just in case.
                int minVersForGames = gameList.playerGamesMinVersion(oldc);
                if (minVersForGames > newc.getVersion())
                {
                    if (minVersForGames < 1000)
                        minVersForGames = 1000;
                    return -minVersForGames; // too old to play
                }
                // it's OK to take over this nickname. A call made soon
                // to nameConnection(c,true) will transfer data from old conn,
                // to new conn.
                return -1;
            }
            else
            {
                // Already sent ping, timeout not yet expired.
                return timeoutNeeded - secondsSincePing;
            }
        }

        // Have not yet sent a ping.
        int minVersForGames = gameList.playerGamesMinVersion(oldc);
        if (minVersForGames > newc.getVersion())
        {
            if (minVersForGames < 1000)
                minVersForGames = 1000;
            return -minVersForGames; // too old to play
        }
        scd.disconnectLastPingMillis = now;
        if (oldc.getVersion() >= 1108)
        {
            // Already-connected client should respond to ping.
            // If not, consider them disconnected.
            oldc.put(ServerPing.toCmd(timeoutNeeded));
        }
        return timeoutNeeded;
    }

    /**
     * For a nickname that seems to be in use, build a text message with the
     * time remaining before someone can attempt to take over that nickname.
     * Used for reconnect when a client loses connection, and server doesn't
     * realize it. A new connection can "take over" the name after a timeout. (
     * {@link #NICKNAME_TAKEOVER_SECONDS_SAME_IP},
     * {@link #NICKNAME_TAKEOVER_SECONDS_DIFFERENT_IP})
     * 
     * @param nameTimeout
     *            Number of seconds before trying to reconnect
     * @return message starting with "Please wait x seconds" or
     *         "Please wait x minute(s)"
     * @since 1.1.08
     */
    private static final String checkNickname_getRetryText(final int nameTimeout)
    {
        StringBuffer sb = new StringBuffer("Please wait ");
        if (nameTimeout <= 90)
        {
            sb.append(nameTimeout);
            sb.append(" seconds");
        }
        else
        {
            sb.append((int) ((nameTimeout + 20) / 60));
            sb.append(" minute(s)");
        }
        sb.append(MSG_NICKNAME_ALREADY_IN_USE_WAIT_TRY_AGAIN);
        sb.append(MSG_NICKNAME_ALREADY_IN_USE);
        return sb.toString();
    }

    /**
     * For a nickname that seems to be in use, build a text message with the
     * minimum version number needed to take over that nickname. Used for
     * reconnect when a client loses connection, and server doesn't realize it.
     * A new connection can "take over" the name after a timeout.
     * 
     * @param needsVersion
     *            Version number required to take it over; a positive integer in
     *            the same format as {@link Game#getClientVersionMinRequired()}
     * @return string containing the version, starting with
     *         {@link #MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P1}.
     * @since 1.1.08
     */
    private static final String checkNickname_getVersionText(
            final int needsVersion)
    {
        StringBuffer sb = new StringBuffer(
                MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P1);
        sb.append(needsVersion);
        sb.append(MSG_NICKNAME_ALREADY_IN_USE_NEWER_VERSION_P2);
        return sb.toString();
    }

    /**
     * Callback to process the client's first message command specially. Look
     * for VERSION message; if none is received, set up a timer to wait for
     * version and (if never received) send out the game list soon.
     * 
     * @param str
     *            Contents of first message from the client
     * @param con
     *            Connection (client) sending this message
     * @return true if processed here (VERSION), false if this message should be
     *         queued up and processed by the normal
     *         {@link #processCommand(String, StringConnection)}.
     */
    public boolean processFirstCommand(String str, StringConnection con)
    {
        try
        {
            Message mes = Message.toMsg(str);
            if ((mes != null) && (mes.getType() == Message.VERSION))
            {
                handleVERSION(con, (SOCVersion) mes);

                return true; // <--- Early return: Version was handled ---
            }
        }
        catch (Throwable e)
        {
            D.ebugPrintStackTrace(e, "ERROR -> processFirstCommand");
        }

        // It wasn't version, it was something else. Set the
        // timer to wait for version, and return false for normal
        // processing of the message.

        ((ClientData) con.getAppData()).setVersionTimer(this, con);
        return false;
    }

    /**
     * Treat the incoming messages. Messages of unknown type are ignored.
     *<P>
     * Called from the single 'treater' thread. <em>Do not block or sleep</em>
     * because this is single-threaded.
     *<P>
     * Note: When there is a choice, always use local information over
     * information from the message. For example, use the nickname from the
     * connection to get the player information rather than the player
     * information from the message. This makes it harder to send false messages
     * making players do things they didn't want to do.
     * 
     * @param s
     *            Contents of message from the client
     * @param c
     *            Connection (client) sending this message
     */
    public void processCommand(String s, StringConnection c)
    {
        try
        {
            Message mes = (Message) Message.toMsg(s);

            // TO-DO: use a login message and check for it first, all others
            // verify that (c.data != null)

            // D.ebugPrintln(c.getData()+" - "+mes);
            if (mes != null)
            {
                switch (mes.getType())
                {

                /**
                 * client's echo of a server ping
                 */
                case Message.SERVERPING:
                    handleSERVERPING(c, (ServerPing) mes);
                    break;

                /**
                 * client's "version" message
                 */
                case Message.VERSION:
                    handleVERSION(c, (SOCVersion) mes);

                    break;

                /**
                 * "join a channel" message
                 */
                case Message.JOIN:
                    handleJOIN(c, (Join) mes);

                    break;

                /**
                 * "leave a channel" message
                 */
                case Message.LEAVE:
                    handleLEAVE(c, (Leave) mes);

                    break;

                /**
                 * "leave all channels" message
                 */
                case Message.LEAVEALL:
                    removeConnection(c);
                    removeConnectionCleanup(c);

                    break;

                /**
                 * text message
                 */
                case Message.TEXTMSG:

                    TextMsg textMsgMes = (TextMsg) mes;

                    if (c.getData().equals("debug"))
                    {
                        if (textMsgMes.getText().startsWith("*KILLCHANNEL*"))
                        {
                            messageToChannel(
                                    textMsgMes.getChannel(),
                                    new TextMsg(
                                            textMsgMes.getChannel(),
                                            SERVERNAME,
                                            "********** "
                                                    + (String) c.getData()
                                                    + " KILLED THE CHANNEL **********"));
                            channelList.takeMonitor();

                            try
                            {
                                channelList.deleteChannel(textMsgMes
                                        .getChannel());
                            }
                            catch (Exception e)
                            {
                                D.ebugPrintStackTrace(e,
                                        "Exception in KILLCHANNEL");
                            }

                            channelList.releaseMonitor();
                            broadcast(DeleteChannel.toCmd(textMsgMes
                                    .getChannel()));
                        }
                        else
                        {
                            /**
                             * Send the message to the members of the channel
                             */
                            messageToChannel(textMsgMes.getChannel(), mes);
                        }
                    }
                    else
                    {
                        /**
                         * Send the message to the members of the channel
                         */
                        messageToChannel(textMsgMes.getChannel(), mes);
                    }

                    break;

                /**
                 * a robot has connected to this server
                 */
                case Message.IMAROBOT:
                    handleIMAROBOT(c, (ImARobot) mes);

                    break;

                /**
                 * text message from a game (includes debug commands)
                 */
                case Message.GAMETEXTMSG:
                    handleGAMETEXTMSG(c, (GameTextMsg) mes);
                    break;

                /**
                 * "join a game" message
                 */
                case Message.JOINGAME:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleJOINGAME(c, (JoinGame) mes);

                    // ga = (Game)gamesData.get(((JoinGame)mes).getGame());
                    // if (ga != null) {
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((JoinGame)mes).getGame());
                    // }
                    break;

                /**
                 * "leave a game" message
                 */
                case Message.LEAVEGAME:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleLEAVEGAME(c, (LeaveGame) mes);

                    // ga = (Game)gamesData.get(((LeaveGame)mes).getGame());
                    // if (ga != null) {
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((LeaveGame)mes).getGame());
                    // }
                    break;

                /**
                 * someone wants to sit down
                 */
                case Message.SITDOWN:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleSITDOWN(c, (SitDown) mes);

                    // ga = (Game)gamesData.get(((SitDown)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((SitDown)mes).getGame());
                    break;

                /**
                 * someone put a piece on the board
                 */
                case Message.PUTPIECE:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handlePUTPIECE(c, (PutPiece) mes);

                    // ga = (Game)gamesData.get(((PutPiece)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((PutPiece)mes).getGame());
                    break;

                /**
                 * a player is moving the robber
                 */
                case Message.MOVEROBBER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMOVEROBBER(c, (MoveRobber) mes);

                    // ga = (Game)gamesData.get(((MoveRobber)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((MoveRobber)mes).getGame());
                    break;

                /**
                 * someone is starting a game
                 */
                case Message.STARTGAME:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleSTARTGAME(c, (StartGame) mes);

                    // ga = (Game)gamesData.get(((StartGame)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((StartGame)mes).getGame());
                    break;

                case Message.ROLLDICE:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleROLLDICE(c, (RollDice) mes);

                    // ga = (Game)gamesData.get(((RollDice)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((RollDice)mes).getGame());
                    break;

                case Message.DISCARD:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleDISCARD(c, (Discard) mes);

                    // ga = (Game)gamesData.get(((Discard)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((Discard)mes).getGame());
                    break;

                case Message.ENDTURN:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleENDTURN(c, (EndTurn) mes);

                    // ga = (Game)gamesData.get(((EndTurn)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((EndTurn)mes).getGame());
                    break;

                case Message.CHOOSEPLAYER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCHOOSEPLAYER(c, (ChoosePlayer) mes);

                    // ga = (Game)gamesData.get(((ChoosePlayer)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((ChoosePlayer)mes).getGame());
                    break;

                case Message.MAKEOFFER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMAKEOFFER(c, (MakeOffer) mes);

                    // ga = (Game)gamesData.get(((MakeOffer)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((MakeOffer)mes).getGame());
                    break;

                case Message.CLEAROFFER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCLEAROFFER(c, (ClearOffer) mes);

                    // ga = (Game)gamesData.get(((ClearOffer)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((ClearOffer)mes).getGame());
                    break;

                case Message.REJECTOFFER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleREJECTOFFER(c, (RejectOffer) mes);

                    // ga = (Game)gamesData.get(((RejectOffer)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((RejectOffer)mes).getGame());
                    break;

                case Message.ACCEPTOFFER:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleACCEPTOFFER(c, (AcceptOffer) mes);

                    // ga = (Game)gamesData.get(((AcceptOffer)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((AcceptOffer)mes).getGame());
                    break;

                case Message.BANKTRADE:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBANKTRADE(c, (BankTrade) mes);

                    // ga = (Game)gamesData.get(((BankTrade)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((BankTrade)mes).getGame());
                    break;

                case Message.BUILDREQUEST:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBUILDREQUEST(c, (BuildRequest) mes);

                    // ga = (Game)gamesData.get(((BuildRequest)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((BuildRequest)mes).getGame());
                    break;

                case Message.CANCELBUILDREQUEST:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCANCELBUILDREQUEST(c, (CancelBuildRequest) mes);

                    // ga =
                    // (Game)gamesData.get(((CancelBuildRequest)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((CancelBuildRequest)mes).getGame());
                    break;

                case Message.BUYCARDREQUEST:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBUYCARDREQUEST(c, (BuyCardRequest) mes);

                    // ga =
                    // (Game)gamesData.get(((BuyCardRequest)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((BuyCardRequest)mes).getGame());
                    break;

                case Message.PLAYDEVCARDREQUEST:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handlePLAYDEVCARDREQUEST(c, (PlayDevCardRequest) mes);

                    // ga =
                    // (Game)gamesData.get(((PlayDevCardRequest)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((PlayDevCardRequest)mes).getGame());
                    break;

                case Message.DISCOVERYPICK:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleDISCOVERYPICK(c, (DiscoveryPick) mes);

                    // ga = (Game)gamesData.get(((DiscoveryPick)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((DiscoveryPick)mes).getGame());
                    break;

                case Message.MONOPOLYPICK:

                    // createNewGameEventRecord();
                    // currentGameEventRecord.setMessageIn(new
                    // SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMONOPOLYPICK(c, (MonopolyPick) mes);

                    // ga = (Game)gamesData.get(((MonopolyPick)mes).getGame());
                    // currentGameEventRecord.setSnapshot(ga);
                    // saveCurrentGameEventRecord(((MonopolyPick)mes).getGame());
                    break;

                case Message.CHANGEFACE:
                    handleCHANGEFACE(c, (ChangeFace) mes);
                    break;

                case Message.SETSEATLOCK:
                    handleSETSEATLOCK(c, (SetSeatLock) mes);
                    break;

                case Message.RESETBOARDREQUEST:
                    handleRESETBOARDREQUEST(c, (ResetBoardRequest) mes);
                    break;

                case Message.RESETBOARDVOTE:
                    handleRESETBOARDVOTE(c, (ResetBoardVote) mes);
                    break;

                case Message.CREATEACCOUNT:
                    handleCREATEACCOUNT(c, (CreateAccount) mes);
                    break;

                case Message.GETSTATISTICS:
                    handleGETSTATISTICS(c, (GetStatistics) mes);
                    break;

                case Message.RESETSTATS:
                    handleRESETSTATS(c, (ResetStatistics) mes);
                    break;

                /**
                 * Game option messages. For the best writeup of these messages'
                 * interaction with the client, see
                 * {@link soc.client.PlayerClient.GameOptionServerSet}'s
                 * javadoc.
                 */

                case Message.GAMEOPTIONGETDEFAULTS:
                    handleGAMEOPTIONGETDEFAULTS(c, (GameOptionGetDefaults) mes);
                    break;

                case Message.GAMEOPTIONGETINFOS:
                    handleGAMEOPTIONGETINFOS(c, (GameOptionGetInfos) mes);
                    break;

                case Message.NEWGAMEWITHOPTIONSREQUEST:
                    handleNEWGAMEWITHOPTIONSREQUEST(c,
                            (NewGameWithOptionsRequest) mes);
                    break;

                } // switch (mes.getType)
            } // if (mes != null)
        }
        catch (Throwable e)
        {
            D.ebugPrintStackTrace(e, "ERROR -> processCommand");
        }

    } // processCommand

    /**
     * Used by {@link #processDebugCommand(StringConnection, String, String)}
     * when *HELP* is requested.
     * 
     * @since 1.1.07
     */
    public static final String[] DEBUG_COMMANDS_HELP =
        { "--- General Commands ---",
                "*ADDTIME*  add 30 minutes before game expiration",
                "*CHECKTIME*  print time remaining before expiration",
                "*VERSION*  show version and build information",
                "*WHO*   show players and observers of this game",
                "--- Debug Commands ---",
                "*BCAST*  broadcast msg to all games/channels",
                "*GC*    trigger the java garbage-collect",
                "*KILLBOT*  botname  End a bot's connection",
                "*KILLGAME*  end the current game",
                "*RESETBOT* botname  End a bot's connection",
                "*STATS*   server stats and current-game stats",
                "*STOP*  kill the server", "--- Debug Resources ---",
                "rsrcs: #cl #or #sh #wh #wo playername",
                "Example  rsrcs: 0 3 0 2 0 Myname",
                "dev: #typ playername",
                "Example  dev: 2 Myname",
                "Development card types are:", // see DevCardConstants
                "0 robber", "1 road-building", "2 year of plenty",
                "3 monopoly", "4 governors house", "5 market", "6 university",
                "7 temple", "8 chapel" };

    /**
     * Process a debug command, sent by the "debug" client/player. See
     * {@link #DEBUG_COMMANDS_HELP} for list of commands.
     */
    public void processDebugCommand(StringConnection debugCli, String ga,
            String dcmd)
    {
        if (dcmd.startsWith("*HELP*") || dcmd.startsWith("*help"))
        {
            for (int i = 0; i < DEBUG_COMMANDS_HELP.length; ++i)
                messageToPlayer(debugCli, ga, DEBUG_COMMANDS_HELP[i]);
            return;
        }

        if (dcmd.startsWith("*KILLGAME*"))
        {
            messageToGameUrgent(ga, ">>> ********** "
                    + (String) debugCli.getData()
                    + " KILLED THE GAME!!! ********** <<<");
            gameList.takeMonitor();

            try
            {
                destroyGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in KILLGAME");
            }

            gameList.releaseMonitor();
            broadcast(DeleteGame.toCmd(ga));
        }
        else if (dcmd.startsWith("*GC*"))
        {
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            messageToGame(ga, "> GARBAGE COLLECTING DONE");
            messageToGame(ga, "> Free Memory: " + rt.freeMemory());
        }
        else if (dcmd.startsWith("*STOP*"))
        {
            String stopMsg = ">>> ********** " + (String) debugCli.getData()
                    + " KILLED THE SERVER!!! ********** <<<";
            stopServer(stopMsg);
            System.exit(0);
        }
        else if (dcmd.startsWith("*BCAST* "))
        {
            // /
            // / broadcast to all chat channels and games
            // /
            broadcast(BCastTextMsg.toCmd(dcmd.substring(8)));
        }
        else if (dcmd.startsWith("*BOTLIST*"))
        {
            Enumeration robotsEnum = robots.elements();

            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum
                        .nextElement();
                messageToGame(ga, "> Robot: " + robotConn.getData());
                robotConn.put(AdminPing.toCmd((ga)));
            }
        }
        else if (dcmd.startsWith("*RESETBOT* "))
        {
            String botName = dcmd.substring(11).trim();
            messageToGame(ga, "> botName = '" + botName + "'");

            Enumeration robotsEnum = robots.elements();

            boolean botFound = false;
            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum
                        .nextElement();
                if (botName.equals((String) robotConn.getData()))
                {
                    botFound = true;
                    messageToGame(ga, "> SENDING RESET COMMAND TO " + botName);

                    AdminReset resetCmd = new AdminReset();
                    robotConn.put(resetCmd.toCmd());

                    break;
                }
            }
            if (!botFound)
                D.ebugPrintln("L2590 Bot not found to reset: " + botName);
        }
        else if (dcmd.startsWith("*KILLBOT* "))
        {
            String botName = dcmd.substring(10).trim();
            messageToGame(ga, "> botName = '" + botName + "'");

            Enumeration robotsEnum = robots.elements();

            boolean botFound = false;
            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum
                        .nextElement();

                if (botName.equals((String) robotConn.getData()))
                {
                    botFound = true;
                    messageToGame(ga, "> DISCONNECTING " + botName);
                    removeConnection(robotConn);
                    removeConnectionCleanup(robotConn);

                    break;
                }
            }
            if (!botFound)
                D.ebugPrintln("L2614 Bot not found to disconnect: " + botName);
        }
    }

    /**
     * The server is being cleanly stopped. Shut down with a final message
     * "The game server is shutting down".
     */
    public synchronized void stopServer()
    {
        stopServer(">>> The game server is shutting down. <<<");
    }

    /**
     * The server is being cleanly stopped. Send a final message, disconnect all
     * the connections, disconnect from database if connected. Currently called
     * only by the debug command "*STOP*", and by PlayerClient's locally hosted
     * TCP server.
     * 
     * @param stopMsg
     *            Final text message to send to all connected clients, or null.
     *            Will be sent as a {@link BCastTextMsg}. As always, if message
     *            starts with ">>" it will be considered urgent.
     */
    public synchronized void stopServer(String stopMsg)
    {
        if (stopMsg != null)
        {
            broadcast(BCastTextMsg.toCmd(stopMsg));
        }

        // / give time for messages to drain (such as urgent text messages
        // / about stopping the server)
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException ie)
        {
            Thread.yield();
        }

        // / now continue with shutdown
        try
        {
            SOCDBHelper.cleanup();
        }
        catch (SQLException x)
        {
        }

        super.stopServer();
    }

    /**
     * Authenticate the user. If the user is in the db, the password is
     * validated. If the user is <i>not</i> in the db, and requireDB is true, or
     * a non empty password was supplied, <code>false</code> is returned.
     * Finally, all other calls return <code>true</code>.
     * 
     * @param c
     *            the user's connection
     * @param userName
     *            the user's nickname
     * @param password
     *            the user's password; trim before calling
     * @param requireDB
     *            true if database authentication is required
     * @return true if the user has been authenticated
     */
    private boolean authenticateUser(StringConnection c, String userName,
            String password, boolean requireDB)
    {
        boolean result = false;

        try
        {
            SOCDBHelper.Auth auth = SOCDBHelper.authenticate(userName,
                    password, c.host());

            if (auth == SOCDBHelper.Auth.PASS)
            {
                result = true;
            }
            else if (auth == SOCDBHelper.Auth.FAIL)
            {
                c
                        .put(StatusMessage
                                .toCmd(StatusMessage.SV_PROBLEM_WITH_DB, c
                                        .getVersion(),
                                        "Problem connecting to database, please try again later."));
            }
            else
            // auth == SOCDBHelper.Auth.UNKNOWN
            {
                if (!password.equals("")) // no such user, but gave password
                {
                    c.put(StatusMessage.toCmd(StatusMessage.SV_PW_WRONG, c
                            .getVersion(), "Incorrect password for '"
                            + userName + "'."));
                }
                else
                // no db in use, or no such user, but check if db required
                {
                    result = !requireDB;
                }
            }
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem
            c.put(StatusMessage.toCmd(StatusMessage.SV_PROBLEM_WITH_DB,
                    "Problem connecting to database, please try again later."));
        }

        return result;
    }

    /**
     * Handle the client's echo of a {@link Message#SERVERPING}.
     * 
     * @since 1.1.08
     */
    private void handleSERVERPING(StringConnection c, ServerPing mes)
    {
        ClientData cd = (ClientData) c.getAppData();
        if (cd == null)
            return;
        cd.disconnectLastPingMillis = 0;

        // TO-DO any other reaction or flags?
    }

    /**
     * Handle the "version" message, client's version report. May ask to
     * disconnect, if version is too old. Otherwise send the game list. If we've
     * already sent the game list, send changes based on true version. If they
     * send another VERSION later, with a different version, disconnect the
     * client.
     *<P>
     * Along with the game list, the client will need to know the game option
     * info. This is sent when the client asks (after VERSION) for
     * {@link GameOptionGetInfos GAMEOPTIONGETINFOS}.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleVERSION(StringConnection c, SOCVersion mes)
    {
        if (c == null)
            return;

        setClientVersSendGamesOrReject(c, mes.getVersionNumber(), true);
    }

    /**
     * Set client's version, and check against minimum required version
     * {@link #CLI_VERSION_MIN}. If version is too low, send
     * {@link RejectConnection REJECTCONNECTION}. If we haven't yet sent the
     * game list, send now. If we've already sent the game list, send changes
     * based on true version.
     *<P>
     * Along with the game list, the client will need to know the game option
     * info. This is sent when the client asks (after VERSION) for
     * {@link GameOptionGetInfos GAMEOPTIONGETINFOS}. Game options are sent
     * after client version is known, so the list of sent options is based on
     * client version.
     *<P>
     * <b>Locks:</b> To set the version, will synchronize briefly on
     * {@link Server#unnamedConns unnamedConns}. If
     * {@link StringConnection#getVersion() c.getVersion()} is already == cvers,
     * don't bother to lock and set it.
     *<P>
     * Package access (not private) is strictly for use of
     * {@link ClientData.SOCCDCliVersionTask#run()}.
     * 
     * @param c
     *            Client's connection
     * @param cvers
     *            Version reported by client, or assumed version if no report
     * @param isKnown
     *            Is this the client's definite version, or just an assumed one?
     *            Affects {@link StringConnection#isVersionKnown()
     *            c.isVersionKnown}. Can set the client's known version only
     *            once; a second "known" call with a different cvers will be
     *            rejected.
     * @return True if OK, false if rejected
     */
    boolean setClientVersSendGamesOrReject(StringConnection c, final int cvers,
            final boolean isKnown)
    {
        final int prevVers = c.getVersion();
        final boolean wasKnown = c.isVersionKnown();

        if (prevVers == -1)
            ((ClientData) c.getAppData()).clearVersionTimer();

        if (prevVers != cvers)
        {
            synchronized (unnamedConns)
            {
                c.setVersion(cvers, isKnown);
            }
        }
        else if (wasKnown)
        {
            return true; // <--- Early return: Already knew it ----
        }

        String rejectMsg = null;
        String rejectLogMsg = null;

        if (cvers < CLI_VERSION_MIN)
        {
            if (cvers > 0)
                rejectMsg = "Sorry, your client version number " + cvers
                        + " is too old, version ";
            else
                rejectMsg = "Sorry, your client version is too old, version number ";
            rejectMsg += Integer.toString(CLI_VERSION_MIN) + " ("
                    + CLI_VERSION_MIN_DISPLAY + ") or above is required.";
            rejectLogMsg = "Rejected client: Version " + cvers + " too old";
        }
        if (wasKnown && isKnown && (cvers != prevVers))
        {
            // can't change the version once known
            rejectMsg = "Sorry, cannot report two different versions.";
            rejectLogMsg = "Rejected client: Already gave VERSION(" + prevVers
                    + "), now says VERSION(" + cvers + ")";
        }

        if (rejectMsg != null)
        {
            c.put(new RejectConnection(rejectMsg).toCmd());
            c.disconnectSoft();
            System.out.println(rejectLogMsg);
            return false;
        }

        // Send game list?
        // Will check c.getAppData().hasSentGameList() flag.
        // prevVers is ignored unless already sent game list.
        sendGameList(c, prevVers);

        // This client version is OK to connect
        return true;
    }

    /**
     * Handle the "join a channel" message. If client hasn't yet sent its
     * version, assume is version 1.0.00 ({@link #CLI_VERSION_ASSUMED_GUESS}),
     * disconnect if too low.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleJOIN(StringConnection c, Join mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOIN: " + mes);

            int cliVers = c.getVersion();

            /**
             * Check the reported version; if none, assume 1000 (1.0.00)
             */
            if (cliVers == -1)
            {
                if (!setClientVersSendGamesOrReject(c,
                        CLI_VERSION_ASSUMED_GUESS, false))
                    return; // <--- Discon and Early return: Client too old ---
                cliVers = c.getVersion();
            }

            /**
             * Check that the nickname is ok
             */
            boolean isTakingOver = false;

            final String msgUser = mes.getNickname().trim();
            String msgPass = mes.getPassword();
            if (msgPass != null)
                msgPass = msgPass.trim();

            if (c.getData() == null)
            {
                if (msgUser.length() > PLAYER_NAME_MAX_LENGTH)
                {
                    c
                            .put(StatusMessage
                                    .toCmd(
                                            StatusMessage.SV_NEWGAME_NAME_TOO_LONG,
                                            cliVers,
                                            StatusMessage.MSG_SV_NEWGAME_NAME_TOO_LONG
                                                    + Integer
                                                            .toString(PLAYER_NAME_MAX_LENGTH)));
                    return;
                }

                final int nameTimeout = checkNickname(msgUser, c,
                        (msgPass != null) && (msgPass.trim().length() > 0));
                if (nameTimeout == -1)
                {
                    isTakingOver = true;
                }
                else if (nameTimeout == -2)
                {
                    c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                            cliVers, MSG_NICKNAME_ALREADY_IN_USE));
                    return;
                }
                else if (nameTimeout <= -1000)
                {
                    c
                            .put(StatusMessage.toCmd(
                                    StatusMessage.SV_NAME_IN_USE, cliVers,
                                    checkNickname_getVersionText(-nameTimeout)));
                    return;
                }
                else if (nameTimeout > 0)
                {
                    c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                            cliVers, checkNickname_getRetryText(nameTimeout)));
                    return;
                }
            }
            // authenticate, sending message
            if (!authenticateUser(c, mes.getNickname(), mes.getPassword(),
                    false))
            {
                return;
            }

            final String ch = mes.getChannel().trim();
            if (!Message.isSingleLineAndSafe(ch))
            {
                c.put(StatusMessage.toCmd(
                        StatusMessage.SV_NEWGAME_NAME_REJECTED, cliVers,
                        StatusMessage.MSG_SV_NEWGAME_NAME_REJECTED));
                // "This game name is not permitted, please choose a different name."

                return; // <---- Early return ----
            }

            if (c.getData() == null)
            {
                c.setData(msgUser);
                nameConnection(c, isTakingOver);
                numberOfUsers++;
            }

            /**
             * Tell the client that everything is good to go
             */
            c.put(JoinAuth.toCmd(msgUser, ch));
            c.put(StatusMessage.toCmd(StatusMessage.SV_OK,
                    "Welcome to Java Settlers of Catan!"));

            /**
             * Add the StringConnection to the channel
             */

            if (channelList.takeMonitorForChannel(ch))
            {
                try
                {
                    connectToChannel(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e,
                            "Exception in handleJOIN (connectToChannel)");
                }

                channelList.releaseMonitorForChannel(ch);
            }
            else
            {
                /**
                 * the channel did not exist, create it
                 */
                channelList.takeMonitor();

                try
                {
                    channelList.createChannel(ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e,
                            "Exception in handleJOIN (createChannel)");
                }

                channelList.releaseMonitor();
                broadcast(NewChannel.toCmd(ch));
                c.put(Members.toCmd(ch, channelList.getMembers(ch)));
                D.ebugPrintln("*** " + c.getData() + " joined the channel "
                        + ch);
                channelList.takeMonitorForChannel(ch);

                try
                {
                    channelList.addMember(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e,
                            "Exception in handleJOIN (addMember)");
                }

                channelList.releaseMonitorForChannel(ch);
            }

            /**
             * let everyone know about the change
             */
            messageToChannel(ch, new Join(msgUser, "", "dummyhost", ch));
        }
    }

    /**
     * Handle the "leave a channel" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleLEAVE(StringConnection c, Leave mes)
    {
        D.ebugPrintln("handleLEAVE: " + mes);

        if (c != null)
        {
            boolean destroyedChannel = false;
            channelList.takeMonitorForChannel(mes.getChannel());

            try
            {
                destroyedChannel = leaveChannel(c, mes.getChannel(), false);
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in handleLEAVE");
            }

            channelList.releaseMonitorForChannel(mes.getChannel());

            if (destroyedChannel)
            {
                broadcast(DeleteChannel.toCmd(mes.getChannel()));
            }
        }
    }

    /**
     * Handle the "I'm a robot" message. Robots send their {@link SOCVersion}
     * before sending this message. Their version is checked here, must equal
     * server's version.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleIMAROBOT(StringConnection c, ImARobot mes)
    {
        if (c != null)
        {
            /**
             * Check the reported version; if none, assume 1000 (1.0.00)
             */
            final int srvVers = Version.versionNumber();
            int cliVers = c.getVersion();
            final String rbc = mes.getRBClass();
            final boolean isBuiltIn = (rbc == null)
                    || (rbc.equals(ImARobot.RBCLASS_BUILTIN));
            if (isBuiltIn)
            {
                if (cliVers != srvVers)
                {
                    String rejectMsg = "Sorry, robot client version does not match, version number "
                            + Integer.toString(srvVers) + " is required.";
                    c.put(new RejectConnection(rejectMsg).toCmd());
                    c.disconnectSoft();
                    System.out.println("Rejected robot " + mes.getNickname()
                            + ": Version " + cliVers
                            + " does not match server version");
                    return; // <--- Early return: Robot client too old ---
                }
                else
                {
                    System.out.println("Robot arrived: " + mes.getNickname()
                            + ": built-in type");
                }
            }
            else
            {
                System.out.println("Robot arrived: " + mes.getNickname()
                        + ": type " + rbc);
            }

            /**
             * Check that the nickname is ok
             */
            if ((c.getData() == null)
                    && (0 != checkNickname(mes.getNickname(), c, false)))
            {
                c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                        cliVers, MSG_NICKNAME_ALREADY_IN_USE));
                RejectConnection rcCommand = new RejectConnection(
                        MSG_NICKNAME_ALREADY_IN_USE);
                c.put(rcCommand.toCmd());
                System.err.println("Robot login attempt, name already in use: "
                        + mes.getNickname());
                // c.disconnect();
                c.disconnectSoft();

                return;
            }

            // Idle robots disconnect and reconnect every so often (socket
            // timeout).
            // In case of disconnect-reconnect, don't print the error or
            // re-arrival debug announcements.
            // The robot's nickname is used as the key for the disconnect
            // announcement.
            {
                ConnExcepDelayedPrintTask depart = (ConnExcepDelayedPrintTask) cliConnDisconPrintsPending
                        .get(mes.getNickname());
                if (depart != null)
                {
                    depart.cancel();
                    cliConnDisconPrintsPending.remove(mes.getNickname());
                    ConnExcepDelayedPrintTask arrive = (ConnExcepDelayedPrintTask) cliConnDisconPrintsPending
                            .get(c);
                    if (arrive != null)
                    {
                        arrive.cancel();
                        cliConnDisconPrintsPending.remove(c);
                    }
                }
            }

            RobotParameters params = null;
            //
            // send the current robot parameters
            //
            try
            {
                params = SOCDBHelper.retrieveRobotParams(mes.getNickname());
                if (params != null)
                    D.ebugPrintln("*** Robot Parameters for "
                            + mes.getNickname() + " = " + params);
            }
            catch (SQLException sqle)
            {
                System.err
                        .println("Error retrieving robot parameters from db: Using defaults.");
            }

            if (params == null)
            {
                params = new RobotParameters(ROBOT_PARAMS_DEFAULT);
            }

            c.put(UpdateRobotParams.toCmd(params));

            //
            // add this connection to the robot list
            //
            c.setData(mes.getNickname());
            c.setHideTimeoutMessage(true);
            robots.addElement(c);
            ClientData scd = (ClientData) c.getAppData();
            scd.isRobot = true;
            scd.isBuiltInRobot = isBuiltIn;
            if (!isBuiltIn)
                scd.robot3rdPartyBrainClass = rbc;
            nameConnection(c);
        }
    }

    /**
     * Handle game text messages, including debug commands. Was part of
     * processCommand before 1.1.07.
     * 
     * @since 1.1.07
     */
    private void handleGAMETEXTMSG(StringConnection c,
            GameTextMsg gameTextMsgMes)
    {
        // createNewGameEventRecord();
        // currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes,
        // c.getData(), "SERVER"));
        final String gaName = gameTextMsgMes.getGame();
        recordGameEvent(gaName, gameTextMsgMes.toCmd());

        Game ga = gameList.getGameData(gaName);
        if (ga == null)
            return; // <---- early return: no game by that name ----

        // currentGameEventRecord.setSnapshot(ga);
        // /
        // / command to add time to a game
        // / If the command text changes from '*ADDTIME*' to something else,
        // / please update the warning text sent in checkForExpiredGames().
        // /
        final String cmdText = gameTextMsgMes.getText();
        if ((cmdText.startsWith("*ADDTIME*"))
                || (cmdText.startsWith("*addtime*"))
                || (cmdText.startsWith("ADDTIME"))
                || (cmdText.startsWith("addtime")))
        {
            // add 30 min. to the expiration time. If this
            // changes to another timespan, please update the
            // warning text sent in checkForExpiredGames().
            // Use ">>>" in messageToGame to mark as urgent.
            if (ga.isLocal)
            {
                messageToGameUrgent(gaName, ">>> Practice games never expire.");
            }
            else
            {
                ga.setExpiration(ga.getExpiration() + (30 * 60 * 1000));
                messageToGameUrgent(gaName,
                        ">>> This game will expire in "
                                + ((ga.getExpiration() - System
                                        .currentTimeMillis()) / 60000)
                                + " minutes.");
            }
        }

        // /
        // / Check the time remaining for this game
        // /
        if (cmdText.startsWith("*CHECKTIME*"))
        {
            processDebugCommand_checktime(c, gaName, ga);
        }
        else if (cmdText.startsWith("*VERSION*"))
        {
            messageToPlayer(c, gaName, "Java Settlers Server "
                    + Version.versionNumber() + " (" + Version.version()
                    + ") build " + Version.buildnum());
        }
        else if (cmdText.startsWith("*STATS*"))
        {
            final long diff = System.currentTimeMillis() - startTime;
            final long hours = diff / (60 * 60 * 1000), minutes = (diff - (hours * 60 * 60 * 1000))
                    / (60 * 1000), seconds = (diff - (hours * 60 * 60 * 1000) - (minutes * 60 * 1000)) / 1000;
            Runtime rt = Runtime.getRuntime();
            messageToPlayer(c, gaName, "> Uptime: " + hours + ":" + minutes
                    + ":" + seconds);
            messageToPlayer(c, gaName, "> Total connections: "
                    + numberOfConnections);
            messageToPlayer(c, gaName, "> Current connections: "
                    + connectionCount());
            messageToPlayer(c, gaName, "> Total Users: " + numberOfUsers);
            messageToPlayer(c, gaName, "> Games started: "
                    + numberOfGamesStarted);
            messageToPlayer(c, gaName, "> Games finished: "
                    + numberOfGamesFinished);
            messageToPlayer(c, gaName, "> Total Memory: " + rt.totalMemory());
            messageToPlayer(c, gaName, "> Free Memory: " + rt.freeMemory());
            messageToPlayer(c, gaName, "> Version: " + Version.versionNumber()
                    + " (" + Version.version() + ") build "
                    + Version.buildnum());

            processDebugCommand_checktime(c, gaName, ga);
        }
        else if (cmdText.startsWith("*WHO*"))
        {
            Vector gameMembers = null;
            gameList.takeMonitorForGame(gaName);

            try
            {
                gameMembers = gameList.getMembers(gaName);
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e, "Exception in *WHO* (gameMembers)");
            }

            gameList.releaseMonitorForGame(gaName);

            if (gameMembers != null)
            {
                Enumeration membersEnum = gameMembers.elements();

                while (membersEnum.hasMoreElements())
                {
                    StringConnection conn = (StringConnection) membersEnum
                            .nextElement();
                    messageToGame(gaName, "> " + conn.getData());
                }
            }
        }

        //
        // useful for debugging
        //
        // 1.1.07: all practice games are debug mode, for ease of debugging;
        // not much use for a chat window in a practice game anyway.
        //
        if (c.getData().equals("debug") || (c instanceof LocalStringConnection))
        {
            final String msgText = cmdText;
            if (cmdText.startsWith("rsrcs:"))
            {
                giveResources(cmdText, ga);
            }
            else if (cmdText.startsWith("dev:"))
            {
                giveDevCard(cmdText, ga);
            }
            else if (gameTextMsgMes.getText().startsWith("*STARTGAME*"))
            {
                handleSTARTGAME(c, new StartGame(ga.getName()));
            }
            else if (cmdText.charAt(0) == '*')
            {
                processDebugCommand(c, ga.getName(), msgText);
            }
            else
            {
                //
                // Send the message to the members of the game
                //
                messageToGame(gaName, new GameTextMsg(gaName, (String) c
                        .getData(), cmdText));
            }
        }
        else
        {
            //
            // Send the message to the members of the game
            //
            messageToGame(gaName, new GameTextMsg(gaName, (String) c.getData(),
                    cmdText));
        }

        // saveCurrentGameEventRecord(gameTextMsgMes.getGame());
    }

    /**
     * Print time-remaining and other game stats. Includes more detail beyond
     * the end-game stats sent in {@link #sendGameStateOVER(Game)}.
     * 
     * @param c
     *            Client requesting the stats
     * @param gameData
     *            Game to print stats
     * @since 1.1.07
     */
    private void processDebugCommand_checktime(StringConnection c,
            final String gaName, Game gameData)
    {
        if (gameData == null)
            return;
        messageToPlayer(c, gaName, "-- Game statistics: --");
        messageToPlayer(c, gaName, "Rounds played: " + gameData.getRoundCount());

        // player's stats
        if (c.getVersion() >= PlayerStats.VERSION_FOR_RES_ROLL)
        {
            Player cp = gameData.getPlayer((String) c.getData());
            if (cp != null)
                messageToPlayer(c, new PlayerStats(cp,
                        PlayerStats.STYPE_RES_ROLL));
        }

        // time
        Date gstart = gameData.getStartTime();
        if (gstart != null)
        {
            long gameSeconds = ((new Date().getTime() - gstart.getTime()) + 500L) / 1000L;
            long gameMinutes = (gameSeconds + 29L) / 60L;
            String gLengthMsg = "This game started " + gameMinutes
                    + " minutes ago.";
            messageToPlayer(c, gaName, gLengthMsg);
            // Ignore possible "1 minutes"; that game is too short to worry
            // about.
        }

        if (!gameData.isLocal) // practice games don't expire
        {
            String expireMsg = ">>> This game will expire in "
                    + ((gameData.getExpiration() - System.currentTimeMillis()) / 60000)
                    + " minutes.";
            messageToPlayer(c, gaName, expireMsg);
        }
    }

    /**
     * Handle the "join a game" message: Join or create a game. Will join the
     * game, or return a STATUSMESSAGE if nickname is not OK. Clients can join
     * game as an observer, if they don't SITDOWN after joining.
     *<P>
     * If client hasn't yet sent its version, assume is version 1.0.00 (
     * {@link #CLI_VERSION_ASSUMED_GUESS}), disconnect if too low. If the client
     * is too old to join a specific game, return a STATUSMESSAGE. (since
     * 1.1.06)
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleJOINGAME(StringConnection c, JoinGame mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOINGAME: " + mes);

            /**
             * Check the client's reported version; if none, assume 1000
             * (1.0.00)
             */
            if (c.getVersion() == -1)
            {
                if (!setClientVersSendGamesOrReject(c,
                        CLI_VERSION_ASSUMED_GUESS, false))
                    return; // <--- Early return: Client too old ---
            }

            createOrJoinGameIfUserOK(c, mes.getNickname().trim(), mes
                    .getPassword(), mes.getGame().trim(), null);

        }
    }

    /**
     * Check username/password and create new game, or join game. Called by
     * handleJOINGAME and handleNEWGAMEWITHOPTIONSREQUEST. JOINGAME or
     * NEWGAMEWITHOPTIONSREQUEST may be the first message with the client's
     * username and password, so c.getData() may be null. Assumes client's
     * version is already received or guessed.
     *<P>
     * Game name and player name have a maximum length and some disallowed
     * characters; see parameters.
     *<P>
     * If client is replacing/taking over their own lost connection, first tell
     * them they're rejoining all their other games. That way, the requested
     * game's window will appear last, not hidden behind the others.
     *<P>
     * <b>Process if gameOpts != null:</b>
     *<UL>
     * <LI>if game with this name already exists, respond with STATUSMESSAGE(
     * {@link StatusMessage#SV_NEWGAME_ALREADY_EXISTS SV_NEWGAME_ALREADY_EXISTS})
     * <LI>compare cli's param name-value pairs, with srv's known values. <br>
     * - if any are above/below max/min, clip to the max/min value <br>
     * - if any are unknown, resp with STATUSMESSAGE(
     * {@link StatusMessage#SV_NEWGAME_OPTION_UNKNOWN SV_NEWGAME_OPTION_UNKNOWN}
     * ) <br>
     * - if any are too new for client's version, resp with STATUSMESSAGE(
     * {@link StatusMessage#SV_NEWGAME_OPTION_VALUE_TOONEW
     * SV_NEWGAME_OPTION_VALUE_TOONEW}) <br>
     * Comparison is done by
     * {@link GameOption#adjustOptionsToKnown(Hashtable, Hashtable)}.
     * <LI>if ok: create new game with params; socgame will calc game's
     * minCliVersion, and this method will check that against cli's version.
     * <LI>announce to all players using NEWGAMEWITHOPTIONS; older clients get
     * NEWGAME, won't see the options
     * <LI>send JOINGAMEAUTH to requesting client, via
     * {@link #joinGame(Game, StringConnection, boolean, boolean)}
     * <LI>send game status details to requesting client, via
     * {@link #joinGame(Game, StringConnection, boolean, boolean)}
     *</UL>
     * 
     * @param c
     *            connection requesting the game, must not be null
     * @param msgUser
     *            username of client in message. Must pass
     *            {@link Message#isSingleLineAndSafe(String)} and be at most
     *            {@link #PLAYER_NAME_MAX_LENGTH} characters.
     * @param msgPass
     *            password of client in message
     * @param gameName
     *            name of game to create/join. Must pass
     *            {@link Message#isSingleLineAndSafe(String)} and be at most
     *            {@link #GAME_NAME_MAX_LENGTH} characters.
     * @param gameOpts
     *            if game has options, contains {@link GameOption} to create new
     *            game; if not null, will not join an existing game. Will
     *            validate by calling
     *            {@link GameOption#adjustOptionsToKnown(Hashtable, Hashtable)}.
     * 
     * @since 1.1.07
     */
    private void createOrJoinGameIfUserOK(StringConnection c,
            final String msgUser, String msgPass, final String gameName,
            Hashtable gameOpts)
    {
        if (msgPass != null)
            msgPass = msgPass.trim();

        /**
         * Check that the nickname is ok
         */
        final int cliVers = c.getVersion();
        boolean isTakingOver = false;
        if (c.getData() == null)
        {
            if (msgUser.length() > PLAYER_NAME_MAX_LENGTH)
            {
                c.put(StatusMessage.toCmd(
                        StatusMessage.SV_NEWGAME_NAME_TOO_LONG, cliVers,
                        StatusMessage.MSG_SV_NEWGAME_NAME_TOO_LONG
                                + Integer.toString(PLAYER_NAME_MAX_LENGTH)));
                return;
            }

            /**
             * check if a nickname is okay, and, if they're already logged in,
             * whether a new replacement connection can "take over" the existing
             * one.
             */
            final int nameTimeout = checkNickname(msgUser, c, (msgPass != null)
                    && (msgPass.trim().length() > 0));

            if (nameTimeout == -1)
            {
                isTakingOver = true;
            }
            else if (nameTimeout == -2)
            {
                c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                        cliVers, MSG_NICKNAME_ALREADY_IN_USE));
                return;
            }
            else if (nameTimeout <= -1000)
            {
                c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                        cliVers, checkNickname_getVersionText(-nameTimeout)));
                return;
            }
            else if (nameTimeout > 0)
            {
                c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE,
                        cliVers, checkNickname_getRetryText(nameTimeout)));
                return;
            }
        }

        /**
         * password check new connection from database, if possible
         */
        if ((c.getData() == null)
                && (!authenticateUser(c, msgUser, msgPass, true)))
        {
            return; // <---- Early return: Password auth failed ----
        }

        /**
         * Check that the game name is ok
         */
        if (!Message.isSingleLineAndSafe(gameName))
        {
            c.put(StatusMessage.toCmd(StatusMessage.SV_NEWGAME_NAME_REJECTED,
                    cliVers, StatusMessage.MSG_SV_NEWGAME_NAME_REJECTED));
            // "This game name is not permitted, please choose a different name."

            return; // <---- Early return ----
        }
        if (gameName.length() > GAME_NAME_MAX_LENGTH)
        {
            c.put(StatusMessage.toCmd(StatusMessage.SV_NEWGAME_NAME_TOO_LONG,
                    cliVers, StatusMessage.MSG_SV_NEWGAME_NAME_TOO_LONG
                            + Integer.toString(GAME_NAME_MAX_LENGTH)));
            // Please choose a shorter name; maximum length: 20

            return; // <---- Early return ----
        }

        /**
         * Now that everything's validated, name this connection/user/player
         */
        if (c.getData() == null)
        {
            c.setData(msgUser);
            nameConnection(c, isTakingOver);
            numberOfUsers++;
        }

        /**
         * If we have game options, we're being asked to create a new game.
         * Validate them and ensure the game doesn't already exist.
         */
        if (gameOpts != null)
        {
            if (gameList.isGame(gameName))
            {
                c.put(StatusMessage.toCmd(
                        StatusMessage.SV_NEWGAME_ALREADY_EXISTS, cliVers,
                        StatusMessage.MSG_SV_NEWGAME_ALREADY_EXISTS));
                // "A game with this name already exists, please choose a different name."

                return; // <---- Early return ----
            }

            if (!GameOption.adjustOptionsToKnown(gameOpts, null))
            {
                c
                        .put(StatusMessage
                                .toCmd(StatusMessage.SV_NEWGAME_OPTION_UNKNOWN,
                                        cliVers,
                                        "Unknown game option(s) were requested, cannot create this game."));

                return; // <---- Early return ----
            }
        }

        /**
         * Try to add player to game, and tell the client that everything is
         * ready; if game doesn't yet exist, it's created in connectToGame, and
         * announced there to all clients.
         *<P>
         * If client's version is too low (based on game options, etc),
         * connectToGame will throw an exception; tell the client if that
         * happens.
         *<P>
         * If rejoining after a lost connection, first rejoin all their other
         * games.
         */
        try
        {
            if (isTakingOver)
            {
                /**
                 * Rejoin the requested game. First, rejoin all other games of
                 * this client. That way, the requested game's window will
                 * appear last, not hidden behind the others. For each game,
                 * calls joinGame to send JOINGAMEAUTH and the entire state of
                 * the game to client.
                 */
                Vector allConnGames = gameList.memberGames(c, gameName);
                if (allConnGames.size() == 0)
                {
                    c
                            .put(StatusMessage
                                    .toCmd(StatusMessage.SV_OK,
                                            "You've taken over the connection, but aren't in any games."));
                }
                else
                {
                    // Send list backwards: requested game will be sent last.
                    for (int i = allConnGames.size() - 1; i >= 0; --i)
                        joinGame((Game) allConnGames.elementAt(i), c, false,
                                true);
                }
            }
            else if (connectToGame(c, gameName, gameOpts))
            {
                /**
                 * send JOINGAMEAUTH to client, send the entire state of the
                 * game to client, send client join event to other players of
                 * game
                 */
                Game gameData = gameList.getGameData(gameName);

                if (gameData != null)
                {
                    joinGame(gameData, c, false, false);
                }
            }
        }
        catch (GameOptionVersionException e)
        {
            // Let them know they can't join; include the game's version.
            // This cli asked to created it, otherwise gameOpts would be null.
            c.put(StatusMessage.toCmd(
                    StatusMessage.SV_NEWGAME_OPTION_VALUE_TOONEW, cliVers,
                    "Cannot create game with these options; requires version "
                            + Integer.toString(e.gameOptsVersion)
                            + Message.sep2_char + gameName + Message.sep2_char
                            + e.problemOptionsList()));
        }
        catch (IllegalArgumentException e)
        {
            // Let them know they can't join; include the game's version.

            c.put(StatusMessage.toCmd(StatusMessage.SV_CANT_JOIN_GAME_VERSION,
                    cliVers, "Cannot join game; requires version "
                            + Integer.toString(gameList.getGameData(gameName)
                                    .getClientVersionMinRequired()) + ": "
                            + gameName));
        }

    } // createOrJoinGameIfUserOK

    /**
     * Handle the "leave game" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleLEAVEGAME(StringConnection c, LeaveGame mes)
    {
        if (c != null)
        {
            recordGameEvent(mes, mes.getGame(), mes.toCmd());
            boolean isMember = false;
            final String gaName = mes.getGame();
            if (!gameList.takeMonitorForGame(gaName))
            {
                return; // <--- Early return: game not in gamelist ---
            }

            try
            {
                isMember = gameList.isMember(c, gaName);
            }
            catch (Exception e)
            {
                D.ebugPrintStackTrace(e,
                        "Exception in handleLEAVEGAME (isMember)");
            }

            gameList.releaseMonitorForGame(gaName);

            if (isMember)
            {
                handleLEAVEGAME_member(c, gaName, mes);
            }
            else if (((ClientData) c.getAppData()).isRobot)
            {
                handleLEAVEGAME_maybeGameReset_oldRobot(gaName);
                // During a game reset, this robot player
                // will not be found among cg's players
                // (isMember is false), because it's
                // attached to the old game object
                // instead of the new one.
                // So, check game state and update game's reset data.
            }
        }
    }

    /**
     * Handle a member leaving the game, from
     * {@link #handleLEAVEGAME(StringConnection, LeaveGame)}.
     * 
     * @since 1.1.07
     */
    private void handleLEAVEGAME_member(StringConnection c,
            final String gaName, LeaveGame mes)
    {
        boolean gameDestroyed = false;
        if (!gameList.takeMonitorForGame(gaName))
        {
            return; // <--- Early return: game not in gamelist ---
        }

        try
        {
            gameDestroyed = leaveGame(c, gaName, false);
        }
        catch (Exception e)
        {
            D
                    .ebugPrintStackTrace(e,
                            "Exception in handleLEAVEGAME (leaveGame)");
        }

        gameList.releaseMonitorForGame(gaName);

        if (gameDestroyed)
        {
            broadcast(DeleteGame.toCmd(gaName));
        }
        else
        {
            /*
             * LeaveGame leaveMessage = new LeaveGame((String)c.getData(),
             * c.host(), mes.getGame()); messageToGame(mes.getGame(),
             * leaveMessage); recordGameEvent(mes.getGame(),
             * leaveMessage.toCmd());
             */
        }

        // if a robot is leaving, it's likely that a real player
        // requested it. If this connection is the leaving robot, get
        // the sit request from the player, and fulfill it
        Map dismissals = (Map) robotDismissals.get(mes.getGame());

        if (dismissals != null)
        {
            ReplaceRequest req = (ReplaceRequest) dismissals.remove(c);
            if (req != null)
            {
                SitDown msg = req.getSitDownMessage();
                Game game = gameList.getGameData(mes.getGame());
                if (game != null)
                {
                    sitDown(game, req.getArriving(), msg.getPlayerNumber(), msg
                            .isRobot(), false);
                }
            }
        }
    }

    /**
     * Handle an unattached robot saying it is leaving the game, from
     * {@link #handleLEAVEGAME(StringConnection, LeaveGame)}. Ignore the robot
     * (since it's not a member of the game) unless gamestate is
     * {@link Game#READY_RESET_WAIT_ROBOT_DISMISS}.
     * 
     * @since 1.1.07
     */
    private void handleLEAVEGAME_maybeGameReset_oldRobot(final String gaName)
    {
        Game cg = gameList.getGameData(gaName);
        if (cg.getGameState() != Game.READY_RESET_WAIT_ROBOT_DISMISS)
            return;

        boolean gameResetRobotsAllDismissed = false;

        // TO-DO locks
        GameBoardReset gr = cg.boardResetOngoingInfo;
        if (gr != null)
        {
            --gr.oldRobotCount;
            if (0 == gr.oldRobotCount)
                gameResetRobotsAllDismissed = true;
        }

        if (gameResetRobotsAllDismissed)
            resetBoardAndNotify_finish(gr, cg); // TO-DO locks?
    }

    /**
     * handle "sit down" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleSITDOWN(StringConnection c, SitDown mes)
    {
        if (c != null)
        {
            final String gaName = mes.getGame();
            Game ga = gameList.getGameData(gaName);

            if (ga != null)
            {
                /**
                 * make sure this player isn't already sitting
                 */
                boolean canSit = true;
                boolean gameIsFull = false;

                /*
                 * for (int i = 0; i < Game.MAXPLAYERS; i++) { if
                 * (ga.getPlayer(i).getName() == (String)c.getData()) { canSit =
                 * false; break; } }
                 */
                // D.ebugPrintln("ga.isSeatVacant(mes.getPlayerNumber()) = "+ga.isSeatVacant(mes.getPlayerNumber()));
                /**
                 * make sure a person isn't sitting here already; if a robot is
                 * sitting there, dismiss the robot.
                 */
                ga.takeMonitor();

                try
                {
                    if (ga.isSeatVacant(mes.getPlayerNumber()))
                    {
                        gameIsFull = (1 > ga.getAvailableSeatCount());
                        if (gameIsFull)
                            canSit = false;
                    }
                    else
                    {
                        Player seatedPlayer = ga.getPlayer(mes
                                .getPlayerNumber());

                        if (seatedPlayer.isRobot()
                                && (!ga.isSeatLocked(mes.getPlayerNumber()))
                                && (ga.getCurrentPlayerNumber() != mes
                                        .getPlayerNumber()))
                        {
                            /**
                             * boot the robot out of the game
                             */
                            StringConnection robotCon = getConnection(seatedPlayer
                                    .getName());
                            robotCon.put(RobotDismiss.toCmd(gaName));

                            /**
                             * this connection has to wait for the robot to
                             * leave and then it can sit down
                             */
                            Map dismissals = (Map) robotDismissals.get(mes
                                    .getGame());
                            ReplaceRequest req = new ReplaceRequest(c,
                                    robotCon, mes);

                            if (dismissals == null)
                            {
                                dismissals = new HashMap();
                                robotDismissals.put(mes.getGame(), dismissals);
                            }
                            dismissals.put(robotCon, req);
                        }

                        canSit = false;
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e, "Exception in handleSITDOWN");
                }

                ga.releaseMonitor();

                /**
                 * if this is a robot, remove it from the request list
                 */
                Vector joinRequests = (Vector) robotJoinRequests.get(gaName);

                if (joinRequests != null)
                {
                    joinRequests.removeElement(c);
                }

                // D.ebugPrintln("canSit 2 = "+canSit);
                if (canSit)
                {
                    sitDown(ga, c, mes.getPlayerNumber(), mes.isRobot(), false);
                }
                else
                {
                    /**
                     * if the robot can't sit, tell it to go away. otherwise if
                     * game is full, tell the player.
                     */
                    if (mes.isRobot())
                    {
                        c.put(RobotDismiss.toCmd(gaName));
                    }
                    else if (gameIsFull)
                    {
                        messageToPlayer(c, gaName,
                                "This game is full, you cannot sit down.");
                    }
                }
            }
        }
    }

    /**
     * handle "put piece" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handlePUTPIECE(StringConnection c, PutPiece mes)
    {
        if (c == null)
            return;
        Game ga = gameList.getGameData(mes.getGame());
        if (ga == null)
            return;

        ga.takeMonitor();

        try
        {
            final String gaName = ga.getName();
            final String plName = (String) c.getData();
            Player player = ga.getPlayer(plName);

            /**
             * make sure the player can do it
             */
            if (checkTurn(c, ga))
            {
                boolean sendDenyReply = false;
                /*
                 * if (D.ebugOn) { D.ebugPrintln("BEFORE"); for (int pn = 0; pn
                 * < Game.MAXPLAYERS; pn++) { Player tmpPlayer =
                 * ga.getPlayer(pn); D.ebugPrintln("Player # "+pn); for (int i =
                 * 0x22; i < 0xCC; i++) { if (tmpPlayer.isPotentialRoad(i))
                 * D.ebugPrintln
                 * ("### POTENTIAL ROAD AT "+Integer.toHexString(i)); } } }
                 */

                final int gameState = ga.getGameState();
                final int coord = mes.getCoordinates();
                switch (mes.getPieceType())
                {
                case PlayingPiece.ROAD:

                    Road rd = new Road(player, coord, null);

                    if ((gameState == Game.START1B)
                            || (gameState == Game.START2B)
                            || (gameState == Game.PLACING_ROAD)
                            || (gameState == Game.PLACING_FREE_ROAD1)
                            || (gameState == Game.PLACING_FREE_ROAD2))
                    {
                        if (player.isPotentialRoad(coord))
                        {
                            ga.putPiece(rd); // Changes state and sometimes
                                             // player

                            /*
                             * if (D.ebugOn) { D.ebugPrintln("AFTER"); for (int
                             * pn = 0; pn < Game.MAXPLAYERS; pn++) { Player
                             * tmpPlayer = ga.getPlayer(pn);
                             * D.ebugPrintln("Player # "+pn); for (int i = 0x22;
                             * i < 0xCC; i++) { if
                             * (tmpPlayer.isPotentialRoad(i))
                             * D.ebugPrintln("### POTENTIAL ROAD AT "
                             * +Integer.toHexString(i)); } } }
                             */
                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new GameTextMsg(
                                    gaName, SERVERNAME, plName
                                            + " built a road."));
                            messageToGameWithMon(gaName, new PutPiece(gaName,
                                    player.getPlayerNumber(),
                                    PlayingPiece.ROAD, coord));
                            gameList.releaseMonitorForGame(gaName);
                            boolean toldRoll = sendGameState(ga, false);
                            broadcastGameStats(ga);
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            if (!checkTurn(c, ga))
                            {
                                // Player changed (or play started), announce
                                // new player.
                                sendTurn(ga, true);
                            }
                            else if (toldRoll)
                            {
                                // When play starts, or after placing 2nd free
                                // road,
                                // announce even though player unchanged,
                                // to trigger auto-roll for the player.
                                // If the client is too old (1.0.6), it will
                                // ignore the prompt.
                                messageToGame(gaName, new RollDicePrompt(
                                        gaName, player.getPlayerNumber()));
                            }
                        }
                        else
                        {
                            D.ebugPrintln("ILLEGAL ROAD: 0x"
                                    + Integer.toHexString(coord) + ": player "
                                    + player.getPlayerNumber());
                            messageToPlayer(c, gaName,
                                    "You can't build a road there.");
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName,
                                "You can't build a road right now.");
                    }

                    break;

                case PlayingPiece.SETTLEMENT:

                    Settlement se = new Settlement(player, coord, null);

                    if ((gameState == Game.START1A)
                            || (gameState == Game.START2A)
                            || (gameState == Game.PLACING_SETTLEMENT))
                    {
                        if (player.isPotentialSettlement(coord))
                        {
                            ga.putPiece(se); // Changes game state and (if game
                                             // start) player
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new GameTextMsg(
                                    gaName, SERVERNAME, plName
                                            + " built a settlement."));
                            messageToGameWithMon(gaName, new PutPiece(gaName,
                                    player.getPlayerNumber(),
                                    PlayingPiece.SETTLEMENT, coord));
                            gameList.releaseMonitorForGame(gaName);
                            broadcastGameStats(ga);
                            sendGameState(ga);

                            if (!checkTurn(c, ga))
                            {
                                sendTurn(ga, false); // Announce new current
                                                     // player.
                            }
                        }
                        else
                        {
                            D.ebugPrintln("ILLEGAL SETTLEMENT: 0x"
                                    + Integer.toHexString(coord) + ": player "
                                    + player.getPlayerNumber());
                            messageToPlayer(c, gaName,
                                    "You can't build a settlement there.");
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName,
                                "You can't build a settlement right now.");
                    }

                    break;

                case PlayingPiece.CITY:

                    City ci = new City(player, coord, null);

                    if (gameState == Game.PLACING_CITY)
                    {
                        if (player.isPotentialCity(coord))
                        {
                            ga.putPiece(ci); // changes game state and maybe
                                             // player
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new GameTextMsg(
                                    gaName, SERVERNAME, plName
                                            + " built a city."));
                            messageToGameWithMon(gaName, new PutPiece(gaName,
                                    player.getPlayerNumber(),
                                    PlayingPiece.CITY, coord));
                            gameList.releaseMonitorForGame(gaName);
                            broadcastGameStats(ga);
                            sendGameState(ga);

                            if (!checkTurn(c, ga))
                            {
                                sendTurn(ga, false); // announce new current
                                                     // player
                            }
                        }
                        else
                        {
                            D.ebugPrintln("ILLEGAL CITY: 0x"
                                    + Integer.toHexString(coord) + ": player "
                                    + player.getPlayerNumber());
                            messageToPlayer(c, gaName,
                                    "You can't build a city there.");
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName,
                                "You can't build a city right now.");
                    }

                    break;

                } // switch (mes.getPieceType())

                if (sendDenyReply)
                {
                    messageToPlayer(c, new CancelBuildRequest(gaName, mes
                            .getPieceType()));
                }
            }
            else
            {
                messageToPlayer(c, gaName, "It's not your turn.");
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception caught in handlePUTPIECE");
        }

        ga.releaseMonitor();
    }

    /**
     * handle "move robber" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleMOVEROBBER(StringConnection c, MoveRobber mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            Game ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    Player player = ga.getPlayer((String) c.getData());

                    /**
                     * make sure the player can do it
                     */
                    final String gaName = ga.getName();
                    if (ga.canMoveRobber(player.getPlayerNumber(), mes
                            .getCoordinates()))
                    {
                        MoveRobberResult result = ga.moveRobber(player
                                .getPlayerNumber(), mes.getCoordinates());
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        messageToGame(gaName, new MoveRobber(gaName, player
                                .getPlayerNumber(), mes.getCoordinates()));

                        Vector victims = result.getVictims();

                        /** only one possible victim */
                        if (victims.size() == 1)
                        {
                            /**
                             * report what was stolen
                             */
                            Player victim = (Player) victims.firstElement();
                            reportRobbery(ga, player, victim, result.getLoot());
                            ChoosePlayer robmes = new ChoosePlayer(gn, victim
                                    .getPlayerNumber());
                            recordGameEvent(robmes, robmes.getGame(), robmes
                                    .toCmd());
                        }
                        /** no victim */
                        else if (victims.size() == 0)
                        {
                            /**
                             * just say it was moved; nothing is stolen
                             */
                            messageToGame(gaName, (String) c.getData()
                                    + " moved the robber.");
                        }
                        else
                        {
                            /**
                             * else, the player needs to choose a victim
                             */
                            messageToGame(
                                    gaName,
                                    (String) c.getData()
                                            + " moved the robber, must choose a victim.");
                        }

                        sendGameState(ga);
                    }
                    else
                    {
                        messageToPlayer(c, gaName, "You can't move the robber.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintStackTrace(e, "Exception caught");
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "start game" message. Game state must be NEW, or this message is
     * ignored. {@link #readyGameAskRobotsJoin(Game, StringConnection[]) Ask
     * some robots} to fill empty seats, or {@link #startGame(Game) begin the
     * game} if no robots needed.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleSTARTGAME(StringConnection c, StartGame mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            Game ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (ga.getGameState() == Game.NEW)
                    {
                        boolean seatsFull = true;
                        boolean anyLocked = false;
                        int numEmpty = 0;
                        int numPlayers = 0;

                        //
                        // count the number of unlocked empty seats
                        //
                        for (int i = 0; i < ga.maxPlayers; i++)
                        {
                            if (ga.isSeatVacant(i))
                            {
                                if (ga.isSeatLocked(i))
                                {
                                    anyLocked = true;
                                }
                                else
                                {
                                    seatsFull = false;
                                    ++numEmpty;
                                }
                            }
                            else
                            {
                                ++numPlayers;
                            }
                        }

                        // Check vs max-players allowed in game (option "PL").
                        // Like seat locks, this can cause robots to be unwanted
                        // in otherwise-empty seats.
                        {
                            final int numAvail = ga.getAvailableSeatCount();
                            if (numAvail < numEmpty)
                            {
                                numEmpty = numAvail;
                                if (numEmpty == 0)
                                    seatsFull = true;
                            }
                        }

                        if (seatsFull && (numPlayers < 2))
                        {
                            seatsFull = false;
                            numEmpty = 3;
                            String m = "Sorry, the only player cannot lock all seats.";
                            messageToGame(gn, m);
                        }
                        else if (!seatsFull)
                        {
                            if (robots.isEmpty())
                            {
                                if (numPlayers < Game.MINPLAYERS)
                                {
                                    messageToGame(gn,
                                            "No robots on this server, please fill at least "
                                                    + Game.MINPLAYERS
                                                    + " seats before starting.");
                                }
                                else
                                {
                                    seatsFull = true; // Enough players to start
                                                      // game.
                                }
                            }
                            else
                            {
                                //
                                // make sure there are enough robots connected,
                                // then set gamestate READY and ask them to
                                // connect.
                                //
                                if (numEmpty > robots.size())
                                {
                                    String m;
                                    if (anyLocked)
                                        m = "Sorry, not enough robots to fill all the seats.  Only "
                                                + robots.size()
                                                + " robots are available.";
                                    else
                                        m = "Sorry, not enough robots to fill all the seats.  Lock some seats.  Only "
                                                + robots.size()
                                                + " robots are available.";
                                    messageToGame(gn, m);
                                }
                                else
                                {
                                    ga.setGameState(Game.READY);

                                    /**
                                     * Fill all the unlocked empty seats with
                                     * robots. Build a Vector of
                                     * StringConnections of robots asked to
                                     * join, and add it to the robotJoinRequests
                                     * table.
                                     */
                                    try
                                    {
                                        readyGameAskRobotsJoin(ga, null);
                                    }
                                    catch (IllegalStateException e)
                                    {
                                        String m = "Sorry, robots cannot join this game: "
                                                + e.getMessage();
                                        messageToGame(gn, m);
                                        System.err
                                                .println("Robot-join problem in game "
                                                        + gn + ": " + m);
                                    }
                                }
                            }
                        }

                        /**
                         * If this doesn't need robots, then start the game.
                         * Otherwise wait for them to sit before starting the
                         * game.
                         */
                        if (seatsFull)
                        {
                            startGame(ga);
                        }
                    }
                }
                catch (Throwable e)
                {
                    D.ebugPrintStackTrace(e, "Exception caught");
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * Fill all the unlocked empty seats with robots, by asking them to join.
     * Builds a Vector of StringConnections of robots asked to join, and adds it
     * to the robotJoinRequests table. Game state should be READY. At most
     * {@link Game#getAvailableSeatCount()} robots will be asked.
     *<P>
     * Called by {@link #handleSTARTGAME(StringConnection, StartGame)
     * handleSTARTGAME}, {@link #resetBoardAndNotify(String, int)
     * resetBoardAndNotify}.
     *<P>
     * Once the robots have all responded (from their own threads/clients) and
     * joined up, the game can begin.
     * 
     * @param ga
     *            Game to ask robots to join
     * @param robotSeats
     *            If robotSeats is null, robots are randomly selected. If
     *            non-null, a MAXPLAYERS-sized array of StringConnections. Any
     *            vacant non-locked seat, with index i, is filled with the robot
     *            whose connection is robotSeats[i]. Other indexes should be
     *            null, and won't be used.
     * 
     * @throws IllegalStateException
     *             if {@link Game#getGameState() ga.gamestate} is not READY, or
     *             if {@link Game#getClientVersionMinRequired() ga.version} is
     *             somehow newer than server's version (which is assumed to be
     *             robots' version).
     * @throws IllegalArgumentException
     *             if robotSeats is not null but wrong length, or if a robotSeat
     *             element is null but that seat wants a robot (vacant
     *             non-locked).
     */
    private void readyGameAskRobotsJoin(Game ga, StringConnection[] robotSeats)
            throws IllegalStateException, IllegalArgumentException
    {
        if (ga.getGameState() != Game.READY)
            throw new IllegalStateException("Game state not READY: "
                    + ga.getGameState());

        if (ga.getClientVersionMinRequired() > Version.versionNumber())
            throw new IllegalStateException(
                    "Game version somehow newer than server and robots, it's "
                            + ga.getClientVersionMinRequired());

        Vector robotRequests = null;

        int[] robotIndexes = null;
        if (robotSeats == null)
        {
            // shuffle the indexes to distribute load
            robotIndexes = robotShuffleForJoin();
        }
        else
        {
            // robotSeats not null: check length
            if (robotSeats.length != ga.maxPlayers)
                throw new IllegalArgumentException(
                        "robotSeats Length must be MAXPLAYERS");
        }

        final String gname = ga.getName();
        final Hashtable gopts = ga.getGameOptions();
        int seatsOpen = ga.getAvailableSeatCount();
        int idx = 0;
        StringConnection[] robotSeatsConns = new StringConnection[ga.maxPlayers];

        for (int i = 0; (i < ga.maxPlayers) && (seatsOpen > 0); i++)
        {
            if (ga.isSeatVacant(i) && !ga.isSeatLocked(i))
            {
                /**
                 * fetch a robot player
                 */
                if (idx < robots.size())
                {
                    messageToGame(gname, "Fetching a robot player...");

                    StringConnection robotConn;
                    if (robotSeats != null)
                    {
                        robotConn = robotSeats[i];
                        if (robotConn == null)
                            throw new IllegalArgumentException("robotSeats["
                                    + i + "] was needed but null");
                    }
                    else
                    {
                        robotConn = (StringConnection) robots
                                .get(robotIndexes[idx]);
                    }
                    idx++;
                    --seatsOpen;
                    robotSeatsConns[i] = robotConn;

                    /**
                     * record the request
                     */
                    D.ebugPrintln("@@@ JOIN GAME REQUEST for "
                            + (String) robotConn.getData());
                    if (robotRequests == null)
                        robotRequests = new Vector();
                    robotRequests.addElement(robotConn);
                }
            }
        }

        if (robotRequests != null)
        {
            // we know it isn't empty,
            // so add to the request table
            robotJoinRequests.put(gname, robotRequests);

            // now, make the requests
            for (int i = 0; i < ga.maxPlayers; ++i)
                if (robotSeatsConns[i] != null)
                    robotSeatsConns[i].put(JoinGameRequest.toCmd(gname, i,
                            gopts));
        }
    }

    /**
     * handle "roll dice" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleROLLDICE(StringConnection c, RollDice mes)
    {
        if (c != null)
        {
            final String gn = mes.getGame();
            Game ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String plName = (String) c.getData();
                    final Player pl = ga.getPlayer(plName);
                    if ((pl != null) && ga.canRollDice(pl.getPlayerNumber()))
                    {
                        /**
                         * Roll dice, distribute resources in game
                         */
                        IntPair dice = ga.rollDice();
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());

                        /**
                         * Send roll results and then text to client. Client
                         * expects to see DiceResult first, then text message;
                         * to reduce visual clutter, PlayerInterface.print
                         * expects text message to follow a certain format. If a
                         * 7 is rolled, sendGameState will also say who must
                         * discard (in a GAMETEXTMSG).
                         */
                        messageToGame(gn, new DiceResult(gn, ga
                                .getCurrentDice()));
                        messageToGame(gn, plName + " rolled a " + dice.getA()
                                + " and a " + dice.getB() + ".");
                        sendGameState(ga); // For 7, give visual feedback before
                                           // sending discard request

                        /**
                         * if the roll is not 7, tell players what they got
                         */
                        if (ga.getCurrentDice() != 7)
                        {
                            boolean noPlayersGained = true;
                            StringBuffer gainsText = new StringBuffer();

                            for (int i = 0; i < ga.maxPlayers; i++)
                            {
                                if (!ga.isSeatVacant(i))
                                {
                                    Player pli = ga.getPlayer(i);
                                    ResourceSet rsrcs = ga
                                            .getResourcesGainedFromRoll(pli, ga
                                                    .getCurrentDice());

                                    if (rsrcs.getTotal() != 0)
                                    {
                                        if (noPlayersGained)
                                        {
                                            noPlayersGained = false;
                                        }
                                        else
                                        {
                                            gainsText.append(" ");
                                        }

                                        gainsText.append(pli.getName());
                                        gainsText.append(" gets ");
                                        // Send PlayerElement messages,
                                        // build resource-text in gainsText.
                                        reportRsrcGainLoss(gn, rsrcs, false, i,
                                                -1, gainsText, null);
                                        gainsText.append(".");
                                    }

                                    //
                                    // send all resource info for accuracy
                                    //
                                    StringConnection playerCon = getConnection(pli
                                            .getName());
                                    if (playerCon != null)
                                    {
                                        // CLAY, ORE, SHEEP, WHEAT, WOOD
                                        ResourceSet resources = pli
                                                .getResources();
                                        for (int res = PlayerElement.CLAY; res <= PlayerElement.WOOD; ++res)
                                            messageToPlayer(
                                                    playerCon,
                                                    new PlayerElement(
                                                            ga.getName(),
                                                            i,
                                                            PlayerElement.SET,
                                                            res,
                                                            resources
                                                                    .getAmount(res)));
                                        messageToGame(
                                                ga.getName(),
                                                new ResourceCount(ga.getName(),
                                                        i, resources.getTotal()));
                                    }
                                } // if (! ga.isSeatVacant(i))
                            } // for (i)

                            String message;
                            if (noPlayersGained)
                            {
                                message = "No player gets anything.";
                            }
                            else
                            {
                                message = gainsText.toString();
                            }
                            messageToGame(gn, message);

                            /*
                             * if (D.ebugOn) { for (int i=0; i <
                             * Game.MAXPLAYERS; i++) { ResourceSet rsrcs =
                             * ga.getPlayer(i).getResources(); String
                             * resourceMessage = "PLAYER "+i+" RESOURCES: ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.CLAY)+" ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.ORE)+" ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.SHEEP)+" ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.WHEAT)+" ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.WOOD)+" ";
                             * resourceMessage +=
                             * rsrcs.getAmount(ResourceConstants.UNKNOWN)+" ";
                             * messageToGame(gn, new GameTextMsg(gn, SERVERNAME,
                             * resourceMessage)); } }
                             */
                        }
                        else
                        {
                            /**
                             * player rolled 7
                             */
                            for (int i = 0; i < ga.maxPlayers; i++)
                            {
                                if ((!ga.isSeatVacant(i))
                                        && (ga.getPlayer(i).getResources()
                                                .getTotal() > 7))
                                {
                                    // Request to discard half (round down)
                                    StringConnection con = getConnection(ga
                                            .getPlayer(i).getName());
                                    if (con != null)
                                    {
                                        con
                                                .put(DiscardRequest.toCmd(ga
                                                        .getName(), ga
                                                        .getPlayer(i)
                                                        .getResources()
                                                        .getTotal() / 2));
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gn, "You can't roll right now.");
                    }
                }
                catch (Exception e)
                {
                    if (D.ebugIsEnabled())
                    {
                        D.ebugPrintln("Exception in handleROLLDICE - " + e);
                        e.printStackTrace(System.out);
                    }
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discard" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleDISCARD(StringConnection c, Discard mes)
    {
        if (c != null)
        {
            final String gn = mes.getGame();
            Game ga = gameList.getGameData(gn);

            if (ga != null)
            {
                final Player player = ga.getPlayer((String) c.getData());
                final int pn;
                if (player != null)
                    pn = player.getPlayerNumber();
                else
                    pn = -1; // c's client no longer in the game

                ga.takeMonitor();
                try
                {
                    if (player == null)
                    {
                        // The catch block will print this out semi-nicely
                        throw new IllegalArgumentException(
                                "player not found in game");
                    }

                    if (ga.canDiscard(pn, mes.getResources()))
                    {
                        ga.discard(pn, mes.getResources());
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());

                        /**
                         * tell the player client that the player discarded the
                         * resources
                         */
                        reportRsrcGainLoss(gn, mes.getResources(), true, pn,
                                -1, null, c);

                        /**
                         * tell everyone else that the player discarded unknown
                         * resources
                         */
                        messageToGameExcept(gn, c, new PlayerElement(gn, pn,
                                PlayerElement.LOSE, PlayerElement.UNKNOWN, mes
                                        .getResources().getTotal()), true);
                        messageToGame(gn, (String) c.getData() + " discarded "
                                + mes.getResources().getTotal() + " resources.");

                        /**
                         * send the new state, or end turn if was marked earlier
                         * as forced
                         */
                        if ((ga.getGameState() != Game.PLAY1)
                                || !ga.isForcingEndTurn())
                        {
                            sendGameState(ga);
                        }
                        else
                        {
                            endGameTurn(ga, player); // already did
                                                     // ga.takeMonitor()
                        }
                    }
                    else
                    {
                        /**
                         * (TO-DO) there could be a better feedback message here
                         */
                        messageToPlayer(c, gn,
                                "You can't discard that many cards.");
                    }
                }
                catch (Throwable e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "end turn" message. This normally ends a player's normal turn
     * (phase {@link Game#PLAY1}). On the 6-player board, it ends their
     * placements during the {@link Game#SPECIAL_BUILDING Special Building
     * Phase}.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleENDTURN(StringConnection c, EndTurn mes)
    {
        if (c == null)
            return;
        Game ga = gameList.getGameData(mes.getGame());
        if (ga == null)
            return;

        final String gname = ga.getName();
        ga.takeMonitor();

        try
        {
            final String plName = (String) c.getData();
            if (ga.getGameState() == Game.OVER)
            {
                // Should not happen; is here just in case.
                Player pl = ga.getPlayer(plName);
                if (pl != null)
                {
                    String msg = ga.gameOverMessageToPlayer(pl);
                    // msg = "The game is over; you are the winner!";
                    // msg = "The game is over; <someone> won.";
                    // msg = "The game is over; no one won.";
                    messageToPlayer(c, gname, msg);
                }
            }
            else if (checkTurn(c, ga))
            {
                Player pl = ga.getPlayer(plName);
                if ((pl != null) && ga.canEndTurn(pl.getPlayerNumber()))
                {
                    endGameTurn(ga, pl);
                }
                else
                {
                    messageToPlayer(c, gname, "You can't end your turn yet.");
                }
            }
            else
            {
                messageToPlayer(c, gname, "It's not your turn.");
            }
        }
        catch (Exception e)
        {
            D.ebugPrintStackTrace(e, "Exception caught");
        }

        ga.releaseMonitor();
    }

    /**
     * Pre-checking already done, end the current player's turn in this game.
     * Alter game state and send messages to players. Calls
     * {@link Game#endTurn()}. On the 6-player board, this may begin the
     * {@link Game#SPECIAL_BUILDING Special Building Phase}, or end a player's
     * placements during that phase.
     *<P>
     * Assumes:
     * <UL>
     * <LI>ga.canEndTurn already called, to validate player
     * <LI>ga.takeMonitor already called (not the same as
     * {@link GameList#takeMonitorForGame(String)})
     * <LI>gamelist.takeMonitorForGame is NOT called, we do NOT have that
     * monitor
     * </UL>
     *<P>
     * As a special case, endTurn is used to begin the Special Building Phase
     * during the start of a player's own turn, if permitted. (Added in 1.1.09)
     * 
     * @param ga
     *            Game to end turn
     * @param pl
     *            Current player in <tt>ga</tt>, or null. Not needed except in
     *            SPECIAL_BUILDING. If null, will be determined within this
     *            method.
     */
    private void endGameTurn(Game ga, Player pl)
    {
        final String gname = ga.getName();

        if (ga.getGameState() == Game.SPECIAL_BUILDING)
        {
            final int cpn = ga.getCurrentPlayerNumber();
            if (pl == null)
                pl = ga.getPlayer(cpn);
            pl.setAskedSpecialBuild(false);
            messageToGame(gname, new PlayerElement(gname, cpn,
                    PlayerElement.SET, PlayerElement.ASK_SPECIAL_BUILD, 0));
        }

        boolean hadBoardResetRequest = (-1 != ga.getResetVoteRequester());

        /**
         * End the Turn:
         */

        ga.endTurn(); // May set state to OVER, if new player has enough points
                      // to win.
        // May begin or continue the Special Building Phase.

        /**
         * Send the results out:
         */

        if (hadBoardResetRequest)
        {
            // Cancel voting at end of turn
            messageToGame(gname, new ResetBoardReject(gname));
        }

        /**
         * send new state number; if game is now OVER, also send end-of-game
         * messages.
         */
        boolean wantsRollPrompt = sendGameState(ga, false);

        /**
         * clear any trade offers
         */
        gameList.takeMonitorForGame(gname);
        if (ga.clientVersionLowest >= ClearOffer.VERSION_FOR_CLEAR_ALL)
        {
            messageToGameWithMon(gname, new ClearOffer(gname, -1));
        }
        else
        {
            for (int i = 0; i < ga.maxPlayers; i++)
                messageToGameWithMon(gname, new ClearOffer(gname, i));
        }
        gameList.releaseMonitorForGame(gname);

        /**
         * send whose turn it is
         */
        sendTurn(ga, wantsRollPrompt);
        if (ga.getGameState() == Game.SPECIAL_BUILDING)
            messageToGame(gname, "Special building phase: "
                    + ga.getPlayer(ga.getCurrentPlayerNumber()).getName()
                    + "'s turn to place.");
    }

    /**
     * Try to force-end the current player's turn in this game. Alter game state
     * and send messages to players. Will call
     * {@link #endGameTurn(Game, Player)} if appropriate. Will send gameState
     * and current player (turn) to clients.
     *<P>
     * If the current player has lost connection, send the {@link LeaveGame
     * LEAVEGAME} message out <b>before</b> calling this method.
     *<P>
     * Assumes, as {@link #endGameTurn(Game, Player)} does:
     * <UL>
     * <LI>ga.canEndTurn already called, returned false
     * <LI>ga.takeMonitor already called (not the same as
     * {@link GameList#takeMonitorForGame(String)})
     * <LI>gamelist.takeMonitorForGame is NOT called, we do NOT have that
     * monitor
     * </UL>
     * 
     * @param ga
     *            Game to force end turn
     * @param plName
     *            Current player's name. Needed because if they have been
     *            disconnected by
     *            {@link #leaveGame(StringConnection, String, boolean)}, their
     *            name within game object is already null.
     * 
     * @see Game#forceEndTurn()
     */
    private void forceEndGameTurn(Game ga, final String plName)
    {
        final String gaName = ga.getName();
        final int cpn = ga.getCurrentPlayerNumber();

        Player cp = ga.getPlayer(cpn);
        if (cp.hasAskedSpecialBuild())
        {
            cp.setAskedSpecialBuild(false);
            messageToGame(gaName, new PlayerElement(gaName, cpn,
                    PlayerElement.SET, PlayerElement.ASK_SPECIAL_BUILD, 0));
        }

        final ForceEndTurnResult res = ga.forceEndTurn();
        // State now hopefully PLAY1, or SPECIAL_BUILDING;
        // also could be initial placement (START1A or START2A).

        /**
         * report any resources lost, gained
         */
        ResourceSet resGainLoss = res.getResourcesGainedLost();
        if (resGainLoss != null)
        {
            /**
             * If returning resources to player (not discarding), report actual
             * types/amounts. For discard, tell the discarding player's client
             * that they discarded the resources, tell everyone else that the
             * player discarded unknown resources.
             */
            if (!res.isLoss())
                reportRsrcGainLoss(gaName, resGainLoss, false, cpn, -1, null,
                        null);
            else
            {
                StringConnection c = getConnection(plName);
                if ((c != null) && c.isConnected())
                    reportRsrcGainLoss(gaName, resGainLoss, true, cpn, -1,
                            null, c);
                int totalRes = resGainLoss.getTotal();
                messageToGameExcept(gaName, c, new PlayerElement(gaName, cpn,
                        PlayerElement.LOSE, PlayerElement.UNKNOWN, totalRes),
                        true);
                messageToGame(gaName, plName + " discarded " + totalRes
                        + " resources.");
            }
        }

        /**
         * report any dev-card returned to player's hand
         */
        int card = res.getDevCardType();
        if (card != -1)
        {
            StringConnection c = getConnection(plName);
            if ((c != null) && c.isConnected())
                messageToPlayer(c, new DevCard(gaName, cpn, DevCard.ADDOLD,
                        card));
            messageToGameExcept(gaName, c, new DevCard(gaName, cpn,
                    DevCard.ADDOLD, DevCardConstants.UNKNOWN), true);
            messageToGame(gaName, plName
                    + "'s just-played development card was returned.");
        }

        /**
         * For initial placements, we don't end turns as normal. (Player number
         * may go forward or backwards, new state isn't PLAY, etc.) Update
         * clients' gamestate, but don't call endGameTurn.
         */
        final int forceRes = res.getResult();
        if ((forceRes == ForceEndTurnResult.FORCE_ENDTURN_SKIP_START_ADV)
                || (forceRes == ForceEndTurnResult.FORCE_ENDTURN_SKIP_START_ADVBACK))
        {
            if (res.didUpdateFP() || res.didUpdateLP())
            {
                // will cause clients to recalculate lastPlayer too
                messageToGame(gaName, new FirstPlayer(gaName, ga
                        .getFirstPlayer()));
            }
            sendGameState(ga, false);
            sendTurn(ga, false);
            return; // <--- Early return ---
        }

        /**
         * If the turn can now end, proceed as if player requested it.
         * Otherwise, send current gamestate. We'll all wait for other players
         * to send discard messages, and afterwards this turn can end.
         */
        if (ga.canEndTurn(cpn))
            endGameTurn(ga, null);
        else
            sendGameState(ga, false);
    }

    /**
     * handle "choose player" message during robbery
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleCHOOSEPLAYER(StringConnection c, ChoosePlayer mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canChoosePlayer(mes.getChoice()))
                        {
                            int rsrc = ga.stealFromPlayer(mes.getChoice());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            reportRobbery(ga, ga
                                    .getPlayer((String) c.getData()), ga
                                    .getPlayer(mes.getChoice()), rsrc);
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, ga.getName(),
                                    "You can't steal from that player.");
                        }
                    }
                    else
                    {
                        messageToPlayer(c, ga.getName(), "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "make offer" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleMAKEOFFER(StringConnection c, MakeOffer mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                final String gaName = ga.getName();
                if (ga.isGameOptionSet("NT"))
                {
                    messageToPlayer(c, gaName,
                            "Trading is not allowed in this game.");
                    return; // <---- Early return: No Trading ----
                }

                ga.takeMonitor();

                try
                {
                    TradeOffer offer = mes.getOffer();

                    /**
                     * remake the offer with data that we know is accurate,
                     * namely the 'from' datum
                     */
                    Player player = ga.getPlayer((String) c.getData());

                    /**
                     * announce the offer, including text message similar to
                     * bank/port trade.
                     */
                    if (player != null)
                    {
                        TradeOffer remadeOffer;
                        {
                            ResourceSet offGive = offer.getGiveSet(), offGet = offer
                                    .getGetSet();
                            remadeOffer = new TradeOffer(gaName, player
                                    .getPlayerNumber(), offer.getTo(), offGive,
                                    offGet);
                            player.setCurrentOffer(remadeOffer);
                            StringBuffer offMsgText = new StringBuffer(
                                    (String) c.getData());
                            offMsgText.append(" made an offer to trade ");
                            offGive.toFriendlyString(offMsgText);
                            offMsgText.append(" for ");
                            offGet.toFriendlyString(offMsgText);
                            offMsgText.append('.');
                            messageToGame(gaName, offMsgText.toString());
                        }

                        MakeOffer makeOfferMessage = new MakeOffer(gaName,
                                remadeOffer);
                        messageToGame(gaName, makeOfferMessage);

                        recordGameEvent(gaName, makeOfferMessage.toCmd());

                        /**
                         * clear all the trade messages because a new offer has
                         * been made
                         */
                        gameList.takeMonitorForGame(gaName);
                        for (int i = 0; i < ga.maxPlayers; i++)
                        {
                            messageToGameWithMon(gaName, new ClearTradeMsg(
                                    gaName, i));
                        }
                        gameList.releaseMonitorForGame(gaName);
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "clear offer" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleCLEAROFFER(StringConnection c, ClearOffer mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    ga.getPlayer((String) c.getData()).setCurrentOffer(null);
                    messageToGame(gaName, new ClearOffer(gaName, ga.getPlayer(
                            (String) c.getData()).getPlayerNumber()));
                    recordGameEvent(mes.getGame(), mes.toCmd());

                    /**
                     * clear all the trade messages
                     */
                    for (int i = 0; i < ga.maxPlayers; i++)
                    {
                        messageToGame(gaName, new ClearTradeMsg(gaName, i));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "reject offer" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleREJECTOFFER(StringConnection c, RejectOffer mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                Player player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    final String gaName = ga.getName();
                    RejectOffer rejectMessage = new RejectOffer(gaName, player
                            .getPlayerNumber());
                    messageToGame(gaName, rejectMessage);

                    recordGameEvent(gaName, rejectMessage.toCmd());
                }
            }
        }
    }

    /**
     * handle "accept offer" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleACCEPTOFFER(StringConnection c, AcceptOffer mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    Player player = ga.getPlayer((String) c.getData());

                    if (player != null)
                    {
                        final int acceptingNumber = player.getPlayerNumber();
                        final int offeringNumber = mes.getOfferingNumber();
                        final String gaName = ga.getName();

                        if (ga.canMakeTrade(offeringNumber, acceptingNumber))
                        {
                            ga.makeTrade(offeringNumber, acceptingNumber);
                            reportTrade(ga, offeringNumber, acceptingNumber);

                            recordGameEvent(mes.getGame(), mes.toCmd());

                            /**
                             * clear all offers
                             */
                            for (int i = 0; i < ga.maxPlayers; i++)
                            {
                                ga.getPlayer(i).setCurrentOffer(null);
                            }
                            gameList.takeMonitorForGame(gaName);
                            if (ga.clientVersionLowest >= ClearOffer.VERSION_FOR_CLEAR_ALL)
                            {
                                messageToGameWithMon(gaName, new ClearOffer(
                                        gaName, -1));
                            }
                            else
                            {
                                for (int i = 0; i < ga.maxPlayers; i++)
                                    messageToGameWithMon(gaName,
                                            new ClearOffer(gaName, i));
                            }
                            gameList.releaseMonitorForGame(gaName);

                            /**
                             * send a message to the bots that the offer was
                             * accepted
                             */
                            messageToGame(gaName, mes);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "You can't make that trade.");
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "bank trade" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleBANKTRADE(StringConnection c, BankTrade mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canMakeBankTrade(mes.getGiveSet(), mes
                                .getGetSet()))
                        {
                            ga.makeBankTrade(mes.getGiveSet(), mes.getGetSet());
                            reportBankTrade(ga, mes.getGiveSet(), mes
                                    .getGetSet());

                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        }
                        else
                        {
                            messageToPlayer(c, ga.getName(),
                                    "You can't make that trade.");
                        }
                    }
                    else
                    {
                        messageToPlayer(c, ga.getName(), "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "build request" message. If client is current player, they want to
     * buy a {@link PlayingPiece}. Otherwise, if 6-player board, they want to
     * build during the {@link Game#SPECIAL_BUILDING Special Building Phase}.
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleBUILDREQUEST(StringConnection c, BuildRequest mes)
    {
        if (c == null)
            return;
        Game ga = gameList.getGameData(mes.getGame());
        if (ga == null)
            return;

        final String gaName = ga.getName();
        ga.takeMonitor();

        try
        {
            final boolean isCurrent = checkTurn(c, ga);
            Player player = ga.getPlayer((String) c.getData());
            final int pn = player.getPlayerNumber();
            final int pieceType = mes.getPieceType();
            boolean sendDenyReply = false; // for robots' benefit

            if (isCurrent)
            {
                if ((ga.getGameState() == Game.PLAY1)
                        || (ga.getGameState() == Game.SPECIAL_BUILDING))
                {
                    switch (pieceType)
                    {
                    case PlayingPiece.ROAD:

                        if (ga.couldBuildRoad(pn))
                        {
                            ga.buyRoad(pn);
                            messageToGame(gaName, new PlayerElement(gaName, pn,
                                    PlayerElement.LOSE, PlayerElement.CLAY, 1));
                            messageToGame(gaName, new PlayerElement(gaName, pn,
                                    PlayerElement.LOSE, PlayerElement.WOOD, 1));
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "You can't build a road.");
                            sendDenyReply = true;
                        }

                        break;

                    case PlayingPiece.SETTLEMENT:

                        if (ga.couldBuildSettlement(pn))
                        {
                            ga.buySettlement(pn);
                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new PlayerElement(
                                    gaName, pn, PlayerElement.LOSE,
                                    PlayerElement.CLAY, 1));
                            messageToGameWithMon(gaName, new PlayerElement(
                                    gaName, pn, PlayerElement.LOSE,
                                    PlayerElement.SHEEP, 1));
                            messageToGameWithMon(gaName, new PlayerElement(
                                    gaName, pn, PlayerElement.LOSE,
                                    PlayerElement.WHEAT, 1));
                            messageToGameWithMon(gaName, new PlayerElement(
                                    gaName, pn, PlayerElement.LOSE,
                                    PlayerElement.WOOD, 1));
                            gameList.releaseMonitorForGame(gaName);
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "You can't build a settlement.");
                            sendDenyReply = true;
                        }

                        break;

                    case PlayingPiece.CITY:

                        if (ga.couldBuildCity(pn))
                        {
                            ga.buyCity(pn);
                            messageToGame(ga.getName(), new PlayerElement(ga
                                    .getName(), pn, PlayerElement.LOSE,
                                    PlayerElement.ORE, 3));
                            messageToGame(ga.getName(), new PlayerElement(ga
                                    .getName(), pn, PlayerElement.LOSE,
                                    PlayerElement.WHEAT, 2));
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "You can't build a city.");
                            sendDenyReply = true;
                        }

                        break;
                    }
                }
                else if (pieceType == -1)
                {
                    // 6-player board: Special Building Phase
                    // during start of own turn
                    try
                    {
                        ga.askSpecialBuild(pn, true);
                        messageToGame(gaName, new PlayerElement(gaName, pn,
                                PlayerElement.SET,
                                PlayerElement.ASK_SPECIAL_BUILD, 1));
                        endGameTurn(ga, player); // triggers start of SBP
                    }
                    catch (IllegalStateException e)
                    {
                        messageToPlayer(c, gaName,
                                "You can't ask to build now.");
                        sendDenyReply = true;
                    }
                }
                else
                {
                    messageToPlayer(c, gaName, "You can't build now.");
                    sendDenyReply = true;
                }
            }
            else
            {
                if (ga.maxPlayers <= 4)
                {
                    messageToPlayer(c, gaName, "It's not your turn.");
                    sendDenyReply = true;
                }
                else
                {
                    // 6-player board: Special Building Phase
                    // during other player's turn
                    try
                    {
                        ga.askSpecialBuild(pn, true); // will validate that they
                                                      // can build now
                        messageToGame(gaName, new PlayerElement(gaName, pn,
                                PlayerElement.SET,
                                PlayerElement.ASK_SPECIAL_BUILD, 1));
                    }
                    catch (IllegalStateException e)
                    {
                        messageToPlayer(c, gaName,
                                "You can't ask to build now.");
                        sendDenyReply = true;
                    }
                }
            }

            if (sendDenyReply && ga.getPlayer(pn).isRobot())
            {
                messageToPlayer(c, new CancelBuildRequest(gaName, pieceType));
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception caught - " + e);
            e.printStackTrace();
        }

        ga.releaseMonitor();
    }

    /**
     * handle "cancel build request" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleCANCELBUILDREQUEST(StringConnection c,
            CancelBuildRequest mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        Player player = ga.getPlayer((String) c.getData());

                        switch (mes.getPieceType())
                        {
                        case PlayingPiece.ROAD:

                            if (ga.getGameState() == Game.PLACING_ROAD)
                            {
                                ga.cancelBuildRoad(player.getPlayerNumber());
                                messageToGame(gaName, new PlayerElement(gaName,
                                        player.getPlayerNumber(),
                                        PlayerElement.GAIN, PlayerElement.CLAY,
                                        1));
                                messageToGame(gaName, new PlayerElement(gaName,
                                        player.getPlayerNumber(),
                                        PlayerElement.GAIN, PlayerElement.WOOD,
                                        1));
                                sendGameState(ga);
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You didn't buy a road.");
                            }

                            break;

                        case PlayingPiece.SETTLEMENT:

                            if (ga.getGameState() == Game.PLACING_SETTLEMENT)
                            {
                                ga.cancelBuildSettlement(player
                                        .getPlayerNumber());
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, player.getPlayerNumber(),
                                        PlayerElement.GAIN, PlayerElement.CLAY,
                                        1));
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, player.getPlayerNumber(),
                                        PlayerElement.GAIN,
                                        PlayerElement.SHEEP, 1));
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, player.getPlayerNumber(),
                                        PlayerElement.GAIN,
                                        PlayerElement.WHEAT, 1));
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, player.getPlayerNumber(),
                                        PlayerElement.GAIN, PlayerElement.WOOD,
                                        1));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else if ((ga.getGameState() == Game.START1B)
                                    || (ga.getGameState() == Game.START2B))
                            {
                                Settlement pp = new Settlement(player, player
                                        .getLastSettlementCoord(), null);
                                ga.undoPutInitSettlement(pp);
                                messageToGame(gaName, mes); // Re-send to all
                                                            // clients to
                                                            // announce it
                                // (Safe since we've validated all message
                                // parameters)
                                messageToGame(
                                        gaName,
                                        player.getName()
                                                + " cancelled this settlement placement.");
                                sendGameState(ga); // This send is redundant, if
                                                   // client reaction changes
                                                   // game state
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You didn't buy a settlement.");
                            }

                            break;

                        case PlayingPiece.CITY:

                            if (ga.getGameState() == Game.PLACING_CITY)
                            {
                                ga.cancelBuildCity(player.getPlayerNumber());
                                messageToGame(gaName, new PlayerElement(gaName,
                                        player.getPlayerNumber(),
                                        PlayerElement.GAIN, PlayerElement.ORE,
                                        3));
                                messageToGame(gaName, new PlayerElement(gaName,
                                        player.getPlayerNumber(),
                                        PlayerElement.GAIN,
                                        PlayerElement.WHEAT, 2));
                                sendGameState(ga);
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You didn't buy a city.");
                            }

                            break;
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName, "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "buy card request" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleBUYCARDREQUEST(StringConnection c, BuyCardRequest mes)
    {
        if (c == null)
            return;
        Game ga = gameList.getGameData(mes.getGame());
        if (ga == null)
            return;

        ga.takeMonitor();

        try
        {
            final String gaName = ga.getName();
            Player player = ga.getPlayer((String) c.getData());
            final int pn = player.getPlayerNumber();
            boolean sendDenyReply = false; // for robots' benefit

            if (checkTurn(c, ga))
            {
                if (((ga.getGameState() == Game.PLAY1) || (ga.getGameState() == Game.SPECIAL_BUILDING))
                        && (ga.couldBuyDevCard(pn)))
                {
                    int card = ga.buyDevCard();
                    gameList.takeMonitorForGame(gaName);
                    messageToGameWithMon(gaName, new PlayerElement(gaName, pn,
                            PlayerElement.LOSE, PlayerElement.ORE, 1));
                    messageToGameWithMon(gaName, new PlayerElement(gaName, pn,
                            PlayerElement.LOSE, PlayerElement.SHEEP, 1));
                    messageToGameWithMon(gaName, new PlayerElement(gaName, pn,
                            PlayerElement.LOSE, PlayerElement.WHEAT, 1));
                    messageToGameWithMon(gaName, new DevCardCount(gaName, ga
                            .getNumDevCards()));
                    gameList.releaseMonitorForGame(gaName);
                    messageToPlayer(c, new DevCard(gaName, pn, DevCard.DRAW,
                            card));

                    messageToGameExcept(gaName, c, new DevCard(gaName, pn,
                            DevCard.DRAW, DevCardConstants.UNKNOWN), true);
                    messageToGame(gaName, (String) c.getData()
                            + " bought a development card.");

                    if (ga.getNumDevCards() > 1)
                    {
                        messageToGame(gaName, "There are "
                                + ga.getNumDevCards() + " cards left.");
                    }
                    else if (ga.getNumDevCards() == 1)
                    {
                        messageToGame(gaName, "There is 1 card left.");
                    }
                    else
                    {
                        messageToGame(gaName,
                                "There are no more Development cards.");
                    }

                    sendGameState(ga);
                }
                else
                {
                    if (ga.getNumDevCards() == 0)
                    {
                        messageToPlayer(c, gaName,
                                "There are no more Development cards.");
                    }
                    else
                    {
                        messageToPlayer(c, gaName,
                                "You can't buy a development card now.");
                    }
                    sendDenyReply = true;
                }
            }
            else
            {
                if (ga.maxPlayers <= 4)
                {
                    messageToPlayer(c, gaName, "It's not your turn.");
                }
                else
                {
                    // 6-player board: Special Building Phase
                    try
                    {
                        ga.askSpecialBuild(pn, true);
                        messageToGame(gaName, new PlayerElement(gaName, pn,
                                PlayerElement.SET,
                                PlayerElement.ASK_SPECIAL_BUILD, 1));
                    }
                    catch (IllegalStateException e)
                    {
                        messageToPlayer(c, gaName,
                                "You can't ask to buy a card now.");
                    }
                }
                sendDenyReply = true;
            }

            if (sendDenyReply && ga.getPlayer(pn).isRobot())
            {
                messageToPlayer(c, new CancelBuildRequest(gaName, -2));
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception caught - " + e);
            e.printStackTrace();
        }

        ga.releaseMonitor();
    }

    /**
     * handle "play development card request" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handlePLAYDEVCARDREQUEST(StringConnection c,
            PlayDevCardRequest mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        Player player = ga.getPlayer((String) c.getData());

                        switch (mes.getDevCard())
                        {
                        case DevCardConstants.KNIGHT:

                            if (ga.canPlayKnight(player.getPlayerNumber()))
                            {
                                ga.playKnight();
                                recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new GameTextMsg(
                                        gaName, SERVERNAME, player.getName()
                                                + " played a Soldier card."));
                                messageToGameWithMon(gaName, new DevCard(
                                        gaName, player.getPlayerNumber(),
                                        DevCard.PLAY, DevCardConstants.KNIGHT));
                                messageToGameWithMon(gaName,
                                        new SetPlayedDevCard(gaName, player
                                                .getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, player.getPlayerNumber(),
                                        PlayerElement.GAIN,
                                        PlayerElement.NUMKNIGHTS, 1));
                                gameList.releaseMonitorForGame(gaName);
                                broadcastGameStats(ga);
                                sendGameState(ga);
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You can't play a Soldier card now.");
                            }

                            break;

                        case DevCardConstants.ROADS:

                            if (ga
                                    .canPlayRoadBuilding(player
                                            .getPlayerNumber()))
                            {
                                ga.playRoadBuilding();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new DevCard(
                                        gaName, player.getPlayerNumber(),
                                        DevCard.PLAY, DevCardConstants.ROADS));
                                messageToGameWithMon(gaName,
                                        new SetPlayedDevCard(gaName, player
                                                .getPlayerNumber(), true));
                                messageToGameWithMon(
                                        gaName,
                                        new GameTextMsg(
                                                gaName,
                                                SERVERNAME,
                                                player.getName()
                                                        + " played a Road Building card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                                if (ga.getGameState() == Game.PLACING_FREE_ROAD1)
                                {
                                    messageToPlayer(c, gaName,
                                            "You may place 2 roads.");
                                }
                                else
                                {
                                    messageToPlayer(c, gaName,
                                            "You may place your 1 remaining road.");
                                }
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You can't play a Road Building card now.");
                            }

                            break;

                        case DevCardConstants.DISC:

                            if (ga.canPlayDiscovery(player.getPlayerNumber()))
                            {
                                ga.playDiscovery();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new DevCard(
                                        gaName, player.getPlayerNumber(),
                                        DevCard.PLAY, DevCardConstants.DISC));
                                messageToGameWithMon(gaName,
                                        new SetPlayedDevCard(gaName, player
                                                .getPlayerNumber(), true));
                                messageToGameWithMon(
                                        gaName,
                                        new GameTextMsg(
                                                gaName,
                                                SERVERNAME,
                                                player.getName()
                                                        + " played a Year of Plenty card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You can't play a Year of Plenty card now.");
                            }

                            break;

                        case DevCardConstants.MONO:

                            if (ga.canPlayMonopoly(player.getPlayerNumber()))
                            {
                                ga.playMonopoly();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new DevCard(
                                        gaName, player.getPlayerNumber(),
                                        DevCard.PLAY, DevCardConstants.MONO));
                                messageToGameWithMon(gaName,
                                        new SetPlayedDevCard(gaName, player
                                                .getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new GameTextMsg(
                                        gaName, SERVERNAME, player.getName()
                                                + " played a Monopoly card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else
                            {
                                messageToPlayer(c, gaName,
                                        "You can't play a Monopoly card now.");
                            }

                            break;

                        // VP cards are secretly played when bought.
                        // (case DevCardConstants.CAP, LIB, UNIV, TEMP, TOW):
                        // If player clicks "Play Card" the message is handled
                        // at the
                        // client, in HandPanel.actionPerformed case CARD.
                        // "You secretly played this VP card when you bought it."
                        // break;

                        default:
                            D
                                    .ebugPrintln("* SOCServer.handlePLAYDEVCARDREQUEST: asked to play unhandled type "
                                            + mes.getDevCard());

                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName, "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discovery pick" message (while playing Discovery card)
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleDISCOVERYPICK(StringConnection c, DiscoveryPick mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        Player player = ga.getPlayer((String) c.getData());

                        if (ga.canDoDiscoveryAction(mes.getResources()))
                        {
                            ga.doDiscoveryAction(mes.getResources());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            StringBuffer message = new StringBuffer((String) c
                                    .getData());
                            message.append(" received ");
                            reportRsrcGainLoss(gaName, mes.getResources(),
                                    false, player.getPlayerNumber(), -1,
                                    message, null);
                            message.append(" from the bank.");
                            messageToGame(gaName, message.toString());
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "That is not a legal Year of Plenty pick.");
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName, "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "monopoly pick" message
     * 
     * @param c
     *            the connection that sent the message
     * @param mes
     *            the messsage
     */
    private void handleMONOPOLYPICK(StringConnection c, MonopolyPick mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        if (ga.canDoMonopolyAction())
                        {
                            int[] monoPicks = ga.doMonopolyAction(mes
                                    .getResource());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            final String monoPlayerName = (String) c.getData();
                            final String resName = " "
                                    + ResourceConstants.resName(mes
                                            .getResource()) + ".";
                            String message = monoPlayerName + " monopolized"
                                    + resName;

                            gameList.takeMonitorForGame(gaName);
                            messageToGameExcept(gaName, c, new GameTextMsg(
                                    gaName, SERVERNAME, message), false);

                            /**
                             * just send all the player's resource counts for
                             * the monopolized resource
                             */
                            for (int i = 0; i < ga.maxPlayers; i++)
                            {
                                /**
                                 * Note: This only works if PlayerElement.CLAY
                                 * == ResourceConstants.CLAY
                                 */
                                messageToGameWithMon(gaName, new PlayerElement(
                                        gaName, i, PlayerElement.SET, mes
                                                .getResource(), ga.getPlayer(i)
                                                .getResources().getAmount(
                                                        mes.getResource())));
                            }
                            gameList.releaseMonitorForGame(gaName);

                            /**
                             * now that monitor is released, notify the
                             * victim(s) of resource amounts taken, and tell the
                             * player how many they won.
                             */
                            int monoTotal = 0;
                            for (int i = 0; i < ga.maxPlayers; i++)
                            {
                                int picked = monoPicks[i];
                                if (picked == 0)
                                    continue;
                                monoTotal += picked;
                                String viName = ga.getPlayer(i).getName();
                                StringConnection viCon = getConnection(viName);
                                if (viCon != null)
                                    messageToPlayer(viCon, gaName,
                                            monoPlayerName
                                                    + "'s Monopoly took your "
                                                    + picked + resName);
                            }

                            messageToPlayer(c, gaName, "You monopolized "
                                    + monoTotal + resName);
                            sendGameState(ga);
                        }
                        else
                        {
                            messageToPlayer(c, gaName,
                                    "You can't do a Monopoly pick now.");
                        }
                    }
                    else
                    {
                        messageToPlayer(c, gaName, "It's not your turn.");
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "change face" message
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleCHANGEFACE(StringConnection c, ChangeFace mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                Player player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    final String gaName = mes.getGame();
                    player.setFaceId(mes.getFaceId());
                    messageToGame(gaName, new ChangeFace(gaName, player
                            .getPlayerNumber(), mes.getFaceId()));
                }
            }
        }
    }

    /**
     * handle "set seat lock" message
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleSETSEATLOCK(StringConnection c, SetSeatLock mes)
    {
        if (c != null)
        {
            Game ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                Player player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    if (mes.getLockState() == true)
                    {
                        ga.lockSeat(mes.getPlayerNumber());
                    }
                    else
                    {
                        ga.unlockSeat(mes.getPlayerNumber());
                    }

                    messageToGame(mes.getGame(), mes);
                }
            }
        }
    }

    /**
     * handle "reset-board request" message. If multiple human players, start a
     * vote. Otherwise, reset the game to a copy with same name and (copy of)
     * same players, new layout.
     *<P>
     * The requesting player doesn't vote, but server still sends the
     * vote-request-message, to tell that client their request was accepted and
     * voting has begun.
     *<P>
     * If only one player remains (all other humans have left at end), ask them
     * to start a new game instead. This is a rare occurrence and we shouldn't
     * bring in new robots and all, since we already have an interface to set up
     * a game.
     *<P>
     * If any human player's client is too old to vote for reset, assume they
     * vote yes.
     * 
     * @see #resetBoardAndNotify(String, int)
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleRESETBOARDREQUEST(StringConnection c,
            ResetBoardRequest mes)
    {
        if (c == null)
            return;
        String gaName = mes.getGame();
        Game ga = gameList.getGameData(gaName);
        if (ga == null)
            return;
        Player reqPlayer = ga.getPlayer((String) c.getData());
        if (reqPlayer == null)
        {
            return; // Not playing in that game (Security)
        }

        /**
         * Is voting already active from another player? Or, has this player
         * already asked for voting this turn?
         */
        if (ga.getResetVoteActive() || reqPlayer.hasAskedBoardReset())
        {
            // Ignore this second request. Can't send REJECT because
            // that would end the already-active round of voting.
            return;
        }

        /**
         * Is there more than one human player? Grab connection information for
         * humans and robots.
         */
        StringConnection[] humanConns = new StringConnection[ga.maxPlayers];
        StringConnection[] robotConns = new StringConnection[ga.maxPlayers];
        int numHuman = GameBoardReset.sortPlayerConnections(ga, null, gameList
                .getMembers(gaName), humanConns, robotConns);

        final int reqPN = reqPlayer.getPlayerNumber();
        if (numHuman < 2)
        {
            // Are there robots? Go ahead and reset if so.
            boolean hadRobot = false;
            for (int i = robotConns.length - 1; i >= 0; --i)
            {
                if (robotConns[i] != null)
                {
                    hadRobot = true;
                    break;
                }
            }
            if (hadRobot)
            {
                resetBoardAndNotify(gaName, reqPN);
            }
            else
            {
                messageToGameUrgent(gaName,
                        "Everyone has left this game. Please start a new game with players or bots.");
            }
        }
        else
        {
            // Probably put it to a vote.
            gameList.takeMonitorForGame(gaName);

            // First, Count number of other players who can vote (connected,
            // version chk)
            int votingPlayers = 0;
            for (int i = ga.maxPlayers - 1; i >= 0; --i)
            {
                if ((i != reqPN) && !ga.isSeatVacant(i))
                {
                    StringConnection pc = getConnection(ga.getPlayer(i)
                            .getName());
                    if ((pc != null) && pc.isConnected()
                            && pc.getVersion() >= 1100)
                    {
                        ++votingPlayers;
                    }
                }
            }

            if (votingPlayers == 0)
            {
                // No one else is capable of voting.
                // Reset the game immediately.
                messageToGameWithMon(
                        gaName,
                        new GameTextMsg(
                                gaName,
                                SERVERNAME,
                                ">>> "
                                        + (String) c.getData()
                                        + " is resetting the game - other connected players are unable to vote (client too old)."));
                gameList.releaseMonitorForGame(gaName);
                resetBoardAndNotify(gaName, reqPN);
            }
            else
            {
                // Put it to a vote
                messageToGameWithMon(
                        gaName,
                        new GameTextMsg(
                                gaName,
                                SERVERNAME,
                                (String) c.getData()
                                        + " requests a board reset - other players please vote."));
                String vrCmd = ResetBoardVoteRequest.toCmd(gaName, reqPN);
                ga.resetVoteBegin(reqPN);
                gameList.releaseMonitorForGame(gaName);
                for (int i = 0; i < ga.maxPlayers; ++i)
                    if (humanConns[i] != null)
                    {
                        if (humanConns[i].getVersion() >= 1100)
                            humanConns[i].put(vrCmd);
                        else
                            ga.resetVoteRegister(ga.getPlayer(
                                    (String) (humanConns[i].getData()))
                                    .getPlayerNumber(), true);
                    }
            }
        }
    }

    /**
     * handle message of player's vote for a "reset-board" request. Register the
     * player's vote. If all votes have now arrived, and the vote is unanimous,
     * reset the game to a copy with same name and players, new layout.
     * 
     * @see #resetBoardAndNotify(String, int)
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleRESETBOARDVOTE(StringConnection c, ResetBoardVote mes)
    {
        if (c == null)
            return;
        Game ga = gameList.getGameData(mes.getGame());
        if (ga == null)
            return;
        final String plName = (String) c.getData();
        Player reqPlayer = ga.getPlayer(plName);
        if (reqPlayer == null)
        {
            return; // Not playing in that game (Security)
        }

        // Register this player's vote, and let game members know.
        // If vote succeeded, go ahead and reset the game.
        // If vote rejected, let everyone know.

        resetBoardVoteNotifyOne(ga, reqPlayer.getPlayerNumber(), plName, mes
                .getPlayerVote());
    }

    /**
     * "Reset-board" request: Register one player's vote, and let game members
     * know. If vote succeeded, go ahead and reset the game. If vote rejected,
     * let everyone know.
     * 
     * @param ga
     *            Game for this reset vote
     * @param pn
     *            Player number who is voting
     * @param plName
     *            Name of player who is voting
     * @param vyes
     *            Player's vote, Yes or no
     */
    private void resetBoardVoteNotifyOne(Game ga, final int pn,
            final String plName, final boolean vyes)
    {
        boolean votingComplete = false;

        final String gaName = ga.getName();
        try
        {
            // Register in game
            votingComplete = ga.resetVoteRegister(pn, vyes);
            // Tell other players
            messageToGame(gaName, new ResetBoardVote(gaName, pn, vyes));
        }
        catch (IllegalArgumentException e)
        {
            D.ebugPrintln("*Error in player voting: game " + gaName + ": " + e);
            return;
        }
        catch (IllegalStateException e)
        {
            D.ebugPrintln("*Voting not active: game " + gaName);
            return;
        }

        if (!votingComplete)
        {
            return;
        }

        if (ga.getResetVoteResult())
        {
            // Vote succeeded - Go ahead and reset.
            resetBoardAndNotify(gaName, ga.getResetVoteRequester());
        }
        else
        {
            // Vote rejected - Let everyone know.
            messageToGame(gaName, new ResetBoardReject(gaName));
        }
    }

    /**
     * process the "game option get defaults" message. Responds to client by
     * sending {@link GameOptionGetDefaults GAMEOPTIONGETDEFAULTS}. All of
     * server's known options are sent, except empty string-valued options.
     * Depending on client version, server's response may include option names
     * that the client is too old to use; the client is able to ignore them.
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     * @since 1.1.07
     */
    private void handleGAMEOPTIONGETDEFAULTS(StringConnection c,
            GameOptionGetDefaults mes)
    {
        if (c == null)
            return;
        c.put(GameOptionGetDefaults.toCmd(GameOption
                .packKnownOptionsToString(true)));
    }

    /**
     * process the "game option get infos" message; reply with the info, with
     * one {@link GameOptionInfo GAMEOPTIONINFO} message per option keyname.
     * Mark the end of the option list with {@link GameOptionInfo
     * GAMEOPTIONINFO}("-"). If this list is empty, "-" will be the only
     * GAMEOPTIONGETINFO message sent.
     *<P>
     * We check the default values, not current values, so the list is
     * unaffected by cases where some option values are restricted to newer
     * client versions. Any option where opt.{@link GameOption#minVersion
     * minVersion} is too new for this client's version, is sent as
     * {@link GameOption#OTYPE_UNKNOWN}.
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     * @since 1.1.07
     */
    private void handleGAMEOPTIONGETINFOS(StringConnection c,
            GameOptionGetInfos mes)
    {
        if (c == null)
            return;
        final int cliVers = c.getVersion();
        boolean vecIsOptObjs = false;
        boolean alreadyTrimmedEnums = false;
        Vector okeys = mes.getOptionKeys();

        if (okeys == null)
        {
            // received "-", look for newer options (cli is older than us).
            // okeys will be null if nothing is new.
            okeys = GameOption.optionsNewerThanVersion(cliVers, false, true,
                    null);
            vecIsOptObjs = true;
            alreadyTrimmedEnums = true;
        }

        if (okeys != null)
        {
            for (int i = 0; i < okeys.size(); ++i)
            {
                GameOption opt;
                if (vecIsOptObjs)
                {
                    opt = (GameOption) okeys.elementAt(i);
                    if (opt.minVersion > cliVers)
                        opt = new GameOption(opt.optKey); // OTYPE_UNKNOWN
                }
                else
                {
                    String okey = (String) okeys.elementAt(i);
                    opt = GameOption.getOption(okey);
                    if ((opt == null) || (opt.minVersion > cliVers)) // Don't
                                                                     // use
                                                                     // opt.getMinVersion()
                                                                     // here
                        opt = new GameOption(okey); // OTYPE_UNKNOWN
                }

                // Enum-type options may have their values restricted by
                // version.
                if ((!alreadyTrimmedEnums) && (opt.enumVals != null)
                        && (opt.optType != GameOption.OTYPE_UNKNOWN)
                        && (opt.lastModVersion > cliVers))
                {
                    opt = GameOption.trimEnumForVersion(opt, cliVers);
                }

                c.put(new GameOptionInfo(opt).toCmd());
            }
        }

        // mark end of list, even if list was empty
        c.put(GameOptionInfo.OPTINFO_NO_MORE_OPTS.toCmd()); // GAMEOPTIONINFO("-")
    }

    /**
     * process the "new game with options request" message. For messages sent,
     * and other details, see
     * {@link #createOrJoinGameIfUserOK(StringConnection, String, String, String, Hashtable)}
     * .
     * <P>
     * Because this message is sent only by clients newer than 1.1.06, we
     * definitely know that the client has already sent its version information.
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     * @since 1.1.07
     */
    private void handleNEWGAMEWITHOPTIONSREQUEST(StringConnection c,
            NewGameWithOptionsRequest mes)
    {
        if (c == null)
            return;

        createOrJoinGameIfUserOK(c, mes.getNickname(), mes.getPassword(), mes
                .getGame(), mes.getOptions());
    }

    /**
     * handle "create account" message
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleCREATEACCOUNT(StringConnection c, CreateAccount mes)
    {
        final int cliVers = c.getVersion();

        //
        // check to see if there is an account with
        // the requested nickname
        //
        String userPassword = null;

        try
        {
            userPassword = SOCDBHelper.getUserPassword(mes.getNickname());
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem: don't continue
            c.put(StatusMessage.toCmd(StatusMessage.SV_PROBLEM_WITH_DB,
                    cliVers,
                    "Problem connecting to database, please try again later."));
            return;
        }

        if (userPassword != null)
        {
            c.put(StatusMessage.toCmd(StatusMessage.SV_NAME_IN_USE, cliVers,
                    "The nickname '" + mes.getNickname()
                            + "' is already in use."));

            return;
        }

        //
        // create the account
        //
        Date currentTime = new Date();

        boolean success = false;

        try
        {
            success = SOCDBHelper.createAccount(mes.getNickname(), c.host(),
                    mes.getPassword(), mes.getEmail(), currentTime.getTime());
        }
        catch (SQLException sqle)
        {
            System.err.println("Error creating account in db.");
        }

        if (success)
        {
            c
                    .put(StatusMessage.toCmd(StatusMessage.SV_ACCT_CREATED_OK,
                            cliVers, "Account created for '"
                                    + mes.getNickname() + "'."));
        }
        else
        {
            c.put(StatusMessage.toCmd(StatusMessage.SV_ACCT_NOT_CREATED_ERR,
                    cliVers, "Account not created due to error."));
        }
    }

    /**
     * Handle "get statistics" message
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleGETSTATISTICS(StringConnection c, GetStatistics mes)
    {
        try
        {
            Vector statistics = SOCDBHelper.getStatistics(mes.getStype());

            c.put(ShowStatistics.toCmd(mes.getStype(), statistics));
        }
        catch (SQLException sqle)
        {
            c.put(StatusMessage.toCmd(StatusMessage.SV_NOT_OK_GENERIC,
                    "Error retrieving statistics."));
        }
    }

    /**
     * handle "create account" message
     * 
     * @param c
     *            the connection
     * @param mes
     *            the message
     */
    private void handleRESETSTATS(StringConnection c, ResetStatistics mes)
    {
        // always verify password for resetting stats (currently not expected
        // to have already authenticated yet)
        // ok, if logged in via other client (gui or other)
        // authenticate User will tell them if they're not allowed in
        if (authenticateUser(c, mes.getNickname(), mes.getPassword(), true))
        {
            try
            {
                if (SOCDBHelper.resetStatistics(mes.getNickname()))
                {
                    c.put(StatusMessage.toCmd(StatusMessage.SV_OK,
                            "Statistics have been successfully reset "));

                    handleGETSTATISTICS(c, new GetStatistics(PlayerInfo.HUMAN));
                }
                else
                    c
                            .put(StatusMessage
                                    .toCmd(StatusMessage.SV_PROBLEM_WITH_DB,
                                            "Not connected to database. Statistics not reset."));
            }
            catch (SQLException sqle)
            {
                // Indicates a db problem: don't continue
                c
                        .put(StatusMessage
                                .toCmd(StatusMessage.SV_PROBLEM_WITH_DB,
                                        "Problem connecting to database, please try again later."));
            }
        }
    }

    /**
     * Client has been approved to join game; send the entire state of the game
     * to client. Unless <tt>isTakingOver</tt>, send client join event to other
     * players. Assumes NEWGAME (or NEWGAMEWITHOPTIONS) has already been sent
     * out. First message sent to connecting client is JOINGAMEAUTH, unless
     * isReset.
     *<P>
     * Among other messages, player names are sent via SITDOWN, and pieces on
     * board sent by PUTPIECE. See comments here for further details. The group
     * of messages sent here ends with GAMEMEMBERS, SETTURN and GAMESTATE.
     *<P>
     * 
     * @param gameData
     *            Game to join
     * @param c
     *            The connection of joining client
     * @param isReset
     *            Game is a board-reset of an existing game
     * @param isTakingOver
     *            Client is re-joining; this connection replaces an earlier one
     *            which is defunct because of a network problem. If
     *            <tt>isTakingOver</tt>, don't send anything to other players.
     */
    private void joinGame(Game gameData, StringConnection c, boolean isReset,
            boolean isTakingOver)
    {
        String gameName = gameData.getName();
        if (!isReset)
        {
            c.put(JoinGameAuth.toCmd(gameName));
            c.put(StatusMessage.toCmd(StatusMessage.SV_OK,
                    "Welcome to Java Settlers of Catan!"));
        }

        // c.put(GameState.toCmd(gameName, gameData.getGameState()));
        for (int i = 0; i < gameData.maxPlayers; i++)
        {
            /**
             * send the already-seated player information; if isReset, don't
             * send, because sitDown will be sent from resetBoardAndNotify.
             */
            if (!isReset)
            {
                Player pl = gameData.getPlayer(i);
                String plName = pl.getName();
                if ((plName != null) && (!gameData.isSeatVacant(i)))
                {
                    c.put(SitDown.toCmd(gameName, plName, i, pl.isRobot()));
                }
            }

            /**
             * send the seat lock information
             */
            messageToPlayer(c, new SetSeatLock(gameName, i, gameData
                    .isSeatLocked(i)));
        }

        c.put(getBoardLayoutMessage(gameData).toCmd());

        for (int i = 0; i < gameData.maxPlayers; i++)
        {
            Player pl = gameData.getPlayer(i);

            // Send piece info even if player has left the game (pl.getName() ==
            // null).
            // This lets them see "their" pieces before sitDown(), if they
            // rejoin at same position.

            Enumeration piecesEnum = pl.getPieces().elements();

            while (piecesEnum.hasMoreElements())
            {
                PlayingPiece piece = (PlayingPiece) piecesEnum.nextElement();

                if (piece.getType() == PlayingPiece.CITY)
                {
                    c.put(PutPiece.toCmd(gameName, i, PlayingPiece.SETTLEMENT,
                            piece.getCoordinates()));
                }

                c.put(PutPiece.toCmd(gameName, i, piece.getType(), piece
                        .getCoordinates()));
            }

            /**
             * send potential settlement list
             */
            Vector psList = new Vector();
            {
                for (int j = gameData.getBoard().getMinNode(); j <= Board.MAXNODE; j++)
                {
                    if (pl.isPotentialSettlement(j))
                    {
                        psList.addElement(new Integer(j));
                    }
                }
            }

            c.put(PotentialSettlements.toCmd(gameName, i, psList));

            /**
             * send coords of the last settlement
             */
            c.put(LastSettlement
                    .toCmd(gameName, i, pl.getLastSettlementCoord()));

            /**
             * send number of playing pieces in hand
             */
            c.put(PlayerElement.toCmd(gameName, i, PlayerElement.SET,
                    PlayerElement.ROADS, pl.getNumPieces(PlayingPiece.ROAD)));
            c.put(PlayerElement.toCmd(gameName, i, PlayerElement.SET,
                    PlayerElement.SETTLEMENTS, pl
                            .getNumPieces(PlayingPiece.SETTLEMENT)));
            c.put(PlayerElement.toCmd(gameName, i, PlayerElement.SET,
                    PlayerElement.CITIES, pl.getNumPieces(PlayingPiece.CITY)));

            c.put(PlayerElement.toCmd(gameName, i, PlayerElement.SET,
                    PlayerElement.UNKNOWN, pl.getResources().getTotal()));

            c.put(PlayerElement.toCmd(gameName, i, PlayerElement.SET,
                    PlayerElement.NUMKNIGHTS, pl.getNumKnights()));

            int numDevCards = pl.getDevCards().getTotal();

            for (int j = 0; j < numDevCards; j++)
            {
                c.put(DevCard.toCmd(gameName, i, DevCard.ADDOLD,
                        DevCardConstants.UNKNOWN));
            }

            c.put(FirstPlayer.toCmd(gameName, gameData.getFirstPlayer()));

            c.put(DevCardCount.toCmd(gameName, gameData.getNumDevCards()));

            c.put(ChangeFace.toCmd(gameName, i, pl.getFaceId()));

            c.put(DiceResult.toCmd(gameName, gameData.getCurrentDice()));
        }

        // /
        // / send who has longest road
        // /
        Player lrPlayer = gameData.getPlayerWithLongestRoad();
        int lrPlayerNum = -1;

        if (lrPlayer != null)
        {
            lrPlayerNum = lrPlayer.getPlayerNumber();
        }

        c.put(LongestRoad.toCmd(gameName, lrPlayerNum));

        // /
        // / send who has largest army
        // /
        final Player laPlayer = gameData.getPlayerWithLargestArmy();
        final int laPlayerNum;
        if (laPlayer != null)
        {
            laPlayerNum = laPlayer.getPlayerNumber();
        }
        else
        {
            laPlayerNum = -1;
        }

        c.put(LargestArmy.toCmd(gameName, laPlayerNum));

        /**
         * If we're rejoining and taking over a seat after a network problem,
         * send our resource and hand information.
         */
        if (isTakingOver)
        {
            Player cliPl = gameData.getPlayer((String) c.getData());
            if (cliPl != null)
            {
                int pn = cliPl.getPlayerNumber();
                if ((pn != -1) && !gameData.isSeatVacant(pn))
                    sitDown_sendPrivateInfo(gameData, c, pn, gameName);
            }
        }

        String membersCommand = null;
        gameList.takeMonitorForGame(gameName);

        /**
         * Almost done; send GAMEMEMBERS as a hint to client that we're almost
         * ready for its input. There's no new data in GAMEMEMBERS, because
         * player names have already been sent by the SITDOWN messages above.
         */
        try
        {
            Vector gameMembers = gameList.getMembers(gameName);
            membersCommand = GameMembers.toCmd(gameName, gameMembers);
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in handleJOINGAME (gameMembers) - " + e);
        }

        gameList.releaseMonitorForGame(gameName);
        c.put(membersCommand);
        c.put(SetTurn.toCmd(gameName, gameData.getCurrentPlayerNumber()));
        c.put(GameState.toCmd(gameName, gameData.getGameState()));
        D.ebugPrintln("*** " + c.getData() + " joined the game " + gameName
                + " from " + c.host());

        // messageToGame(gameName, new GameTextMsg(gameName, SERVERNAME,
        // n+" joined the game"));
        /**
         * Let everyone else know about the change
         */
        if (isTakingOver)
        {
            return;
        }
        messageToGame(gameName, new JoinGame((String) c.getData(), "",
                "dummyhost", gameName));
    }

    /**
     * This player is sitting down at the game
     * 
     * @param ga
     *            the game
     * @param c
     *            the connection for the player
     * @param pn
     *            which seat the player is taking
     * @param robot
     *            true if this player is a robot
     * @param isReset
     *            Game is a board-reset of an existing game
     * @throws NullPointerExceptioin
     *             if ga or c are null
     */
    private void sitDown(Game ga, StringConnection c, int pn, boolean robot,
            boolean isReset)
    {
        if (c == null)
            throw new NullPointerException("c");
        if (ga == null)
            throw new NullPointerException("ga");
        else
        {
            int face = 1;
            String gaName = ga.getName();
            ga.takeMonitor();

            try
            {
                if (!isReset)
                {
                    // If reset, player is already added and knows if robot.
                    try
                    {
                        ClientData cd = (ClientData) c.getAppData();
                        ga.addPlayer((String) c.getData(), pn);
                        ga.getPlayer(pn).setRobotFlag(robot,
                                (cd != null) && cd.isBuiltInRobot);

                        face = SOCDBHelper.getUserFace(ga.getPlayer(pn)
                                .getName());
                    }
                    catch (IllegalStateException e)
                    {
                        // Maybe already seated? (network lag)
                        if (!robot)
                            messageToPlayer(c, gaName,
                                    "You cannot sit down here.");
                        ga.releaseMonitor();
                        return; // <---- Early return: cannot sit down ----
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln(e.toString());
                        D.ebugPrintln("Error retrieving player face.");
                    }
                }

                ga.getPlayer(pn).setFaceId(face);

                /**
                 * if the player can sit, then tell the other clients in the
                 * game
                 */
                SitDown sitMessage = new SitDown(gaName, (String) c.getData(),
                        pn, robot);
                messageToGame(gaName, sitMessage);

                D.ebugPrintln("*** sent SitDown message to game ***");

                recordGameEvent(gaName, sitMessage.toCmd());

                Vector requests;
                if (!isReset)
                {
                    requests = (Vector) robotJoinRequests.get(gaName);
                }
                else
                {
                    requests = null; // Game already has all players from old
                                     // game
                }

                if (requests != null)
                {
                    /**
                     * if the request list is empty and the game hasn't started
                     * yet, then start the game
                     */
                    if (requests.isEmpty()
                            && (ga.getGameState() < Game.START1A))
                    {
                        startGame(ga);
                    }

                    /**
                     * if the request list is empty, remove the empty list
                     */
                    if (requests.isEmpty())
                    {
                        robotJoinRequests.remove(gaName);
                    }
                }

                broadcastGameStats(ga);

                /**
                 * send all the private information
                 */
                sitDown_sendPrivateInfo(ga, c, pn, gaName);
            }
            catch (Throwable e)
            {
                D.ebugPrintln("Exception caught - " + e);
                e.printStackTrace();
            }

            ga.releaseMonitor();
        }
    }

    /**
     * When player has just sat down at a seat, send all the private
     * information. Called from
     * {@link #sitDown(Game, StringConnection, int, boolean, boolean)}.
     *<P>
     * <b>Locks:</b> Assumes ga.takeMonitor() is held, and should remain held.
     * 
     * @param ga
     *            the game
     * @param c
     *            the connection for the player
     * @param pn
     *            which seat the player is taking
     * @param gaName
     *            the game's name (for convenience)
     * @since 1.1.08
     */
    private void sitDown_sendPrivateInfo(Game ga, StringConnection c, int pn,
            final String gaName)
    {
        /**
         * send all the private information
         */
        ResourceSet resources = ga.getPlayer(pn).getResources();
        // CLAY, ORE, SHEEP, WHEAT, WOOD, UNKNOWN
        for (int res = PlayerElement.CLAY; res <= PlayerElement.UNKNOWN; ++res)
            messageToPlayer(c, new PlayerElement(gaName, pn, PlayerElement.SET,
                    res, resources.getAmount(res)));

        DevCardSet devCards = ga.getPlayer(pn).getDevCards();

        /**
         * remove the unknown cards
         */
        int i;

        for (i = 0; i < devCards.getTotal(); i++)
        {
            messageToPlayer(c, new DevCard(gaName, pn, DevCard.PLAY,
                    DevCardConstants.UNKNOWN));
        }

        /**
         * send first all new cards, then all old cards
         */
        for (int dcAge = DevCardSet.NEW; dcAge >= DevCardSet.OLD; --dcAge)
        {
            final int addCmd = (dcAge == DevCardSet.NEW) ? DevCard.ADDNEW
                    : DevCard.ADDOLD;

            /**
             * loop from KNIGHT to TOW (MIN to MAX_KNOWN)
             */
            for (int dcType = DevCardConstants.MIN; dcType <= DevCardConstants.MAX_KNOWN; ++dcType)
            {
                int cardAmt = devCards.getAmount(dcAge, dcType);
                if (cardAmt > 0)
                {
                    DevCard addMsg = new DevCard(gaName, pn, addCmd, dcType);
                    for (; cardAmt > 0; --cardAmt)
                        messageToPlayer(c, addMsg);
                }

            } // for (dcType)

        } // for (dcAge)

        /**
         * send game state info such as requests for discards
         */
        sendGameState(ga);

        if ((ga.getCurrentDice() == 7) && ga.getPlayer(pn).getNeedToDiscard())
        {
            messageToPlayer(c, new DiscardRequest(gaName, ga.getPlayer(pn)
                    .getResources().getTotal() / 2));
        }

        /**
         * send what face this player is using
         */
        messageToGame(gaName, new ChangeFace(gaName, pn, ga.getPlayer(pn)
                .getFaceId()));
    }

    /**
     * The current player is stealing from another player. Send messages saying
     * what was stolen.
     * 
     * @param ga
     *            the game
     * @param pe
     *            the perpetrator
     * @param vi
     *            the the victim
     * @param rsrc
     *            type of resource stolen, as in ResourceConstants
     * @throws NullPointerExceptioin
     *             if ga is null
     */
    private void reportRobbery(Game ga, Player pe, Player vi, int rsrc)
    {
        if (ga == null)
            throw new NullPointerException("ga");
        else
        {
            final String gaName = ga.getName();
            final String peName = pe.getName();
            final String viName = vi.getName();
            final int pePN = pe.getPlayerNumber();
            final int viPN = vi.getPlayerNumber();
            StringBuffer mes = new StringBuffer(" stole "); // " stole a sheep resource from "
            PlayerElement gainRsrc = null;
            PlayerElement loseRsrc = null;
            PlayerElement gainUnknown;
            PlayerElement loseUnknown;

            final String aResource = ResourceConstants.aResName(rsrc);
            mes.append(aResource); // "a clay"

            // This works because PlayerElement.SHEEP ==
            // ResourceConstants.SHEEP.
            gainRsrc = new PlayerElement(gaName, pePN, PlayerElement.GAIN,
                    rsrc, 1);
            loseRsrc = new PlayerElement(gaName, viPN, PlayerElement.LOSE,
                    rsrc, 1);

            mes.append(" resource from ");

            // send the game messages
            StringConnection peCon = getConnection(peName);
            StringConnection viCon = getConnection(viName);
            messageToPlayer(peCon, gainRsrc);
            messageToPlayer(peCon, loseRsrc);
            messageToPlayer(viCon, gainRsrc);
            messageToPlayer(viCon, loseRsrc);
            // Don't send generic message to pe or vi
            Vector exceptions = new Vector(2);
            exceptions.addElement(peCon);
            exceptions.addElement(viCon);
            gainUnknown = new PlayerElement(gaName, pePN, PlayerElement.GAIN,
                    PlayerElement.UNKNOWN, 1);
            loseUnknown = new PlayerElement(gaName, viPN, PlayerElement.LOSE,
                    PlayerElement.UNKNOWN, 1);
            messageToGameExcept(gaName, exceptions, gainUnknown, true);
            messageToGameExcept(gaName, exceptions, loseUnknown, true);

            /*
             * send the text messages "You stole a sheep resource from viName."
             * "peName stole a sheep resource from you."
             * "peName stole a resource from viName."
             */
            messageToPlayer(peCon, gaName, "You" + mes.toString() + viName
                    + '.');
            messageToPlayer(viCon, gaName, peName + mes.toString() + "you.");
            messageToGameExcept(gaName, exceptions, new GameTextMsg(gaName,
                    SERVERNAME, peName + " stole a resource from " + viName),
                    true);
        }
    }

    /**
     * send the current state of the game with a message. Assumes current player
     * does not change during this state. If we send a text message to prompt
     * the new player to roll, also sends a RollDicePrompt data message. If the
     * client is too old (1.0.6), it will ignore the prompt.
     * 
     * @param ga
     *            the game
     * 
     * @see #sendGameState(Game, boolean)
     */
    protected void sendGameState(Game ga)
    {
        sendGameState(ga, true);
    }

    /**
     * send the current state of the game with a message. Note that the current
     * (or new) player number is not sent here. If game is now OVER, send
     * appropriate messages.
     * 
     * @see #sendTurn(Game, boolean)
     * @see #sendGameState(Game)
     * @see #sendGameStateOVER(Game)
     * 
     * @param ga
     *            the game
     * @param sendRollPrompt
     *            If true, and if we send a text message to prompt the player to
     *            roll, send a RollDicePrompt data message. If the client is too
     *            old (1.0.6), it will ignore the prompt.
     * 
     * @return did we send a text message to prompt the player to roll? If so,
     *         sendTurn can also send a RollDicePrompt data message.
     */
    protected boolean sendGameState(Game ga, boolean sendRollPrompt)
    {
        if (ga == null)
            return false;

        final String gname = ga.getName();
        boolean promptedRoll = false;
        if (ga.getGameState() == Game.OVER)
        {
            /**
             * Before sending state "OVER", enforce current player number. This
             * helps the client's copy of game recognize winning condition.
             */
            messageToGame(gname,
                    new SetTurn(gname, ga.getCurrentPlayerNumber()));
        }
        messageToGame(gname, new GameState(gname, ga.getGameState()));

        Player player = null;

        if (ga.getCurrentPlayerNumber() != -1)
        {
            player = ga.getPlayer(ga.getCurrentPlayerNumber());
        }

        switch (ga.getGameState())
        {
        case Game.START1A:
        case Game.START2A:
            messageToGame(gname, "It's " + player.getName()
                    + "'s turn to build a settlement.");

            break;

        case Game.START1B:
        case Game.START2B:
            messageToGame(gname, "It's " + player.getName()
                    + "'s turn to build a road.");

            break;

        case Game.PLAY:
            messageToGame(gname, "It's " + player.getName()
                    + "'s turn to roll the dice.");
            promptedRoll = true;
            if (sendRollPrompt)
                messageToGame(gname, new RollDicePrompt(gname, player
                        .getPlayerNumber()));

            break;

        case Game.WAITING_FOR_DISCARDS:

            int count = 0;
            String message = "error at sendGameState()";
            String[] names = new String[ga.maxPlayers];

            for (int i = 0; i < ga.maxPlayers; i++)
            {
                if (ga.getPlayer(i).getNeedToDiscard())
                {
                    names[count] = ga.getPlayer(i).getName();
                    count++;
                }
            }

            if (count == 1)
            {
                message = names[0] + " needs to discard.";
            }
            else if (count == 2)
            {
                message = names[0] + " and " + names[1] + " need to discard.";
            }
            else if (count > 2)
            {
                message = names[0];

                for (int i = 1; i < (count - 1); i++)
                {
                    message += (", " + names[i]);
                }

                message += (" and " + names[count - 1] + " need to discard.");
            }

            messageToGame(gname, message);

            break;

        case Game.PLACING_ROBBER:
            messageToGame(gname, player.getName() + " will move the robber.");

            break;

        case Game.WAITING_FOR_CHOICE:

            /**
             * get the choices from the game
             */
            boolean[] choices = new boolean[ga.maxPlayers];

            for (int i = 0; i < ga.maxPlayers; i++)
            {
                choices[i] = false;
            }

            Enumeration plEnum = ga.getPossibleVictims().elements();

            while (plEnum.hasMoreElements())
            {
                Player pl = (Player) plEnum.nextElement();
                choices[pl.getPlayerNumber()] = true;
            }

            /**
             * ask the current player to choose a player to steal from
             */
            StringConnection con = getConnection(ga.getPlayer(
                    ga.getCurrentPlayerNumber()).getName());
            if (con != null)
            {
                con.put(ChoosePlayerRequest.toCmd(gname, choices));
            }

            break;

        case Game.OVER:

            sendGameStateOVER(ga);

            break;

        } // switch ga.getGameState

        return promptedRoll;
    }

    /**
     * If game is OVER, send messages reporting winner, final score, and each
     * player's victory-point cards. Also give stats on game length, and on each
     * player's connect time. If player has finished more than 1 game since
     * connecting, send win-loss count.
     * 
     * @param ga
     *            This game is over; state should be OVER
     */
    protected void sendGameStateOVER(Game ga)
    {
        final String gname = ga.getName();
        String msg;

        /**
         * Find and announce the winner (the real "found winner" code is in
         * Game.checkForWinner; that was already called before
         * sendGameStateOVER.)
         */
        Player winPl = ga.getPlayer(ga.getCurrentPlayerNumber());

        if (winPl.getTotalVP() < Game.VP_WINNER)
        {
            // Should not happen: By rules FAQ, only current player can be
            // winner.
            // This is fallback code.
            for (int i = 0; i < ga.maxPlayers; i++)
            {
                winPl = ga.getPlayer(i);
                if (winPl.getTotalVP() >= Game.VP_WINNER)
                {
                    break;
                }
            }
        }
        msg = ">>> " + winPl.getName() + " has won the game with "
                + winPl.getTotalVP() + " points.";
        messageToGameUrgent(gname, msg);

        // / send a message with the revealed final scores
        {
            int[] scores = new int[ga.maxPlayers];
            boolean[] isRobot = new boolean[ga.maxPlayers];
            for (int i = 0; i < ga.maxPlayers; ++i)
            {
                scores[i] = ga.getPlayer(i).getTotalVP();
                isRobot[i] = ga.getPlayer(i).isRobot();
            }
            messageToGame(gname, new GameStats(gname, scores, isRobot));
        }

        // /
        // / send a message saying what VP cards each player has
        // /
        for (int i = 0; i < ga.maxPlayers; i++)
        {
            Player pl = ga.getPlayer(i);
            DevCardSet devCards = pl.getDevCards();

            if (devCards.getNumVPCards() > 0)
            {
                msg = pl.getName() + " has";
                int vpCardCount = 0;

                for (int devCardType = DevCardConstants.CAP; devCardType < DevCardConstants.UNKNOWN; devCardType++)
                {
                    if ((devCards.getAmount(DevCardSet.OLD, devCardType) > 0)
                            || (devCards.getAmount(DevCardSet.NEW, devCardType) > 0))
                    {
                        if (vpCardCount > 0)
                        {
                            if ((devCards.getNumVPCards() - vpCardCount) == 1)
                            {
                                msg += " and";
                            }
                            else if ((devCards.getNumVPCards() - vpCardCount) > 0)
                            {
                                msg += ",";
                            }
                        }

                        vpCardCount++;

                        switch (devCardType)
                        {
                        case DevCardConstants.CAP:
                            msg += " a Gov.House (+1VP)";

                            break;

                        case DevCardConstants.LIB:
                            msg += " a Market (+1VP)";

                            break;

                        case DevCardConstants.UNIV:
                            msg += " a University (+1VP)";

                            break;

                        case DevCardConstants.TEMP:
                            msg += " a Temple (+1VP)";

                            break;

                        case DevCardConstants.TOW:
                            msg += " a Chapel (+1VP)";

                            break;
                        }
                    }
                } // for each devcard type

                messageToGame(gname, msg);
                break;

            } // if devcards
        } // for each player

        /*
         * From Monte Carlo, still necessary? sleep(2000);
         * messageToGame(ga.getName(), new DeleteGame(ga.getName() ));
         * sleep(8000); System.out.println("Clean exit."); System.exit(0);
         */

        storeGameScores(ga);
        storePlayerFaces(ga);

        /**
         * send game-length and connect-length messages, possibly win-loss
         * count.
         */
        {
            Date now = new Date();
            Date gstart = ga.getStartTime();
            final String gLengthMsg;
            if (gstart != null)
            {
                StringBuffer sb = new StringBuffer("This game was ");
                sb.append(ga.getRoundCount());
                sb.append(" rounds, and took ");
                long gameSeconds = ((now.getTime() - gstart.getTime()) + 500L) / 1000L;
                long gameMinutes = gameSeconds / 60L;
                gameSeconds = gameSeconds % 60L;
                sb.append(gameMinutes);
                if (gameSeconds == 0)
                {
                    sb.append(" minutes.");
                }
                else if (gameSeconds == 1)
                {
                    sb.append(" minutes 1 second.");
                }
                else
                {
                    sb.append(" minutes ");
                    sb.append(gameSeconds);
                    sb.append(" seconds.");
                }
                gLengthMsg = sb.toString();
                messageToGame(gname, gLengthMsg);

                // Ignore possible "1 minutes"; that game is too short to worry
                // about.
            }
            else
            {
                gLengthMsg = null;
            }

            /**
             * Update each player's win-loss count for this session. Tell each
             * player their resource roll totals. Tell each player how long
             * they've been connected. (Robot players aren't told this, it's not
             * necessary.)
             */
            String connMsg;
            if ((strSocketName != null)
                    && (strSocketName.equals(PRACTICE_STRINGPORT)))
                connMsg = "You have been practicing ";
            else
                connMsg = "You have been connected ";

            for (int i = 0; i < ga.maxPlayers; i++)
            {
                if (ga.isSeatVacant(i))
                    continue;

                Player pl = ga.getPlayer(i);
                StringConnection plConn = (StringConnection) conns.get(pl
                        .getName());
                ClientData cd;
                if (plConn != null)
                {
                    // Update win-loss count, even for robots
                    cd = (ClientData) plConn.getAppData();
                    if (pl == winPl)
                        cd.wonGame();
                    else
                        cd.lostGame();
                }
                else
                {
                    cd = null; // To satisfy compiler warning
                }

                if (pl.isRobot())
                    continue; // <-- Don't bother to send any stats text to
                              // robots --

                if (plConn != null)
                {
                    if (plConn.getVersion() >= PlayerStats.VERSION_FOR_RES_ROLL)
                    {
                        // Send total resources rolled
                        messageToPlayer(plConn, new PlayerStats(pl,
                                PlayerStats.STYPE_RES_ROLL));
                    }

                    long connTime = plConn.getConnectTime().getTime();
                    long connMinutes = (((now.getTime() - connTime)) + 30000L) / 60000L;
                    StringBuffer cLengthMsg = new StringBuffer(connMsg);
                    cLengthMsg.append(connMinutes);
                    if (connMinutes == 1)
                        cLengthMsg.append(" minute.");
                    else
                        cLengthMsg.append(" minutes.");
                    messageToPlayer(plConn, gname, cLengthMsg.toString());

                    // Send client's win-loss count for this session,
                    // if more than 1 game has been played
                    {
                        int wins = cd.getWins();
                        int losses = cd.getLosses();
                        if (wins + losses < 2)
                            continue; // Only 1 game played so far

                        StringBuffer winLossMsg = new StringBuffer("You have ");
                        if (wins > 0)
                        {
                            winLossMsg.append("won ");
                            winLossMsg.append(wins);
                            if (losses == 0)
                            {
                                if (wins != 1)
                                    winLossMsg.append(" games");
                                else
                                    winLossMsg.append(" game");
                            }
                            else
                            {
                                winLossMsg.append(" and ");
                            }
                        }
                        if (losses > 0)
                        {
                            winLossMsg.append("lost ");
                            winLossMsg.append(losses);
                            if (losses != 1)
                                winLossMsg.append(" games");
                            else
                                winLossMsg.append(" game");
                        }
                        winLossMsg.append(" since connecting.");
                        messageToPlayer(plConn, gname, winLossMsg.toString());
                    }
                }
            } // for each player

        } // send game timing stats, win-loss stats
    }

    /**
     * report a trade that has taken place between players, using
     * {@link PlayerElement} and {@link GameTextMsg} messages. Trades are also
     * reported to robots by re-sending the accepting player's
     * {@link AcceptOffer} message.
     * 
     * @param ga
     *            the game
     * @param offering
     *            the number of the player making the offer
     * @param accepting
     *            the number of the player accepting the offer
     * 
     * @see #reportBankTrade(Game, ResourceSet, ResourceSet)
     */
    protected void reportTrade(Game ga, int offering, int accepting)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();
            final TradeOffer offer = ga.getPlayer(offering).getCurrentOffer();

            StringBuffer message = new StringBuffer(ga.getPlayer(offering)
                    .getName());
            message.append(" traded ");
            reportRsrcGainLoss(gaName, offer.getGiveSet(), true, offering,
                    accepting, message, null);
            message.append(" for ");
            reportRsrcGainLoss(gaName, offer.getGetSet(), false, offering,
                    accepting, message, null);
            message.append(" from ");
            message.append(ga.getPlayer(accepting).getName());
            message.append('.');
            messageToGame(gaName, message.toString());
        }
    }

    /**
     * report that the current player traded with the bank or a port, using
     * {@link PlayerElement} and {@link GameTextMsg} messages.
     * 
     * @param ga
     *            the game
     * @param give
     *            the number of the player making the offer
     * @param get
     *            the number of the player accepting the offer
     * 
     * @see #reportTrade(Game, int, int)
     */
    protected void reportBankTrade(Game ga, ResourceSet give, ResourceSet get)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();
            final int cpn = ga.getCurrentPlayerNumber();
            StringBuffer message = new StringBuffer(ga.getPlayer(cpn).getName());
            message.append(" traded ");
            reportRsrcGainLoss(gaName, give, true, cpn, -1, message, null);
            message.append(" for ");
            reportRsrcGainLoss(gaName, get, false, cpn, -1, message, null);

            if ((give.getTotal() / get.getTotal()) == 4)
            {
                message.append(" from the bank."); // 4:1 trade
            }
            else
            {
                message.append(" from a port."); // 3:1 or 2:1 trade
            }

            messageToGame(gaName, message.toString());
        }
    }

    /**
     * Report the resources gained/lost by a player, and optionally (for
     * trading) lost/gained by a second player. Sends PLAYERELEMENT messages,
     * either to entire game, or to player only. Builds the resource-amount
     * string used to report the trade as text. Takes and releases the gameList
     * monitor for this game.
     *<P>
     * Used to report the resources gained from a roll, discard, or discovery
     * (year-of-plenty) pick. Also used to report the "give" or "get" half of a
     * resource trade.
     * 
     * @param gaName
     *            Game name
     * @param rset
     *            Resource set (from a roll, or the "give" or "get" side of a
     *            trade). Resource type {@link ResourceConstants#UNKNOWN} is
     *            ignored. Only positive resource amounts are sent (negative is
     *            ignored).
     * @param isLoss
     *            If true, "give" ({@link PlayerElement#LOSE}), otherwise "get"
     *            ({@link PlayerElement#GAIN})
     * @param mainPlayer
     *            Player number "giving" if isLose==true, otherwise "getting".
     *            For each nonzero resource involved, PLAYERELEMENT messages
     *            will be sent about this player.
     * @param tradingPlayer
     *            Player number on other side of trade, or -1 if no second
     *            player is involved. If not -1, PLAYERELEMENT messages will
     *            also be sent about this player.
     * @param message
     *            Append resource numbers/types to this stringbuffer, format
     *            like "3 clay,3 wood"; can be null.
     * @param playerConn
     *            Null or mainPlayer's connection; send messages here instead of
     *            sending to all players in game. Because trades are public,
     *            there is no such parameter for tradingPlayer.
     * 
     * @see #reportTrade(Game, int, int)
     * @see #reportBankTrade(Game, ResourceSet, ResourceSet)
     * @see #handleDISCARD(StringConnection, Discard)
     * @see #handleDISCOVERYPICK(StringConnection, DiscoveryPick)
     * @see #handleROLLDICE(StringConnection, RollDice)
     */
    private void reportRsrcGainLoss(final String gaName,
            final ResourceSet rset, final boolean isLoss, final int mainPlayer,
            final int tradingPlayer, StringBuffer message,
            StringConnection playerConn)
    {
        final int losegain = isLoss ? PlayerElement.LOSE : PlayerElement.GAIN; // for
                                                                               // pnA
        final int gainlose = isLoss ? PlayerElement.GAIN : PlayerElement.LOSE; // for
                                                                               // pnB

        boolean needComma = false; // Has a resource already been appended to
                                   // message?

        gameList.takeMonitorForGame(gaName);

        for (int res = ResourceConstants.CLAY; res <= ResourceConstants.WOOD; ++res)
        {
            // This works because PlayerElement.SHEEP ==
            // ResourceConstants.SHEEP.

            final int amt = rset.getAmount(res);
            if (amt <= 0)
                continue;
            if (playerConn != null)
                messageToPlayer(playerConn, new PlayerElement(gaName,
                        mainPlayer, losegain, res, amt));
            else
                messageToGameWithMon(gaName, new PlayerElement(gaName,
                        mainPlayer, losegain, res, amt));
            if (tradingPlayer != -1)
                messageToGameWithMon(gaName, new PlayerElement(gaName,
                        tradingPlayer, gainlose, res, amt));
            if (message != null)
            {
                if (needComma)
                    message.append(", ");
                message.append(amt);
                message.append(" ");
                message.append(ResourceConstants.resName(res));
                needComma = true;
            }
        }

        gameList.releaseMonitorForGame(gaName);
    }

    /**
     * make sure it's the player's turn
     * 
     * @param c
     *            the connection for player
     * @param ga
     *            the game
     * 
     * @return true if it is the player's turn; false if another player's turn,
     *         or if this player isn't in the game
     */
    protected boolean checkTurn(StringConnection c, Game ga)
    {
        boolean result = false;
        Player player = ga.getPlayer((String) c.getData());

        return (player != null && ga.getCurrentPlayerNumber() == player
                .getPlayerNumber());
    }

    /**
     * do the stuff you need to do to start a game
     * 
     * @param ga
     *            the game
     * @throws NullPointerExceptioin
     *             if ga is null
     */
    private void startGame(Game ga)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();

            numberOfGamesStarted++;
            ga.startGame();
            gameList.takeMonitorForGame(gaName);

            /**
             * send the board layout
             */
            messageToGameWithMon(gaName, getBoardLayoutMessage(ga));

            /**
             * send the player info
             */
            for (int i = 0; i < ga.maxPlayers; i++)
            {
                if (!ga.isSeatVacant(i))
                {
                    Player pl = ga.getPlayer(i);
                    messageToGameWithMon(gaName, new PlayerElement(gaName, i,
                            PlayerElement.SET, PlayerElement.ROADS, pl
                                    .getNumPieces(PlayingPiece.ROAD)));
                    messageToGameWithMon(gaName, new PlayerElement(gaName, i,
                            PlayerElement.SET, PlayerElement.SETTLEMENTS, pl
                                    .getNumPieces(PlayingPiece.SETTLEMENT)));
                    messageToGameWithMon(gaName, new PlayerElement(gaName, i,
                            PlayerElement.SET, PlayerElement.CITIES, pl
                                    .getNumPieces(PlayingPiece.CITY)));
                    messageToGameWithMon(gaName, new SetPlayedDevCard(gaName,
                            i, false));
                }
            }

            /**
             * send the number of dev cards
             */
            messageToGameWithMon(gaName, new DevCardCount(gaName, ga
                    .getNumDevCards()));

            /**
             * ga.startGame() picks who goes first, but feedback is nice
             */
            messageToGameWithMon(gaName, new GameTextMsg(gaName, SERVERNAME,
                    "Randomly picking a starting player..."));

            gameList.releaseMonitorForGame(gaName);

            /**
             * send the game state
             */
            sendGameState(ga, false);

            /**
             * start the game
             */
            messageToGame(gaName, new StartGame(gaName));

            /**
             * send whose turn it is
             */
            sendTurn(ga, false);
        }
    }

    /**
     * Reset the board, to a copy with same players but new layout. Here's the
     * general outline; step 1 and 2 are done immediately here, steps 3 through
     * n are done (after robots are dismissed) within
     * {@link #resetBoardAndNotify_finish(GameBoardReset, Game)}.
     *<OL>
     * <LI value=1>Reset the board, remember player positions. If there are
     * robots, set game state to {@link Game#READY_RESET_WAIT_ROBOT_DISMISS}.
     * <LI value=2a>Send ResetBoardAuth to each client (like sending
     * JoinGameAuth at new game) Humans will reset their copy of the game.
     * Robots will leave the game, and soon be requested to re-join. (This
     * simplifies the robot client.) If the game was already over at reset time,
     * different robots will be randomly chosen to join the reset game.
     * <LI value=2b>If there were robots, wait for them all to leave the old
     * game. Otherwise, (race condition) they may leave the new game as it is
     * forming. Set {@link Game#boardResetOngoingInfo}. Wait for them to leave
     * the old game before continuing. The call will be made from
     * {@link #handleLEAVEGAME_maybeGameReset_oldRobot(String)}.
     * <LI value=2c>If no robots, immediately call
     * {@link #resetBoardAndNotify_finish(GameBoardReset, Game)}.
     * <P>
     * <b>This ends this method.</b> Step 3 and the rest are in
     * {@link #resetBoardAndNotify_finish(GameBoardReset, Game)}.
     * <LI value=3>Send messages as if each human player has clicked "join"
     * (except JoinGameAuth)
     * <LI value=4>Send as if each human player has clicked "sit here"
     * <LI value=5a>If no robots, send to game as if someone else has clicked
     * "start game", and set up state to begin game play.
     * <LI value=5b>If there are robots, set up wait-request queue
     * (robotJoinRequests). Game will wait for robots to send JOINGAME and
     * SITDOWN, as they do when joining a newly created game. Once all robots
     * have re-joined, the game will begin.
     *</OL>
     */
    private void resetBoardAndNotify(final String gaName,
            final int requestingPlayer)
    {
        /**
         * 1. Reset the board, remember player positions. Takes the
         * monitorForGame and (when reset is ready) releases it. If robots,
         * resetBoard will also set gamestate and boardResetOngoingInfo field.
         */
        GameBoardReset reBoard = gameList.resetBoard(gaName);
        if (reBoard == null)
        {
            messageToGameUrgent(gaName, ">>> Internal error, Game " + gaName
                    + " board reset failed");
            return; // <---- Early return: reset failed ----
        }
        Game reGame = reBoard.newGame;

        // Announce who asked for this reset
        {
            String plName = reGame.getPlayer(requestingPlayer).getName();
            if (plName == null)
                plName = "player who left";
            messageToGameUrgent(gaName, ">>> Game " + gaName
                    + " board reset by " + plName);
        }

        // If game was over, we'll shuffle the robots
        final boolean gameWasOverAtReset = (Game.OVER == reBoard.oldGameState);

        /**
         * Player connection data: - Humans are copied from old to new game -
         * Robots aren't copied to new game, must re-join
         */
        StringConnection[] huConns = reBoard.humanConns;
        StringConnection[] roConns = reBoard.robotConns;

        /**
         * Notify old game's players. (Humans and robots)
         * 
         * 2a. Send ResetBoardAuth to each (like sending JoinGameAuth at new
         * game). Humans will reset their copy of the game. Robots will leave
         * the game, and soon will be requested to re-join.
         */
        for (int pn = 0; pn < reGame.maxPlayers; ++pn)
        {
            ResetBoardAuth resetMsg = new ResetBoardAuth(gaName, pn,
                    requestingPlayer);
            if (huConns[pn] != null)
                messageToPlayer(huConns[pn], resetMsg);
            else if (roConns[pn] != null)
            {
                if (!gameWasOverAtReset)
                    messageToPlayer(roConns[pn], resetMsg); // same robot will
                                                            // rejoin
                else
                    messageToPlayer(roConns[pn], new RobotDismiss(gaName)); // could
                                                                            // be
                                                                            // different
                                                                            // bot
            }
        }

        // If there are robots, wait for them to leave
        // before doing anything else. Otherwise, go ahead.

        if (!reBoard.hadRobots)
            resetBoardAndNotify_finish(reBoard, reGame);
        // else
        // gameState is READY_RESET_WAIT_ROBOT_DISMISS,
        // and once the last robot leaves this game,
        // handleLEAVEGAME will take care of the reset,
        // by calling resetBoardAndNotify_finish.

    } // resetBoardAndNotify

    /**
     * Complete steps 3 - n of the board-reset process outlined in
     * {@link #resetBoardAndNotify(String, int)}, after any robots have left the
     * old game.
     * 
     * @param reBoard
     * @param reGame
     * @since 1.1.07
     */
    private void resetBoardAndNotify_finish(GameBoardReset reBoard, Game reGame)
    {
        final boolean gameWasOverAtReset = (Game.OVER == reBoard.oldGameState);
        StringConnection[] huConns = reBoard.humanConns;

        /**
         * 3. Send messages as if each human player has clicked "join" (except
         * JoinGameAuth)
         */
        for (int pn = 0; pn < reGame.maxPlayers; ++pn)
        {
            if (huConns[pn] != null)
                joinGame(reGame, huConns[pn], true, false);
        }

        /**
         * 4. Send as if each human player has clicked "sit here"
         */
        for (int pn = 0; pn < reGame.maxPlayers; ++pn)
        {
            if (huConns[pn] != null)
                sitDown(reGame, huConns[pn], pn, false /* isRobot */, true /* isReset */);
        }

        /**
         * 5a. If no robots, send to game as if someone else has clicked
         * "start game", and set up state to begin game play.
         */
        if (!reBoard.hadRobots)
        {
            startGame(reGame);
        }
        else
        {

            /**
             * 5b. If there are robots, set up wait-request queue
             * (robotJoinRequests) and ask robots to re-join. Game will wait for
             * robots to send JOINGAME and SITDOWN, as they do when joining a
             * newly created game. Once all robots have re-joined, the game will
             * begin.
             */
            reGame.setGameState(Game.READY);
            readyGameAskRobotsJoin(reGame, gameWasOverAtReset ? null
                    : reBoard.robotConns);
        }

        // All set.
    } // resetBoardAndNotify_finish

    /**
     * send whose turn it is. Optionally also send a prompt to roll. If the
     * client is too old (1.0.6), it will ignore the prompt.
     * 
     * @param ga
     *            the game
     * @param sendRollPrompt
     *            whether to send a RollDicePrompt message afterwards
     */
    private void sendTurn(Game ga, boolean sendRollPrompt)
    {
        if (ga != null)
        {
            String gname = ga.getName();
            int pn = ga.getCurrentPlayerNumber();

            messageToGame(gname, new SetPlayedDevCard(gname, pn, false));

            Turn turnMessage = new Turn(gname, pn);
            messageToGame(gname, turnMessage);
            recordGameEvent(gname, turnMessage.toCmd());

            if (sendRollPrompt)
                messageToGame(gname, new RollDicePrompt(gname, pn));
        }
    }

    /**
     * put together the board layout message. Message type will be
     * {@link BoardLayout} or {@link BoardLayout2}, depending on
     * {@link Board#getBoardEncodingFormat()
     * ga.getBoard().getBoardEncodingFormat()} and
     * {@link Game#getClientVersionMinRequired()}.
     * 
     * @param ga
     *            the game
     * @return a board layout message
     * @throws NullPointerExceptioin
     *             if ga is null
     */
    private Message getBoardLayoutMessage(Game ga)
    {
        Board board;
        int[] hexes;
        int[] numbers;
        int robber;

        board = ga.getBoard();
        hexes = board.getHexLayout();
        numbers = board.getNumberLayout();
        robber = board.getRobberHex();
        int bef = board.getBoardEncodingFormat();
        if ((bef == 1)
                && (ga.getClientVersionMinRequired() < BoardLayout2.VERSION_FOR_BOARDLAYOUT2))
        {
            return new BoardLayout(ga.getName(), hexes, numbers, robber);
        }
        else
        {
            return new BoardLayout2(ga.getName(), bef, hexes, numbers, board
                    .getPortsLayout(), robber);
        }
    }

    /**
     * write a gameRecord out to disk
     * 
     * @param na
     *            the name of the record
     * @param gr
     *            the game record
     */

    /*
     * private void writeGameRecord(String na, SOCGameRecord gr) {
     * FileOutputStream os = null; ObjectOutput output = null;
     * 
     * try { Date theTime = new Date(); os = new
     * FileOutputStream("dataFiles/"+na+"."+theTime.getTime()); output = new
     * ObjectOutputStream(os); } catch (Exception e) {
     * D.ebugPrintln(e.toString());
     * D.ebugPrintln("Unable to open output stream."); } try{
     * output.writeObject(gr); //
     * D.ebugPrintln("*** Wrote "+na+" out to disk. ***"); output.close(); }
     * catch (Exception e) { D.ebugPrintln(e.toString());
     * D.ebugPrintln("Unable to write game record to disk."); } }
     */

    /**
     * if all the players stayed for the whole game, record the scores in the
     * database
     * 
     * @param ga
     *            the game
     * @throws NullPointerExceptioin
     *             if ga is null
     */
    private void storeGameScores(Game ga)
    {
        // D.ebugPrintln("allOriginalPlayers for "+ga.getName()+" : "+ga.allOriginalPlayers());
        if ((ga.getGameState() == Game.OVER) && (ga.allOriginalPlayers()))
        {
            // if (ga.allOriginalPlayers()) {
            try
            {
                // TO-DO 6-player: save their scores too, if
                // those fields are in the database.
                SOCDBHelper.saveGameScores(ga);
            }
            catch (SQLException sqle)
            {
                D.ebugPrintln(sqle.toString());
                D.ebugPrintln("Error saving player face in db.");
            }
        }
    }

    /**
     * if all the players stayed for the whole game, record the scores in the
     * database
     * 
     * @param ga
     *            the game
     * @throws NullPointerExceptioin
     *             if ga is null
     */
    private void storePlayerFaces(Game ga)
    {
        if (ga.getGameState() == Game.OVER)
        {
            try
            {
                SOCDBHelper.saveFaces(ga);
            }
            catch (SQLException sqle)
            {
                D.ebugPrintln(sqle.toString());
                D.ebugPrintln("Error saving player face in db.");
            }
        }
    }

    /**
     * record events that happen during the game
     * 
     * @param gameName
     *            the name of the game
     * @param event
     *            the event
     */
    protected void recordGameEvent(String gameName, String event)
    {
        recordGameEvent(null, gameName, event);
    }

    /**
     * record events that happen during the game
     * 
     * @param gameName
     *            the name of the game
     * @param event
     *            the event
     */
    protected void recordGameEvent(Message mes, String gameName, String event)
    {
        FileWriter fw = (FileWriter) gameDataFiles.get(gameName);
        if (fw != null)
        {
            try
            {
                fw
                        .write((mes == null ? "" : mes.toCmd()) + ": " + event
                                + "\n");
                // D.ebugPrintln("WROTE |"+event+"|");
            }
            catch (Exception e)
            {
                D.ebugPrintln(e.toString());
                D.ebugPrintln("Unable to write to disk.");
            }
        }
    }

    /**
     * this is a debugging command that gives resources to a player. Format:
     * rsrcs: #cl #or #sh #wh #wo playername
     * 
     * @throws NullPointerExceptioin
     *             if game or mes are null
     */
    protected void giveResources(String mes, Game game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(6));
        int[] resources = new int[ResourceConstants.WOOD + 1];
        int resourceType = ResourceConstants.CLAY;
        String name = "";

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (resourceType <= ResourceConstants.WOOD)
            {
                resources[resourceType] = Integer.parseInt(token);
                resourceType++;
            }
            else
            {
                name = token;
                break;
            }
        }

        ResourceSet rset = game.getPlayer(name).getResources();
        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " gets";

        for (resourceType = ResourceConstants.CLAY; resourceType <= ResourceConstants.WOOD; resourceType++)
        {
            rset.add(resources[resourceType], resourceType);
            outMes += (" " + resources[resourceType]);

            // ResourceConstants.CLAY == PlayerElement.CLAY
            messageToGame(game.getName(), new PlayerElement(game.getName(),
                    pnum, PlayerElement.GAIN, resourceType,
                    resources[resourceType]));
        }

        messageToGame(game.getName(), outMes);
    }

    /**
     * this broadcasts game information to all people connected used to display
     * the scores on the player client
     */
    protected void broadcastGameStats(Game ga)
    {
        /*
         * if (ga != null) { int scores[] = new int[Game.MAXPLAYERS]; boolean
         * robots[] = new boolean[Game.MAXPLAYERS]; for (int i = 0; i <
         * Game.MAXPLAYERS; i++) { Player player = ga.getPlayer(i); if (player
         * != null) { if (ga.isSeatVacant(i)) { scores[i] = -1; robots[i] =
         * false; } else { scores[i] = player.getPublicVP(); robots[i] =
         * player.isRobot(); } } else { scores[i] = -1; } }
         * 
         * broadcast(GameStats.toCmd(ga.getName(), scores, robots)); }
         */
    }

    /**
     * check for games that have expired and destroy them. If games are about to
     * expire, send a warning. As of version 1.1.09, practice games (
     * {@link Game#isLocal} flag set) don't expire.
     * 
     * @see #GAME_EXPIRE_WARN_MINUTES
     * @see GameTimeoutChecker#run()
     */
    public void checkForExpiredGames()
    {
        Vector expired = new Vector();

        gameList.takeMonitor();

        // Add 2 minutes because of coarse 5-minute granularity in
        // GameTimeoutChecker.run()
        long warn_ms = (2 + GAME_EXPIRE_WARN_MINUTES) * 60L * 1000L;

        try
        {
            final long currentTimeMillis = System.currentTimeMillis();
            for (Enumeration k = gameList.getGamesData(); k.hasMoreElements();)
            {
                Game gameData = (Game) k.nextElement();
                if (gameData.isLocal)
                    continue; // <--- Skip practice games, they don't expire ---

                long gameExpir = gameData.getExpiration();

                // Start our text messages with ">>>" to mark as urgent to the
                // client.

                if (gameExpir <= currentTimeMillis)
                {
                    final String gameName = gameData.getName();
                    expired.addElement(gameName);
                    messageToGameUrgent(gameName,
                            ">>> The time limit on this game has expired and will now be destroyed.");
                }
                else if ((gameExpir - warn_ms) <= currentTimeMillis)
                {
                    //
                    // Give people a few minutes' warning (they may have a few
                    // warnings)
                    //
                    long minutes = ((gameExpir - currentTimeMillis) / 60000);
                    if (minutes < 1L)
                        minutes = 1; // in case of rounding down

                    messageToGameUrgent(
                            gameData.getName(),
                            ">>> Less than "
                                    + minutes
                                    + " minutes remaining.  Type *ADDTIME* to extend this game another 30 minutes.");
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in checkForExpiredGames - " + e);
        }

        gameList.releaseMonitor();

        //
        // destroy the expired games
        //
        for (Enumeration ex = expired.elements(); ex.hasMoreElements();)
        {
            String ga = (String) ex.nextElement();
            gameList.takeMonitor();

            try
            {
                destroyGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in checkForExpired - " + e);
            }

            gameList.releaseMonitor();
            broadcast(DeleteGame.toCmd(ga));
        }
    }

    /**
     * this is a debugging command that gives a dev card to a player.
     * 
     * <PRE>
     * dev: cardtype player
     * </PRE>
     * 
     * For card-types numbers, see {@link DevCardConstants}
     */
    protected void giveDevCard(String mes, Game game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(5));
        String name = "";
        int cardType = -1;

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (cardType < 0)
            {
                cardType = Integer.parseInt(token);
            }
            else
            {
                name = token;
                break;
            }
        }

        DevCardSet dcSet = game.getPlayer(name).getDevCards();
        dcSet.add(1, DevCardSet.NEW, cardType);

        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " gets a " + cardType + " card.";
        messageToGame(game.getName(), new DevCard(game.getName(), pnum,
                DevCard.DRAW, cardType));
        messageToGame(game.getName(), outMes);
    }

    /**
     * Quick-and-dirty command line parsing of game options. Calls
     * {@link GameOption#setKnownOptionCurrentValue(GameOption)}.
     * 
     * @param optNameValue
     *            Game option name+value, of form expected by
     *            {@link GameOption#parseOptionNameValue(String, boolean)}
     * @return true if OK, false if bad name or value
     * @since 1.1.07
     */
    public static boolean parseCmdline_GameOption(final String optNameValue)
    {
        GameOption op = GameOption.parseOptionNameValue(optNameValue, true);
        if (op == null)
        {
            System.err.println("Unknown or malformed game option: "
                    + optNameValue);
            return false;
        }
        if (op.optType == GameOption.OTYPE_UNKNOWN)
        {
            System.err.println("Unknown game option: " + op.optKey);
            return false;
        }

        try
        {
            GameOption.setKnownOptionCurrentValue(op);
            return true;
        }
        catch (Throwable t)
        {
            System.err.println("Bad value, cannot set game option: "
                    + op.optKey);
            return false;
        }
    }

    /**
     * Quick-and-dirty parsing of command-line arguments with dashes.
     *<P>
     * If any game options are set ("-o", "--option"), then
     * {@link #hasSetGameOptions} is set to true, and
     * {@link GameOption#setKnownOptionCurrentValue(GameOption)} is called to
     * set them globally.
     * 
     * @param args
     *            args as passed to main
     * @return Properties collection of args, or null for argument error. Will
     *         contain at least {@link #PROP_OPENSETTLERS_PORT},
     *         {@link #PROP_OPENSETTLERS_CONNECTIONS},
     *         {@link SOCDBHelper#PROP_OPENSETTLERS_DB_USER},
     *         {@link SOCDBHelper#PROP_OPENSETTLERS_DB_PASS}.
     * @since 1.1.07
     */
    public static Properties parseCmdline_DashedArgs(String[] args)
    {
        Properties argp = new Properties();

        int aidx = 0;
        while ((aidx < args.length) && (args[aidx].startsWith("-")))
        {
            String arg = args[aidx];

            if (arg.equals("-V") || arg.equalsIgnoreCase("--version"))
            {
                printVersionText();
            }
            else if (arg.equalsIgnoreCase("-h") || arg.equals("?")
                    || arg.equalsIgnoreCase("--help"))
            {
                printUsage(true);
            }
            else if (arg.startsWith("-o") || arg.equalsIgnoreCase("--option"))
            {
                hasSetGameOptions = true;
                String argValue;
                if (arg.startsWith("-o") && (arg.length() > 2))
                {
                    argValue = arg.substring(2);
                }
                else
                {
                    ++aidx;
                    if (aidx < args.length)
                        argValue = args[aidx];
                    else
                        argValue = null;
                }
                if (argValue != null)
                {
                    if (!parseCmdline_GameOption(argValue))
                        argValue = null;
                }
                if (argValue == null)
                {
                    System.err
                            .println("Missing required option name/value after "
                                    + arg);
                    printGameOptions();
                    return null;
                }
            }
            else if (arg.startsWith("-D")) // java-style props defines
            {
                // We get to here when a user uses -Dname=value. However, in
                // some cases, the OS goes ahead and parses this out to args
                // {"-Dname", "value"}
                // so instead of parsing on "=", we just make the "-D"
                // characters go away and skip one argument forward.

                String name;
                if (arg.length() == 2) // "-D something"
                {
                    ++aidx;
                    if (aidx < args.length)
                    {
                        name = args[aidx];
                    }
                    else
                    {
                        System.err.println("Missing property name after -D");
                        return null;
                    }
                }
                else
                {
                    name = arg.substring(2, arg.length());
                }
                String value = null;
                int posEq = name.indexOf("=");
                if (posEq > 0)
                {
                    value = name.substring(posEq + 1);
                    name = name.substring(0, posEq);
                }
                else if (aidx < args.length - 1)
                {
                    ++aidx;
                    value = args[aidx];
                }
                else
                {
                    System.err.println("Missing value for property " + name);
                    return null;
                }
                argp.setProperty(name, value);

            }
            else
            {
                System.err.println("Unknown argument: " + arg);
            }
            ++aidx;
        }

        // Done parsing flagged parameters.
        // Look for the positional ones.
        if ((args.length - aidx) < 4)
        {
            if (!printedUsageAlready)
            {
                // Print this hint only if parsed OK up to now, and
                // if we haven't responded to -h / --help already.
                System.err
                        .println("SOCServer: Some required command-line parameters are missing.");
            }
            printUsage(false);
            return null;
        }
        argp.setProperty(PROP_OPENSETTLERS_PORT, args[aidx]);
        ++aidx;
        argp.setProperty(PROP_OPENSETTLERS_CONNECTIONS, args[aidx]);
        ++aidx;
        argp.setProperty(SOCDBHelper.PROP_OPENSETTLERS_DB_USER, args[aidx]);
        ++aidx;
        argp.setProperty(SOCDBHelper.PROP_OPENSETTLERS_DB_PASS, args[aidx]);
        ++aidx;

        if (aidx < args.length)
        {
            if (!printedUsageAlready)
            {
                if (args[aidx].startsWith("-"))
                {
                    System.err
                            .println("SOCServer: Options must appear before, not after, the port number.");
                }
                else
                {
                    System.err
                            .println("SOCServer: Options must appear before the port number, not after dbpass.");
                }
                printUsage(false);
            }
            return null;
        }

        // Done parsing.
        return argp;
    }

    /**
     * Track whether we've already called {@link #printUsage(boolean)}.
     * 
     * @since 1.1.07
     */
    public static boolean printedUsageAlready = false;

    /**
     * Print command line parameter information, including options ("--" / "-").
     * 
     * @param longFormat
     *            short or long? Long format gives details and also calls
     *            {@link #printVersionText()} beforehand. Short format is
     *            printed at most once, after checking
     *            {@link #printedUsageAlready}.
     * @since 1.1.07
     */
    public static void printUsage(final boolean longFormat)
    {
        if (printedUsageAlready && !longFormat)
            return;
        printedUsageAlready = true;

        if (longFormat)
        {
            printVersionText();
        }
        System.err
                .println("usage: java soc.server.SOCServer [option...] port_number max_connections dbUser dbPass");
        if (longFormat)
        {
            System.err.println("usage: recognized options:");
            System.err
                    .println("       -V or --version    : print version information");
            System.err.println("       -h or --help or -? : print this screen");
            System.err
                    .println("       -o or --option name=value : set per-game options' default values");
            System.err.println("       -D name=value : set properties such as "
                    + SOCDBHelper.PROP_OPENSETTLERS_DB_USER);
            printGameOptions();
        }
        else
        {
            System.err
                    .println("       use java soc.server.SOCServer --help to see recognized options");
        }
    }

    /**
     * Print out the list of possible game options, and current values.
     * 
     * @since 1.1.07
     */
    public static void printGameOptions()
    {
        Hashtable allopts = GameOption.getAllKnownOptions();
        System.err.println("-- Current default game options: --");
        for (Enumeration e = allopts.keys(); e.hasMoreElements();)
        {
            String okey = (String) e.nextElement();
            GameOption opt = (GameOption) allopts.get(okey);
            boolean quotes = (opt.optType == GameOption.OTYPE_STR)
                    || (opt.optType == GameOption.OTYPE_STRHIDE);
            // OTYPE_* - consider any type-specific output in this method.

            StringBuffer sb = new StringBuffer("  ");
            sb.append(okey);
            sb.append(" (");
            sb.append(GameOption.optionTypeName(opt.optType));
            sb.append(") ");
            if (quotes)
                sb.append('"');
            opt.packValue(sb);
            if (quotes)
                sb.append('"');
            sb.append("  ");
            sb.append(opt.optDesc);
            System.err.println(sb.toString());
            if (opt.enumVals != null) // possible values of OTYPE_ENUM
            {
                sb = new StringBuffer("    option choices (1-n): ");
                for (int i = 1; i <= opt.maxIntValue; ++i)
                {
                    sb.append(' ');
                    sb.append(i);
                    sb.append(' ');
                    sb.append(opt.enumVals[i - 1]);
                    sb.append(' ');
                }
                System.err.println(sb.toString());
            }
        }

        int optsVers = GameOption.optionsMinimumVersion(allopts);
        if (optsVers > -1)
        {
            System.err.println("*** Note: Client version "
                    + Version.version(optsVers)
                    + " or newer is required for these game options. ***");
            System.err
                    .println("          Games created with different options may not have this restriction.");
        }
    }

    /**
     * Starting the server from the command line
     * 
     * @param args
     *            arguments: port number, etc
     * @see #printUsage(boolean)
     */
    static public void main(String[] args)
    {
        // if (args.length < 4)
        // {
        // if (! printedUsageAlready)
        // {
        // // Print this hint only if parsed OK up to now, and
        // // if we haven't responded to -h / --help already.
        // System.err.println("SOCServer: Some required command-line parameters are missing.");
        // }
        // printUsage(false);
        // return;
        // }

        Properties argp = parseCmdline_DashedArgs(args);
        if (argp == null)
        {
            printUsage(false);
            return;
        }

        try
        {
            int port = Integer.parseInt(argp
                    .getProperty(PROP_OPENSETTLERS_PORT));

            // SOCServer constructor will also print game options if we've set
            // them on
            // commandline, or if any option defaults require a minimum client
            // version.

            SOCServer server = new SOCServer(port, argp);
            server.setPriority(5);
            server.start(); // <---- Start the Main SOCServer Thread ----

            // Most threads are started in the SOCServer constructor, via
            // initSocServer.
        }
        catch (Throwable e)
        {
            printUsage(false);
            return;
        }
    } // main

    /**
     * Each local robot gets its own thread. Equivalent to main thread in
     * RobotClient in network games.
     *<P>
     * Before 1.1.09, this class was part of PlayerClient.
     * 
     * @see SOCServer#setupLocalRobots(int, int, int)
     * @since 1.1.00
     */
    private static class SOCPlayerLocalRobotRunner implements Runnable
    {
        RobotClient rob;

        protected SOCPlayerLocalRobotRunner(RobotClient rc)
        {
            rob = rc;
        }

        public void run()
        {
            Thread.currentThread().setName("robotrunner-" + rob.getNickname());
            rob.init();
        }

        /**
         * Create and start a robot client within a
         * {@link SOCPlayerLocalRobotRunner} thread. After creating it,
         * {@link Thread#yield() yield} the current thread and then sleep 75
         * milliseconds, to give the robot time to start itself up.
         * 
         * @param rname
         *            Name of robot
         * @param strSocketName
         *            Server's stringport socket name, or null
         * @param port
         *            Server's tcp port, if <tt>strSocketName</tt> is null
         * @since 1.1.09
         * @see SOCServer#setupLocalRobots(int, int)
         */
        public static void createAndStartRobotClientThread(final String rname,
                final String strSocketName, final int port)
        {
            RobotClient rcli;
            if (strSocketName != null)
                rcli = new RobotClient(strSocketName, rname, "pw");
            else
                rcli = new RobotClient("localhost", port, rname, "pw");
            Thread rth = new Thread(new SOCPlayerLocalRobotRunner(rcli));
            rth.setDaemon(true);
            rth.start();

            Thread.yield();
            try
            {
                Thread.sleep(75); // Let that robot go for a bit.
                // robot runner thread will call its init()
            }
            catch (InterruptedException ie)
            {
            }
        }

    } // nested static class SOCPlayerLocalRobotRunner

} // public class SOCServer
