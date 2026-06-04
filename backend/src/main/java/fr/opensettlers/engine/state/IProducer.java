package fr.opensettlers.engine.state;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Interface for production building. */
public interface IProducer {
    /** Produces goods from available input resources. */
    public void produce();

    /**
     * Checks if the building can produce. Must be called before produce().
     *
     * @return boolean
     */
    public boolean canProduce();
}
