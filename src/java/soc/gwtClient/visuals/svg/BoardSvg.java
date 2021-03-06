package soc.gwtClient.visuals.svg;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.shape.Rectangle;

import soc.common.board.Board;
import soc.common.board.HexChangedEvent;
import soc.common.board.HexChangedEventHandler;
import soc.common.board.HexLocation;
import soc.common.board.HexPoint;
import soc.common.board.HexPointType;
import soc.common.board.HexSide;
import soc.common.board.hexes.Hex;
import soc.common.client.visuals.board.AbstractBoardVisual;
import soc.common.client.visuals.board.HexVisual;
import soc.gwtClient.editor.BehaviourChanged;
import soc.gwtClient.game.Point2D;

import com.google.gwt.user.client.ui.Widget;

public class BoardSvg extends AbstractBoardVisual implements
        HexChangedEventHandler
{
    private DrawingArea drawingArea;
    private Rectangle enabledOverlay;

    /**
     * @return the drawingArea
     */
    public DrawingArea getDrawingArea()
    {
        return drawingArea;
    }

    public BoardSvg(int widthInPixels, int heightInPixels, Board b)
    {
        drawingArea = new DrawingArea(widthInPixels, heightInPixels);

        board = b;

        initializeBoard();

        board.getHexes().addHexChangedHandler(this);
    }

    private void initializeBoard()
    {
        // Iterate over all hexes, create a HexVisual and attach event handlers
        for (Hex hex : board.getHexes())
        {
            final HexSvg hv = new HexSvg(hex, this);

            drawingArea.add(hv.getGroup());
            hexVisuals.put(hex, hv);
        }

        enabledOverlay = new Rectangle(0, 0, drawingArea.getWidth(),
                drawingArea.getHeight());
        enabledOverlay.setFillColor("Black");
        enabledOverlay.setFillOpacity(0.5);
    }

    public Point2D calculatePosition(HexLocation location)
    {
        double margin = 5;
        double marginLeft = getHalfWidth();
        double x = location.getW() * (getWidth() + margin);
        double y = location.getH() * (getPartialHeight() + margin);

        x += marginLeft;

        // Alternate half the width of an hex
        if (location.getH() % 2 == 0)
            x += getHalfWidth();

        // center the position (not necessary in 2D view)
        // x -= Hex.getHalfWidth() * board.getWidth();
        // y -= ((Hex.getPartialHeight() * board.getHeight()) +
        // Hex.getBottomHeight()) / 2;

        return new Point2D((int) x, (int) y);
    }

    public Point2D CalculatePosition(HexSide location)
    {
        Point2D result = calculatePosition(location.getHighestOrLeftestHex());
        double x = result.getX();
        double y = result.getY();
        x -= getHalfWidth();

        switch (location.getDirection())
        {
        case SLOPEDOWN:
            x += getWidth() * 0.25;
            y += (getBottomHeight() * 0.5) + getPartialHeight();
            break;
        case SLOPEUP:
            x += getWidth() * 0.75;
            y += (getBottomHeight() * 0.5) + getPartialHeight();
            break;
        case UPDOWN:
            x += getWidth();
            y += getHeight() * 0.5;
            break;
        }
        return new Point2D((int) x, (int) y);
    }

    public Point2D CalculatePosition(HexPoint location)
    {
        // get the x,y coordinate of the topmost HexLocation
        Point2D point = calculatePosition(location.getTopMost());

        // Point is immutable, so cache the values
        double x = point.getX();
        double y = point.getY();

        if (location.getPointType() == HexPointType.UPPERROW1)
        {
            // x += getHalfWidth();
            y += getHeight();
        }
        else
        {
            x += getHalfWidth();
            y += getPartialHeight();
        }

        return new Point2D((int) x, (int) y);
    }

    @Override
    public void onBehaviourChanged(BehaviourChanged behaviourChanged)
    {
        editBehaviour = behaviourChanged.getBehaviour();
    }

    /*
     * (non-Javadoc)
     * 
     * @see soc.common.client.visuals.PieceVisual#updateEnabled()
     */
    @Override
    protected void updateEnabled()
    {
        enabledOverlay.setVisible(enabled);
    }

    @Override
    public Widget asWidget()
    {
        return drawingArea;
    }

    @Override
    public void resize(int width, int height)
    {
        // Ensure underlying drawing area uses up all available space
        drawingArea.setSize(width + "px", height + "px");

        // First, calculate the projected width & height from the old values
        double projectedWidth = (board.getWidth() + 1) * getWidth();
        double projectedHeight = (board.getHeight() + 1) * getBottomHeight();

        // Then, calculate for width and height a scale factor
        double widthFactor = projectedWidth / width;
        double heightFactor = projectedHeight / height;

        // Determine the factor to use, which is th lowest of the scale factors
        double factor = widthFactor > heightFactor ? heightFactor : widthFactor;

        // Scale the sidelength to the new factor
        sideLength = sideLength * factor;

        // Update the rest of the hex sizes
        calculateHexSizes();

        // Let all children resize and reposition themselves
        for (HexVisual hexVisual : hexVisuals.values())
        {
            HexSvg svgHexVisual = (HexSvg) hexVisual;
            svgHexVisual.resizeAndPosition();
        }
    }

    @Override
    public void onHexChanged(HexChangedEvent event)
    {
        // Get rid of old hex
        HexVisual hv = hexVisuals.get(event.getOldHex());
        hv.setHex(event.getNewHex());
    }
}
