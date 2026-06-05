package fr.opensettlers.engine.mapgen;

import fr.opensettlers.engine.state.MapTile;

public class MapGenTest {
    public static void main(String[] args) {
        try {
            int gridSizeX = 284;
            int gridSizeY = 160;

            System.out.println("1. Compiling raw tactical resource grid...");
            MapGenerator generator = new MapGenerator();
            MapTile[][] gridMap = generator.generateContinentalGrid(gridSizeX, gridSizeY);

            System.out.println("2. Displaying ASCII overview (w=wheat, o=ore):\n");
            System.out.println(MapVisualizer.toAsciiString(gridMap));

            System.out.println("3. Rendering high-res hex map file...");
            MapVisualizer.saveHexagonalMap(gridMap, "map_test.png");

            System.out.println("Success! Run multiple times to view unique resource distributions.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}