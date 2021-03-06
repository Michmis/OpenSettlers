package soc.gwtClient.editor;

import soc.common.client.behaviour.InteractionBehaviour;

import com.google.gwt.event.shared.GwtEvent;


public class BehaviourChanged extends GwtEvent<IBehaviourChangedHandler>
{
    public static Type<IBehaviourChangedHandler> TYPE = new Type<IBehaviourChangedHandler>();
    InteractionBehaviour behaviour;
    
    public BehaviourChanged(InteractionBehaviour behaviour)
    {
        this.behaviour=behaviour;
    }
    /**
     * @return the behaviour
     */
    public InteractionBehaviour getBehaviour()
    {
        return behaviour;
    }

    @Override
    protected void dispatch(IBehaviourChangedHandler handler)
    {
        handler.onBehaviourChanged(this);
        
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<IBehaviourChangedHandler> getAssociatedType()
    {
        return TYPE;
    }

}
