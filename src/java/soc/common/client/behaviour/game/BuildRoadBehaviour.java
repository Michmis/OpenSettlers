package soc.common.client.behaviour.game;

import java.util.Set;

import soc.common.actions.gameAction.GameAction;
import soc.common.actions.gameAction.turnActions.standard.BuildRoad;
import soc.common.board.routing.GraphSide;
import soc.common.client.visuals.PieceVisual;
import soc.common.client.visuals.game.GameBoardVisual;
import soc.common.client.visuals.game.SideVisual;
import soc.common.game.GamePlayer;
import soc.common.game.gamePhase.InitialPlacementGamePhase;

public class BuildRoadBehaviour extends BuildSideBehaviour
{
    BuildRoad buildRoad;
    Set<GraphSide> sides;

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.client.behaviour.game.BuildSideBehaviour#setNeutral(soc.common
     * .client.visuals.game.GameBoardVisual)
     */
    @Override
    public void setNeutral(GameBoardVisual visual)
    {
        if (sides != null)
        {
            for (GraphSide side : sides)
            {
                visual.getSideVisuals().get(side).setVisible(false);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.client.behaviour.game.BuildSideBehaviour#start(soc.common.
     * client.visuals.game.GameBoardVisual)
     */
    @Override
    public void start(GameBoardVisual gameVisual)
    {
        if (gameVisual.getGame().getCurrentPhase() instanceof InitialPlacementGamePhase)
        {
            GamePlayer player = gameVisual.getGame().getCurrentTurn()
                    .getPlayer();
            if (player.getBuildPieces().size() == 1)
            {
                sides = gameVisual.getBoard().getGraph()
                        .getRoadCandidatesFirstTown(player);
            }
            else
            {
                sides = gameVisual.getBoard().getGraph()
                        .getRoadCandidatesSecondTown(player);
            }
            for (GraphSide side : sides)
            {
                gameVisual.getSideVisuals().get(side).setVisible(true);
            }
        }
    }

    /**
     * @return the buildRoad
     */
    public BuildRoad getBuildRoad()
    {
        return buildRoad;
    }

    public BuildRoadBehaviour(BuildRoad buildRoad)
    {
        this.buildRoad = buildRoad;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * soc.common.client.behaviour.game.BuildSideBehaviour#clicked(soc.common
     * .client.visuals.IPieceVisual,
     * soc.common.client.visuals.board.IBoardVisual)
     */
    @Override
    public void clicked(PieceVisual pieceVisual, GameBoardVisual board)
    {
        if (pieceVisual instanceof SideVisual)
        {
            SideVisual sideVisual = (SideVisual) pieceVisual;
            buildRoad.setSideLocation(sideVisual.getHexSide());
            board.onBehaviourDone();
        }
    }

    @Override
    public GameAction getGameAction()
    {
        return buildRoad;
    }

}
