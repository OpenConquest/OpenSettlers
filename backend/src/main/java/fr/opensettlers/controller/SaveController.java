package fr.opensettlers.controller;

import fr.opensettlers.persistence.GamePersistenceService;
import fr.opensettlers.service.GameEngineService;
import fr.opensettlers.state.GameSession;
import fr.opensettlers.state.GameState;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints to save running games to the database and restore them later.
 *
 * <ul>
 *   <li>{@code POST /games/{gameId}/save} — persist a snapshot of a running game.</li>
 *   <li>{@code GET /saves} — list every saved game.</li>
 *   <li>{@code POST /saves/{saveId}/load} — restart a saved game as a new game.</li>
 * </ul>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SaveController {

    @Inject
    GameEngineService gameEngineService;

    @Inject
    GamePersistenceService persistenceService;

    /** Request body for a save. */
    public record SaveRequest(String name) {}

    /** Response body for a save. */
    public record SaveResponse(Long saveId) {}

    /** Response body for a load. */
    public record LoadResponse(UUID gameId, String websocketPath) {}

    /**
     * Saves a snapshot of a running game.
     *
     * @param gameId  the running game to save
     * @param request the save name
     * @return 201 with the save ID, or 404 if the game is not running
     */
    @POST
    @Path("/games/{gameId}/save")
    public Response saveGame(@PathParam("gameId") UUID gameId, SaveRequest request) {
        GameState state = gameEngineService.getGame(gameId);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String name = request != null && request.name() != null ? request.name() : "save-" + gameId;
        Long saveId = persistenceService.save(state, name);
        return Response.status(Response.Status.CREATED).entity(new SaveResponse(saveId)).build();
    }

    /**
     * Lists all saved games.
     *
     * @return the saved-game metadata
     */
    @GET
    @Path("/saves")
    public List<GamePersistenceService.SaveSummary> listSaves() {
        return persistenceService.list();
    }

    /**
     * Restores a saved game and starts it as a new running game.
     *
     * @param saveId the save to load
     * @return 201 with the new game ID, or 404 if the save does not exist
     */
    @POST
    @Path("/saves/{saveId}/load")
    public Response loadSave(@PathParam("saveId") Long saveId) {
        GameState state = persistenceService.restore(saveId);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        GameSession session = gameEngineService.loadGame(state);
        return Response.status(Response.Status.CREATED)
                .entity(new LoadResponse(session.getId(), "/game/" + session.getId()))
                .build();
    }
}
