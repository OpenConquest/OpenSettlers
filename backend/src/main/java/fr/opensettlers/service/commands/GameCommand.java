package fr.opensettlers.service.commands;

import fr.opensettlers.state.GameState;

public interface GameCommand {
    int getPlayerId();
    void execute(GameState state);
}
