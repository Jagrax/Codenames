package ar.com.codenames;

import ar.com.codenames.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Registro {

    private Set<Player> players;
    private Integer winnerTeamId;
    private boolean deathTileFlipped;
    private Player playerWhoTouchedDeathTile;

    public Registro() {
        players = new HashSet<>();
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public void setPlayers(Set<Player> players) {
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

    public Player getPlayerWhoTouchedDeathTile() {
        return playerWhoTouchedDeathTile;
    }

    public void setPlayerWhoTouchedDeathTile(Player playerWhoTouchedDeathTile) {
        this.playerWhoTouchedDeathTile = playerWhoTouchedDeathTile;
    }
}