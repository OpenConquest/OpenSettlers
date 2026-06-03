package fr.opensettlers.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Double heighted double widthed coordinates for hexagonal grid. Refer to
 * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">this article</a> for more information.
 */
@Data
@AllArgsConstructor
public class Coordinates {
    /**
     * The X horizontal axis coordinate. Refer to
     * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">this article</a> for more information.
     */
    private int x;

    /**
     * The Y vertical axis coordinate. Refer to
     * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">this article</a> for more information.
     */
    private int y;

    /**
     * Shifts the coordinates.
     * @param dx X axis difference
     * @param dy Y axis difference
     */
    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Shifts the coordinates
     * @param vector Another Coordinates object that will be added.
     */
    public void move(Coordinates vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

    /**
     * Shifts the coordinates by a distance of 1 towards the specified direction.
     * @param vector - A unit vector indicating the direction to apply.
     */
    public void move(Direction vector) {
        this.move(this.neighbor(vector));
    }

    /**
     * This function converts Direction enum values into unit vectors according to the double heighted double widthed
     * hexagonal coordinates system. Refer to
     * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">this article</a> for more information.
     * @param direction The direction enum.
     * @return A unit vector in the specified direction.
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
}