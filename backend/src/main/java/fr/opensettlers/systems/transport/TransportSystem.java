package fr.opensettlers.systems.transport;

import fr.opensettlers.state.GameState;
import fr.opensettlers.systems.transport.TransportManager;
import fr.opensettlers.entities.unit.Carrier;
import fr.opensettlers.entities.world.Road;

import java.util.Collection;
import fr.opensettlers.systems.ISystem;

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
