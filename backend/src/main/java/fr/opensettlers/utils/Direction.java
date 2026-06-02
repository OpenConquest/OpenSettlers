package main.java.fr.opensettlers.utils;

@Data
@AllArgsConstructor
public enum Direction {
    NORTH(0, -2),
    NORTHEAST(1, -1),
    SOUTHEAST(1, 1),
    SOUTH(0, 2),
    SOUTHWEST(-1, 1),
    NORTHWEST(-1, -1);

    private final int dx;
    private final int dy;
}
