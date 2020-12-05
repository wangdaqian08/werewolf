$(document).ready(function (){

    $('#btnSendMessage').click(function (){
        sendMessage();
    });

    $("#btnConnect").click(function () {
        connect();
    });

    $('#btnDisconnect').click(function (){
        disconnect();
    });


    $("#btnBroadcast").click(function () {
        sendBroadcastMessage();
    });

    $("#btnReady").click(function () {
        var userId = $("#player-id").text();
        ready(userId);
        //update player-list
        check_status();
        toggleReadyButton(false);
    });


    function toggleReadyButton(show) {
        if (show) {
            $('#btnReady').show()
        } else {
            $('#btnReady').hide();
        }

    }

    function ready(userId) {
        var url = "/game/ready/" + userId;
        $.getJSON(url, function (result) {
            console.log(result);
            toggleReadyButton(false);
        });
    }



    function sendBroadcastMessage() {
        let nickname = $('#from').val();
        stompClient.send("/app/chat/broadcast", {}, JSON.stringify({'from': nickname, 'text': "Hello"}));

    }


    function showBroadcastMessageOutput(messageOutput) {
        $('#player-list').text(messageOutput.name);

    }


    var error_callback = function (error) {
        setConnectStatus(DISCONNECTED_STATUS);
        // display the error's message header:
        console.log("error:" + error.headers);
    };

    function connect() {

        var socket = new SockJS('/connect');
        stompClient = Stomp.over(socket);
        stompClient.heartbeat.outgoing = 20000;

        stompClient.debug = function (str) {
            // append the debug log to a #debug div somewhere in the page using JQuery:
            findUserId(str);
            var div = "<div>"+ str + "</div>";

            $("#debug").append(div);
        };

        stompClient.connect({}, function (frame) {

            setConnected(true);
            setConnectStatus(CONNECTED_STATUS);

            toggleReadyButton(true);

            console.log('Connected: ' + frame);
            stompClient.subscribe('/topic/messages', function (messageOutput) {
                console.log("Receive boradcast message:" + JSON.parse(messageOutput.body));
                showBroadcastMessageOutput(JSON.parse(messageOutput.body));
            });

            stompClient.subscribe('/user/topic/private/messages', function (messageOutput) {

                console.log("Receive private message:" + JSON.parse(messageOutput.body));
                showMessageOutput(JSON.parse(messageOutput.body));
            });

        }, error_callback);
    }


    function findUserId(str){
        if(str.includes('user-name:')){
            var userId = str.substr(51);
            $('#player-id').text(userId);
        }
    }

    function sendMessage() {

        var from = document.getElementById('from').value;
        var text = document.getElementById('text').value;
        stompClient.send("/app/chat/private", {}, JSON.stringify({'from': from, 'text': text}));
    }


    function showMessageOutput(messageOutput) {
        var response = document.getElementById('response');
        var p = document.createElement('p');
        p.style.wordWrap = 'break-word';
        p.appendChild(document.createTextNode(messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")"));
        response.appendChild(p);
        $('#player-id').text(messageOutput.name);
    }



    setInterval("check_players()", 3000);


});

function check_status() {
    var url = "/game/player/status";
    $.getJSON(url, function (onlinePlayersList) {


        console.log(onlinePlayersList)
        var playerListDiv = $('#player-list');
        playerListDiv.empty();
        $.each(onlinePlayersList, function(key,player) {

            console.log(player.name+" "+player.ready);

            var readyStatusSpan;
            if(player.ready){
                readyStatusSpan = "<img src='../img/circle_green_512.png' alt='readyIcon' width='15' height='15'/>&nbsp<span>"+player.name+"</span>&nbsp &nbsp<span>Ready!</span>"
            }else {
                readyStatusSpan = "<img src='../img/circle_red_600.png' alt='notReadyIcon' width='15' height='15'/>&nbsp<span>"+player.name+"</span>&nbsp &nbsp<span>Waiting...</span>"
            }
            playerListDiv.append("<br/>").append(readyStatusSpan);
        });
    });
}

function check_players() {
    setTimeout("check_status()")
}


function disconnect() {

    if(stompClient != null) {
        stompClient.disconnect();
        stompClient.close();
    }

    setConnected(false);
    console.log("Disconnected");
}


const CONNECTED_STATUS = 'connected';
const DISCONNECTED_STATUS = 'disconnected';

var stompClient = null;



/**
 * update connect status ui
 * @param status
 */
function setConnectStatus(status) {
    document.getElementById('connect-status').className = status
    document.getElementById('connect-status').innerHTML = status;
}

function setConnected(connected) {

    document.getElementById('btnConnect').disabled = connected;
    document.getElementById('btnDisconnect').disabled = !connected;
    document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
    document.getElementById('response').innerHTML = '';
}

window.onload = disconnect();

