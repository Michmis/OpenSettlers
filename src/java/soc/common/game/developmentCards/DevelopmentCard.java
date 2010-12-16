package soc.common.game.developmentCards;

import soc.common.board.resources.Ore;
import soc.common.board.resources.ResourceList;
import soc.common.board.resources.Sheep;
import soc.common.board.resources.Wheat;
import soc.common.game.Game;
import soc.common.game.Player;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.turnPhase.TurnPhase;
import soc.common.utils.ClassUtils;

public class DevelopmentCard
{
    protected String invalidMessage;
    protected String message;
    protected int turnBought = 0;
    private int id = 0;
    private boolean isPlayable = false;
    private static ResourceList cost = new ResourceList();
    
    static
    {
        cost.add(new Wheat());
        cost.add(new Ore());
        cost.add(new Sheep());
    }
    
    /**
     * @return the hasSummoningSickness
     */
    public boolean isHasSummoningSickness()
    {
        return true;
    }

    public static boolean canPay(Player player)
    {
        //TODO: implement
        return true;
    }
    
    public ResourceList getCost()
    {
        return cost;
    }
    
    public void play(Game game, Player player)
    {
        isPlayable = false;
    }
    
    public boolean isValid(Game game)
    {
        return true;
    }
    
    /*
     * Default is not to keep the DevelopmentCard in stock
     */
    public boolean keepInStock()
    {
        return false;
    }
    
    /* 
     * Returns true if player is allowed to play this card in given TurnPhase
     */
    public boolean isAllowed(TurnPhase turnPhase)
    {
        throw new RuntimeException();
    }
    
    /* 
     * Returns true if player is allowed to play this card in given GamePhase
     */
    public boolean isAllowed(GamePhase turnPhase)
    {
        throw new RuntimeException();
    }
    
    public String getInvalidMessage()
    {
        return invalidMessage;
    }
    public void setInvalidMessage(String invalidMessage)
    {
        this.invalidMessage = invalidMessage;
    }
    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
    }
    public int getTurnBought()
    {
        return turnBought;
    }
    public void setTurnBought(int turnBought)
    {
        this.turnBought = turnBought;
    }
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public boolean isPlayable()
    {
        return isPlayable;
    }
    public void setPlayable(boolean isPlayable)
    {
        this.isPlayable = isPlayable;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return ClassUtils.getSimpleClassName(this.getClass().getName());
    }
    
    
}
