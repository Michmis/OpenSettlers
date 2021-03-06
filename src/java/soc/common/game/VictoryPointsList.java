package soc.common.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.event.shared.SimpleEventBus;

public class VictoryPointsList implements Iterable<VictoryPointItem>
{
    private List<VictoryPointItem> points = new ArrayList<VictoryPointItem>();
    private SimpleEventBus eventBus;
    
    private SimpleEventBus getEventBus()
    {
        if (eventBus == null)
        {
            eventBus = new SimpleEventBus();
        }
        
        return eventBus;
    }
    
    private void safelyFireEvent(VictoryPointsChangedEvent event)
    {
        if (eventBus != null)
        {
            eventBus.fireEvent(event);
        }
    }
    
    public void add(VictoryPointItem item)
    {
        points.add(item);
        
        safelyFireEvent(new VictoryPointsChangedEvent(item, null));
    }
    
    public void remove(VictoryPointItem item)
    {
        points.remove(item);
        
        safelyFireEvent(new VictoryPointsChangedEvent(null, item));
    }
    
    /*
     * Returns total amount of victory points in this list 
     */
    public int getTotalPoints()
    {
        int result=0;
        
        for (VictoryPointItem vp : points)
        {
            result += vp.getVictoryPoints();
        }
        
        return result;
    }

    @Override
    public Iterator<VictoryPointItem> iterator()
    {
        return points.iterator();
    }
    
    public void addVictoryPointsChangedListener(VictoryPointsChangedEventHandler handler)
    {
        getEventBus().addHandler(VictoryPointsChangedEvent.TYPE, handler);
    }
}
