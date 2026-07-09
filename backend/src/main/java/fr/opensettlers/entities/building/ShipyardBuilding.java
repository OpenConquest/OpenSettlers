package fr.opensettlers.entities.building;

import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;

/**
 * Coastal building that gives a player access to the seas.
 *
 * <p>While a player owns at least one operational shipyard, their coastal
 * {@link BuildingName#HARBOR harbors} are allowed to launch colonization
 * expeditions (see {@link fr.opensettlers.systems.exploration.NavalSystem}). The shipyard
 * itself holds no resources and projects no territory; it simply represents the
 * ability to build the ships that carry expeditions.</p>
 */
public class ShipyardBuilding extends Building {

    /**
     * Creates a new shipyard.
     *
     * @param playerId owning player ID
     * @param position map coordinates (must be a coastal tile)
     */
    public ShipyardBuilding(int playerId, Coordinates position) {
        super(playerId, position);
        setName(BuildingName.SHIPYARD);
    }
}
