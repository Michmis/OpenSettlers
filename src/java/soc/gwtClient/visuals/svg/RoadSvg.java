package soc.gwtClient.visuals.svg;

import org.vaadin.gwtgraphics.client.VectorObject;
import org.vaadin.gwtgraphics.client.shape.Rectangle;

import soc.common.board.pieces.Road;
import soc.common.client.visuals.game.AbstractRoadVisual;
import soc.gwtClient.game.Point2D;

public class RoadSvg extends AbstractRoadVisual implements SvgVisual
{
    GameBoardSvg parent;
    Rectangle rectangle;

    public RoadSvg(Road road, GameBoardSvg parent)
    {
        super(road);
        this.parent = parent;

        Point2D location = parent.getBoardSvg().CalculatePosition(
                road.getSide());

        double width = parent.getSize() * 0.6;
        double height = parent.getSize() * 0.2;

        rectangle = new Rectangle((int) ((int) location.getX() - (width / 2)),
                (int) location.getY(), (int) width, (int) height);
        rectangle.setStrokeWidth(2);
        rectangle.setFillColor(parent.getGame().getCurrentTurn().getPlayer()
                .getColor());

        int degrees = 0;
        switch (road.getSide().getDirection())
        {
        case SLOPEDOWN:
            degrees = 30;
            break;
        case SLOPEUP:
            degrees = 330;
            break;
        case UPDOWN:
            degrees = 90;
            break;
        }

        rectangle.setRotation(degrees);
    }

    @Override
    public VectorObject getVectorObject()
    {
        return rectangle;
    }

}
