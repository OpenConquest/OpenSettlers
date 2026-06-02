package fr.opensettlers.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Double heighted double widthed coordinates for hexagonal grid.
 * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">More here</a>
 */
@Data
@AllArgsConstructor
public class Coordinates {
    private int x;
    private int y;

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }


    public void move(Coordinates vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

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