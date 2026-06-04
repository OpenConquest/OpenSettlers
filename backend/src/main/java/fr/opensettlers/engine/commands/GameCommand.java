package fr.opensettlers.engine.commands;

import fr.opensettlers.engine.GameState;

public interface GameCommand {
    int getPlayerId();
    void execute(GameState state);
}
