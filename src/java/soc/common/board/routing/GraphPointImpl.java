package soc.common.board.routing;

import soc.common.board.HexPoint;
import soc.common.board.pieces.PlayerPiece;
import soc.common.game.GamePlayer;

public class GraphPointImpl implements GraphPoint
{
    private HexPoint pointLocation;
    private transient PlayerPiece playerPiece;
    private boolean townBuildable = true;

    @Override
    public HexPoint getPoint()
    {
        return pointLocation;
    }

    public GraphPoint setPoint(HexPoint point)
    {
        this.pointLocation = point;

        return this;
    }

    @Override
    public PlayerPiece getPiece()
    {
        return playerPiece;
    }

    @Override
    public GamePlayer getPlayer()
    {
        return playerPiece == null ? null : playerPiece.getPlayer();
    }

    @Override
    public GraphElement setPlayerPiece(PlayerPiece piece)
    {
        this.playerPiece = piece;

        return this;
    }

    @Override
    public boolean isTownBuildable()
    {
        return townBuildable;
    }

    @Override
    public GraphPoint setTownBuildable(boolean townBuildable)
    {
        this.townBuildable = townBuildable;

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return pointLocation.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GraphPointImpl other = (GraphPointImpl) obj;
        if (other.getPoint().equals(pointLocation))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
