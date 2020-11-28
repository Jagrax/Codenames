package ar.com.codewords;

import ar.com.codenames.RequestObject;
import ar.com.codenames.Room;
import ar.com.codenames.entity.Codewords;
import ar.com.codenames.entity.Player;
import ar.com.codenames.entity.Team;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

    private final static Logger log = LoggerFactory.getLogger(App.class);

    private final SocketIOServer server;

    private final Map<String, Player> PLAYER_LIST = new HashMap<>();
    private final Map<String, SocketIOClient> SOCKET_LIST = new HashMap<>();
    private final Map<String, Room> ROOM_LIST = new HashMap<>();

    private final Map<String, Set<String>> wordsFilesMap = new HashMap<>();

    private final Map<String, String> coloursMap = Stream.of(
            new AbstractMap.SimpleEntry<>("Rojo", "danger"),
            new AbstractMap.SimpleEntry<>("Azul", "primary"),
            new AbstractMap.SimpleEntry<>("Verde", "success"),
            new AbstractMap.SimpleEntry<>("Amarillo", "warning"),
            new AbstractMap.SimpleEntry<>("Cyan", "info"),
            new AbstractMap.SimpleEntry<>("Gryffindor", "danger"),
            new AbstractMap.SimpleEntry<>("Slytherin", "success"),
            new AbstractMap.SimpleEntry<>("Miyagi-do", "primary"),
            new AbstractMap.SimpleEntry<>("Cobra-Kai", "warning"),
            new AbstractMap.SimpleEntry<>("Swing-City", "swing-city"),
            new AbstractMap.SimpleEntry<>("Baila-Swing", "baila-swing"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public App(String hostname, int port) {
        log.info("Initializing App...");

        Configuration configuration = new Configuration();
        configuration.setHostname(hostname);
        configuration.setPort(port);

        server = new SocketIOServer(configuration);

        /* Server logic
         *****************************************************************************************/
        server.addConnectListener(client -> {
            // Alert server of the socket connection
            SOCKET_LIST.put(client.getSessionId().toString(), client);
            logStats("CONNECT: " + client.getSessionId().toString());

            // Pass server stats to client and the words
            JSONObject serverStats = new JSONObject();
            serverStats.put("wordPacks", wordsFilesMap.keySet());
            serverStats.put("rooms", ROOM_LIST.values());
            client.sendEvent("serverStats", serverStats.toString());
        });

        /* Lobby Stuff
         *****************************************************************************************/

        // Room Creation. Called when client attempts to create a rooom
        // Data: player nickname, room name, room password
        server.addEventListener("createRoom", RequestObject.class, (client, data, ackSender) -> createRoom(client, data));

        // Room Joining. Called when client attempts to join a room
        // Data: player nickname, room name, room password
        server.addEventListener("joinRoom", RequestObject.class, (client, data, ackSender) -> joinRoom(client, data));

        // Room Leaving. Called when client leaves a room
        server.addEventListener("leaveRoom", RequestObject.class, (client, data, ackSender) -> leaveRoom(client));

        // Client Disconnect
        server.addDisconnectListener(this::socketDisconnect);


        server.addEventListener("startGame", RequestObject.class, (client, data, ackSender) -> {
            logEvent("startGame", client);
            startGame(client, data.getTeamColors(), data.getWordsPacksSelected(), data.getBoardSize(), data.getWordsByTeam(), data.getTurnDuration());
            broadcastServerStats();
        });

        /* Game Stuff
         *****************************************************************************************/

        // Join Team. Called when client joins a team
        server.addEventListener("joinTeam", RequestObject.class, ((client, data, ackSender) -> {
            // Prevent Crash
            if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
            // Get player who made request
            Player player = PLAYER_LIST.get(client.getSessionId().toString());
            // Update their team
            player.setTeamId(data.getTeamId());
            // Update the game for everyone in their room
            gameUpdate(player.getRoomName());
        }));

        // Randomize Team. Called when client randomizes the teams
        server.addEventListener("randomizeTeams", RequestObject.class, ((client, data, ackSender) -> randomizeTeams(client)));

        // New Game. Called when client starts a new game
        server.addEventListener("newGame", RequestObject.class, (client, data, ackSender) -> newGame(client));

        // Switch Role. Called when client switches to spymaster / guesser
        server.addEventListener("switchRole", RequestObject.class, (client, data, ackSender) -> switchRole(client, data));

        // End Turn. Called when client ends teams turn
        server.addEventListener("endTurn", RequestObject.class, (client, data, ackSender) -> {
            // Prevent Crash
            if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
            // Get the room the client was in
            String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
            // Switch the room's game's turn
            ROOM_LIST.get(roomName).getGame().switchTurn();
            // Update the game for everyone in this room
            gameUpdate(roomName);
        });

        // Click Tile. Called when client clicks a tile
        // Data: x and y location of tile in grid
        server.addEventListener("clickTile", RequestObject.class, (client, data, ackSender) -> clickTile(client, data));






        server.addEventListener("requestGameReportHtml", RequestObject.class, (client, data, ackSender) -> {
            logEvent("requestGameReport", client);
            Room room = ROOM_LIST.get(client.getSessionId().toString());
            if (room != null) {
                client.sendEvent("responseGameReportHtml", room.getGame().generateReportHtml());
            }
        });

        server.addEventListener("sendSticker", RequestObject.class, (client, data, ackSender) -> {
            logEvent("sendSticker", client);
            server.getBroadcastOperations().sendEvent("stickerResponse", data.getSticker());
        });

        server.start();

        initWords();

        log.info("App initialized successfully!");
    }

    // Create room function
    // Gets a room name and password and attempts to make a new room if one doesn't exist
    // On creation, the client that created the room is created and added to the room
    private void createRoom(SocketIOClient client, RequestObject data) {
        String roomName = StringUtils.trimToNull(data.getRoomName());
        String roomPassword = StringUtils.trimToNull(data.getRoomPassword());
        String excelName = StringUtils.trimToNull(data.getExcelName());
        String nickname = StringUtils.trimToNull(data.getNickname());


        if (StringUtils.isEmpty(roomName)) {
            // Tell the client they need a valid room name
            JSONObject createRoomResponse = new JSONObject();
            createRoomResponse.put("success", false);
            createRoomResponse.put("msg", "Debes ingresar un nombre para la sala");
            createRoomResponse.put("field", "room-name");
            client.sendEvent("createRoomResponse", createRoomResponse.toString());
        } else {
            // If the requested room name is taken
            if (ROOM_LIST.get(roomName) != null) {
                // Tell the client the room arleady exists
                JSONObject createRoomResponse = new JSONObject();
                createRoomResponse.put("success", false);
                createRoomResponse.put("msg", "Ya existe una sala con ese nombre");
                createRoomResponse.put("field", "room-name");
                client.sendEvent("createRoomResponse", createRoomResponse.toString());
            } else {
                if (StringUtils.isEmpty(nickname)) {
                    JSONObject createRoomResponse = new JSONObject();
                    createRoomResponse.put("success", false);
                    createRoomResponse.put("msg", "Ingrese un apodo");
                    createRoomResponse.put("field", "nickname");
                    client.sendEvent("createRoomResponse", createRoomResponse.toString());
                } else {
                    // If the room name and nickname are both valid, proceed
                    // Create a new room
                    new Room(roomName, roomPassword, ROOM_LIST);
                    // Create a new player
                    Player player = new Player(client, excelName, nickname, roomName, PLAYER_LIST);
                    // Add player to room
                    ROOM_LIST.get(roomName).addPlayer(client.getSessionId().toString(), player);
                    // Tell client creation was successful
                    client.sendEvent("createRoomResponse", new JSONObject().put("success", true).toString());
                    logStats(client.getSessionId().toString() + "(" + player.getNickname() + ") CREATED ROOM '" + ROOM_LIST.get(player.getRoomName()).getName() + "'");
                    broadcastServerStats();
                }
            }
        }
    }

    // Join room function
    // Gets a room name and password and attempts to join said room
    // On joining, the client that joined the room is created and added to the room
    private void joinRoom(SocketIOClient client, RequestObject data) {
        String roomName = StringUtils.trimToNull(data.getRoomName());
        String roomPassword = StringUtils.trimToNull(data.getRoomPassword());
        String excelName = StringUtils.trimToNull(data.getExcelName());
        String nickname = StringUtils.trimToNull(data.getNickname());

        if (ROOM_LIST.get(roomName) == null) {
            // Tell the client the room does not exists
            JSONObject joinRoomResponse = new JSONObject();
            joinRoomResponse.put("success", false);
            joinRoomResponse.put("msg", "La sala a que que intenta unirse no existe");
            client.sendEvent("joinRoomResponse", joinRoomResponse.toString());
        } else {
            if (ROOM_LIST.get(roomName).getPassword() != null && !ROOM_LIST.get(roomName).getPassword().equals(roomPassword)) {
                JSONObject joinRoomResponse = new JSONObject();
                joinRoomResponse.put("success", false);
                joinRoomResponse.put("msg", "Contraseña incorrecta");
                joinRoomResponse.put("field", "room-" + roomName + "-password");
                client.sendEvent("joinRoomResponse", joinRoomResponse.toString());
            } else {
                if (StringUtils.isEmpty(nickname)) {
                    JSONObject joinRoomResponse = new JSONObject();
                    joinRoomResponse.put("success", false);
                    joinRoomResponse.put("msg", "Ingrese un apodo");
                    joinRoomResponse.put("field", "nickname");
                    client.sendEvent("joinRoomResponse", joinRoomResponse.toString());
                } else {
                    boolean anotherPlayerWithSameNickname = false;
                    for (Player player : ROOM_LIST.get(roomName).getPlayers().values()) {
                        if (player.getNickname().equalsIgnoreCase(nickname)) {
                            anotherPlayerWithSameNickname = true;
                            JSONObject joinRoomResponse = new JSONObject();
                            joinRoomResponse.put("success", false);
                            joinRoomResponse.put("msg", "Ya hay otro jugador en la sala con ese nombre");
                            joinRoomResponse.put("field", "nickname");
                            client.sendEvent("joinRoomResponse", joinRoomResponse.toString());
                        }
                    }

                    if (!anotherPlayerWithSameNickname) {
                        // If the room exists and the password / nickname are valid, proceed
                        // Create a new player
                        Player player = new Player(client, excelName, nickname, roomName, PLAYER_LIST);
                        // Add player to room
                        ROOM_LIST.get(roomName).addPlayer(client.getSessionId().toString(), player);
                        // Tell client join was successful
                        client.sendEvent("joinRoomResponse", new JSONObject().put("success", true).put("game", new JSONObject(ROOM_LIST.get(roomName).getGame()).toString()).toString());
                        broadcastServerStats();
                        gameUpdate(roomName);
                        // Server Log
                        logStats(client.getSessionId().toString() + "(" + player.getNickname() + ") JOINED '" + player.getRoomName() + "'(" + ROOM_LIST.get(player.getRoomName()).getPlayers().size() + ")");
                    }
                }
            }
        }
    }

    // Leave room function
    // Gets the client that left the room and removes them from the room's player list
    private void leaveRoom(SocketIOClient client) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the player that made the request
        Player player = PLAYER_LIST.get(client.getSessionId().toString());
        // Delete the player from the player list
        PLAYER_LIST.remove(player.getId());
        // Remove the player from their room
        ROOM_LIST.get(player.getRoomName()).getPlayers().remove(player.getId());
        gameUpdate(player.getRoomName());
        // Server Log
        logStats(client.getSessionId().toString() + "(" + player.getNickname() + ") LEFT '" + player.getRoomName() + "'(" + ROOM_LIST.get(player.getRoomName()).getPlayers().size() + ")");

        // If the number of players in the room is 0 at this point, delete the room entirely
        if (ROOM_LIST.get(player.getRoomName()).getPlayers().isEmpty()) {
            ROOM_LIST.get(player.getRoomName()).getTurnTimer().cancel();
            ROOM_LIST.remove(player.getRoomName());
            logStats("DELETE ROOM: '" + player.getRoomName() + "'");
        }

        broadcastServerStats();

        // Tell the client the action was successful
        client.sendEvent("leaveResponse", new JSONObject().put("success", true).toString());
    }

    // Disconnect function
    // Called when a client closes the browser tab
    private void socketDisconnect(SocketIOClient client) {
        // Get the player that made the request
        Player player = PLAYER_LIST.get(client.getSessionId().toString());
        // Delete the client from the socket list
        SOCKET_LIST.remove(client.getSessionId().toString());
        // Delete the player from the player list
        PLAYER_LIST.remove(client.getSessionId().toString());
        // If the player was in a room
        if (player != null) {
            // Remove the player from their room
            ROOM_LIST.get(player.getRoomName()).getPlayers().remove(client.getSessionId().toString());
            // Update everyone in the room
            gameUpdate(player.getRoomName());
            // Server Log
            logStats(client.getSessionId().toString() + "(" + player.getNickname() + ") LEFT '" + player.getRoomName() + "'(" + ROOM_LIST.get(player.getRoomName()).getPlayers().size() + ")");

            // If the number of players in the room is 0 at this point, delete the room entirely
            if (ROOM_LIST.get(player.getRoomName()).getPlayers().isEmpty()) {
                ROOM_LIST.get(player.getRoomName()).getTurnTimer().cancel();
                ROOM_LIST.remove(player.getRoomName());
                logStats("DELETE ROOM: '" + player.getRoomName() + "'");
            }
        }

        broadcastServerStats();

        // Server Log
        logStats("DISCONNECT: " + client.getSessionId().toString());
    }

    // Randomize Teams function
    // Will mix up the teams in the room that the client is in
    private void randomizeTeams(SocketIOClient client) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the room that the client called from
        String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
        // Get the players in the room
        Map<String, Player> players = ROOM_LIST.get(roomName).getPlayers();

        // Get a starting team
        int teamId = 0;

        Random r = new Random();
        // Get a list of players in the room from the dictionary
        ArrayList<String> keys = new ArrayList<>(players.keySet());
        // Init a temp array to keep track of who has already moved
        ArrayList<String> placed = new ArrayList<>();
        while (placed.size() != keys.size()) {
            // Select random player index
            String selection = keys.get(r.nextInt(keys.size()));
            // If index hasn't moved, move them
            if (!placed.contains(selection)) placed.add(selection);
        }

        // Place the players in alternating teams from the new random order
        for (String key : placed) {
            Player player = players.get(key);
            player.setTeamId(teamId++);
            if (teamId >= ROOM_LIST.get(roomName).getGame().getTeams().size()) {
                teamId = 0;
            }
        }

        gameUpdate(roomName);
    }

    // New game function
    // Gets client that requested the new game and instantiates a new game board for the room
    private void newGame(SocketIOClient client) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the room that the client called from
        String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
        // Make a new game for that room
        ROOM_LIST.get(roomName).getGame().init();

        // Make everyone in the room a guesser and tell their client the game is new
        for (String id : ROOM_LIST.get(roomName).getPlayers().keySet()) {
            PLAYER_LIST.get(id).setRole(Player.Role.GUESSER);
            SOCKET_LIST.get(id).sendEvent("switchRoleResponse", new JSONObject().put("success", true).put("role", Player.Role.GUESSER.name()).toString());
            SOCKET_LIST.get(id).sendEvent("'newGameResponse'", new JSONObject().put("success", true).toString());
        }
        // Update everyone in the room
        gameUpdate(roomName);
    }

    // Switch role function
    // Gets clients requested role and switches it
    private void switchRole(SocketIOClient client, RequestObject data) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the room that the client called from
        String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
        // Set the new role
        PLAYER_LIST.get(client.getSessionId().toString()).setRole(data.getRole());
        // Alert client
        client.sendEvent("switchRoleResponse", new JSONObject().put("success", true).put("role", data.getRole()).toString());
        // Update everyone in the room
        gameUpdate(roomName);
    }

    // Click tile function
    // Gets client and the tile they clicked and pushes that change to the rooms game
    private void clickTile(SocketIOClient client, RequestObject data) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the room that the client called from
        String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
        // If it was this players turn
        if (PLAYER_LIST.get(client.getSessionId().toString()).getTeamId().equals(ROOM_LIST.get(roomName).getGame().getTurnId())) {
            // If the game is not over
            if (!ROOM_LIST.get(roomName).getGame().isOver()) {
                // If the client is not spymaster
                if (!PLAYER_LIST.get(client.getSessionId().toString()).getRole().equals(Player.Role.SPYMASTER)) {
                    // Send the flipped tile info to the game
                    if (ROOM_LIST.get(roomName).getGame().flipTile(data.getX(), data.getY(), PLAYER_LIST.get(client.getSessionId().toString()), ROOM_LIST.get(roomName).getPlayers().values())) {
                        // Tell each player that a tile was flipped (text for on-screen game log)
                        for (String id : ROOM_LIST.get(roomName).getPlayers().keySet()) {
                            SOCKET_LIST.get(id).sendEvent("serverMsgToLog", PLAYER_LIST.get(client.getSessionId().toString()).getNickname() + " ha tocado la palabra " + ROOM_LIST.get(roomName).getGame().getBoard().getTile(data.getX(), data.getY()).getWord());
                        }
                        // Update everyone in the room
                        gameUpdate(roomName);
                    }
                }
            }
        }
    }

    // Update the gamestate for every client in the room that is passed to this function
    private void gameUpdate(String roomName) {
        // Create data package to send to the client
        JSONObject gameState = new JSONObject();
        gameState.put("room", roomName);
        gameState.put("players", ROOM_LIST.get(roomName).getPlayers());
        gameState.put("game", new JSONObject(ROOM_LIST.get(roomName).getGame()).toString());
        // For everyone in the passed room
        for (String id : ROOM_LIST.get(roomName).getPlayers().keySet()) {
            // Add specific clients team info
            gameState.put("team", PLAYER_LIST.get(id).getTeamId());
            // Pass data to the client
            SOCKET_LIST.get(id).sendEvent("gameState", gameState.toString());
        }
    }

    private void logStats(String addition) {
        int inLobby = SOCKET_LIST.size() - PLAYER_LIST.size();
        String stats = "[R:" + ROOM_LIST.size() + " P:" + PLAYER_LIST.size() + " L:" + inLobby + "] ";
        log.info(stats + addition);
    }

    /* ------------------------- OK ------------------------- */








    private void startGame(SocketIOClient client, ArrayList<String> teamColors, ArrayList<String> wordsPacksSelected, int boardSize, int wordsByTeam, int turnDuration) {
        // Prevent Crash
        if (PLAYER_LIST.get(client.getSessionId().toString()) == null) return;
        // Get the room that the client called from
        String roomName = PLAYER_LIST.get(client.getSessionId().toString()).getRoomName();
        Codewords game;
        boolean success = true;
        String msg = null;
        String field = null;
        JSONObject startGameResponse = new JSONObject();
        if (teamColors != null && teamColors.size() >= 2) {
            if (wordsPacksSelected != null && !wordsPacksSelected.isEmpty()) {
                if (validateWords(boardSize, teamColors.size(), wordsByTeam)) {
                    // Creo los teams
                    ArrayList<Team> teams = new ArrayList<>();
                    for (String teamColor : teamColors) {
                        teams.add(new Team(teamColor, coloursMap.get(teamColor)));
                    }

                    ArrayList<String> words = new ArrayList<>();
                    for (String wordPack : wordsPacksSelected) {
                        words.addAll(wordsFilesMap.get(wordPack));
                    }

                    // Inicializo el juego
                    game = ROOM_LIST.get(roomName).initGame(teams, boardSize, wordsByTeam, words, turnDuration);

                    ROOM_LIST.get(roomName).getTurnTimer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Codewords game = ROOM_LIST.get(roomName).getGame();
                            game.setTimer(game.getTimer() - 1);
                            if (game.getTimer() < 0) {
                                game.switchTurn();
                                game.setTimer(game.getTimerAmount());
                                gameUpdate(roomName);
                            }
                            for (String id : ROOM_LIST.get(roomName).getPlayers().keySet()) {
                                SOCKET_LIST.get(id).sendEvent("timerUpdate", game.getTimer());
                            }
                        }
                    }, 0, 1000);

                    startGameResponse.put("game", new JSONObject(game).toString());
                } else {
                    success = false;
                    msg = "Cantidad de palabras minimo no alcanzado. Es necesario agrandar el tablero o reducir la cantidad de palabras por equipo (Configuracion avanzada)";
                }
            } else {
                success = false;
                msg = "Debe seleccionar al menos 1 pack de palabras";
                field = "words-packs";
            }
        } else {
            success = false;
            msg = "Para jugar se deben elegir al menos 2 colores";
        }

        startGameResponse.put("success", success);
        startGameResponse.put("msg", msg);
        startGameResponse.put("field", field);

        client.sendEvent("startGameResponse", startGameResponse.toString());
        if (success) {
            // Le envio al jugador que se cambio, su nuevo teamId
            client.sendEvent("newTeamAssigned", ROOM_LIST.get(roomName).getPlayer(client).getTeamId());
            broadcastServerStats();
            gameUpdate(roomName);
        }
    }

    private boolean validateWords(int boardSize, int teams, int wordsByTeam) {
        int maxWords = boardSize * boardSize;
        return ((wordsByTeam * teams) + 1) < maxWords - 1;
    }

    private void logEvent(String eventName, SocketIOClient client) {
        String clientId = client.getSessionId().toString();
        String clientIp = client.getHandshakeData().getAddress().getHostString();
        String nickname = null;
        Room room = ROOM_LIST.get(clientId);
        if (room != null) {
            Player player = room.getPlayer(client);
            if (player != null) nickname += ", nickname=" + player.getNickname();
        }
        String msg = "[REQUEST]";
        if (eventName != null) {
            msg += " EventName='" + eventName + "'.";
        }

        msg += " Client[sessionId=" + clientId + ", ip=" + clientIp;
        if (nickname != null) {
            msg += ", nickname=" + nickname;
        }

        msg += "]";
        log.info(msg);
    }

    private void broadcastServerStats() {
        server.getBroadcastOperations().sendEvent("serverStats", new JSONObject().put("rooms", ROOM_LIST.values()).toString());
    }

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

    public static void main(String[] args) {
        String hostName = null;
        Integer port = null;
        if (args != null && args.length > 0) {
            for (int n = 0; n < args.length; n++) {
                // Es un comando
                if (args[n].startsWith("-")) {
                    switch (args[n].substring(1)) {
                        case "hostName":
                            hostName = args[++n];
                            break;
                        case "port":
                            port = Integer.parseInt(args[++n]);
                            break;
                        default:
                            System.exit(-1);
                    }
                }
            }
        }

        if (hostName == null) {
//            System.out.println("No se difinio el 'hostName' como parametro, se utiliza el default '0.0.0.0'. Ejemplo para definirlo: -hostName 192.168.0.13");
            hostName = "0.0.0.0";
        }

        if (port == null) {
            System.err.println("No se difinio el 'port'. Para hacerlo pasar como parametro: -port 9093");
            port = Integer.valueOf(System.getenv("PORT"));
        }

        new App(hostName, port);
    }
}