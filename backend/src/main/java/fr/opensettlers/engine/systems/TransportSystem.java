package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.TransportManager;
import fr.opensettlers.engine.state.Carrier;
import fr.opensettlers.engine.state.Road;

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
