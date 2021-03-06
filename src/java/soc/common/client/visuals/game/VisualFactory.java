package soc.common.client.visuals.game;

import soc.common.board.Board;
import soc.common.board.pieces.City;
import soc.common.board.pieces.Piece;
import soc.common.board.pieces.Pirate;
import soc.common.board.pieces.PlayerPiece;
import soc.common.board.pieces.Road;
import soc.common.board.pieces.Robber;
import soc.common.board.pieces.Town;
import soc.common.board.routing.GraphPoint;
import soc.common.board.routing.GraphSide;
import soc.common.client.visuals.PieceVisual;
import soc.common.client.visuals.board.BoardVisual;

public interface VisualFactory
{
    public PieceVisual createPlayerPieceVisual(PlayerPiece piece);

    public PieceVisual createPieceVisual(Piece piece);

    public RobberVisual createRobberVisual(Robber robber);

    public PirateVisual createPirateVisual(Pirate pirate);

    public TownVisual createTownVisual(Town town);

    public CityVisual createCityVisual(City city);

    public RoadVisual createRoadVisual(Road road);

    public BoardVisual createBoardVisual(Board board);

    public SideVisual createSideVisual(GraphSide side);

    public PointVisual createPointVisual(GraphPoint point);
}
