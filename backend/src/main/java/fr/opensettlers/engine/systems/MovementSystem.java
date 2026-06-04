package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Soldier;

import java.util.List;

public class MovementSystem implements ISystem {
    /**
     * Process function, called every tock by the game loop.
     *
     * @param gameState Game state of the current session.
     */
    @Override
    public void process(GameState gameState) {
        List<Soldier> soldiers = gameState.getSoldiers();
    }
}
