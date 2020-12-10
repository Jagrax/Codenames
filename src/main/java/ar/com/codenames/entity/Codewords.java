package ar.com.codenames.entity;

import ar.com.codenames.Registro;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Codewords {

    private final static Logger log = LoggerFactory.getLogger(Codewords.class);

    /**
     * Mapa que asocia un Id a un Team
     */
    public Map<Integer, Team> teams;

    /**
     * Contador que se muestra en la pagina
     */
    public int timer;

    /**
     * Cantidad de segundos por turno
     */
    public int timerAmount;

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
     * Id del equipo del turno actual
     */
    public Integer turnId;

    public ArrayList<Integer> winnerId;

    /**
     * Indica si la partida finalizo o no. Puede ser true si:
     * - A un equipo no le quedan palabras por descubrir
     * - Si un equipo toca la negra
     */
    boolean over;

    /**
     * Tabler del juego
     */
    public Board board;

    /**
     * Id del equipo que comenzo en la partida anterior
     */
    public Integer startedLastGameTeamId;

    private Registro registroPartidaActual;

    private final ArrayList<Registro> registros;

    private int lastQuantityOfRegistrosInReport = 0;

    public StringBuilder gameReport;

    private boolean useTilesWithAddTime;

    public Codewords(ArrayList<Team> teams, int boardSize, int wordsByTeam, ArrayList<String> words, Integer turnDuration) {
        // Set timer value
        this.timerAmount = turnDuration != null ? turnDuration + 1 : 61;
        // Load words of packs selected
        this.words = words;
        this.boardSize = boardSize;
        this.wordsByTeam = wordsByTeam;

        this.teams = new HashMap<>();
        for (int index = 0; index < teams.size(); index++) {
            this.teams.put(index, teams.get(index));
        }

        registros = new ArrayList<>();
        gameReport = null;

        // Inicializo el juego
        init();
    }

    public void init() {
        // When game is created, select a team to start, randomly
        this.randomTurn();
        // Whether or not the game has been won / lost
        this.over = false;
        // Winning team
        this.winnerId = new ArrayList<>();
        // Set the timer
        timer = timerAmount;

        // Init the board
        this.board = new Board(boardSize);
        // Populate the board
        this.newBoard();
    }

    /**
     * Controla si a algun equipo le quedan baldosas por voltear y lo declara ganador si no le queda ninguna
     */
    public void checkWin(Collection<Player> playersInRoom) {
        for (int teamId = 0; teamId < teams.size(); teamId++) {
            if (countPendingTiles(teamId) == 0) {
                over = true;
                winnerId.add(teamId);
                for (Player player : playersInRoom) {
                    registroPartidaActual.getPlayers().add(SerializationUtils.clone(player));
                }
                registroPartidaActual.setWinnerTeamId(teamId);
                registroPartidaActual.setDeathTileFlipped(false);
                registros.add(registroPartidaActual);
            }
        }
    }

    // When called, will change a tiles state to flipped
    public boolean flipTile(int x, int y, Player whoTouchedTile, Collection<Player> playersInRoom) {
        Tile tile = board.getTile(x, y);
        if (tile.isFlipped()) return false;

        // Flip tile
        tile.setFlipped(true);
        // If death was flipped, end the game and find winner
        if (tile.isDeath()) {
            over = true;
            for (Integer teamId : teams.keySet()) {
                if (!teamId.equals(turnId)) {
                    winnerId.add(teamId);
                }
            }
            for (Player player : playersInRoom) {
                registroPartidaActual.getPlayers().add(SerializationUtils.clone(player));
            }
            registroPartidaActual.setDeathTileFlipped(true);
            registroPartidaActual.setPlayerWhoTouchedDeathTile(whoTouchedTile);
            registroPartidaActual.setWinnerTeamId(winnerId.get(0));
            registros.add(registroPartidaActual);
        } else if (tile.isNeutral()) {
            // Switch turn if neutral was flipped
            if (!tile.isAddTime()) switchTurn();
        } else {
            // Find the team of tile
            Team tileFlippedTeam = teams.get(tile.getTeamId());
            tileFlippedTeam.setPendingTiles(tileFlippedTeam.getPendingTiles() - 1);

            if (!tile.getTeamId().equals(turnId)) {
                // Switch turn if opposite teams tile was flipped
                switchTurn();
            }
        }

        if (tile.isAddTime()) {
            timer += timerAmount;
        }

        checkWin(playersInRoom); // See if the game is over
        return true;
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

    // Reset the timer and swap the turn over to the next team
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

    // Randomly assigns a death tile and teams tiles
    public void newBoard() {
        registroPartidaActual = new Registro();

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

        if (useTilesWithAddTime) {
            randomCoordinates = randomCoordinates();
            // Si se dio que justo agarre la que es negra, vuelvo a generar las coordenadas
            while (board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).isDeath()) {
                randomCoordinates = randomCoordinates();
            }

            // Hago que una tile sea la que te agregar 60"
            board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).setAddTime(true);
        }

        // Selecciono el equipo que comenzara la partida
        Integer currentTeam = turnId;

        ArrayList<Integer> tmpAddTimeTiles = new ArrayList<>();

        // Recorro las baldosas asignandoles un color (equipo)
        for (int i = 0; i < ((wordsByTeam * teams.size()) + 1); i++) {
            // Obtengo unas coordenadas aleatorias
            randomCoordinates = randomCoordinates();

            // Si a esta baldosa ya se le asigno un color (no es mas neutral) o tiene el timer, busco una nueva
            while (!board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).isNeutral() || board.getTile(randomCoordinates.getKey(), randomCoordinates.getValue()).isAddTime()) {
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

        printBoard();
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

    // Debugging purposes
    private void printBoard() {
        for (int x = 0; x < boardSize; x++) {
            String sep = "";
            StringBuilder tilesRow = new StringBuilder();
            for (int y = 0; y < boardSize; y++) {
                tilesRow.append(sep).append(this.board.getTile(x, y).getType());
                sep = "|";
            }
            log.debug(tilesRow.toString());
        }
    }

    // -------------------- GETTERS & SETTERS --------------------

    public Map<Integer, Team> getTeams() {
        return teams;
    }

    public void setTeams(Map<Integer, Team> teams) {
        this.teams = teams;
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

    public void setTimerAmount(int timerAmount) {
        this.timerAmount = timerAmount;
    }

    public ArrayList<String> getWords() {
        return words;
    }

    public void setWords(ArrayList<String> words) {
        this.words = words;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public int getWordsByTeam() {
        return wordsByTeam;
    }

    public void setWordsByTeam(int wordsByTeam) {
        this.wordsByTeam = wordsByTeam;
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

    public boolean isOver() {
        return over;
    }

    public Board getBoard() {
        return board;
    }

    public Object generateReportHtml() {
        if (lastQuantityOfRegistrosInReport == registros.size()) return gameReport != null ? gameReport.toString() : "<p>Aun no hay estadisticas computadas</p>";

        Map<Integer, Set<Player>> playersByTeam = getPlayersByTeam();
        Map<Player, Integer> winsByPlayer = new HashMap<>();
        Map<Player, Integer> defeatsByPlayer = new HashMap<>();
        Map<Player, Integer> spymasterWinsByPlayer = new HashMap<>();
        Map<Player, Integer> spymasterDefeatsByPlayer = new HashMap<>();
        Map<Player, Integer> spymasterWithoutBlacksWinsByPlayer = new HashMap<>();
        Map<Player, Integer> deathTilesByPlayer = new HashMap<>();
        Map<Team, Integer> winsByTeam = new HashMap<>();
        Map<Team, Integer> deathTilesByTeam = new HashMap<>();
        for (Integer teamId : teams.keySet()) {
            winsByTeam.put(teams.get(teamId), 0);
            deathTilesByTeam.put(teams.get(teamId), 0);
        }

        Set<Player> allPlayers = new TreeSet<>();
        for (Registro registro : registros) allPlayers.addAll(registro.getPlayers());

        for (Registro registro : registros) {
            Integer countWinsByTeam = winsByTeam.get(teams.get(registro.getWinnerTeamId()));
            winsByTeam.put(teams.get(registro.getWinnerTeamId()), ++countWinsByTeam);

            if (registro.isDeathTileFlipped()) {
                for (Integer teamId : teams.keySet()) if (!registro.getWinnerTeamId().equals(teamId)) {
                    Integer contDeathTiles = deathTilesByTeam.get(teams.get(teamId));
                    deathTilesByTeam.put(teams.get(teamId), ++contDeathTiles);
                }

                deathTilesByPlayer.putIfAbsent(registro.getPlayerWhoTouchedDeathTile(), 0);
                Integer deathTilesOfPlayer = deathTilesByPlayer.get(registro.getPlayerWhoTouchedDeathTile());
                deathTilesByPlayer.put(registro.getPlayerWhoTouchedDeathTile(), ++deathTilesOfPlayer);
            }

            for (Integer teamId : playersByTeam.keySet()) {
                for (Player player : playersByTeam.get(teamId)) {
                    winsByPlayer.putIfAbsent(player, 0);
                    defeatsByPlayer.putIfAbsent(player, 0);
                    spymasterWinsByPlayer.putIfAbsent(player, 0);
                    spymasterDefeatsByPlayer.putIfAbsent(player, 0);
                    deathTilesByPlayer.putIfAbsent(player, 0);
                    spymasterWithoutBlacksWinsByPlayer.putIfAbsent(player, 0);

                    // Si esta en este Registro, significa que estuvo conectado durante esta partida
                    Player playerInCurrentRegistry = getPlayerInSet(registro.getPlayers(), player);
                    if (playerInCurrentRegistry != null) {
                        // Si el jugador actual estuvo en este equipo durante esa partida me fijo si gano o perdio
                        if (playerInCurrentRegistry.getTeamId().equals(player.getTeamId())) {
                            if (player.getTeamId().equals(registro.getWinnerTeamId())) {
                                Integer cantGanadas = winsByPlayer.get(player);
                                winsByPlayer.put(player, ++cantGanadas);
                                if (playerInCurrentRegistry.getRole().equals(Player.Role.SPYMASTER)) {
                                    // Reutilizo la variable
                                    cantGanadas = spymasterWinsByPlayer.get(player);
                                    spymasterWinsByPlayer.put(player, ++cantGanadas);
                                    if (!registro.isDeathTileFlipped()) {
                                        // Reutilizo la variable
                                        cantGanadas = spymasterWithoutBlacksWinsByPlayer.get(player);
                                        spymasterWithoutBlacksWinsByPlayer.put(player, ++cantGanadas);
                                    }
                                }
                            } else {
                                Integer cantPerdidas = defeatsByPlayer.get(player);
                                defeatsByPlayer.put(player, ++cantPerdidas);
                                if (playerInCurrentRegistry.getRole().equals(Player.Role.SPYMASTER)) {
                                    // Reutilizo la variable
                                    cantPerdidas = spymasterDefeatsByPlayer.get(player);
                                    spymasterDefeatsByPlayer.put(player, ++cantPerdidas);
                                }
                            }
                        }
                    }
                }
            }
        }

        gameReport = new StringBuilder(
                "<h5>Puntajes individuales</h5>" +
                        "<table class='table table-sm table-bordered mb-3'>" +
                        "  <thead class='table-dark'>" +
                        "    <tr>" +
                        "      <th scope='col'>Jugadores</th>" +
                        "      <th scope='col'>Ganadas</th>" +
                        "      <th scope='col'>Perdidas</th>" +
                        "      <th scope='col'>Balance</th>" +
                        "    </tr>" +
                        "  </thead>" +
                        "  <tbody>");
        for (Player player : allPlayers) {
            gameReport.append("<tr><td>").append(player.getExcelname()).append("</td><td>").append(winsByPlayer.get(player)).append("</td><td>").append(defeatsByPlayer.get(player)).append("</td><td>").append(winsByPlayer.get(player) - defeatsByPlayer.get(player)).append("</td></tr>");
        }

        gameReport.append(
                "  </tbody>" +
                        "</table>" +
                        "<h5>Puntajes por equipo</h5>" +
                        "<table class='table table-sm table-bordered mb-3'>" +
                        "  <thead class='table-dark'>" +
                        "    <tr>" +
                        "      <th scope='col'></th>");
        for (Integer teamId : teams.keySet()) {
            gameReport.append("<th scope='col'>").append(teams.get(teamId).getName()).append("</th>");
        }
        gameReport.append(
                "    </tr>" +
                        "  </thead>" +
                        "  <tbody>" +
                        "    <tr>" +
                        "      <td>Ganadas</td>");
        for (Integer teamId : teams.keySet()) {
            gameReport.append("<td>").append(winsByTeam.get(teams.get(teamId))).append("</td>");
        }

        gameReport.append(
                "    </tr>" +
                        "  </tbody>" +
                        "</table>" +
                        "<h5>Puntajes individuales como Spymaster</h5>" +
                        "<table class='table table-sm table-bordered mb-3'>" +
                        "  <thead class='table-dark'>" +
                        "    <tr>" +
                        "      <th scope='col'>Jugadores</th>" +
                        "      <th scope='col'>Ganadas</th>" +
                        "      <th scope='col'>Ganadas (sin negra)</th>" +
                        "      <th scope='col'>Perdidas</th>" +
                        "    </tr>" +
                        "  </thead>" +
                        "  <tbody>");
        for (Player player : allPlayers) {
            gameReport.append("<tr><td>").append(player.getExcelname()).append("</td><td>").append(spymasterWinsByPlayer.get(player)).append("</td><td>").append(spymasterWithoutBlacksWinsByPlayer.get(player)).append("</td><td>").append(spymasterDefeatsByPlayer.get(player)).append("</td></tr>");
        }
        gameReport.append(
                "  </tbody>" +
                        "</table>" +
                        "<h5>Negras por equipo</h5>" +
                        "<table class='table table-sm table-bordered mb-3'>" +
                        "  <thead class='table-dark'>" +
                        "    <tr>" +
                        "      <th scope='col'></th>");
        for (Integer teamId : teams.keySet()) {
            gameReport.append("<th scope='col'>").append(teams.get(teamId).getName()).append("</th>");
        }
        gameReport.append(
                "    </tr>" +
                        "  </thead>" +
                        "  <tbody>" +
                        "    <tr>" +
                        "      <td>Negras</td>");
        for (Integer teamId : teams.keySet()) {
            gameReport.append("<td>").append(deathTilesByTeam.get(teams.get(teamId))).append("</td>");
        }

        gameReport.append(
                "    </tr>" +
                        "  </tbody>" +
                        "</table>" +
                        "<h5>Negras por jugador</h5>" +
                        "<table class='table table-sm table-bordered mb-3'>" +
                        "  <thead class='table-dark'>" +
                        "    <tr>" +
                        "      <th scope='col'>Jugador</th>" +
                        "      <th scope='col'>Jugador</th>" +
                        "    </tr>" +
                        "  </thead>" +
                        "  <tbody>");
        for (Player player : allPlayers) {
            gameReport.append("<tr><td>").append(player.getExcelname()).append("</td><td>").append(deathTilesByPlayer.get(player)).append("</td></tr>");
        }

        gameReport.append(
                "  </tbody>" +
                        "</table>");

        lastQuantityOfRegistrosInReport = registros.size();

        return gameReport.toString();
    }

    public boolean isUseTilesWithAddTime() {
        return useTilesWithAddTime;
    }

    public void setUseTilesWithAddTime(boolean useTilesWithAddTime) {
        this.useTilesWithAddTime = useTilesWithAddTime;
    }

    private Map<Integer, Set<Player>> getPlayersByTeam() {
        Map<Integer, Set<Player>> playersByTeam = new HashMap<>();
        for (Registro registro : registros) {
            for (Player player : registro.getPlayers()) {
                // Si no tengo ningun jugador de este equipo, inicializo el Set
                playersByTeam.computeIfAbsent(player.getTeamId(), k -> new LinkedHashSet<>());
                // Agrego al jugador al Set del equipo al que corresponde
                playersByTeam.get(player.getTeamId()).add(player);
            }
        }

        return playersByTeam;
    }

    private Player getPlayerInSet(Set<Player> sourcePlayers, Player playerToFind) {
        if (sourcePlayers != null && !sourcePlayers.isEmpty()) {
            for (Player sourcePlayer : sourcePlayers) {
                if (sourcePlayer.equals(playerToFind)) return sourcePlayer;
            }
        }
        return null;
    }
}