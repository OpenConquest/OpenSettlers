package fr.opensettlers.service.mapgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr.opensettlers.utils.Coordinates;

public class PoisonDisk{

    private final double width;
    private final double height;
    private final double radius;
    private final int k;
    private final double cellSize;

    private final List<Coordinates> points;
    private final List<Coordinates> activeList;
    private final Coordinates[][] grid;
    private final Random seed;

    
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

    private void insertIntoGrid(Coordinates coord) {
        int cellX = (int) (coord.getX() / cellSize);
        int cellY = (int) (coord.getY() / cellSize);
        grid[cellX][cellY] = coord;
    }

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

