package ar.com.codenames;

import ar.com.codenames.entity.Codewords;
import ar.com.codenames.entity.Player;
import ar.com.codenames.entity.Team;
import com.corundumstudio.socketio.SocketIOClient;

import java.util.*;

public class Room {

    private final String name;
    private final String password;
    private final Map<String, Player> players;
    private Codewords game;
    private Timer turnTimer;

    public Room(String name, String password, Map<String, Room> ROOM_LIST) {
        this.name = name;
        this.password = password;
        this.players = new HashMap<>();

        // Add room to room list
        ROOM_LIST.put(this.name, this);
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public Codewords getGame() {
        return game;
    }

    public Timer getTurnTimer() {
        return turnTimer;
    }

    public Codewords initGame(ArrayList<Team> teams, int boardSize, int wordsByTeam, ArrayList<String> words, int turnDuration) {
        game = new Codewords(teams, boardSize, wordsByTeam, words, turnDuration);
        turnTimer = new Timer();
        return game;
    }

    public void addPlayer(String id, Player newPlayer) {
        int candidateTeam = 0;
        // Si es null es porque estoy creando el juego por primera vez y aun no se settearon los teams
        if (game != null && game.getTeams() != null) {
            // Cuento los jugadores por equipo
            Map<Integer, Integer> playersByTeam = new HashMap<>();
            for (Player player : players.values()) {
                Integer playersInTeam = playersByTeam.get(player.getTeamId());
                if (playersInTeam == null) playersInTeam = 0;
                playersByTeam.put(player.getTeamId(), ++playersInTeam);
            }

            Integer mayorCantidadDeJugadoresEnUnEquipo = playersByTeam.get(0);
            Integer menorCantidadDeJugadoresEnUnEquipo = playersByTeam.get(0);
            for (Integer teamId : game.getTeams().keySet()) {
                // Si en un equipo no hay jugadores, lo asigno a ese y me voy
                if (playersByTeam.get(teamId) == null) {
                    candidateTeam = teamId;
                    break;
                }

                // Controlo el maximo de jugadores en un equipo
                Integer cantidadDeJugadoresEnElEquipo = playersByTeam.get(teamId);
                if (cantidadDeJugadoresEnElEquipo > mayorCantidadDeJugadoresEnUnEquipo) {
                    mayorCantidadDeJugadoresEnUnEquipo = cantidadDeJugadoresEnElEquipo;
                }

                // Si el equipo actual tiene menos jugadores que el equipo con menor cantidad de jugadores hasta ahora, el equipo actual es el candidato a colocar el nuevo jugador
                if (cantidadDeJugadoresEnElEquipo < menorCantidadDeJugadoresEnUnEquipo) {
                    menorCantidadDeJugadoresEnUnEquipo = cantidadDeJugadoresEnElEquipo;
                    candidateTeam = teamId;
                }
            }
        }
        newPlayer.setTeamId(candidateTeam);// Lo mando al team con menos jugadores o al 0 si todos tienen la misma cantidad (o si estoy creado el juego)
        players.put(id, newPlayer);
    }

    /**
     * Busca un jugador por id
     * @param client Usuario que envio el request
     * @return Jugador correspondiente al request
     */
    public Player getPlayer(SocketIOClient client) {
        for (Player player : players.values()) {
            if (player.getId().equals(client.getSessionId().toString())) {
                return player;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "Room ["
                + ((name != null) ? "name=" + name + ", " : "")
                + ((password != null) ? "password=" + password + ", " : "")
                + ((players != null) ? "players=" + players + ", " : "")
                + ((game != null) ? "game=" + game : "")
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return name.equals(room.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}