package soc.gwtClient.game.widgets.abstractWidgets;

import soc.common.board.resources.AbstractResource;
import soc.common.board.resources.Resource;

import com.google.gwt.event.shared.GwtEvent;

public class ResourceClickedEvent extends GwtEvent<ResourceClickedEventHandler>
{
    public static Type<ResourceClickedEventHandler> TYPE = new Type<ResourceClickedEventHandler>();
    private Resource resource;
    
    public ResourceClickedEvent(Resource resource)
    {
        super();
        this.resource = resource;
    }

    /**
     * @return the resource
     */
    public Resource getResource()
    {
        return resource;
    }

    @Override
    protected void dispatch(ResourceClickedEventHandler arg0)
    {
        arg0.onResourceClicked(this);
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ResourceClickedEventHandler> getAssociatedType()
    {
        return TYPE;
    }

}
