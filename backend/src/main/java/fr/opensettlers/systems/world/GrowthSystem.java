package fr.opensettlers.systems.world;

import fr.opensettlers.entities.world.MapTile;
import fr.opensettlers.entities.world.NaturalResourceNode;
import fr.opensettlers.state.GameState;
import fr.opensettlers.systems.ISystem;

/**
 * System advancing the growth of planted natural resources each tick: saplings
 * planted by foresters and wheat sown by farmers mature over time before they
 * can be harvested, as in The Settlers II.
 */
public class GrowthSystem implements ISystem {

    /**
     * Advances every growing resource node on the map by one tick.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (MapTile tile : gameState.getMapTiles().values()) {
            NaturalResourceNode node = tile.getNaturalResource();
            if (node != null && !node.isMature()) {
                node.grow();
            }
        }
    }
}
