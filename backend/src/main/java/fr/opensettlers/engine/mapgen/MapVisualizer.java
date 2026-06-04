package fr.opensettlers.engine.mapgen;

import fr.opensettlers.engine.state.MapTile;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.TileType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MapVisualizer {
    
    public static void saveHexagonalMap(MapTile[][] gridMap, String filePath) throws Exception {
        int gridSize = gridMap.length;
        int w = 16; 
        int h = 16; 
        
        int imgWidth = (gridSize - 1) * w;
        int imgHeight = (int) ((gridSize - 1) * h * 0.75);

        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        int offsetX = -(w / 2);
        int offsetY = -(h / 4);

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                MapTile tile = gridMap[x][y];
                
                switch (tile.getType()) {
                    case WATER -> g.setColor(new Color(40, 100, 200));   // Deep blue water
                    case GRASS -> g.setColor(new Color(70, 160, 60));    // Lush meadow green
                    case FOREST -> g.setColor(new Color(30, 100, 40));   // Dark forest green
                    case DESERT -> g.setColor(new Color(240, 220, 150)); // Bright sand beach
                    case HILLS -> g.setColor(new Color(120, 140, 120));  // Greenish rock
                    case MOUNTAIN -> g.setColor(new Color(180, 180, 180)); // Grey peaks
                    case STONE -> g.setColor(new Color(150, 150, 150));  // Surface boulders
                }

                // Apply the offsets to the starting coordinates
                int xL = (x * w) + offsetX;
                int yT = (int) (y * h * 0.75) + offsetY;
                
                if (y % 2 != 0) {
                    xL += w / 2;
                }

                int[] px = { xL + 8, xL + 16, xL + 16, xL + 8, xL, xL };
                int[] py = { yT, yT + 4, yT + 12, yT + 16, yT + 12, yT + 4 };

                g.fillPolygon(px, py, 6);

                int cX = xL + 8; 
                int cY = yT + 8; 

                ResourceType res = tile.getNaturalResource() != null ? tile.getNaturalResource().getType() : null;

                if (res == ResourceType.IRON || res == ResourceType.COAL || res == ResourceType.STONE) {
                    if (res == ResourceType.IRON) {
                        g.setColor(new Color(200, 80, 80)); // Rust red
                    } else if (res == ResourceType.COAL) {
                        g.setColor(new Color(40, 40, 40));  // Dark black
                    } else {
                        g.setColor(new Color(100, 100, 100)); // Dark grey stone
                    }
                    int[] ox = { cX, cX + 4, cX, cX - 4 };
                    int[] oy = { cY - 4, cY, cY + 4, cY };
                    g.fillPolygon(ox, oy, 4);

                } else if (res == ResourceType.FISH) {
                    g.setColor(new Color(255, 99, 71));
                    g.fillOval(cX - 2, cY - 2, 5, 4);
                    
                    int[] fx = { cX - 1, cX - 5, cX - 5 };
                    int[] fy = { cY, cY - 3, cY + 3 };
                    g.fillPolygon(fx, fy, 3);
                }
            }
        }

        g.dispose();
        ImageIO.write(img, "png", new File(filePath));
    }

    public static String toAsciiString(MapTile[][] gridMap) {
        StringBuilder sb = new StringBuilder();
        for (MapTile[] row : gridMap) {
            for (MapTile tile : row) {
                ResourceType res = tile.getNaturalResource() != null ? tile.getNaturalResource().getType() : null;
                if (res == ResourceType.IRON) {
                    sb.append("i ");
                } else if (res == ResourceType.COAL) {
                    sb.append("c ");
                } else if (res == ResourceType.STONE && tile.getType() == TileType.MOUNTAIN) {
                    sb.append("s ");
                } else {
                    switch (tile.getType()) {
                        case WATER -> sb.append("~ ");
                        case GRASS -> sb.append(". ");
                        case HILLS -> sb.append("m ");
                        case MOUNTAIN -> sb.append("▲ ");
                        case DESERT -> sb.append("d ");
                        case FOREST -> sb.append("F ");
                        default -> sb.append("? ");
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}