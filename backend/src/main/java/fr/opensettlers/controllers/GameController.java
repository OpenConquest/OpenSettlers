package fr.opensettlers.controllers;

import fr.opensettlers.engine.GameSession;
import fr.opensettlers.services.GameEngineService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Set;
import java.util.UUID;

/**
 * REST endpoints to create, list, and stop games.
 * Clients then join the real-time loop via the WebSocket at /game/{gameId}.
 */
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameController {

    @Inject
    GameEngineService gameEngineService;

    /**
     * Request body for game creation.
     *
     * @param playerCount the number of players (1 to 4)
     */
    public record CreateGameRequest(int playerCount) {}

    /**
     * Response body describing a created game.
     *
     * @param gameId      the new game's identifier
     * @param playerCount the number of players
     * @param websocketPath the WebSocket path to join the game
     */
    public record GameCreatedResponse(UUID gameId, int playerCount, String websocketPath) {}

    /**
     * Creates a new game and starts its loop.
     *
     * @param request the creation parameters
     * @return 201 with the game ID, or 400 if the player count is invalid
     */
    @POST
    public Response createGame(CreateGameRequest request) {
        int playerCount = request != null ? request.playerCount() : 0;
        if (playerCount < 1 || playerCount > 4) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"playerCount must be between 1 and 4\"}")
                    .build();
        }
        GameSession session = gameEngineService.createGame(playerCount);
        return Response.status(Response.Status.CREATED)
                .entity(new GameCreatedResponse(session.getId(), playerCount, "/game/" + session.getId()))
                .build();
    }

    /**
     * Lists the identifiers of all active games.
     *
     * @return the set of active game IDs
     */
    @GET
    public Set<UUID> listGames() {
        return gameEngineService.getActiveGameIds();
    }

    /**
     * Stops a running game and discards its session.
     *
     * @param gameId the game to stop
     * @return 204 if stopped, 404 if the game does not exist
     */
    @DELETE
    @Path("/{gameId}")
    public Response stopGame(@PathParam("gameId") UUID gameId) {
        if (!gameEngineService.stopGame(gameId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
