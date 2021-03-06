package soc.common.game.variants.rules;

import soc.common.annotations.SeaFarers;
import soc.common.board.hexes.ResourceHex;
import soc.common.board.resources.Gold;
import soc.common.game.GameRules;

@SeaFarers
public class AddGold implements GameRule
{

    @Override
    public void set(GameRules rules)
    {
        rules.getHexTypes().add(new ResourceHex(new Gold()));
        rules.getSupportedResources().add(new Gold());
    }

    @Override
    public String getDescription()
    {
        // TODO fix message
        return null;
    }

}
