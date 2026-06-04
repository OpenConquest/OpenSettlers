package fr.opensettlers.engine.mapgen;

import fr.opensettlers.engine.state.MapTile;
import fr.opensettlers.engine.state.NaturalResourceNode;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.TileType;
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
                // Lower frequencies for larger, smoother landmasses like Settlers
                double ex = x * 0.05;
                double ey = y * 0.05;
                double eNoise = (elevationNoise.noise(ex, ey) + 0.5 * elevationNoise.noise(ex * 2, ey * 2)) / 1.5;
                
                // Radial mask to make it an island (surrounded by water)
                double cx = gridSize / 2.0;
                double cy = gridSize / 2.0;
                double distanceToCenter = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                double maxDistance = gridSize / 2.0;
                double distanceNormalized = distanceToCenter / maxDistance; // 0 at center, ~1.4 at corners
                
                // Use an even higher power (6.0) so the island extends almost to the edges,
                // significantly reducing the surrounding ocean.
                double dropOff = Math.pow(distanceNormalized, 6.0) * 1.5;
                
                // No artificial bump: let the noise naturally dip to create inland lakes
                double baseElevation = (eNoise + 1.0) / 2.0; 
                double elevation = baseElevation - dropOff;

                double mx = x * 0.05;
                double my = y * 0.05;
                double mNoise = (moistureNoise.noise(mx, my) + 0.5 * moistureNoise.noise(mx * 2, my * 2)) / 1.5;
                double moisture = (mNoise + 1.0) / 2.0;

                // Settlers 2 style: discrete elevation steps with controlled thresholds
                int discreteElevation;
                if (elevation < 0.28) {
                    discreteElevation = 0; // Water (Ocean & Inland Lakes)
                } else if (elevation < 0.40) {
                    discreteElevation = 1; // Beaches / Low Grass
                } else if (elevation < 0.63) {
                    discreteElevation = 2; // High Grass / Forests
                } else if (elevation < 0.74) {
                    discreteElevation = 3; // Hills
                } else if (elevation < 0.85) {
                    discreteElevation = 4; // Mountains
                } else {
                    discreteElevation = 5; // High Mountains
                }

                TileType type;
                if (discreteElevation == 0) {
                    type = TileType.WATER;
                } else if (discreteElevation >= 4) {
                    type = TileType.MOUNTAIN;
                } else if (discreteElevation == 3) {
                    type = TileType.HILLS;
                } else {
                    // Elevations 1 and 2 (Meadows, Forests, Beaches)
                    if (discreteElevation == 1 && moisture < 0.40) {
                        type = TileType.DESERT; // Sandy beach near water
                    } else if (moisture > 0.55) {
                        type = TileType.FOREST; // Lush forest
                    } else if (moisture < 0.15) {
                        type = TileType.STONE;  // Rocky surface outcrops
                    } else {
                        type = TileType.GRASS;  // Standard green meadow
                    }
                }

                ResourceType resource = null;
                double roll = rand.nextDouble();
                if (type == TileType.WATER && roll < 0.08) {
                    resource = ResourceType.FISH;
                } else if (type == TileType.HILLS && roll < 0.08) {
                    resource = ResourceType.STONE; // Rocky outcrops
                } else if (type == TileType.MOUNTAIN && roll < 0.18) {
                    // Use moisture noise to create natural clusters/veins of ores
                    if (moisture < 0.40) {
                        resource = ResourceType.IRON;
                    } else if (moisture < 0.80) {
                        resource = ResourceType.COAL;
                    } else {
                        resource = ResourceType.STONE;
                    }
                }

                MapTile tile = new MapTile(new Coordinates(x, y), type, discreteElevation);
                if (resource != null) {
                    tile.setNaturalResource(new NaturalResourceNode(resource, 5)); // Dummy node with 5 quantity
                }
                gridMap[x][y] = tile;
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
                if (gridMap[x][y].getType() == TileType.WATER && labels[x][y] == 0) {
                    Queue<int[]> queue = new LinkedList<>();
                    queue.add(new int[]{x, y});
                    labels[x][y] = currentLabel;

                    while (!queue.isEmpty()) {
                        int[] curr = queue.poll();
                        for (int[] neighbor : getHexNeighbors(curr[0], curr[1], gridSize)) {
                            int nx = neighbor[0];
                            int ny = neighbor[1];
                            if (gridMap[nx][ny].getType() == TileType.WATER && labels[nx][ny] == 0) {
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
                if (gridMap[x][y].getType() != TileType.WATER) {
                    
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
            ResourceType res = (rand.nextDouble() < 0.08) ? ResourceType.FISH : null;
            MapTile tile = new MapTile(new Coordinates(x, y), TileType.WATER, 0);
            if (res != null) {
                tile.setNaturalResource(new NaturalResourceNode(res, 5));
            }
            gridMap[x][y] = tile;
        }
    }

    private void removeSmallLakes(MapTile[][] gridMap, int gridSize) {
        boolean[][] visited = new boolean[gridSize][gridSize];

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                if (gridMap[x][y].getType() == TileType.WATER && !visited[x][y]) {
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
                            if (gridMap[nx][ny].getType() == TileType.WATER && !visited[nx][ny]) {
                                visited[nx][ny] = true;
                                queue.add(new int[]{nx, ny});
                            }
                        }
                    }

                    if (lakeTiles.size() < 20) {
                        for (int[] coord : lakeTiles) {
                            MapTile t = gridMap[coord[0]][coord[1]];
                            t.setType(TileType.GRASS);
                            t.setElevation(1); // Raise elevation so it's not underwater
                            t.setNaturalResource(null);
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