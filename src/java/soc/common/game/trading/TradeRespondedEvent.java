package soc.common.game.trading;

import com.google.gwt.event.shared.GwtEvent;

public class TradeRespondedEvent extends GwtEvent<TradeRespondedEventHandler>
{
    public static Type<TradeRespondedEventHandler> TYPE = new Type<TradeRespondedEventHandler>();
    private TradeResponse tradeResponse;
    
    public TradeRespondedEvent(TradeResponse tradeResponse)
    {
        super();
        this.tradeResponse = tradeResponse;
    }

    /**
     * @return the tradeResponse
     */
    public TradeResponse getTradeResponse()
    {
        return tradeResponse;
    }

    @Override
    protected void dispatch(TradeRespondedEventHandler arg0)
    {
        arg0.onTradeResponded(this);
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<TradeRespondedEventHandler> getAssociatedType()
    {
        return TYPE;
    }

}
