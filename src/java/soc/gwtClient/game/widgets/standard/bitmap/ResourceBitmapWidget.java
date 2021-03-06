package soc.gwtClient.game.widgets.standard.bitmap;

import soc.common.board.resources.Resource;
import soc.gwtClient.game.widgets.abstractWidgets.IResourceWidget;
import soc.gwtClient.game.widgets.abstractWidgets.ResourceClickedEvent;
import soc.gwtClient.game.widgets.abstractWidgets.ResourceClickedEventHandler;
import soc.gwtClient.images.Resources;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ResourceBitmapWidget extends AbsolutePanel implements
        IResourceWidget
{
    private Image image;
    private Resource resource;
    private boolean enabled = true;
    private SimpleEventBus eventBus = new SimpleEventBus();

    public ResourceBitmapWidget(final Resource resource)
    {
        super();
        this.resource = resource;
        image = new Image(Resources.card(resource));
        image.addClickHandler(new ClickHandler()
        {
            @Override
            public void onClick(ClickEvent arg0)
            {
                // only fire the event when we are enabled
                if (enabled)
                {
                    eventBus.fireEvent(new ResourceClickedEvent(resource));
                }
            }
        });
        this.add(image);
    }

    @Override
    public Widget asWidget()
    {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.gwtClient.game.widgets.abstractWidgets.IResourceWidget#setEnabled
     * (boolean)
     */
    @Override
    public IResourceWidget setEnabled(boolean enabled)
    {
        this.enabled = enabled;

        // TODO: Set image opacity to like 50%

        return this;
    }

    @Override
    public Resource getResource()
    {
        return resource;
    }

    @Override
    public HandlerRegistration addClickHandler(
            ResourceClickedEventHandler handler)
    {
        return eventBus.addHandler(ResourceClickedEvent.TYPE, handler);
    }

}
