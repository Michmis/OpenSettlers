package soc.common.client.visuals.board;

import soc.common.board.Chit;
import soc.common.client.visuals.AbstractPieceVisual;

public abstract class AbstractChitVisual extends AbstractPieceVisual implements
        ChitVisual
{
    protected Chit chit;
    final protected BoardVisual parent;

    protected void updateChit()
    {
    }

    public AbstractChitVisual(Chit chit, BoardVisual parent)
    {
        this.chit = chit;
        this.parent = parent;
    }

    @Override
    public Chit getChit()
    {
        return chit;
    }

    @Override
    public ChitVisual setChit(Chit chit)
    {
        this.chit = chit;

        updateChit();

        return this;
    }
}
