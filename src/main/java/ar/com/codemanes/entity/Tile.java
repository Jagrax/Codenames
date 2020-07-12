package ar.com.codemanes.entity;

public class Tile {

    private int x;
    private int y;
    private Team team;
    private Integer teamId;
    private boolean flipped;
    private String word;
    private Type type;

    public enum Type {
        COLORED, NEUTRAL, DEATH
    }

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.type = Type.NEUTRAL;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public boolean isNotFlipped() {
        return !flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isDeath() {
        return type == Type.DEATH;
    }

    public void setDeath() {
        type = Type.DEATH;
    }

    public boolean isNeutral() {
        return type == Type.NEUTRAL;
    }

    public void setNeutral() {
        type = Type.NEUTRAL;
    }

    public boolean isColored() {
        return type == Type.COLORED;
    }

    public void setColored() {
        type = Type.COLORED;
    }

    @Override
    public String toString() {
        return "Tile ["
                + "x=" + x + ", "
                + "y=" + y + ", "
                + ((team != null) ? "team=" + team + ", " : "")
                + "flipped=" + flipped + ", "
                + ((word != null) ? "word=" + word : "")
                + "]";
    }
}
