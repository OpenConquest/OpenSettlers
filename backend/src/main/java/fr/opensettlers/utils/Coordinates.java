package fr.opensettlers.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Double-heighted, double-widthed hexagonal grid coordinates.
 * See <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">redblobgames</a>.
 */
@Data
@AllArgsConstructor
public class Coordinates {
    /**
     * Horizontal axis coordinate. See <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">redblobgames</a>.
     */
    private double x;

    /**
     * Vertical axis coordinate. See <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">redblobgames</a>.
     */
    private double y;

    /**
     * Shifts coordinates by the given deltas.
     *
     * @param dx horizontal offset
     * @param dy vertical offset
     */
    public void move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Shifts coordinates by adding the given vector.
     *
     * @param vector offset to apply
     */
    public void move(Coordinates vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

    /**
     * Shifts coordinates one step in the given direction.
     *
     * @param vector direction to move toward
     */
    public void move(Direction vector) {
        this.move(this.neighbor(vector));
    }

    /**
     * Converts a {@link Direction} to the neighboring coordinates in the doubled hex grid.
     * See <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">redblobgames</a>.
     *
     * @param direction the direction
     * @return neighbor coordinates in that direction
     */
    public Coordinates neighbor(Direction direction) {
        return switch (direction) {
            case NORTH -> new Coordinates(this.x, this.y - 2);
            case NORTHEAST -> new Coordinates(this.x + 1, this.y - 1);
            case SOUTHEAST -> new Coordinates(this.x + 1, this.y + 1);
            case SOUTH -> new Coordinates(this.x, this.y + 2);
            case SOUTHWEST -> new Coordinates(this.x - 1, this.y + 1);
            case NORTHWEST -> new Coordinates(this.x - 1, this.y - 1);
        };
    }

    /**
     * Calculates the hex grid distance (number of steps) between this and another coordinate.
     * Uses the double height hex distance formula.
     *
     * @param other the other coordinates
     * @return the distance in hex steps
     */
    public int distanceTo(Coordinates other) {
        double dx = Math.abs(this.x - other.x);
        double dy = Math.abs(this.y - other.y);
        return (int) (dx + Math.max(0, (dy - dx) / 2));
    }
}