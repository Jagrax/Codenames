package ar.com.codemanes.entity;

public class Team {

    private String name;
    private String color;
    private Integer pendingTiles;

    public Team(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getPendingTiles() {
        return pendingTiles;
    }

    public void setPendingTiles(Integer pendingTiles) {
        this.pendingTiles = pendingTiles;
    }

    @Override
    public String toString() {
        return "Team ["
                + ((name != null) ? "name=" + name + ", " : "")
                + ((color != null) ? "color=" + color + ", " : "")
                + ((pendingTiles != null) ? "pendingTiles=" + pendingTiles : "")
                + "]";
    }
}
