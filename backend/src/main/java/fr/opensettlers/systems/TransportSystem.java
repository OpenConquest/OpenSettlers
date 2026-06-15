package fr.opensettlers.systems;

import fr.opensettlers.state.GameState;
import fr.opensettlers.systems.TransportManager;
import fr.opensettlers.entities.Carrier;
import fr.opensettlers.entities.Road;

import java.util.Collection;

/**
 * System that advances every carrier along its road each tick, delegating the
 * pick-up, walk and drop-off logic to the {@link TransportManager}. It is the
 * heartbeat of the logistics network that moves resources between flags.
 */
public class TransportSystem implements ISystem {
    /**
     * Steps each road's carrier once.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        TransportManager transportManager = gameState.getTransportManager();
        Collection<Road> allRoads = transportManager.getRoadNetwork().getAllRoads();

        for (Road road : allRoads) {
            Carrier carrier = road.getCarrier();
            if (carrier == null) continue;

            transportManager.processCarrier(carrier, road);
        }
    }
}
