//const socket = io('http://localhost:9093/'); // Connect to server
const socket = io('https://agile-falls-86851.herokuapp.com/'); // Connect to server

let completePollContainer = $("#complete-poll-container");
let pollsList = $("#polls-list");
let pollsListContainer = $("#polls-list-container");
let createPollContainer = $("#create-poll-container");
let statisticsContainer = $("#statistics-container");
let txtPollCreatorUserEmail = $("#txtPollCreatorUserEmail");
let txtPollUrl = $("#txtPollUrl");
let btnCreateNewPoll = $("#btnCreateNewPoll");
let btnAddNewQuestion = $("#btnAddNewQuestion");
let btnCreatePoll = $("#btnCreatePoll");

socket.on('connect', function () {
    socket.emit("getAllPolls");
});

btnCreateNewPoll.on("click", function () {
    completePollContainer.hide();
    pollsListContainer.hide();
    addNewQuestion();
    createPollContainer.show();
});

btnAddNewQuestion.on("click", function () {
    addNewQuestion();
});

btnCreatePoll.on("click", function () {
    for (var n = 0; n < questionsText.length; n++) {
        questionsText[n] = $("#question-" + (n + 1) + "-text").val();
        questionsType[n] = $("#question-" + (n + 1) + "-type").val();
        if (questionsType[n] === 'checkbox' || questionsType[n] === 'radio') {
            for (var m = 0; m < options[n].length; m++) {
                options[n][m] = $("#option-" + n + (m + 1) + "-text").val();
            }
        }
    }
    socket.emit("createPoll", {pollCreatorUserEmail: txtPollCreatorUserEmail.val(), pollUrl: txtPollUrl.val(), questionsText: questionsText, questionsType: questionsType, options: options});
});

socket.on("getAllPollsResponse", function (data) {
    data = JSON.parse(data);
    if (data && data.polls.length > 0) {
        pollsList.empty();
        data.polls.forEach(poll => {
            let pollDiv = $("<div class='border rounded-3 shadow p-3'></div>");
            let videoID = new URLSearchParams(poll.url.split('?')[1]).get('v');
            let iframe = $("<div class='ratio ratio-16x9'><iframe src='https://www.youtube.com/embed/" + videoID + "' frameborder='0' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' allowfullscreen></iframe></div>");
            pollDiv.append(iframe);
            let p = $("<span class='text-muted'>Creada el " + poll.creationDate + "</span>");
            let btnStatistics = $("<button class='btn btn-primary'>Ver estadisticas</button>");
            btnStatistics.on("click", function () {
                socket.emit("getStatistics", {pollId: poll.id});
            });
            let btnCompletePoll = $("<button class='btn btn-success'>Votar</button>");
            btnCompletePoll.on("click", function () {
                socket.emit("getPoll", {pollId: poll.id});
            });
            pollDiv.append($("<div class='d-flex justify-content-between align-items-center'></div>").append(p).append(btnStatistics).append(btnCompletePoll));
            pollsList.append($("<div class='col-12 col-lg-6 col-xl-4'></div>").append(pollDiv));
        });
    }
});

socket.on("createPollResponse", function (data) {
    alert(JSON.parse(data).msg);
});

socket.on("getPollResponse", function (data) {
    data = JSON.parse(data);
    if (data) {
        completePollContainer.empty();
        let videoID = new URLSearchParams(data.poll.url.split('?')[1]).get('v');
        let iframe = $("<div class='ratio ratio-16x9'><iframe src='https://www.youtube.com/embed/" + videoID + "' frameborder='0' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' allowfullscreen></iframe></div>");
        completePollContainer.append(iframe);
        data.questions.forEach(question => {
            completePollContainer.append($("<h4>" + question.text + "</h4>"));
            if (question.type === 'text') {
                completePollContainer.append($("<input type='text' class='form-control' id='" + question.id + "'/>"));
            } else {
                data.options.forEach(option => {
                    if (option.questionId === question.id) {
                        let div = $("<div class='form-check'></div>");
                        let input = $("<input class='form-check-input' type='" + question.type + "' id='" + question.id + "-" + option.id + "' value='" + option.id + "'>");
                        if (question.type === 'radio') input.attr("name", question.id);
                        div.append(input);
                        div.append($("<label class='form-check-label' for='" + question.id + "-" + option.id + "'>" + option.text + "</label>"));
                        completePollContainer.append(div);
                    }
                });
            }
        });
        let row = $("<div class='row mt-3'></div>");
        let btnBack = $("<button class='btn btn-danger' onclick='back()'><svg width='1em' height='1em' viewBox='0 0 16 16' class='bi bi-chevron-left' fill='currentColor' xmlns='http://www.w3.org/2000/svg'><path fill-rule='evenodd' d='M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z'/></svg><span class='align-middle'>Volver</span></button>");
        let btnNewAnswer = $("<button class='btn btn-success'><svg width='1em' height='1em' viewBox='0 0 16 16' class='bi bi-check2' fill='currentColor' xmlns='http://www.w3.org/2000/svg'><path fill-rule='evenodd' d='M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z'/></svg><span class='ml-2 align-middle'>Votar</span></button>");
        const pollId = data.poll.id;
        const questionsInPoll = data.questions;
        const optionsOfQuestions = data.options;
        btnNewAnswer.on("click", function () {
            let answers = {};
            questionsInPoll.forEach(q => {
                let answer = "";
                let sep = "";
                if (q.type === 'text') {
                    answer = $("#" + q.id).val();
                } else {
                    optionsOfQuestions.forEach(o => {
                        if (o.questionId === q.id) {
                            if ($("#" + q.id + "-" + o.id).is(':checked')) {
                                answer += sep + o.id
                                sep = "|";
                            }
                        }
                    });
                }
                answers[q.id] = answer;
            });
            socket.emit("createAnswer", {answerUserEmail: "test@mail.com", pollId: pollId, answers: answers});
        });
        row.append($("<div class='col-6'></div>").append(btnBack)).append($("<div class='col-6 text-right'></div>").append(btnNewAnswer));
        completePollContainer.append(row);
        completePollContainer.show();
        pollsListContainer.hide();
        createPollContainer.hide();
    }
});

socket.on("createAnswerResponse", function (data) {
    data = JSON.parse(data);
    if (data) {
        if (data.success) {
            back();
        } else {
            alert(data.msg);
        }
    }
});

socket.on("getStatisticsResponse", function (data) {
    data = JSON.parse(data);
    if (data) {
        if (data.success) {
            statisticsContainer.empty();
            statisticsContainer.show();
            let statisticsTable = $("<table class='table'></table>");
            let statisticsTableBody = $("<tbody></tbody>");
            let questionId = -1;
            $.each(data.statistics, function (question, answers) {
                question = JSON.parse(question);
                if (question.id !== questionId) {
                    statisticsTableBody.append($("<tr><td scope='col'>Respuestas</td><td scope='col'>" + question.text + "</td></tr>"));
                }
                if (question.type === 'text') {
                    for (let i = 0; i < answers.length; i++) {
                        statisticsTableBody.append($("<tr><td colspan='2'>" + answers[i].answer + "</td></tr>"));
                    }
                } else {
                    $.each(data.options, function (optionQuestion, options) {
                        optionQuestion = JSON.parse(optionQuestion);
                        if (optionQuestion.id === question.id) {
                            for (let n = 0; n < options.length; n++) {
                                let count = 0;
                                for (let i = 0; i < answers.length; i++) {
                                    if (answers[i].answer.includes(options[n].id)) {
                                        count++;
                                    }
                                }
                                statisticsTableBody.append($("<tr><td scope='row'>" + options[n].text + "</td><td scope='col'>" + count + "</td></tr>"));
                            }
                        }
                    });
                }
                statisticsContainer.append(statisticsTable.append(statisticsTableBody));
                questionId = question.id;
                //alert( + " | Cant. de respuestas registradas: " + answers.length);
            });
        }
    }
});

let questionsText = [];
let questionsType = [];

function addNewQuestion() {
    questionsText.push("");
    questionsType.push("text");
    const questionId = questionsText.length;
    let txtQuestion = $("<input type='text' class='form-control' id='question-" + questionId + "-text' placeholder='Pregunta'/>");
    let lblQuestion = $("<label for='question-" + questionId + "-text'>Pregunta</label>");
    let selectQuestionType = $("<select class='form-select' id='question-" + questionId + "-type'></select>").on("change", function () {
        if (this.value === 'radio' || this.value === 'checkbox') {
            if (!options[questionId] || !options[questionId].length === 0) {
                addNewOption(this, questionId - 1, true);
            }
        }
    })
        .append($("<option value='text' selected>Texto libre</option>"))
        .append($("<option value='radio'>Opcion única</option>"))
        .append($("<option value='checkbox'>Múltiples opciones</option>"));
    let lblQuestionType = $("<label for='question-" + questionId + "-type'>Tipo de respuesta</label>");

    let questionRow = $("<div class='col-11 col-md-7 order-md-1'></div>").append($("<div class='form-floating'></div>").append(txtQuestion).append(lblQuestion));
    let btnDeleteQuestion = $("<button class='btn btn-danger h-1'><svg width=\"1.5em\" height=\"1.5em\" viewBox=\"0 0 16 16\" class=\"bi bi-trash\" fill=\"currentColor\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
        "  <path d=\"M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z\"/>\n" +
        "  <path fill-rule=\"evenodd\" d=\"M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z\"/>\n" +
        "</svg></button>").on("click", function () {
        $("[data-question='" + questionId + "']").hide();
    });
    let row = $("<div class='row g-3 mb-3' data-question='" + questionId + "'></div>");
    row.append(questionRow).append($("<div class='col-1 order-md-3'></div>").append($("<div class='d-grid h-100'></div>").append(btnDeleteQuestion))).append($("<div class='col-12 col-md-4 order-md-2'></div>").append($("<div class='form-floating'></div>").append(selectQuestionType).append(lblQuestionType)));
    $("#qAndA").append(row);
}

let options = [];

function addNewOption(select, questionId, addButton) {
    if (!options[questionId]) {
        options[questionId] = [];
    }
    if (options[questionId][0] === undefined) {
        options[questionId][0] = "";
    } else {
        options[questionId][options[questionId].length] = "";
    }
    const optionId = options[questionId].length;
    let txtOption = $("<input type='text' class='form-control' id='option-" + questionId + optionId + "-text' placeholder='Respuesta'/>");
    let lblOption = $("<label for='option-" + questionId + optionId + "-text'>Respuesta</label>");
    let row = $("<div class='row g-3 mt-3' data-option='" + questionId + optionId + "'></div>");
    let inputCol = $("<div class='col-11'></div>").append($("<div class='form-floating'></div>").append(txtOption).append(lblOption));
    let btnDeleteOption = $("<button class='btn btn-danger h-1'><svg width=\"1.5em\" height=\"1.5em\" viewBox=\"0 0 16 16\" class=\"bi bi-trash\" fill=\"currentColor\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
        "  <path d=\"M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z\"/>\n" +
        "  <path fill-rule=\"evenodd\" d=\"M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z\"/>\n" +
        "</svg></button>").on("click", function () {
        $("[data-option='" + questionId + optionId + "']").hide();
        $("#option-" + questionId + optionId + "-text").val("");
    });
    row.append(inputCol).append($("<div class='col-1'></div>").append($("<div class='d-grid h-100'></div>").append(btnDeleteOption)));
    const optionSectionId = "options" + questionId;
    let optionSection = $("#" + optionSectionId);
    if (!optionSection || optionSection.length === 0) {
        optionSection = $("<section id='" + optionSectionId + "'></section>");
        optionSection.insertAfter($(select).parent().parent().parent());
    }
    row.appendTo(optionSection);
    if (addButton) {
        let btnAddOption = $("<button class='btn btn-success mt-3'>Agregar otra respuesta</button>").on("click", function () {
            addNewOption(null, questionId, false);
        });
        ($("<div class='col-12 text-center'></div>").append(btnAddOption)).insertAfter(optionSection);
    }
}

function back() {
    completePollContainer.hide();
    pollsListContainer.show();
    createPollContainer.hide();
}