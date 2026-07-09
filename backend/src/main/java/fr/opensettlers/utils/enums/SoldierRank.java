package fr.opensettlers.utils.enums;

/**
 * Military ranks, from weakest to strongest. Soldiers are promoted one rank
 * at a time by gold coins delivered to their garrison building.
 * Higher ranks have more health points and therefore win more duels.
 */
public enum SoldierRank {
    /** Rank 0 — freshly recruited soldier. */
    PRIVATE(3),

    /** Rank 1. */
    PRIVATE_FIRST_CLASS(4),

    /** Rank 2. */
    SERGEANT(5),

    /** Rank 3. */
    OFFICER(6),

    /** Rank 4 — highest rank. */
    GENERAL(7);

    /** Maximum health points for this rank. */
    private final int maxHealth;

    SoldierRank(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * Returns the maximum health points of a soldier of this rank.
     *
     * @return the max health
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Returns the next rank, or this rank if already at the top.
     *
     * @return the rank one step above
     */
    public SoldierRank next() {
        int next = ordinal() + 1;
        SoldierRank[] ranks = values();
        return next < ranks.length ? ranks[next] : this;
    }

    /**
     * Checks whether this rank can still be promoted.
     *
     * @return {@code true} if a higher rank exists
     */
    public boolean isPromotable() {
        return this != GENERAL;
    }
}
