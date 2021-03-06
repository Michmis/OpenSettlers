package soc.common.board.territories;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soc.common.annotations.SeaFarers;

import com.google.gwt.event.shared.SimpleEventBus;

@SeaFarers
public class TerritoryList implements Iterable<TerritoryImpl>
{
    List<TerritoryImpl> territories = new ArrayList<TerritoryImpl>();
    SimpleEventBus eventBus = new SimpleEventBus();

    public void add(TerritoryImpl territory)
    {
        territories.add(territory);
        eventBus.fireEvent(new TerritoriesChangedEvent(territory, null));
    }

    public Territory findByID(int id)
    {
        for (Territory t : this)
        {
            if (t.getID() == id)
                return t;
        }

        throw new RuntimeException();
    }

    public TerritoryList addNew(TerritoryImpl t)
    {
        this.add(t);

        return this;
    }

    private boolean containsMainland()
    {
        for (Territory territory : this)
        {
            if (territory.isMainland())
                return true;
        }
        return false;
    }

    public Territory createTerritory(boolean mainland)
    {
        Territory result = new TerritoryImpl();

        if (mainland)
        {
            result.setID(0);
        }
        else
        {
            int id = this.size();

            if (!containsMainland())
            {
                id--;
            }
            result.setID(id);
        }

        return result;
    }

    @Override
    public Iterator<TerritoryImpl> iterator()
    {
        return territories.iterator();
    }

    public int size()
    {
        return territories.size();
    }

    public Territory get(int index)
    {
        return territories.get(index);
    }
}
