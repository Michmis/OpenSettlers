package soc.common.game.statuses;

/*
 * Status where the game has been ended. 
 */
public class Ended implements GameStatus
{
    @Override
    public boolean isGameBlocking()
    {
        return false;
    }
}
