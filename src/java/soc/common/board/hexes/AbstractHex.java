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
