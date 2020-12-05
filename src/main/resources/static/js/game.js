$(document).ready(function () {

    let StompConnector = function StompConnector(url) {

        this.broadcastMsgUrl = "/app/chat/broadcast";
        this.privateMsgUrl = "/app/chat/private";
        if (url) {
            this.url = url
            this.socket = new SockJS(url);
            this.stompClient = Stomp.over(this.socket);
            this.stompClient.heartbeat.outgoing = 20000;
        }

        StompConnector.prototype.enableDebug = function (isEnable) {
            if (isEnable && this.stompClient) {
                this.stompClient.debug = function (str) {
                    // append the debug log to a #debug div somewhere in the page using JQuery:
                    findUserId(str);
                    var div = "<div>" + str + "</div>";

                    $("#debug").append(div);
                };
            }
        }

        StompConnector.prototype.connect = function () {

            this.stompClient.connect({}, this.successCallback, this.errorCallback);

        }

        StompConnector.prototype.errorCallback = function (error) {
            setConnectStatus(DISCONNECTED_STATUS);
            // display the error's message header:
            console.log("error:" + error.headers);
        };

        StompConnector.prototype.successCallback = function (frame) {
            setConnected(true);
            setConnectStatus(CONNECTED_STATUS);

            toggleReadyButton(true);

            console.log('Connected: ' + frame);
            this.subscribe('/broadcast/messages', function (messageOutput) {
                console.log("Receive broadcast message");
                showBroadcastMessageOutput(JSON.parse(messageOutput.body));
            });

            this.subscribe('/user/private/messages', function (messageOutput) {

                console.log("Receive private message");
                showMessageOutput(JSON.parse(messageOutput.body));
            });
        }

        StompConnector.prototype.privateMsg = function () {
            var from = document.getElementById('from').value;
            var text = document.getElementById('text').value;
            this.stompClient.send(this.privateMsgUrl, {}, JSON.stringify({'from': from, 'text': text}));
        }

        StompConnector.prototype.broadcastMsg = function () {
            let nickname = $('#from').val();
            this.stompClient.send(this.broadcastMsgUrl, {}, JSON.stringify({'from': nickname, 'text': "hello"}));
        }


        StompConnector.prototype.disconnect = function () {
            if (this.stompClient) {
                this.stompClient.disconnect();
            }

            setConnected(false);
            console.log("Disconnected");
        }
    }


    let myStomp = null;
    $('#btnSendMessage').click(function () {
        myStomp.privateMsg();
    });

    $("#btnConnect").click(function () {
        myStomp = new StompConnector("/connect");
        myStomp.enableDebug(true);
        myStomp.connect();
    });

    $('#btnDisconnect').click(function () {
        myStomp.disconnect();
    });


    $("#btnBroadcast").click(function () {
        myStomp.broadcastMsg();
    });

    $("#btnReady").click(function () {
        var userId = $("#player-id").text();
        ready(userId);
        //update player-list
        werewolf.check_status();
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

    function showMessageOutput(messageOutput) {
        var response = document.getElementById('response');
        var p = document.createElement('p');
        p.style.wordWrap = 'break-word';
        p.appendChild(document.createTextNode(messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")"));
        response.appendChild(p);
        $('#player-id').text(messageOutput.name);
    }

    function showBroadcastMessageOutput(messageOutput) {
        $('#player-list').text(messageOutput.name);
    }


    function findUserId(str) {
        if (str.includes('user-name:')) {
            var userId = str.substr(51);
            $('#player-id').text(userId);
        }
    }

    function setConnectStatus(status) {
        $("#connect-status").toggleClass(status).html(status);
    }

    function setConnected(connected) {

        document.getElementById('btnConnect').disabled = connected;
        document.getElementById('btnDisconnect').disabled = !connected;
        document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
        document.getElementById('response').innerHTML = '';
    }


    let Werewolf = class Werewolf {
        constructor() {
            this.resetStomp();
            setInterval(this.check_status, 3000)
        }

        //reset stomp connection
        resetStomp() {
            let myStomp = new StompConnector();
            myStomp.enableDebug(true, myStomp);
            myStomp.disconnect();
        }

        //get request check player status
        check_status() {
            var url = "/game/player/status";
            $.getJSON(url, function (onlinePlayersList) {


                console.log(onlinePlayersList)
                var playerListDiv = $('#player-list');
                playerListDiv.empty();
                $.each(onlinePlayersList, function (key, player) {

                    console.log(player.name + " " + player.ready);

                    var readyStatusSpan;
                    if (player.ready) {
                        readyStatusSpan = "<img src='../img/circle_green_512.png' alt='readyIcon' width='15' height='15'/>&nbsp<span>" + player.name + "</span>&nbsp &nbsp<span>Ready!</span>"
                    } else {
                        readyStatusSpan = "<img src='../img/circle_red_600.png' alt='notReadyIcon' width='15' height='15'/>&nbsp<span>" + player.name + "</span>&nbsp &nbsp<span>Waiting...</span>"
                    }
                    playerListDiv.append("<br/>").append(readyStatusSpan);
                });
            });
        }
    }
    //,reset stomp connection, start polling on player status
    let werewolf = new Werewolf();

});


const CONNECTED_STATUS = 'connected';
const DISCONNECTED_STATUS = 'disconnected';
