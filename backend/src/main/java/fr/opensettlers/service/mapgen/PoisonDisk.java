package fr.opensettlers.service.mapgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr.opensettlers.utils.Coordinates;

/**
 * Poisson-disk sampler (Bridson's algorithm): produces points that are randomly
 * but evenly spread, with no two closer than a minimum radius. The
 * {@link MapGenerator} uses it to scatter resource clusters naturally across the
 * map.
 */
public class PoisonDisk{

    /** Width of the sampling area. */
    private final double width;

    /** Height of the sampling area. */
    private final double height;

    /** Minimum allowed distance between any two sampled points. */
    private final double radius;

    /** Number of candidate points tried around an active point before giving up. */
    private final int k;

    /** Size of a background-grid cell, sized so each cell holds at most one point. */
    private final double cellSize;

    /** All accepted sample points. */
    private final List<Coordinates> points;

    /** Points still able to spawn new neighbors, used as the algorithm's frontier. */
    private final List<Coordinates> activeList;

    /** Spatial acceleration grid mapping a cell to the point it contains. */
    private final Coordinates[][] grid;

    /** Random source driving point placement. */
    private final Random seed;

    /**
     * Creates a sampler for a {@code width} × {@code height} area.
     *
     * @param width  width of the sampling area
     * @param height height of the sampling area
     * @param radius minimum distance separating any two generated points
     */
    public PoisonDisk(double width, double height, double radius) {
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.k = 30;
        
        this.cellSize = radius / Math.sqrt(2);
        
        int gridWidth = (int) Math.ceil(width / cellSize);
        int gridHeight = (int) Math.ceil(height / cellSize);
        this.grid = new Coordinates[gridWidth][gridHeight];
        
        this.points = new ArrayList<>();
        this.activeList = new ArrayList<>();
        this.seed = new Random();
    }

    /**
     * Tests whether a candidate point lies inside the area and is at least
     * {@code radius} away from every already-accepted point, using the
     * background grid to check only nearby cells.
     *
     * @param coord the candidate point
     * @return {@code true} if the point can be accepted
     */
    public boolean isValid(Coordinates coord){
        double x = coord.getX();
        double y = coord.getY();
        if (x < 0 || x > this.width || y < 0 || y > this.height){
            return false;
        }

        int cellX = (int) (x / cellSize);
        int cellY = (int) (y / cellSize);

        int startX = Math.max(0, cellX - 2);
        int endX = Math.min(grid.length - 1, cellX + 2);
        int startY = Math.max(0, cellY - 2);
        int endY = Math.min(grid[0].length - 1, cellY + 2);

        for (int checkx = startX; checkx <= endX; checkx++) {
            for (int checky = startY; checky <= endY; checky++) {
                Coordinates neighbor = grid[checkx][checky];
                if (neighbor != null) {
                    double dx = neighbor.getX() - x;
                    double dy = neighbor.getY() - y;
                    double distanceSquared = (dx * dx) + (dy * dy);
                    if (distanceSquared < radius * radius) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Records a point in the background grid cell covering its position.
     *
     * @param coord the point to index
     */
    private void insertIntoGrid(Coordinates coord) {
        int cellX = (int) (coord.getX() / cellSize);
        int cellY = (int) (coord.getY() / cellSize);
        grid[cellX][cellY] = coord;
    }

    /**
     * Runs Bridson's algorithm and returns the full set of evenly spread points.
     * Starts from a random seed point and repeatedly spawns candidates around
     * active points until no more can be placed.
     *
     * @return the generated points, each at least {@code radius} apart
     */
    public List<Coordinates> generate() {
        Coordinates startPoint = new Coordinates(seed.nextDouble() * width, seed.nextDouble() * height);
        points.add(startPoint);
        activeList.add(startPoint);
        insertIntoGrid(startPoint);

        while (!activeList.isEmpty()) {
            int activeIndex = seed.nextInt(activeList.size());
            Coordinates currentPoint = activeList.get(activeIndex);
            boolean pointFound = false;

            for (int i = 0; i < k; i++) {
                double angle = 2 * Math.PI * seed.nextDouble();

                double distance = radius + seed.nextDouble() * radius; 
                
                double newX = currentPoint.getX() + distance * Math.cos(angle);
                double newY = currentPoint.getY() + distance * Math.sin(angle);
                Coordinates newPoint = new Coordinates(newX, newY);

                if (isValid(newPoint)) {
                    points.add(newPoint);
                    activeList.add(newPoint);
                    insertIntoGrid(newPoint);
                    pointFound = true;
                    break;
                }
            }

            if (!pointFound) {
                activeList.remove(activeIndex);
            }
        }

        return points;
    }
}

