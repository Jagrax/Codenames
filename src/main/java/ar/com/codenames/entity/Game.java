package ar.com.codenames.entity;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Game {

    private final static Logger log = LoggerFactory.getLogger(Game.class);

    public static Set<String> baseWords = new HashSet<>();
    public static Set<String> fruitWords = new HashSet<>();

    public Map<Integer, Team> teams; // ID, TEAM

    public ArrayList<Player> players = new ArrayList<>();
    public ArrayList<String> words;
    private int boardSize;
    private int wordsByTeam;
    /**
     * Contador que se muestra en la pagina
     */
    public int timer;

    /**
     * Cantidad de segundos por turno
     */
    public int timerAmount;

    /**
     * TODO - ELIMINAR
     */
    public Team turn;
    public Integer turnId;
    public ArrayList<Team> winner = new ArrayList<>();
    public ArrayList<Integer> winnerId = new ArrayList<>();
    boolean over;
    private Timer turnTimer;

    public Board board;

    public Game() {
    }

    public void initGame(ArrayList<Team> teams, int boardSize, int wordsByTeam, ArrayList<String> words, int turnDuration, SocketIOServer server) {
        this.words = words;
        this.boardSize = boardSize;
        this.wordsByTeam = wordsByTeam;
        this.timerAmount = turnDuration + 1; // Establezco un tiempo base para cada turno equivalente a 1 min + 1 seg

        this.teams = new HashMap<>();
        for (int index = 0; index < teams.size(); index++) {
            this.teams.put(index, teams.get(index));
        }

        // Inicializo el juego
        newBoard();

        turnTimer = new Timer();
        turnTimer.schedule(new TurnTimer(server, this), 0, 1000);
    }

    // When called, will change a tiles state to flipped
    public boolean flipTile(int x, int y) {
        Tile tile = board.getTile(x, y);
        if (tile.isNotFlipped()) {
            // Flip tile
            tile.setFlipped(true);
            if (tile.isDeath()) {
                over = true;
                turnTimer.cancel();
                for (Team team : teams.values()) {
                    if (!team.equals(turn)) {
                        winner.add(team);
                    }
                }
            } else {
                // Find the team of tile
                Team tileFlippedTeam = tile.getTeam();

                for (Team team : teams.values()) if (team.equals(tileFlippedTeam)) team.setPendingTiles(team.getPendingTiles() - 1);

                if (tile.isNeutral()) {
                    switchTurn(); // Switch turn if neutral was flipped
                } else if (!tileFlippedTeam.equals(turn)) {
                    switchTurn(); // Switch turn if opposite teams tile was flipped
                }
            }
            checkWin(); // See if the game is over
            return true;
        } else {
            return false;
        }
    }

    // Reset the timer and swap the turn over to the other team
    public void switchTurn() {
        // Reset timer
        timer = timerAmount + 1;
        int nextTeamId = getId(teams, turn) + 1;
        if (nextTeamId >= teams.size()) {
            nextTeamId = 0;
        }

        // Swith turn
        turn = teams.get(nextTeamId);
        turnId = nextTeamId;
    }

    public void updateWordPool(String... wordsPacks) {
        Set<String> pool = new HashSet<>();
        for (String wordsPack : wordsPacks) {
            switch (wordsPack) {
                case "BASE":
                    pool.addAll(baseWords);
                    break;
                case "FRUITS":
                    pool.addAll(fruitWords);
                    break;
            }
        }

        // Un default por las dudas
        if (pool.isEmpty()) {
            pool = baseWords;
        }

        words = new ArrayList<>(pool);
    }

    public void addPlayer(Player player) {
        player.setTeamId(0);// Lo mando al team 0 y que se acomode manualmente
        players.add(player);
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public Map<Integer, Team> getTeams() {
        return teams;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public int getWordsByTeam() {
        return wordsByTeam;
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public int getTimerAmount() {
        return timerAmount;
    }

    public Team getTurn() {
        return turn;
    }

    public ArrayList<Team> getWinner() {
        return winner;
    }

    public boolean isOver() {
        return over;
    }

    public Board getBoard() {
        return board;
    }

    public Timer getTurnTimer() {
        return turnTimer;
    }

    public <K, V> K getId(Map<K, V> map, V value) {
        return map.keySet()
                .stream()
                .filter(key -> value.equals(map.get(key)))
                .findFirst().get();
    }

    public void setTeams(Map<Integer, Team> teams) {
        this.teams = teams;
    }

    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }

    public ArrayList<String> getWords() {
        return words;
    }

    public void setWords(ArrayList<String> words) {
        this.words = words;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public void setWordsByTeam(int wordsByTeam) {
        this.wordsByTeam = wordsByTeam;
    }

    public void setTimerAmount(int timerAmount) {
        this.timerAmount = timerAmount;
    }

    public void setTurn(Team turn) {
        this.turn = turn;
    }

    public Integer getTurnId() {
        return turnId;
    }

    public void setTurnId(Integer turnId) {
        this.turnId = turnId;
    }

    public void setWinner(ArrayList<Team> winner) {
        this.winner = winner;
    }

    public ArrayList<Integer> getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(ArrayList<Integer> winnerId) {
        this.winnerId = winnerId;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    /*
     *  ----------------------- OK -----------------------
     */

    /**
     * Busca un jugador por id
     * @param client Usuario que envio el request
     * @return Jugador correspondiente al request
     */
    public Player getPlayer(SocketIOClient client) {
        for (Player player : players) {
            if (player.getId().equals(client.getSessionId().toString())) {
                return player;
            }
        }

        return null;
    }

    /**
     * Inicializacion de juego:
     * <ol>
     * <li>Determina que equipo comienza</li>
     * <li>Llena el tablero con palabras</li>
     * <li>Asigna los colores a las baldosas</li>
     * </ol>
     */
    public void newBoard() {
        // Determino que equipo comienza
        randomTurn();
        this.over = false;
        this.winner = new ArrayList<>();
        this.winnerId = new ArrayList<>();
        // Inicializo el timer de la UI
        timer = timerAmount;

        board = new Board(boardSize);

        // Variable para guardar las palabras ya incluidas en el tablero
        ArrayList<String> usedWords = new ArrayList<>();

        String foundWord;
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                // Elijo una palabra al azar del Set de palabras
                foundWord = words.get(new Random().nextInt(words.size()));
                // Si ya fue agregada, elijo otra
                while (usedWords.contains(foundWord)) {
                    foundWord = words.get(new Random().nextInt(words.size()));
                }
                // Agrego la palabra al listado de palabras ya agregadas
                usedWords.add(foundWord);

                // Creo una baldosa nueva
                Tile tile = new Tile(x, y);
                tile.setFlipped(false);
                tile.setWord(foundWord);
                // Y la agrego al tablero
                if (board.addTile(tile)) {
                    log.debug("Se agergo al table " + tile);
                } else {
                    System.exit(-1);
                }
            }
        }

        // Genero unas coordenadas aleatorias
        AbstractMap.SimpleEntry<Integer, Integer> randomCoordinates = randomCoordinates();

        // Hago que la baldosa esas cooredanas sea la negra
        board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).setDeath();

        // Selecciono el equipo que comenzara la partida
        Integer currentTeam = turnId;

        // Recorro las baldosas asignandoles un color (equipo)
        for (int i = 0; i < ((wordsByTeam * teams.size()) + 1); i++) {
            // Obtengo unas coordenadas aleatorias
            randomCoordinates = randomCoordinates();

            // Si a esta baldosa ya se le asigno un color (no es mas neutral), busco una nueva
            while (!board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).isNeutral()) {
                randomCoordinates = randomCoordinates();
            }

            // La asigno a un color/equipo
            board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).setTeamId(currentTeam);
            board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).setTeam(teams.get(currentTeam));
            board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).setColored();

            int nextTeamId = currentTeam + 1;
            if (nextTeamId >= teams.size()) {
                nextTeamId = 0;
            }

            // Cambio de equipo para asignar el color
            currentTeam = nextTeamId;
        }

        // Actualizo la cantidad de baldosas pendientes de cada equipo
        for (int teamId = 0; teamId < teams.size(); teamId++) {
            teams.get(teamId).setPendingTiles(countPendingTiles(teamId));
        }

        // TODO - Comentar o parametrizar
        printBoard();
    }

    /**
     * Imprime en el log el tablero con las baldosas. No muestra las Neutrales
     */
    public void printBoard() {
        for (int x = 0; x < boardSize; x++) {
            String sep = "";
            StringBuilder row = new StringBuilder();
            for (int y = 0; y < boardSize; y++) {
                Tile tile = board.getTile(x, y);
                row.append(sep).append(tile.getWord());
                if (tile.isColored()) {
                    row.append(" (").append(teams.get(tile.getTeamId()).getColor()).append(")");
                } else if (tile.isDeath()) {
                    row.append(" (DEATH)");
                }
                sep = "; ";
            }
            log.info(row.toString());
        }
    }

    /**
     * Controla si a algun equipo le quedan baldosas por voltear y lo declara ganador si no le queda ninguna
     */
    public void checkWin() {
        for (int teamId = 0; teamId < teams.size(); teamId++) {
            if (countPendingTiles(teamId) == 0) {
                over = true;
                winner.add(teams.get(teamId));
                winnerId.add(teamId);
                turnTimer.cancel();
            }
        }
    }

    /**
     *  Determina de que equipo es el turno de manera aleatorio
     */
    private void randomTurn() {
        int teamId = new Random().nextInt(teams.size());
        for (Integer teamIndex : teams.keySet()) {
            if (teamIndex.equals(teamId)) {
                turn = teams.get(teamIndex);
                turnId = teamIndex;
                break;
            }
        }
    }

    /**
     * Busca un numero aleatorio entre 0 y el tamaño del tablero.
     * Luego, en base a ese numero, genera coordenadas
     * @return  Coordenadas &lt;X,Y&gt;
     */
    public AbstractMap.SimpleEntry<Integer, Integer> randomCoordinates() {
        double num = Math.floor(Math.random() * (boardSize * boardSize));
        double x = Math.floor(num / boardSize);
        double y = num % boardSize;
        return new AbstractMap.SimpleEntry<>((int) x, (int) y);
    }

    /**
     * Cuenta las baldosas que aun no fueron voltedas de un determinado equipo
     * @param teamId Id del equipo
     * @return cantidad de baldosas restantes
     */
    public int countPendingTiles(int teamId) {
        int count = 0;
        for (int x = 0; x < boardSize; x++){
            for (int y = 0; y < boardSize; y++){
                if (board.getTile(x, y).isNotFlipped() && board.getTile(x, y).getTeamId() != null && board.getTile(x, y).getTeamId().equals(teamId)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void removePlayer(SocketIOClient client) {
        if (players != null && !players.isEmpty()) {
            players.removeIf(player -> player.getId().equals(client.getSessionId().toString()));
        }
    }

    /**
     * TODO
     */
    public void randomizeTeams() {
        /*
        let color = 0;    // Get a starting color
        if (Math.random() < 0.5) color = 1

        let keys = Object.keys(players) // Get a list of players in the room from the dictionary
        let placed = []                 // Init a temp array to keep track of who has already moved

        while (placed.length < keys.length){
            let selection = keys[Math.floor(Math.random() * keys.length)] // Select random player index
            if (!placed.includes(selection)) placed.push(selection) // If index hasn't moved, move them
        }

        // Place the players in alternating teams from the new random order
        for (let i = 0; i < placed.length; i++){
            let player = players[placed[i]]
            if (color === 0){
                player.team = 'red'
                color = 1
            } else {
                player.team = 'blue'
                color = 0
            }
        }
        */
    }
}

class TurnTimer extends TimerTask {

    private final SocketIOServer server;
    private final Game game;

    public TurnTimer(SocketIOServer server, Game game) {
        this.server = server;
        this.game = game;
    }

    @Override
    public void run() {
        game.setTimer(game.getTimer() - 1);
        if (game.getTimer() < 0) {
            game.switchTurn();
            game.setTimer(game.getTimerAmount());
            JSONObject gameStateResponse = new JSONObject();
            gameStateResponse.put("success", true);
            gameStateResponse.put("game", new JSONObject(game).toString());
            server.getBroadcastOperations().sendEvent("gameState", gameStateResponse.toString());
        }
        server.getBroadcastOperations().sendEvent("timerUpdate", game.getTimer());
    }
}