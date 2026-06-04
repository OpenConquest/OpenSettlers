package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Soldier;

import java.util.List;

public class MovementSystem implements ISystem {
    @Override
    public void process(GameState gameState) {
        List<Soldier> soldiers = gameState.getSoldiers();
    }
}
