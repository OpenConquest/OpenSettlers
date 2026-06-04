package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * A single resource item sitting on a flag, with routing information
 * toward its final destination. Each flag slot holds one ResourceStack.
 */
@Data
@AllArgsConstructor
public class ResourceStack {
    /** The type of resource. */
    private final ResourceType type;

    /**
     * The final destination flag ID where this resource needs to be delivered.
     * May be {@code null} if the destination has not been assigned yet.
     */
    private UUID targetFlagId;
}
