package soc.common.client.behaviour.editor;

import soc.common.board.hexes.Hex;
import soc.common.board.hexes.ResourceHex;
import soc.common.board.resources.Timber;
import soc.common.client.behaviour.IInteractionBehaviour;
import soc.common.client.visuals.IPieceVisual;
import soc.common.client.visuals.board.IBoardVisual;
import soc.common.client.visuals.board.IHexVisual;

public class SetHexBehaviour implements IInteractionBehaviour
{
    private Hex hex = new ResourceHex(new Timber());
    /**
     * @return the hex
     */
    public Hex getHex()
    {
        return hex;
    }

    /**
     * @param hex the hex to set
     */
    public SetHexBehaviour setHex(Hex h)
    {
        this.hex = h;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }

    
    @Override
    public void clicked(IPieceVisual pieceVisual, IBoardVisual board)
    {
        if (pieceVisual instanceof IHexVisual)
        {
            IHexVisual hexVisual = (IHexVisual)pieceVisual;
            Hex newHex = hex.copy();
            newHex.setLocation(hexVisual.getHex().getLocation());
            hexVisual.setHex(newHex);
        }
    }

    @Override
    public void mouseEnter(IPieceVisual pieceVisual, IBoardVisual board)
    {
        pieceVisual.setSelected(true);
    }

    @Override
    public void mouseOut(IPieceVisual pieceVisual, IBoardVisual board)
    {
        pieceVisual.setSelected(false);
        
    }
}