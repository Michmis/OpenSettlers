package soc.common.board.hexes;

import soc.common.board.HexLocation;
import soc.common.board.territories.Territory;
import soc.common.utils.ClassUtils;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;

/* Represents the base type for each hex.
 * @seealso cref="http://www.codeproject.com/KB/cs/hexagonal_part1.aspx"/>
 * @seealso cref="http://gmc.yoyogames.com/index.php?showtopic=336183"/>
 */
public abstract class AbstractHex implements Hex
{
    protected HexLocation hexLocation;
    protected Territory territory;
    protected SimpleEventBus eventBus = new SimpleEventBus();
    protected String name;

    /*
     * Length of a side. Base constant from where all other values are
     * calculated from.
     */
    private static double sideLength = 50;
    private static double h;
    private static double halfWidth;
    private static double b;
    private static double width;

    static
    {
        // TODO: come up with descriptive names for h and b
        h = Math.sin(DegreesToRadians(30)) * sideLength;
        halfWidth = Math.cos(DegreesToRadians(30)) * sideLength;
        b = sideLength + 2 * h;
        width = 2 * halfWidth;
    }

    // / <summary>
    // / The width of the hex measured from outer left to the middle
    // /
    // / HalfWidth
    // / | |
    // / __
    // / / \
    // / | |
    // / \ /
    // / --
    // / </summary>
    public static double getHalfWidth()
    {
        return halfWidth;
    }

    // / <summary>
    // / Total width of the hex
    // / </summary>
    // /
    // / Width
    // / | |
    // / __
    // / / \
    // / | |
    // / \ /
    // / --
    // / </summary>
    public static double getWidth()
    {
        return width;
    }

    // / <summary>
    // / Total height of the hex
    // / </summary>
    public static double getHeight()
    {
        return b;
    }

    // / <summary>
    // / Height measured from top to the first line
    // / __ _
    // / / \ _ } PartialHeight
    // / | |
    // / \ /
    // / --
    // / </summary>
    public static double getPartialHeight()
    {
        return sideLength + h;
    }

    // / <summary>
    // / Height measured from the top to the second line
    // / __ _
    // / / \ } BottomHeight
    // / | | _ }
    // / \ /
    // / --
    // / </summary>
    public static double getBottomHeight()
    {
        return h;
    }

    public static double getHalfHeight()
    {
        return b / 2;
    }

    // / <summary>
    // / Size of the hex, measured one line
    // /
    // / size
    // / | |
    // / __
    // / / \ _
    // / | | _ } size
    // / \ /
    // / --
    // / </summary>
    public static int getSize()
    {
        return (int) sideLength;
    }

    // / <summary>
    // / Helper function for size calculation
    // / </summary>
    // / @param degrees
    static private double DegreesToRadians(double degrees)
    {
        return degrees * Math.PI / 180;
    }

    @Override
    public Territory getTerritory()
    {
        return territory;
    }

    @Override
    public Hex setTerritory(Territory t)
    {
        if (t != territory)
        {
            this.territory = t;
            eventBus.fireEvent(new TerritoryChangedEvent(t));
        }

        return this;
    }

    public AbstractHex()
    {
        name = ClassUtils.getSimpleClassName(this.getClass().getName());
    }

    public AbstractHex setLocation(HexLocation hexLocation)
    {
        this.hexLocation = hexLocation;

        return this;
    }

    public HexLocation getLocation()
    {
        return hexLocation;
    }

    public AbstractHex copy()
    {
        throw new RuntimeException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.getClass().toString() + " [hexLocation=" + hexLocation
                + "]";
    }

    public String getName()
    {
        return name;
    }

    public HandlerRegistration addTerritoryChangedEventHandler(
            TerritoryChangedEventHandler handler)
    {
        return eventBus.addHandler(TerritoryChangedEvent.TYPE, handler);
    }
}