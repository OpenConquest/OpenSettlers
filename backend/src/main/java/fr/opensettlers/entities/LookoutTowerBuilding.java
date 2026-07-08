package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;

/**
 * Lookout tower: reveals a wide area around itself through the fog of war
 * without projecting any territory, mirroring the Settlers II Sp&auml;hturm.
 * It holds no garrison, no stock and produces nothing.
 */
public class LookoutTowerBuilding extends Building {

    /**
     * Initializes a new lookout tower.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public LookoutTowerBuilding(int playerId, Coordinates position) {
        super(playerId, position);
    }
}
