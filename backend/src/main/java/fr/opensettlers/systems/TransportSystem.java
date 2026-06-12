package fr.opensettlers.systems;

import fr.opensettlers.state.GameState;
import fr.opensettlers.systems.TransportManager;
import fr.opensettlers.entities.Carrier;
import fr.opensettlers.entities.Road;

import java.util.Collection;

public class TransportSystem implements ISystem {
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
