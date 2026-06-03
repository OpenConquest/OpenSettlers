package fr.opensettlers.services;

import fr.opensettlers.model.Tile;
import fr.opensettlers.model.MapTile;
import fr.opensettlers.model.Ressource;
import fr.opensettlers.model.Terrain;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.PerlinNoise;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class MapGenerator {

    public MapTile[][] generateContinentalGrid(int gridSize) {
        MapTile[][] gridMap = new MapTile[gridSize][gridSize];
        Random rand = new Random();
        
        PerlinNoise elevationNoise = new PerlinNoise(rand.nextLong());
        PerlinNoise moistureNoise = new PerlinNoise(rand.nextLong());

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                double ex = x * 0.12;
                double ey = y * 0.12;
                double eNoise = (elevationNoise.noise(ex, ey) + 0.5 * elevationNoise.noise(ex * 2, ey * 2)) / 1.5;
                double elevation = (eNoise + 1.0) / 2.0;

                double mx = x * 0.10;
                double my = y * 0.10;
                double mNoise = (moistureNoise.noise(mx, my) + 0.5 * moistureNoise.noise(mx * 2, my * 2)) / 1.5;
                double moisture = (mNoise + 1.0) / 2.0;

                Tile type;
                if (elevation < 0.20) {
                    type = Tile.WATER;
                } else if (elevation >= 0.85) {
                    type = Tile.MOUNTAIN;
                } else if (elevation >= 0.70) {
                    type = Tile.HILLS;
                } else {
                    if (moisture < 0.25) {
                        type = Tile.DESSERT;
                    } else if (moisture > 0.65) {
                        type = Tile.FOREST;
                    } else {
                        type = Tile.PLAINS;
                    }
                }

                Ressource resource = Ressource.NONE;
                double roll = rand.nextDouble();
                if (type == Tile.WATER && roll < 0.08) resource = Ressource.FISH;
                else if (type == Tile.PLAINS && roll < 0.05) resource = Ressource.WHEAT;
                else if (type == Tile.HILLS && roll < 0.12) resource = Ressource.ORE;
                else if (type == Tile.MOUNTAIN && roll < 0.30) resource = Ressource.ORE;

                gridMap[x][y] = new MapTile(type, resource);
            }
        }

        fuseCloseLakes(gridMap, gridSize, rand);

        removeSmallLakes(gridMap, gridSize);

        return gridMap;
    }

    private void fuseCloseLakes(MapTile[][] gridMap, int gridSize, Random rand) {
        int[][] labels = new int[gridSize][gridSize];
        int currentLabel = 1;

        // 1. Group individual water clusters using a Hexagonal BFS Flood Fill
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                if (gridMap[x][y].getType() == Tile.WATER && labels[x][y] == 0) {
                    Queue<int[]> queue = new LinkedList<>();
                    queue.add(new int[]{x, y});
                    labels[x][y] = currentLabel;

                    while (!queue.isEmpty()) {
                        int[] curr = queue.poll();
                        for (int[] neighbor : getHexNeighbors(curr[0], curr[1], gridSize)) {
                            int nx = neighbor[0];
                            int ny = neighbor[1];
                            if (gridMap[nx][ny].getType() == Tile.WATER && labels[nx][ny] == 0) {
                                labels[nx][ny] = currentLabel;
                                queue.add(new int[]{nx, ny});
                            }
                        }
                    }
                    currentLabel++;
                }
            }
        }

        // 2. Identify land tiles that form a bridge up to 3 tiles wide
        List<int[]> tilesToVaporize = new ArrayList<>();
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                if (gridMap[x][y].getType() != Tile.WATER) {
                    
                    // Track the absolute shortest distance to any unique lake ID found
                    Map<Integer, Integer> lakeDistances = new HashMap<>();
                    Queue<int[]> bfsQueue = new LinkedList<>();
                    Set<String> visited = new HashSet<>();
                    
                    bfsQueue.add(new int[]{x, y, 0}); // Array tracks: [x, y, current_distance]
                    visited.add(x + "," + y);
                    
                    while (!bfsQueue.isEmpty()) {
                        int[] curr = bfsQueue.poll();
                        int cx = curr[0];
                        int cy = curr[1];
                        int dist = curr[2];
                        
                        int label = labels[cx][cy];
                        if (label > 0) {
                            if (!lakeDistances.containsKey(label) || dist < lakeDistances.get(label)) {
                                lakeDistances.put(label, dist);
                            }
                        }
                        
                        // Look ahead up to a maximum radius of 3 tiles
                        if (dist < 3) {
                            for (int[] neighbor : getHexNeighbors(cx, cy, gridSize)) {
                                int nx = neighbor[0];
                                int ny = neighbor[1];
                                String key = nx + "," + ny;
                                if (!visited.contains(key)) {
                                    visited.add(key);
                                    bfsQueue.add(new int[]{nx, ny, dist + 1});
                                }
                            }
                        }
                    }
                    
                    // Evaluate path thickness: total step distance between Lake A and Lake B through this tile
                    boolean shouldVaporize = false;
                    List<Integer> lakeIds = new ArrayList<>(lakeDistances.keySet());
                    for (int i = 0; i < lakeIds.size(); i++) {
                        for (int j = i + 1; j < lakeIds.size(); j++) {
                            int idA = lakeIds.get(i);
                            int idB = lakeIds.get(j);
                            
                            if (lakeDistances.get(idA) + lakeDistances.get(idB) <= 4) {
                                shouldVaporize = true;
                                break;
                            }
                        }
                        if (shouldVaporize) break;
                    }
                    
                    if (shouldVaporize) {
                        tilesToVaporize.add(new int[]{x, y});
                    }
                }
            }
        }

        for (int[] coord : tilesToVaporize) {
            int x = coord[0];
            int y = coord[1];
            Ressource res = (rand.nextDouble() < 0.08) ? Ressource.FISH : Ressource.NONE;
            gridMap[x][y] = new MapTile(Tile.WATER, res);
        }
    }

    private void removeSmallLakes(MapTile[][] gridMap, int gridSize) {
        boolean[][] visited = new boolean[gridSize][gridSize];

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                if (gridMap[x][y].getType() == Tile.WATER && !visited[x][y]) {
                    List<int[]> lakeTiles = new ArrayList<>();
                    Queue<int[]> queue = new LinkedList<>();
                    
                    queue.add(new int[]{x, y});
                    visited[x][y] = true;

                    while (!queue.isEmpty()) {
                        int[] curr = queue.poll();
                        lakeTiles.add(curr);

                        for (int[] neighbor : getHexNeighbors(curr[0], curr[1], gridSize)) {
                            int nx = neighbor[0];
                            int ny = neighbor[1];
                            if (gridMap[nx][ny].getType() == Tile.WATER && !visited[nx][ny]) {
                                visited[nx][ny] = true;
                                queue.add(new int[]{nx, ny});
                            }
                        }
                    }

                    if (lakeTiles.size() < 20) {
                        for (int[] coord : lakeTiles) {
                            gridMap[coord[0]][coord[1]] = new MapTile(Tile.PLAINS, Ressource.NONE);
                        }
                    }
                }
            }
        }
    }

    private List<int[]> getHexNeighbors(int x, int y, int gridSize) {
        List<int[]> neighbors = new ArrayList<>();
        int[][] offsets;
        
        if (y % 2 == 0) {
            offsets = new int[][]{{-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {-1, 1}, {0, 1}};
        } else {
            offsets = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {1, -1}, {0, 1}, {1, 1}};
        }

        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize) {
                neighbors.add(new int[]{nx, ny});
            }
        }
        return neighbors;
    }
}