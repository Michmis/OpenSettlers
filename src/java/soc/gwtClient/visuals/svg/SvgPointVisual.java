package soc.gwtClient.visuals.svg;

import org.vaadin.gwtgraphics.client.Group;
import org.vaadin.gwtgraphics.client.VectorObject;
import org.vaadin.gwtgraphics.client.animation.Animate;
import org.vaadin.gwtgraphics.client.shape.Circle;

import soc.common.board.HexPoint;
import soc.common.client.visuals.PieceVisual;
import soc.common.client.visuals.game.GameBoardVisual;
import soc.common.client.visuals.game.PointVisual;
import soc.gwtClient.game.Point2D;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;

public class SvgPointVisual extends PointVisual implements SvgVisual,
        MouseMoveHandler, MouseOutHandler, ClickHandler
{
    private Point2D location;
    private Group group;
    private Circle circle;
    private int radius;

    public SvgPointVisual(GameBoardVisual parent, HexPoint hexPoint,
            Point2D location)
    {
        super(parent, hexPoint);

        this.location = location;

        this.group = new Group();
        radius = parent.getSize() / 5;
        circle = new Circle((int) location.getX(), (int) location.getY(),
                radius);
        circle.setFillColor("yellow");
        circle.setStrokeWidth(0);
        group.add(circle);

        group.addMouseOutHandler(this);
        group.addMouseMoveHandler(this);
        group.addClickHandler(this);

        // default on not showing
        setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see soc.common.client.visuals.PieceVisual#updateSelected()
     */
    @Override
    protected void updateSelected()
    {
        if (selected)
        {
            // Animate to grow 2x its size
            new Animate(circle, "radius", radius, radius * 2, 100).start();
        }
        else
        {
            // Animate back to 1x its size
            new Animate(circle, "radius", radius * 2, radius, 100).start();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see soc.common.client.visuals.AbstractPieceVisual#updateVisible()
     */
    @Override
    protected void updateVisible()
    {
        group.setVisible(visible);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.client.visuals.game.IPointVisual#addPieceVisual(soc.common
     * .client.visuals.PieceVisual)
     */
    @Override
    public void addPieceVisual(PieceVisual pieceVisual)
    {
        pieceVisuals.add(pieceVisual);
        group.add(((SvgVisual) pieceVisual).getVectorObject());
    }

    @Override
    public VectorObject getVectorObject()
    {
        return group;
    }

    @Override
    public void onMouseMove(MouseMoveEvent event)
    {
        parent.getBehaviour().mouseEnter(this, parent);
    }

    @Override
    public void onMouseOut(MouseOutEvent event)
    {
        parent.getBehaviour().mouseOut(this, parent);
    }

    @Override
    public void onClick(ClickEvent event)
    {
        parent.getBehaviour().clicked(this, parent);
    }

}
