package soc.common.board.resources;

import com.google.gwt.event.shared.GwtEvent;

public class ResourcesChangedEvent extends GwtEvent<ResourcesChangedEventHandler>
{
    private static Type<ResourcesChangedEventHandler> TYPE = new Type<ResourcesChangedEventHandler>();
    private ResourceList addedResources;
    private ResourceList removedResources;

    public ResourcesChangedEvent(ResourceList addedResources, ResourceList removedResources)
    {
        this.addedResources=addedResources;
        this.removedResources=removedResources;
    }    
    
    /**
     * @return the addedResources
     */
    public ResourceList getAddedResources()
    {
        return addedResources;
    }
    /**
     * @return the removedResources
     */
    public ResourceList getRemovedResources()
    {
        return removedResources;
    }
    @Override
    protected void dispatch(ResourcesChangedEventHandler handler)
    {
        handler.onResourcesChanged(this);
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ResourcesChangedEventHandler> getAssociatedType()
    {
        return TYPE;
    }

}
