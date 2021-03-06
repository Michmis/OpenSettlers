package soc.common.game.dices;

import soc.common.annotations.SeaFarers;
import soc.common.server.random.Random;

/*
 * Represents a single 6-sided dice. Rolled when a volcano produces resources by
 * the player currently on turn. Each HexPoint matching the rolled dice number is
 * hit by the volcano. A volcano has 6 HexPoints, corresponding with the rolld number.
 * 
 * When an HexPoint is hit four things can happen. When no town or city is present,
 * nothing happens (1). When a town resides on the HexPoint, it will be blown up 
 * (removed from the board, put back into players' stock) (2). If a city is present,
 * a town is put back when the player has a town in stock (3). If a city is present 
 * and the player does not have a town in stock, the city is removed and no town is 
 * placed back on the city's HexPoint (4).
 */
@SeaFarers
public class VolcanoDice implements Dice
{
    private int dice = 0;

    /**
     * @return the dice
     */
    public int getDice()
    {
        return dice;
    }

    /*
     * (non-Javadoc)
     * 
     * @see soc.common.game.dices.Dice#roll(java.util.Random)
     */
    @Override
    public void roll(Random random)
    {
        dice = random.nextInt(6);
    }

    /**
     * @param dice
     *            the dice to set
     */
    public VolcanoDice setDice(int dice)
    {
        this.dice = dice;

        return this;
    }

}
