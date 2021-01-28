let jazzSteps = ["Apple Jack",
"Black Bottom",
"Boogie Back",
"Boogie Forward",
"Boogie Forward (clap behind)",
"Boogie Step",
"Box Step",
"Break (Shim Sham)",
"Break (Half break)",
"Break-a-leg",
"Cakewalk / Camelwalk",
"Camelwalk (backward)",
"Cartwheel",
"Charleston (1920s)",
"Charleston (Jump)",
"Charleston (Kansas City)",
"Charleston (Squat)",
"Cool Breeze (1953) aka ‘Grapevine'",
"Cool Breeze (1970s)",
"Crazy Legs",
"Crossovers",
"Cross step",
"Eagle Slide",
"Fall off the Log",
"Fishtails",
"Gaze Afar",
"Grinds",
"Hallelujahs aka ‘Shouts'",
"Heels (Digs)",
"Heels (Russian wing)",
"Hitchhiker",
"Itches",
"Jig Walks",
"Knee Slaps",
"Lockstep",
"Lowdown",
"Mess Around",
"Mooch aka ‘Four Corners'",
"Opposites aka ‘Push n Pull'",
"Paddle Turn",
"Pecks",
"Pecks (Jackson 5)",
"Ride the Pony",
"Rocks",
"Rusty Dusty",
"Sailor Kicks",
"Savoy Kicks",
"Scarecrow",
"Scissor Kicks",
"Shim Sham",
"Shish-ka-boom-ba",
"Shoe Shine",
"Shorty George",
"Skate",
"Skip up",
"Slip Slops",
"Spank the Baby",
"Stomp off",
"Struttin'",
"Suzie Q",
"Tabby the Cat",
"Tacky Annie",
"Tick Tocks",
"Truckin'",
"Turkey Trots"];

const songs = [
	{
		"name": "Jive at Five",
		"filename": "songs/Jive at Five 180.mp3",
		"bpm": 183,
		"startAt": 4300,
		"artist": "Count Basie and His Orchestra"
	},
	{
		"name": "Echoes of Swing",
		"filename": "songs/Echoes_Of_Swing_-_5_Yacht_Club_Swing.mp3",
		"bpm": 162,
		"startAt": 2450,
		"artist": "Yacht Club Swing"
	}
];

const minute = 60;
let bps = 0;
let currentBeat = 0;
let txtCount = $("#txtCount");
let stepBox = $("#step-box");
let txtStepsEightCountsRefresh = $("#stepsEightCountsRefresh");
let eightCounts = 1;

var countFunction;

function startCount(bpm) {
	if (bpm !== null) {
		stepBox.html(jazzSteps[Math.floor(Math.random() * jazzSteps.length)]);
		let stepsEightCountsRefresh = txtStepsEightCountsRefresh.val();
		if (!stepsEightCountsRefresh) stepsEightCountsRefresh = 1;
		stepsEightCountsRefresh = parseInt(stepsEightCountsRefresh);
		bps = bpm / minute;
		if (bps !== 0) {
			let interval = 1000/bps;
			countFunction = setInterval(function() {
				currentBeat++;
				if (currentBeat > 8) {
					eightCounts++;
					currentBeat = 1;
				}
				if (eightCounts === stepsEightCountsRefresh && currentBeat === 7) showNextJazzStep();
				txtCount.val(currentBeat);
			}, interval);
		}
	}
}

function stopCount() {
	clearInterval(countFunction);
	txtCount.val("")
	currentBeat = 0;
}

function showNextJazzStep() {
	stepBox.html(jazzSteps[Math.floor(Math.random() * jazzSteps.length)]);
	eightCounts = 0;
}

const songsRow = $("#songs-row");
$(document).ready(function() {
	$.each(songs, function(index, song) {
		let src = $("<source src='" + song.filename + "' type='audio/mpeg'>");
		let audio = $("<audio controls id='song-" + index + "'>").append(src);
		// Funcion que arranca a contar y muestra los pasos al darle Play a la cancion
		audio.on("play", function() {setTimeout(function() {startCount(song.bpm)}, song.startAt)});
		// Funcion que deja de contar y esconde el paso cuando la cancion se pause o se termina
		audio.on("ended", function() {stopCount()});
		let songName = $("<h3>" + song.name + "</h3>");
		let songArtist = "<b>Artista:</b> " + song.artist + "<br>";
		let songBPM = "<b>BPM:</b> " + song.bpm;
		let col = $("<div class='col'></div>").append(songName).append($("<p></p>").append(songArtist).append(songBPM)).append(audio);
		songsRow.append(col);
	});
});