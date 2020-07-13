package ar.com.codemanes;

import ar.com.codemanes.entity.Game;
import ar.com.codemanes.entity.Player;
import ar.com.codemanes.entity.Team;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameLauncher {

    private final static Logger log = LoggerFactory.getLogger(GameLauncher.class);

    private final Map<String, String> coloursMap = Stream.of(
            new AbstractMap.SimpleEntry<>("Rojo", "danger"),
            new AbstractMap.SimpleEntry<>("Azul", "primary"),
            new AbstractMap.SimpleEntry<>("Verde", "success"),
            new AbstractMap.SimpleEntry<>("Amarillo", "warning"),
            new AbstractMap.SimpleEntry<>("Cyan", "info"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private final SocketIOServer server;
    private final Map<String, Set<String>> wordsFilesMap = new HashMap<>();
    private Game game = new Game();

    public GameLauncher(String hostName, int port, String wordsFilesFolderPath) {
        try {
            for (String wordFile : listFilesUsingDirectoryStream(wordsFilesFolderPath)) {
                Set<String> wordsInFile = new HashSet<>();
                File file = new File(wordFile);
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().equals("")) wordsInFile.add(line.trim());
                }
                String packName = file.getName().substring(0, file.getName().lastIndexOf(".")).toUpperCase();
                wordsFilesMap.put(packName, wordsInFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configuration = new Configuration();
        configuration.setHostname(hostName);
        configuration.setPort(port);

        server = new SocketIOServer(configuration);

        server.addConnectListener(client -> log.info("Se ha conectado el cliente " + client.getSessionId().toString()));

        server.addDisconnectListener(client -> {
            game.removePlayer(client);
//            if (game.getPlayers().isEmpty()) {
//                game = new Game();
//            } else {
                gameUpdate();
//            }
        });

        // LOBBY STUFF
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        server.addEventListener("isGameCreated", RequestObject.class, (client, data, ackSender) -> {
            JSONObject gameCreatedResponse = new JSONObject();
            gameCreatedResponse.put("gameCreated", !game.getPlayers().isEmpty());
            gameCreatedResponse.put("wordPacks", wordsFilesMap.keySet());
            client.sendEvent("gameCreatedResponse", gameCreatedResponse.toString());
        });

        server.addEventListener("createGame", RequestObject.class, (client, data, ackSender) -> {
            boolean success = true;
            String msg = null;
            JSONObject createResponse = new JSONObject();
            if (data.getNickname() != null && !"".equals(data.getNickname().trim())) {
                if (data.getTeamColors() != null && data.getTeamColors().size() >= 2) {
                    if (data.getWordsPacksSelected() != null && !data.getWordsPacksSelected().isEmpty()) {
                        if (validateWords(data.getBoardSize(), data.getTeamColors().size(), data.getWordsByTeam())) {
                            // Agrego al jugador que creo el juego
                            game.addPlayer(new Player(client, data.getNickname()));

                            // Creo los teams
                            ArrayList<Team> teams = new ArrayList<>();
                            for (String teamColor : data.getTeamColors()) {
                                teams.add(new Team(teamColor, coloursMap.get(teamColor)));
                            }

                            ArrayList<String> words = new ArrayList<>();
                            for (String wordPack : data.getWordsPacksSelected()) {
                                words.addAll(wordsFilesMap.get(wordPack));
                            }

                            // Inicializo el juego
                            game.initGame(teams, data.getBoardSize(), data.getWordsByTeam(), words, data.getTurnDuration(), server);

                            createResponse.put("game", new JSONObject(game).toString());
                        } else {
                            success = false;
                            msg = "Hay mas palabras que espacios en el tablero. O me agrandas el tablero o me bajas la cantidad de palabras por equipo";
                        }
                    } else {
                        success = false;
                        msg = "Poneme 1 pack de palabras! Sino no puedo prepararte el juego";
                    }
                } else {
                    success = false;
                    msg = "Poneme 2 colores de equipos! O pensas jugar solari?";
                }
            } else {
                success = false;
                msg = "Te olvidaste de tu apodo!";
            }

            createResponse.put("success", success);
            createResponse.put("msg", msg);

            client.sendEvent("createResponse", createResponse.toString());
            if (success) gameUpdate();
        });

        server.addEventListener("joinGame", RequestObject.class, (client, data, ackSender) -> {
            JSONObject joinResponse = addPlayer(client, data.getNickname());
            joinResponse.put("game", new JSONObject(game).toString());
            client.sendEvent("joinResponse", joinResponse.toString());
            gameUpdate();
        });

        // GAME STUFF
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Join Team. Se llama cuando un jugador hace click en el boton de 'Cambiar de equipo a (red, blue, green, yellow, cyan)'
        server.addEventListener("joinTeam", RequestObject.class, ((client, data, ackSender) -> {
            boolean playerChanged = false;
            Player player = game.getPlayer(client);
            if (!player.getTeamId().equals(data.getTeamId())) {
                // Si el equipo al que quiere ir no es en el que ya esta, procedo a moverlo
                player.setTeamId(data.getTeamId());
                playerChanged = true;
            }

            // Solo si lo movi de equipo aviso a todos los jugadores que hubo un cambio
            if (playerChanged) {
                // Le envio al jugador que se cambio, su nuevo teamId
                client.sendEvent("newTeamAssigned", game.getPlayer(client).getTeamId());
                gameUpdate();
            }
        }));

        // Randomize Team. Se llama cuando un jugador hace click en el boton de 'Aleatorizar equipos'
        server.addEventListener("randomizeTeams", RequestObject.class, ((client, data, ackSender) -> {
            game.randomizeTeams();
            gameUpdate();
        }));

        // New Game. Se llama cuando un jugador hace click en el boton 'Nueva partida'
        server.addEventListener("newGame", RequestObject.class, ((client, data, ackSender) -> {
            // Inicializo una nueva partida
            game.newBoard();
            // Establezco a todos los jugadores como 'Adivinos' (guessers)
            for (Player player : game.getPlayers()) player.setRole(Player.Role.GUESSER);

            server.getBroadcastOperations().sendEvent("switchRoleResponse", new JSONObject().put("success", true).put("role", Player.Role.GUESSER).toString());
            server.getBroadcastOperations().sendEvent("newGameResponse", new JSONObject().put("success", true).toString());
            gameUpdate();
        }));

        // Switch role. Se llama cuando el jugador hace click en el toogle de Spymaster
        server.addEventListener("switchRole", RequestObject.class, ((client, data, ackSender) -> {
            Player player = game.getPlayer(client);
            if (player != null) {
                player.setRole(data.getRole());
                client.sendEvent("switchRoleResponse", new JSONObject().put("success", true).put("role", player.getRole()).toString());
                gameUpdate();
            } else {
                log.error("Se intento cambiar el rol de un jugador que no se encuentra en el juego");
            }
        }));

        // End Turn. Se llama cuando un jugador hace click en 'Finalizar turno'
        server.addEventListener("endTurn", RequestObject.class, ((client, data, ackSender) -> {
            game.switchTurn();
            gameUpdate();
        }));

        server.addEventListener("clickTile", RequestObject.class, (client, data, ackSender) -> {
            Player player = game.getPlayer(client);

            // Para dar vuelta una ficha tiene:
            // - El juego no haber terminado
            // - Tengo que ser adivino (guesser)
            // - Tiene que ser mi turno
            // - Y la ficha a voltear no tiene que estar ya volteada (esto lo hace la clase Game)
            if (!game.isOver() && player.getRole().equals(Player.Role.GUESSER) && game.getTurnId().equals(player.getTeamId()) && game.flipTile(data.getX(), data.getY())) {
                gameUpdate();
            }
        });

        server.start();
    }

    private boolean validateWords(int boardSize, int teams, int wordsByTeam) {
        int maxWords = boardSize * boardSize;
        return ((wordsByTeam * teams) + 1) < maxWords - 1;
    }

    public JSONObject addPlayer(SocketIOClient client, String nickname) {
        JSONObject addPlayerResult = new JSONObject();
        if (nickname != null && !"".equals(nickname.trim())) {
            if (game.getPlayers().stream().anyMatch(playerInRoom -> playerInRoom.getNickname().equalsIgnoreCase(nickname.trim()))) {
                addPlayerResult.put("success", false);
                addPlayerResult.put("msg", "Ya hay un jugador con ese nickname");
            } else {
                addPlayerResult.put("success", true);
                game.addPlayer(new Player(client, nickname));
            }
        } else {
            addPlayerResult.put("success", false);
            addPlayerResult.put("msg", "Nickname is null or empty");
        }
        return addPlayerResult;
    }

    public static void main(String[] args) {
        String hostName = null;
        Integer port = null;
        String wordsFilesFolderPath = null;
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
                        case "wordsFilesFolderPath":
                            wordsFilesFolderPath = args[++n];
                            break;
                        default:
                            System.exit(-1);
                    }
                }
            }
        }

        if (hostName == null) {
            System.err.println("No se difinio el 'hostName'. Para hacerlo pasar como parametro: -hostName 192.168.0.13");
            System.exit(-1);
        }

        if (port == null) {
            System.err.println("No se difinio el 'port'. Para hacerlo pasar como parametro: -port 9093");
            System.exit(-1);
        }

        if (wordsFilesFolderPath == null) {
            System.err.println("No se difinio el 'wordsFilesFolderPath'. Para hacerlo pasar como parametro: -wordsFilesFolderPath C:/Users/Jagrax/Desktop/words/");
            System.exit(-1);
        }

        new GameLauncher(hostName, port, wordsFilesFolderPath);
    }

    public Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.toString());
                }
            }
        }
        return fileList;
    }

    private void gameUpdate() {
        JSONObject gameStateResponse = new JSONObject();
        gameStateResponse.put("success", true);
        gameStateResponse.put("game", new JSONObject(game).toString());
        server.getBroadcastOperations().sendEvent("gameState", gameStateResponse.toString());
    }
}
