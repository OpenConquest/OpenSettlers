package fr.opensettlers.model;

public class MapTile {
    private final Tile type;
    private final Ressource resource;

    public MapTile(Tile type, Ressource resource) {
        this.type = type;
        this.resource = resource;
    }

    public Tile getType() { return type; }
    public Ressource getResource() { return resource; }
}