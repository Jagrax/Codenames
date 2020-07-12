package ar.com.codemanes.entity;

import java.util.Arrays;

public class Board {

    private Tile[][] tiles;

    public Board(int size) {
        tiles = new Tile[size][size];
    }

    public boolean addTile(Tile tile) {
        // Si ya tengo asiganada una tile en esa posicion, no la agrego
        if (tiles[tile.getX()][tile.getY()] != null) {
            return false;
        }

        tiles[tile.getX()][tile.getY()] = tile;
        return true;
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[][] tiles) {
        this.tiles = tiles;
    }

    public Tile getTile(int x, int y) {
        return tiles[x][y];
    }

    @Override
    public String toString() {
        return "Board ["
                + ((tiles != null) ? "tiles=" + Arrays.toString(tiles) + ", " : "")
                + "]";
    }
}
