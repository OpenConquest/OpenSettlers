package fr.opensettlers.systems;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.state.GameState;

import java.util.HashSet;
import java.util.Set;

/**
 * System resolving the end of a game.
 *
 * <p>As in The Settlers II, a player is knocked out once they no longer own any
 * storage building — that is, their headquarters and every warehouse and harbor
 * have been destroyed or captured. Without a warehouse a player can neither
 * train settlers nor recruit soldiers, so they can no longer act.</p>
 *
 * <p>The game ends as soon as at most one player is still standing in a
 * multi-player match; that survivor is declared the winner. Single-player
 * sandboxes (one player) never end on their own.</p>
 */
public class VictorySystem implements ISystem {

    /**
     * Updates the set of eliminated players and, when only one remains, marks
     * the game as over and records the winner.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        if (gameState.isOver() || gameState.getPlayerCount() <= 1) {
            return;
        }

        Set<Integer> alivePlayers = playersWithStorage(gameState);

        for (int playerId = 0; playerId < gameState.getPlayerCount(); playerId++) {
            if (!alivePlayers.contains(playerId)) {
                gameState.getEliminatedPlayers().add(playerId);
            }
        }

        if (alivePlayers.size() <= 1) {
            gameState.setOver(true);
            gameState.setWinnerPlayerId(alivePlayers.stream().findFirst().orElse(null));
        }
    }

    /**
     * Computes the set of players that still own at least one non-destroyed
     * storage building (headquarters, warehouse or harbor).
     *
     * @param state the current game state
     * @return the IDs of the players still in the game
     */
    private Set<Integer> playersWithStorage(GameState state) {
        Set<Integer> alive = new HashSet<>();
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding && !b.isDestroyed()) {
                alive.add(b.getPlayerId());
            }
        }
        return alive;
    }
}
