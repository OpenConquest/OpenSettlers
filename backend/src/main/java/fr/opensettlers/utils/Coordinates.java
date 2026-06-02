package main.java.fr.opensettlers.utils;

@Data
@AllArgsConstructor
public class Coordinates {
    private final int x;
    private final int y;

    public void add(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }


    public void move(Coordinates vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

    public Coordinates neighbor(Direction direction) {
        return new Coordinates(this.x + direction.getDx(), this.y + direction.getDy());
    }
}