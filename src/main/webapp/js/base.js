//const socket = io('http://localhost:9093/'); // Connect to server
const socket = io('http://pacific-badlands-39971.herokuapp.com'); // Connect to server

let gameLobby = $('#game-lobby');
let gameContainer = $('#game-container');

// Sign In Page Elements
////////////////////////////////////////////////////////////////////////////
let roomsTable = $('#rooms');
// Input Fields
let excelName = $('#excel-name');
let nickname = $('#nickname');
let roomName = $('#room-name');
let roomPassword = $('#room-password');
let boardSize = $('#boardSize');
let wordsByTeam = $('#wordsByTeam');
let turnDuration = $('#turnDuration');
let team1color = $('#team-1-color');
let team2color = $('#team-2-color');
let team3color = $('#team-3-color');
let team4color = $('#team-4-color');
let team5color = $('#team-5-color');
let wordsPacks = $('#words-packs');
//Buttons
let joinCreate = $('#join-create');
let backToLobby = $('#back-to-lobby');
let startGame = $('#start-game');
let leaveGame = $('#leave-game');

// Game Page Elements
////////////////////////////////////////////////////////////////////////////
// Divs
let gameBoard = $('#game-board');
let teamsTables = $('#teams-tables');
let gameLogTextarea = $('#game-log');
let dlgGameStatistics = $('#dlgGameStatistics');
// Buttons
let randomizeTeams = $('#randomize-teams');
let endTurn = $('#end-turn');
let newGame = $('#new-game');
let reqGameStatistics = $('#reqGameStatistics');
let switchRole = $('#role-spymaster');
let searchStickerInput = $('#searchSticker');
// UI Elements
let turnMessage = $('#status');
let timer = $('#timer');

// init
////////////////////////////////////////////////////////////////////////////
// Default game settings
let playerRole = 'GUESSER';

// Helper functions
function output(message) {
    console.log(moment().format('HH:mm:ss') + ' - ' + message);
}

socket.on('connect', function () {
    output('Client has connected to the server!');
    gameLobby.show();
    gameContainer.hide();
});

socket.on('disconnect', function () {
    gameContainer.hide();
    gameLobby.hide();
    gameBoard.empty();
    $('#config-card > div:nth-child(1)').show();
    $('#config-card > div:nth-child(2)').hide();
    $('#config-card > div:nth-child(3)').hide();
    $('#config-card > div:nth-child(4)').hide();
    $('#config-card > table:nth-child(5)').show();
    $('#config-card > div:nth-child(6)').show();
    $('#config-card > div:nth-child(7)').show();
    output('The client has disconnected!');
});

// UI Interaction with server
////////////////////////////////////////////////////////////////////////////

// User Creates Room
joinCreate.on("click", function () {
    nickname.removeClass('is-invalid');
    roomName.removeClass('is-invalid');
    socket.emit('createRoom', {roomName: roomName.val(), roomPassword: roomPassword.val(), excelName: excelName.val(), nickname: nickname.val()});
});

// User in Config Screen back to the lobby
backToLobby.on("click", function () {
    socket.emit("leaveRoom");
});

// User Start Game
startGame.on("click", function () {
    let teamColors = [];
    if (team1color.val()) teamColors.push(team1color.val());
    if (team2color.val()) teamColors.push(team2color.val());
    if (team3color.val()) teamColors.push(team3color.val());
    if (team4color.val()) teamColors.push(team4color.val());
    if (team5color.val()) teamColors.push(team5color.val());
    socket.emit('startGame', {boardSize: boardSize.val(), wordsByTeam: wordsByTeam.val(), turnDuration: turnDuration.val(), teamColors: teamColors, wordsPacksSelected: wordsPacks.val()});
});

leaveGame.on("click", function () {
    socket.emit('leaveRoom', {});
});

// User Randomizes Team
randomizeTeams.on("click", function () {
    socket.emit('randomizeTeams', {});
});

// User Starts New Game
newGame.on("click", function () {
    socket.emit('newGame', {});
});

// User Picks spymaster Role
switchRole.on("click", function () {
    socket.emit('switchRole', {role: (switchRole.is(':checked') ? 'SPYMASTER' : 'GUESSER')});
});

// User Ends Turn
endTurn.on("click", function () {
    socket.emit('endTurn', {});
});

// User Clicks Tile
function tileClicked(x, y) {
    socket.emit('clickTile', {x: x, y: y});
}

// Server Responses to this client
////////////////////////////////////////////////////////////////////////////
socket.on('serverStats', function (data) {
    data = JSON.parse(data);
    if (data.wordPacks) {
        wordsPacks.empty();
        for (var n = 0; n < data.wordPacks.length; n++) {
            $("<option/>", {html: data.wordPacks[n]}).appendTo(wordsPacks);
        }
    }
    if (data.rooms) {
        // Vacio la tabla
        roomsTable.children("tbody").empty();
        // La lleno con la info del server
        if (data.rooms.length > 0) {
            $.each(data.rooms, function (i, room) {
                var playersInRoom = 0;
                if (room.players) playersInRoom = Object.keys(room.players).length;
                var roomHasPassword = false;
                var roomSecurity;
                if (room.password && room.password.trim().length > 0) {
                    roomHasPassword = true;
                    roomSecurity = '<svg width="1.5em" height="1.5em" viewBox="0 0 16 16" class="bi bi-lock" fill="currentColor" xmlns="http://www.w3.org/2000/svg">\n' +
                        '  <path fill-rule="evenodd" d="M11.5 8h-7a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h7a1 1 0 0 0 1-1V9a1 1 0 0 0-1-1zm-7-1a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-7zm0-3a3.5 3.5 0 1 1 7 0v3h-1V4a2.5 2.5 0 0 0-5 0v3h-1V4z"/>\n' +
                        '</svg>';
                } else {
                    roomSecurity = '<svg width="1.5em" height="1.5em" viewBox="0 0 16 16" class="bi bi-unlock" fill="currentColor" xmlns="http://www.w3.org/2000/svg">\n' +
                        '  <path fill-rule="evenodd" d="M9.655 8H2.333c-.264 0-.398.068-.471.121a.73.73 0 0 0-.224.296 1.626 1.626 0 0 0-.138.59V14c0 .342.076.531.14.635.064.106.151.18.256.237a1.122 1.122 0 0 0 .436.127l.013.001h7.322c.264 0 .398-.068.471-.121a.73.73 0 0 0 .224-.296 1.627 1.627 0 0 0 .138-.59V9c0-.342-.076-.531-.14-.635a.658.658 0 0 0-.255-.237A1.122 1.122 0 0 0 9.655 8zm.012-1H2.333C.5 7 .5 9 .5 9v5c0 2 1.833 2 1.833 2h7.334c1.833 0 1.833-2 1.833-2V9c0-2-1.833-2-1.833-2zM8.5 4a3.5 3.5 0 1 1 7 0v3h-1V4a2.5 2.5 0 0 0-5 0v3h-1V4z"/>\n' +
                        '</svg>';
                }

                var joinButton = $('<button class="btn btn-sm btn-block btn-success" title="Unirse a la sala">');
                let inputPassword = "";
                if (roomHasPassword) {
                    inputPassword = $('<input type="password" id="room-' + room.name + '-password" class="form-control form-control-sm" onkeypress="">');
                    inputPassword.on("keypress", function (e) {
                        if (e.which === 13) {
                            joinButton.click();
                        }
                    });
                }

                joinButton.html('<svg width="1.5em" height="1.5em" viewBox="0 0 16 16" class="bi bi-play" fill="currentColor" xmlns="http://www.w3.org/2000/svg">\n' +
                    '  <path fill-rule="evenodd" d="M10.804 8L5 4.633v6.734L10.804 8zm.792-.696a.802.802 0 0 1 0 1.392l-6.363 3.692C4.713 12.69 4 12.345 4 11.692V4.308c0-.653.713-.998 1.233-.696l6.363 3.692z"/>\n' +
                    '</svg>');

                joinButton.on("click", function () {
                    nickname.removeClass('is-invalid');
                    roomName.removeClass('is-invalid');
                    socket.emit('joinRoom', {roomName: room.name, roomPassword: inputPassword ? inputPassword.val() : null, excelName: excelName.val(), nickname: nickname.val()});
                });

                if (!room.game) {
                    joinButton.attr("disabled", true);
                }

                var tr = $('<tr>').append(
                    $('<td class="text-center">').html(roomSecurity),
                    $('<td>').text(room.name),
                    $('<td class="text-center d-none d-xl-table-cell">').text(playersInRoom),
                    $('<td class="text-center">').append(inputPassword).append('<div class="invalid-feedback">'),
                    $('<td class="text-center">').append(joinButton)
                );
                tr.appendTo(roomsTable.children("tbody"));
            });
        }
    }
});

socket.on('joinRoomResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        drawBoard(JSON.parse(data.game).board);
        gameLobby.hide();
        gameContainer.show();
    } else {
        var input = $('#' + data.field);
        input.addClass('is-invalid');
        input.closest('div').find('.invalid-feedback').text(data.msg);
    }
});

socket.on('createRoomResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        $('#config-card > div:nth-child(1)').hide();
        $('#config-card > div:nth-child(2)').show();
        $('#config-card > div:nth-child(3)').show();
        $('#config-card > div:nth-child(4)').show();
        $('#config-card > table:nth-child(5)').hide();
        $('#config-card > div:nth-child(6)').hide();
        $('#config-card > div:nth-child(7)').hide();
    } else {
        var input = $('#' + data.field);
        input.addClass('is-invalid');
        input.closest('div').find('.invalid-feedback').text(data.msg);
    }
});

// Response to leaving game
socket.on('leaveResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        $('#config-card > div:nth-child(1)').show();
        $('#config-card > div:nth-child(2)').hide();
        $('#config-card > div:nth-child(3)').hide();
        $('#config-card > div:nth-child(4)').hide();
        $('#config-card > table:nth-child(5)').show();
        $('#config-card > div:nth-child(6)').show();
        $('#config-card > div:nth-child(7)').show();
        gameLobby.show();
        gameContainer.hide();
    }
});

// Server update client timer
socket.on('timerUpdate', function (data) {
    timer.text(data);
});

// Response to New Game
socket.on('newGameResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        switchRole.prop('checked', false);
        gameLogTextarea.val("Registro de la partida");
        wipeBoard();
    }
});

// Response to Switching Role
socket.on('switchRoleResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        playerRole = data.role;
        wipeBoard();
    }
});

// Response to gamestate update
socket.on('gameState', function (data) {
    data = JSON.parse(data);
    var game = JSON.parse(data.game);
    // Update the games turn information
    updateInfo(game, data.team);
    // Update the player list for the room
    updatePlayerlist(data.players, game.teams);
    // Update the board display
    updateBoard(game.board, game.teams);
});

socket.on('startGameResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        drawBoard(JSON.parse(data.game).board);
        gameLobby.hide();
        gameContainer.show();
    } else {
        let input = $('#' + data.field);
        if (input) {
            input.addClass('is-invalid');
            input.closest('div').find('.invalid-feedback').text(data.msg);
        }
    }
});

// Utility Functions
////////////////////////////////////////////////////////////////////////////

// Wipe all of the descriptor tile classes from each tile
function wipeBoard() {
    let boardSize = Math.sqrt(gameBoard.children('button').length);
    for (let x = 0; x < boardSize; x++) {
        for (let y = 0; y < boardSize; y++) {
            let button = $('#tile-' + x + y);
            button.prop("class", "btn bg-white tile");
        }
    }
}

// Update the game info displayed to the client
function updateInfo(game, team) {
    // Update team tiles left
    for (var n = 0; n < getTeamsSize(game.teams); n++) {
        var pendingTilesSpan = $('#score-team-' + n);
        if (pendingTilesSpan.is(":hidden")) {
            pendingTilesSpan.show();
        }
        if (!pendingTilesSpan.hasClass('text-' + game.teams[n].color)) {
            pendingTilesSpan.addClass('text-' + game.teams[n].color);
        }
        pendingTilesSpan.text(game.teams[n].pendingTiles);

        if (n > 0) {
            var pendingTilesSepSpan = $('#score-team-' + n + '-sep');
            if (pendingTilesSepSpan.is(":hidden")) {
                pendingTilesSepSpan.show();
            }
        }
    }

    // Update the turn msg or display winner(s)
    turnMessage.removeClass();
    if (game.over) {
        turnMessage.text('Ganó el equipo ' + game.teams[game.winnerId].name + '!');
        turnMessage.addClass('text-' + game.teams[game.winnerId].color);
        timer.text("");
    } else {
        turnMessage.text('Turno del ' + game.teams[game.turnId].name);
        turnMessage.addClass('text-' + game.teams[game.turnId].color);
        timer.prop('class', 'ml-3 bg-dark bg-gradient text-center text-white border border-' + game.teams[game.turnId].color);
    }

    // Disable end turn button for opposite team or spymaster
    endTurn.attr("disabled", team !== game.turnId || playerRole === 'SPYMASTER');
}

// Update the board
function updateBoard(board, teams) {
    // Add description classes to each tile depending on the tiles color
    for (let x = 0; x < board.tiles.length; x++) {
        for (let y = 0; y < board.tiles.length; y++) {
            let button = $('#tile-' + x + y);
            if (button.text() !== board.tiles[x][y].word) button.text(board.tiles[x][y].word);
            if (board.tiles[x][y].flipped || playerRole === 'SPYMASTER') {
                var color = 'btn-';
                if (board.tiles[x][y].type === 'DEATH') {
                    color += 'dark';
                } else if (board.tiles[x][y].type === 'NEUTRAL') {
                    color += 'neutral';
                } else {
                    color += teams[board.tiles[x][y].teamId].color;
                }
                button.removeClass("bg-white");
                button.addClass(color);
                // Unflipped tile mark
                if (!board.tiles[x][y].flipped) {
                    button.addClass('unflipped');
                } else {
                    button.addClass('font-weight-bold');
                    button.removeClass('unflipped');
                }
            }
        }
    }
}

// Update the player list
function updatePlayerlist(players, teams) {
    for (let i in teams) {
        let teamTable = $('#team-table-' + i);
        if (teamTable.length === 0) {
            teamTable = $("<table class='w-100'></table>").attr("id", "team-table-" + i);
            let btnTile = $("<button class='btn btn-sm btn-" + teams[i].color + "'>Unirse al equipo " + teams[i].name + "</button>").on("click", function () {
                socket.emit('joinTeam', {teamId: i});
            });
            teamTable.append($("<thead></thead>").append($("<tr></tr>").append($("<td></td>").append($("<div class='d-grid'></div>").append(btnTile)))));
            teamTable.append($("<tbody></tbody>"));
            teamsTables.append($("<div class='col'></div>").append(teamTable));
        } else {
            teamTable.children("tbody").empty();
        }
    }
    for (let i in players) {
        let playerTeamId = players[i].teamId;
        // Intento obtener la tablar del team al que pertenece el jugador
        let text;
        if (players[i].role === 'SPYMASTER') {
            text = '[' + players[i].nickname + ']';
        } else {
            text = players[i].nickname;
        }

        $('#team-table-' + playerTeamId).find("tbody").append($("<tr></tr>").append($('<td></td>').text(text)));
    }
}

function drawBoard(board) {
    // Limpio el tablero
    gameBoard.empty();
    // Recorro la matriz de palabras y las dibujo en pantalla
    for (let x = 0; x < board.tiles.length; x++) {
        for (let y = 0; y < board.tiles.length; y++) {
            let btnTile = $("<button class='btn bg-white tile' id='tile-" + x + y +"'>" + board.tiles[x][y].word + "</button>").on("click", function () {
                tileClicked(x, y);
            });
            gameBoard.append(btnTile);
        }
    }
}

function getTeamsSize(teamsMap) {
    var key, count = 0;
    for (key in teamsMap) {
        if (teamsMap.hasOwnProperty(key)) {
            count++;
        }
    }
    return count;
}