const socket = io('http://pacific-badlands-39971.herokuapp.com'); // Connect to server

let animationWrapper = $('.animation-wrapper');
let mainCarousel = $('.carousel');
let myTeamId;

// Sign In Page Elements
////////////////////////////////////////////////////////////////////////////
// Divs
let joinErrorMessage = $('#error-message');
let formFirstRow = $('.form-group.row').first();
// Input Fields
let excelName = $('#excel-name');
let nickname = $('#join-nickname');
let boardSize = $('#boardSize');
let wordsByTeam = $('#wordsByTeam');
let turnDuration = $('#turnDuration');
let team1color = $('#team-1-color');
let team2color = $('#team-2-color');
let team3color = $('#team-3-color');
let team4color = $('#team-4-color');
let team5color = $('#team-5-color');
let wordsPacks = $('#words-packs');
// Buttons
let joinGame = $('#join-enter');
let joinCreate = $('#join-create');
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
let playerRole = 'GUESSER';
mainCarousel.carousel({
    interval: false
});

// Helper functions
function output(message) {
    console.log(moment().format('HH:mm:ss') + ' - ' + message);
}

// Response events

socket.on('connect', function () {
    output('Client has connected to the server!');
    mainCarousel.show();
    socket.emit('isGameCreated', {});
});

socket.on('disconnect', function () {
    mainCarousel.hide();
    mainCarousel.carousel(0);
    gameBoard.empty();
    joinErrorMessage.text("");
    joinErrorMessage.hide();
    output('The client has disconnected!');
});

socket.on('gameCreatedResponse', function (data) {
    data = JSON.parse(data);
    if (data.wordPacks) {
        wordsPacks.empty();
        for (var n = 0; n < data.wordPacks.length; n++) {
            $("<option/>", {html: data.wordPacks[n]}).appendTo(wordsPacks);
        }
    }
    joinCreate.prop("disabled", data.gameCreated);
    joinGame.prop("disabled", !data.gameCreated);
    if (data.gameCreated) {
        joinGame.parent().show();
        joinCreate.hide();
        $('[config-field]').each(function () {
            $(this).hide();
        });
        formFirstRow.first().removeClass('form-group');
    } else {
        $('[config-field]').each(function () {
            $(this).show();
        });
        joinGame.parent().hide();
        joinCreate.show();
        formFirstRow.addClass('form-group');
    }
});

socket.on('newTeamAssigned', function (newTeamId) {
    myTeamId = newTeamId;
});

function drawBoard(board) {
    gameBoard.empty();
    var table = $('<table></table>').addClass("table table-borderless table-fixed");
    for (let x = 0; x < board.tiles.length; x++) {
        let tr = $("<tr></tr>").attr("id", "row-" + (x + 1));
        for (let y = 0; y < board.tiles.length; y++) {
            let btnTile = $("<button class='btn btn-block bg-white tile'>" + board.tiles[x][y].word + "</button>").on("click", function () {
                tileClicked(x, y);
            });
            tr.append($("<td></td>").append(btnTile));
        }
        table.append(tr);
    }
    gameBoard.append(table);
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

// UI Interaction with server
////////////////////////////////////////////////////////////////////////////
// User Joins Game
joinGame.on("click", function () {
    socket.emit('joinGame', {excelName: excelName.val(), nickname: nickname.val()});
});

// User Creates Game
joinCreate.on("click", function () {
    let teamColors = [];
    if (team1color.val()) teamColors.push(team1color.val());
    if (team2color.val()) teamColors.push(team2color.val());
    if (team3color.val()) teamColors.push(team3color.val());
    if (team4color.val()) teamColors.push(team4color.val());
    if (team5color.val()) teamColors.push(team5color.val());
    socket.emit('createGame', {excelName: excelName.val(), nickname: nickname.val(), boardSize: boardSize.val(), wordsByTeam: wordsByTeam.val(), turnDuration: turnDuration.val(), teamColors: teamColors, wordsPacksSelected: wordsPacks.val()});
});

// User Leaves Room
leaveGame.on("click", function () {
    socket.emit('leaveRoom', {});
});

randomizeTeams.on("click", function () {
    socket.emit('randomizeTeams', {});
});

// User Starts New Game
newGame.on("click", function () {
    socket.emit('newGame', {});
});

// User Picks a new Role
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

// Server Responses to this client
////////////////////////////////////////////////////////////////////////////
// Response to joining game
socket.on('joinResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        drawBoard(JSON.parse(data.game).board);
        mainCarousel.carousel(1);
    } else {
        joinErrorMessage.show();
        joinErrorMessage.text(data.msg);
    }
});

// Response to creating game
socket.on('createResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        drawBoard(JSON.parse(data.game).board);
        mainCarousel.carousel(1);
    } else {
        joinErrorMessage.show();
        joinErrorMessage.text(data.msg);
    }
});

// Response to leaving game
socket.on('leaveResponse', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        mainCarousel.carousel(0);
        wipeBoard();
    }
});

// Server update client timer
socket.on('timerUpdate', function (data) {
    timer.text("[" + data + "]");
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

socket.on('gameState', function (data) {
    data = JSON.parse(data);
    if (data.success) {
        var game = JSON.parse(data.game);
        // Update the games turn information
        updateInfo(game, myTeamId);
        // Update the games timer slider
        // updateTimerSlider(data.game, data.mode);
        // Update the games pack information
        // updatePacks(data.game);
        // Update the player list for the room
        updatePlayerlist(game.players, game.teams);
        // Update the board display
        updateBoard(game.board, game.teams);
    }
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

socket.on('responseGameReport', function (gameReport) {
    if (gameReport) {
        let hiddenElement = document.createElement('a');
        hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(gameReport);
        hiddenElement.target = '_blank';
        hiddenElement.download = 'Registro.csv';
        hiddenElement.click();
    }
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
    let boardSize = gameBoard.children('table').children('tr').length;
    for (let x = 0; x < boardSize; x++) {
        let row = $('#row-' + (x + 1));
        for (let y = 0; y < boardSize; y++) {
            let button = row.children().eq(y).children().first();
            // button.prop("disabled", false);
            button.prop("class", "btn btn-block bg-white tile");
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
    }

    // Disable end turn button for opposite team or spymaster
    endTurn.attr("disabled", team !== game.turnId || playerRole === 'SPYMASTER');
}

// Update the board
function updateBoard(board, teams) {
    // Add description classes to each tile depending on the tiles color
    for (let x = 0; x < board.tiles.length; x++) {
        let row = $('#row-' + (x + 1));
        for (let y = 0; y < board.tiles.length; y++) {
            let button = row.children().eq(y).children().first();
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
            let btnTile = $("<button class='btn btn-block btn-sm btn-" + teams[i].color + "'>Unirse al equipo " + teams[i].name + "</button>").on("click", function () {
                socket.emit('joinTeam', {teamId: i});
            });
            teamTable.append($("<thead></thead>").append($("<tr></tr>").append($("<td></td>").append(btnTile))));
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
stickers.push({onclick : 'en-un-cumpleañito', text : 'En un cumpleañito', keys : ''});

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