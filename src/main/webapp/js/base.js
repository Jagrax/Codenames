//const socket = io('http://localhost:9093/'); // Connect to server
const socket = io('http://pacific-badlands-39971.herokuapp.com'); // Connect to server

let animationWrapper = $('.animation-wrapper');
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
let switchAddTime = $('#game-tile-addtime');
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
    wordsPacks.removeClass('is-invalid');
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

function sendSticker(stickerName) {
    socket.emit('sendSticker', {sticker: stickerName});
}

reqGameStatistics.on("click", function () {
    socket.emit('requestGameReportHtml', {});
});

switchAddTime.on("click", function () {
    socket.emit('switchAddTime', !!switchAddTime.is(':checked'));
});

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
                    if (inputPassword) inputPassword.removeClass('is-invalid');
                    socket.emit('joinRoom', {roomName: room.name, roomPassword: inputPassword ? inputPassword.val() : null, excelName: excelName.val(), nickname: nickname.val()});
                });

                if (!room.game) {
                    joinButton.attr("disabled", true);
                }

                var tr = $('<tr>').append(
                    $('<td class="text-center">').html(roomSecurity),
                    $('<td>').text(room.name),
                    $('<td class="text-center d-none d-xl-table-cell">').text(playersInRoom),
                    $('<td>').append(inputPassword).append('<div class="invalid-feedback">'),
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

socket.on('startGameResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        drawBoard(JSON.parse(data.game).board);
        gameLobby.hide();
        gameContainer.show();
    } else {
        if (data.field) {
            let input = $('#' + data.field);
            input.addClass('is-invalid');
            input.closest('div').find('.invalid-feedback').text(data.msg);
        } else {
            alert(data.msg);
        }
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
        gameLogTextarea.val("Registro de la partida");
        wipeBoard();
    }
});

// Response to Switching Role
socket.on('switchRoleResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        playerRole = data.role;
        switchRole.prop('checked', playerRole !== 'GUESSER');
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

socket.on('stickerResponse', function (stickerName) {
    let cssLeft = Math.floor((Math.random() * 50) + 25);
    let stickerDiv = $("<div class='position-absolute'></div>").addClass(stickerName).css('left', cssLeft + 'vw');
    animationWrapper.append(stickerDiv);
    setTimeout(function () {
        stickerDiv.remove();
    }, 10000);
});

socket.on('serverMsgToLog', function (serverMsgToLog) {
    gameLogTextarea.val(gameLogTextarea.val() + "\n" + serverMsgToLog);
    gameLogTextarea.scrollTop(gameLogTextarea[0].scrollHeight - gameLogTextarea.height());
});

socket.on('responseGameReportHtml', function (gameReportHtml) {
    if (gameReportHtml) {
        dlgGameStatistics.find("div.modal-body").html(gameReportHtml);
        dlgGameStatistics.modal('show');
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
            button.prop("class", "btn bg-white bg-gradient tile");
        }
    }
}

// Update the game info displayed to the client
function updateInfo(game, team) {
    // Update team tiles left
    for (let n = 0; n < getTeamsSize(game.teams); n++) {
        const pendingTilesSpan = $('#score-team-' + n);
        if (pendingTilesSpan.is(":hidden")) {
            pendingTilesSpan.show();
        }
        pendingTilesSpan.prop('class', 'text-' + game.teams[n].color);
        pendingTilesSpan.text(game.teams[n].pendingTiles);

        if (n > 0) {
            const pendingTilesSepSpan = $('#score-team-' + n + '-sep');
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
        timer.prop('class', 'd-none');
    } else {
        turnMessage.text('Turno del ' + game.teams[game.turnId].name);
        turnMessage.addClass('text-' + game.teams[game.turnId].color);
        timer.prop('class', 'ml-3 bg-dark bg-gradient text-center text-white border border-' + game.teams[game.turnId].color);
    }

    // Disable end turn button for opposite team or spymaster
    endTurn.attr("disabled", team !== game.turnId || playerRole === 'SPYMASTER');

    switchAddTime.prop('checked', game.useTilesWithAddTime);
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
                    button.removeClass('bg-gradient');
                } else {
                    button.addClass('font-weight-bold');
                    button.addClass('bg-gradient');
                    button.removeClass('unflipped');
                }

                if (board.tiles[x][y].addTime) {
                    button.addClass('btn-addtime');
                }
            }
        }
    }
}

// Update the player list
function updatePlayerlist(players, teams) {
    teamsTables.empty();
    for (let i in teams) {
        let teamTable = $('#team-table-' + i);
        if (teamTable.length === 0) {
            teamTable = $("<table class='w-100'></table>").attr("id", "team-table-" + i);
            let btnTile = $("<button class='btn btn-sm btn-" + teams[i].color + " bg-gradient'>Unirse al equipo " + teams[i].name + "</button>").on("click", function () {
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
    gameBoard.prop("class", "my-3 d-grid gap-3 board-size-" + board.tiles.length);
    // Recorro la matriz de palabras y las dibujo en pantalla
    for (let x = 0; x < board.tiles.length; x++) {
        for (let y = 0; y < board.tiles.length; y++) {
            let btnTile = $("<button class='btn bg-white bg-gradient tile' id='tile-" + x + y +"'>" + board.tiles[x][y].word + "</button>").on("click", function () {
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

let stickers = [];
stickers.push({onclick : 'tienen-un-chat-paralelo', text : 'Gasti: -Tienen un chat paralelo', keys : 'GASTON, GASTI, CHAT, PARALELO'});
stickers.push({onclick : 'van-a-tener-que-pensar', text : 'Mario: -Van a tener que pensar', keys : ''});
stickers.push({onclick : 'a-ver-mario-tira-un-x4', text : 'Mario: -A ver Mario, tira un X4', keys : ''});
stickers.push({onclick : 'mario-hace-trampa', text : 'Mario: -Hacer trampa', keys : ''});
stickers.push({onclick : 'entendiendonos-mucho', text : 'Pamkard: -Entendiendonos mucho...', keys : ''});
stickers.push({onclick : 'tratando-de-entender-a-mario', text : 'Sofi: -Tratando de entender a Mario', keys : ''});
stickers.push({onclick : 'ok-sofi', text : 'Ok Sofi!', keys : ''});
stickers.push({onclick : 'no-se-puede-jugar-con-todos-gritando', text : 'Martín: -no se puede jugar con todos gritando', keys : ''});
stickers.push({onclick : 'no-vamos-a-pensar', text : 'Martín: -NO, vamos a pensar', keys : ''});
stickers.push({onclick : 'no-no-no-estan-haciendo-trampa', text : 'Martín: -NO NO NO, están haciendo trampa', keys : ''});
stickers.push({onclick : 'martin-hace-trampa', text : 'Martin: -Hacer trampa', keys : ''});
stickers.push({onclick : 'amor', text : 'Martín: -AMOR', keys : ''});
stickers.push({onclick : 'que-complejo-che', text : 'Martín: -Que complejo che, que complejo', keys : ''});
stickers.push({onclick : 'vamo-a-melinear', text : 'Meli: -Vamo a melinear', keys : ''});
stickers.push({onclick : 'wat', text : 'Meli: -Wat', keys : ''});
stickers.push({onclick : 'perdon-no-lei', text : 'Meli: -Perdón, no leí', keys : ''});
stickers.push({onclick : 'mmmmm', text : 'Meli: -Mmmmm', keys : ''});
stickers.push({onclick : 'vos-tenes-que-sentir-el-juego', text : 'Javi: -Vos tenes que sentir el juego', keys : ''});
stickers.push({onclick : 'se-viene-la-negra', text : 'Javi: -Se viene la negra', keys : ''});
stickers.push({onclick : 'nene', text : 'Nene', keys : ''});
stickers.push({onclick : 'muy-buena-la-pista-super-clara-che', text : 'Anita: -Muy buena la pista, super clara che', keys : ''});
stickers.push({onclick : 'puluu-olvidense-todo', text : 'Puluu: -Olvídense todo', keys : ''});
stickers.push({onclick : 'pensa-como-mario', text : 'Pensá como Mario', keys : ''});
stickers.push({onclick : 'como-decirlo', text : 'Martín: -Cómo decirlo', keys : ''});
stickers.push({onclick : 'i-lan-olvidense-todo', text : 'I Lan: -Olvídense todo', keys : ''});
stickers.push({onclick : 'meli-javi-abriendo-puertas', text : 'Meli y Javi: -Abriendo puertas', keys : ''});
stickers.push({onclick : 'esta-complicado-che', text : 'Martín: -Está complicado che', keys : ''});
stickers.push({onclick : 'reafirmo-mi-certeza', text : 'Respeto tu duda...pero reafirmo mi certeza tocando de todos modos', keys : ''});
stickers.push({onclick : 'ganen-o-mueren', text : 'Ganen o mueren', keys : ''});
stickers.push({onclick : 'sofi-googleen', text : 'Sofi: -Googleen', keys : ''});
stickers.push({onclick : 'olvidense-de-todo-pero-no-se-olviden-todo', text : 'Olvidense de todo...pero no se olviden de todo', keys : ''});
stickers.push({onclick : 'ya-dame-la-maldita-pista', text : 'Ya dame la maldita pista', keys : ''});
stickers.push({onclick : 'joaco-pero-porque-tocas-eso', text : 'Joaco: Pero porqué tocas eso?!', keys : ''});
stickers.push({onclick : 'moria-hace-lo-que-se-te-cante', text : 'Moria: Bueno mamita, hacé lo que se te cante', keys : ''});
stickers.push({onclick : 'meli-mario-de-mierda', text : 'Meli: Mario de mierda', keys : ''});
stickers.push({onclick : 'mario-no-toques', text : 'Mario no toques', keys : ''});
stickers.push({onclick : 'puluu-podia-fallar', text : 'Puluu: -Podía fallar', keys : ''});
stickers.push({onclick : 'mario-a-ver-puede-fallar', text : 'Mario: -A ver...puede fallar', keys : ''});
stickers.push({onclick : 'javi-yo-no-fui', text : 'Javi: -Yo no fui', keys : ''});
stickers.push({onclick : 'sirius-puede-fallar', text : 'Sirius: -Puede fallar', keys : ''});
stickers.push({onclick : 'ron-podia-fallar', text : 'Ron: -Podia fallar', keys : ''});
stickers.push({onclick : 'tobi-ah-se-les-agrego-tiempo', text : 'Tobi: -Ah, se les agregó tiempo?', keys : ''});
stickers.push({onclick : 'lau-listo-o-te-quedo-alguna-palabra', text : 'Lau: -Listo? O te quedó alguna palabra por decir en la pista?', keys : ''});
stickers.push({onclick : 'tobi-pista-antes-de-los-diez', text : 'Tobi: -En algun universo me das la pista antes de los 10"?', keys : ''});
stickers.push({onclick : 'en-un-cumpleanito', text : 'En un cumpleañito', keys : ''});
stickers.push({onclick : 'gano-quien-tenia-que-ganar', text : 'Gano quien tenía que ganar', keys : ''});
stickers.push({onclick : 'pero-por-que-tocas', text : 'Pero por qué tocas si estamos debatiendo', keys : ''});
stickers.push({onclick : 'meli-cual-fue-la-pista', text : 'Meli: -Cuál fue la pista?', keys : ''});
stickers.push({onclick : 'esto-va-a-terapia', text : 'La Papa y Marian: -Esto va a terapia', keys : ''});
stickers.push({onclick : 'pistas-de-mario', text : 'Pistas de Mario', keys : ''});
stickers.push({onclick : 'jose-y-lu-no-esperaba-nada', text : 'Jose y Lu: No esperaba nada de ustedes y aun asi logran decepcionarme', keys : ''});
stickers.push({onclick : 'eso-es-lo-mejor-que-podes-dar', text : 'Eso es lo mejor que podes dar?', keys : ''});
stickers.push({onclick : 'mariel-es-lo-que-hay', text : 'Mariel: -Es lo que hay', keys : ''});
stickers.push({onclick : 'mariano-paren-todo', text : 'Mariano: PAREN TODO', keys : ''});
stickers.push({onclick : 'lu-mis-esperanzas', text : 'Lu: -Mis esperanzas...', keys : ''});
stickers.push({onclick : 'juani-ruido-de-gin', text : '*Ruido de gin', keys : ''});
stickers.push({onclick : 'jose-aca-esta-la-pista', text : 'Jose: -Aca esta la pista', keys : ''});
stickers.push({onclick : 'celeste-mmm-vs-decis', text : 'Celeste: -Mmm vs decis', keys : ''});
stickers.push({onclick : 'manu-y-al-que-no-le-gusta', text : 'Manu: -Y al que no le guste, que se joda', keys : ''});
stickers.push({onclick : 'juani-tierra-llamando-a-mario', text : 'Juani: -Tierra llamando a Mario', keys : ''});

function buildDropdown(value) {
    let contents = [];
    for (let sticker = 0; sticker < value.length; sticker++) {
        contents.push('<input type="button" class="dropdown-item" onclick="sendSticker(\'' + value[sticker].onclick + '\')" value="' + value[sticker].text + '"/>');
    }
    $('#menuItems').append(contents.join(""));

    //Hide the row that shows no items were found
    $('#empty').hide();
}

//Capture the event when user types into the search box
searchStickerInput.keyup(function () {
    filter($(this).val().trim());
});

let items = $('.dropdown-item');

//For every word entered by the user, check if the symbol starts with that word
//If it does show the symbol, else hide it
function filter(word) {
    if (items.length === 0) items = $('.dropdown-item');
    let length = items.length;
    let hidden = 0;
    for (let i = 0; i < length; i++) {
        let btnSticker = $(items.get(i));
        if (btnSticker.val().toUpperCase().includes(word.toUpperCase())) {
            btnSticker.show();
        } else {
            btnSticker.hide();
            hidden++;
        }
    }

    //If all items are hidden, show the empty view
    if (hidden === length) {
        $('#empty').show();
    } else {
        $('#empty').hide();
    }
}

//If the user clicks on any item, set the title of the button as the text of the item
$('#menuItems').on('click', '.dropdown-item', function(){
    $("#dropdownMenuButton").dropdown('toggle');
})

buildDropdown(stickers);