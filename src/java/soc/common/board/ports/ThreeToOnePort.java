package soc.common.board.ports;

import soc.common.board.resources.Resource;
import soc.common.board.resources.ResourceList;

public class ThreeToOnePort extends AbstractPort
{
    private static final long serialVersionUID = -2264455291636281867L;

    /*
     * Performs a 3:1 trade on a list of resources
     * 
     * @see
     * soc.common.board.ports.Port#divide(soc.common.board.resources.ResourceList
     * , soc.common.board.resources.Resource)
     */
    @Override
    public int divide(ResourceList resources, Resource type)
    {
        return resources.size() / getInAmount();
    }

    /*
     * Three resources are needed for one trade
     * 
     * @see soc.common.board.ports.Port#getInAmount()
     */
    @Override
    public int getInAmount()
    {
        return 3;
    }

    /*
     * One gold is gained from one trade
     * 
     * @see soc.common.board.ports.Port#getOutAmount()
     */
    @Override
    public int getOutAmount()
    {
        return 1;
    }

    /*
     * @see
     * soc.common.board.ports.Port#canTrade(soc.common.board.resources.Resource)
     */
    @Override
    public boolean canTrade(Resource resource)
    {
        return resource.isTradeable();
    }

    @Override
    public Port copy()
    {
        return new ThreeToOnePort();
    }

    @Override
    public String getColor()
    {
        return "White";
    }
}
