package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.PerlinNoise;

public class Terrain {
    private final Coordinates coords;
    private final Tile type;

    public Terrain(Coordinates coords, double mapWidth, double mapHeight, PerlinNoise noise) {
        this.coords = coords;

        // 1. Distance from center (0.0 at center, 1.0 at edges)
        double centerX = mapWidth / 2.0;
        double centerY = mapHeight / 2.0;
        double dx = (coords.getX() - centerX) / centerX;
        double dy = (coords.getY() - centerY) / centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 2. INCREASE FREQUENCY (0.005 -> 0.008) & Add 3 Octaves for varied peaks
        double nx = coords.getX() * 0.008;
        double ny = coords.getY() * 0.008;
        
        double n = 1.0 * noise.noise(nx, ny) 
                 + 0.5 * noise.noise(nx * 2, ny * 2) 
                 + 0.25 * noise.noise(nx * 4, ny * 4);
        
        double noiseVal = (n / 1.75 + 1.0) / 2.0; // Normalize noise to [0, 1]

        // 3. SUBTRACTION FORMULA (Allows outer islands & broadens the main landmass)
        // Adjust the 0.55 factor down if you want an even bigger main island
        double elevation = noiseVal - 0.55 * Math.pow(distance, 2);

        // 4. Calibrated thresholds for 50-75% land allocation
        if (elevation < 0.22) {
            this.type = Tile.WATER;
        } else if (elevation < 0.55) {
            this.type = Tile.PLAINS;
        } else if (elevation < 0.72) {
            this.type = Tile.HILLS;
        } else {
            this.type = Tile.MOUNTAIN; // Generates 1-3 distinct clusters
        }
    }

    public Coordinates getCoords() { return coords; }
    public Tile getType() { return type; }
}