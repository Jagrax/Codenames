package ar.com.codenames;

import ar.com.codenames.entity.Player;

import java.util.ArrayList;

public class Registro {

    private ArrayList<Player> players;
    private Integer winnerTeamId;
    private boolean deathTileFlipped;

    public Registro() {
        players = new ArrayList<>();
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }

    public Integer getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(Integer winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public boolean isDeathTileFlipped() {
        return deathTileFlipped;
    }

    public void setDeathTileFlipped(boolean deathTileFlipped) {
        this.deathTileFlipped = deathTileFlipped;
    }
}
