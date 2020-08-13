package ar.com.codenames.entity;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

public class Game {

    private final static Logger log = LoggerFactory.getLogger(Game.class);

    /**
     * Mapa que asocia un Id a un Team
     */
    public Map<Integer, Team> teams;

    /**
     * Jugadores en el juego
     */
    public ArrayList<Player> players = new ArrayList<>();

    /**
     * Palabras a usar en la partida
     */
    public ArrayList<String> words;

    /**
     * Tamaño de tablero. Es cuadrado, o sea que siempre se multiplica por si mismo. Ej.: 5x5, 6x6, 7x7
     */
    private int boardSize;

    /**
     * Cantidad de palabras a descubrir por equipo
     */
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
     * Id del equipo del turno actual
     */
    public Integer turnId;

    public ArrayList<Integer> winnerId = new ArrayList<>();

    /**
     * Indica si la partida finalizo o no. Puede ser true si:
     * - A un equipo no le quedan palabras por descubrir
     * - Si un equipo toca la negra
     */
    boolean over;

    /**
     * Timer de 1 min por turno
     */
    private Timer turnTimer;

    /**
     * Tabler del juego
     */
    public Board board;

    /**
     * Id del equipo que comenzo en la partida anterior
     */
    public Integer startedLastGameTeamId;

    private final Map<String, Set<String>> wordsFilesMap = new HashMap<>();

    private void initWords() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL url = loader.getResource("words");
            if (url == null) {
                throw new Exception("No se encontro el directorio 'words' dentro de la carpeta resources.");
            }
            String path = url.getPath();
            for (File wordsFile : Objects.requireNonNull(new File(path).listFiles())) {
                Set<String> wordsInFile = new HashSet<>();
                FileReader fileReader = new FileReader(wordsFile);
                BufferedReader reader = new BufferedReader(fileReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().equals("")) {
                        if (wordsInFile.contains(line.trim())) log.info("El archivo " + wordsFile.getName() + " tiene mas de una vez la palabra " + line.trim());
                        wordsInFile.add(line.trim());
                    }
                }
                String packName = wordsFile.getName().substring(0, wordsFile.getName().lastIndexOf(".")).toUpperCase();
                wordsFilesMap.put(packName, wordsInFile);
            }
        } catch (Exception e) {
            log.error("Se produjo un error al inicializar los packs de palabras", e);
            System.exit(-1);
        }
    }

    public Game() {
        initWords();
    }

    public Map<String, Set<String>> getWordsFilesMap() {
        return wordsFilesMap;
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

    public boolean isOver() {
        return over;
    }

    public Board getBoard() {
        return board;
    }

    public Timer getTurnTimer() {
        return turnTimer;
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

    public Integer getTurnId() {
        return turnId;
    }

    public void setTurnId(Integer turnId) {
        this.turnId = turnId;
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

    public void initGame(ArrayList<Team> teams, int boardSize, int wordsByTeam, ArrayList<String> words, int turnDuration, SocketIOServer server) {
        log.info("initGame called");
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

    public void updateWordPool(String... wordsPacks) {
        /*
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
        */
    }

    // When called, will change a tiles state to flipped
    public boolean flipTile(int x, int y) {
        Tile tile = board.getTile(x, y);
        if (tile.isNotFlipped()) {
            // Flip tile
            tile.setFlipped(true);
            if (tile.isDeath()) {
                over = true;
                for (Integer teamId : teams.keySet()) {
                    if (!teamId.equals(turnId)) {
                        winnerId.add(teamId);
                    }
                }
            } else if (tile.isNeutral()) {
                switchTurn(); // Switch turn if neutral was flipped
            } else {
                // Find the team of tile
                Team tileFlippedTeam = teams.get(tile.getTeamId());
                tileFlippedTeam.setPendingTiles(tileFlippedTeam.getPendingTiles() - 1);

                if (!tile.getTeamId().equals(turnId)) {
                    switchTurn(); // Switch turn if opposite teams tile was flipped
                }
            }
            checkWin(); // See if the game is over
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reinicio el timer y paso el turno al siguiente equipo
     */
    public void switchTurn() {
        // Reset timer
        timer = timerAmount + 1;
        int nextTeamId = turnId + 1;
        if (nextTeamId >= teams.size()) {
            nextTeamId = 0;
        }

        // Swith turn
        turnId = nextTeamId;
    }

    /**
     * Agrega un nuevo jugador al juego, asignandolo al equipo con menor cantidad de jugadores o al equipo 0 si todos tienen la misma cantidad
     *
     * @param newPlayer Jugador a agregar
     */
    public void addPlayer(Player newPlayer) {
        int candidateTeam = 0;
        // Si es null es porque estoy creando el juego por primera vez y aun no se settearon los teams
        if (teams != null) {
            // Cuento los jugadores por equipo
            Map<Integer, Integer> playersByTeam = new HashMap<>();
            for (Player player : getPlayers()) {
                Integer playersInTeam = playersByTeam.get(player.getTeamId());
                if (playersInTeam == null) playersInTeam = 0;
                playersByTeam.put(player.getTeamId(), ++playersInTeam);
            }

            Integer mayorCantidadDeJugadoresEnUnEquipo = playersByTeam.get(0);
            Integer menorCantidadDeJugadoresEnUnEquipo = playersByTeam.get(0);
            for (Integer teamId : teams.keySet()) {
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
        players.add(newPlayer);
    }

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
        //printBoard();
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
                winnerId.add(teamId);
            }
        }
    }

    /**
     *  Determina de que equipo es el turno en base al turno del ultimo equipo
     */
    private void randomTurn() {
        int teamId;
        if (startedLastGameTeamId == null) {
            startedLastGameTeamId = 0;
            teamId = startedLastGameTeamId;
        } else {
            teamId = startedLastGameTeamId + 1;
        }
        if (teamId == teams.size()) teamId = 0;
        for (Integer teamIndex : teams.keySet()) {
            if (teamIndex.equals(teamId)) {
                turnId = teamIndex;
                startedLastGameTeamId = turnId;
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
            if (players.isEmpty()) {
                turnTimer.cancel();
            }
        }
    }

    /**
     * Aleatoriza los todos los jugadores de manera pareja entre todos los equipos
     */
    public void randomizeTeams() {
        Random r = new Random();
        ArrayList<Player> playersAlreadyMoved = new ArrayList<>();
        while (playersAlreadyMoved.size() != getPlayers().size()) {
            Player randomPlayer = getPlayers().get(r.nextInt(getPlayers().size()));
            while (playersAlreadyMoved.contains(randomPlayer)) {
                randomPlayer = getPlayers().get(r.nextInt(getPlayers().size()));
            }
            playersAlreadyMoved.add(randomPlayer);
        }

        int teamId= 0;
        for (Player player : playersAlreadyMoved) {
            player.setTeamId(teamId++);
            if (teamId >= teams.size()) {
                teamId = 0;
            }
        }
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