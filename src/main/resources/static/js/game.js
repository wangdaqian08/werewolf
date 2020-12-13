$(document).ready(function () {

    function testCode() {
        let randomStr = Math.random().toString(36).substring(4);
        $('#from').val(randomStr);
    }

    testCode();

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
            this.subscribe('/broadcast/player/status', function (messageOutput) {
                console.log("Receive broadcast message player ready status");
                showBroadcastMessageOutputForStatus(JSON.parse(messageOutput.body));
            });

            this.subscribe('/user/private/messages', function (messageOutput) {
                console.log("Receive private message");
                showMessageOutput(JSON.parse(messageOutput.body));
            });

            this.subscribe('/user/private/role', function (messageOutput) {
                console.log("Receive private message for role");
                showPrivateMessageOutputForRole(JSON.parse(messageOutput.body));
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

    $('#btn-role').click(function () {
        let $this = $(this);
        let role = $('#role');
        let isHidden = role.is(":visible");
        if (isHidden) {
            $this.text('show my role');
        } else {
            $this.text('hide my role');
        }
        role.toggle();
    })

    //make sure the nick name is set
    $("#btnConnect").click(function () {
        let nickname = $('#from').val();
        if (nickname.trim()) {
            myStomp = new StompConnector("/connect");
            myStomp.enableDebug(true);
            myStomp.connect();
        } else {
            alert("please enter a nick name")
        }

    });

    $('#btnDisconnect').click(function () {
        myStomp.disconnect();
    });


    $("#btnBroadcast").click(function () {
        myStomp.broadcastMsg();
    });

    $("#btnReady").click(function () {

        let nickname = $('#from').val();
        if (nickname.trim()) {
            let userId = $("#player-id").text();
            ready(userId, nickname);
            //update player-list
            werewolf.check_status();
            toggleReadyButton(false);
        } else {
            alert("please enter a nick name")
        }
    });


    function toggleReadyButton(show) {
        if (show) {
            $('#btnReady').show()
        } else {
            $('#btnReady').hide();
        }

    }

    function ready(userId, nickname) {
        let url = "/game/ready/" + nickname + "/" + userId;
        $.getJSON(url, function (result) {
            toggleReadyButton(false);
        }).fail(function (result) {
            console.log(result);
        });
    }

    function showBroadcastMessageOutputForStatus(messageOutput) {
        console.log(messageOutput)
        let onlinePlayersList = JSON.parse(messageOutput.message);
        let playerListDiv = $('#player-list');
        playerListDiv.empty();
        $.each(onlinePlayersList, function (key, player) {

            var readyStatusSpan;
            if (player.isReady) {
                let playerValue = player.nickName ? player.nickName : player.name;
                readyStatusSpan = "<img src='../img/circle_green_512.png' alt='readyIcon' width='15' height='15'/>&nbsp<span>" + playerValue + "</span>&nbsp &nbsp<span>Ready!</span>"
            } else {
                readyStatusSpan = "<img src='../img/circle_red_600.png' alt='notReadyIcon' width='15' height='15'/>&nbsp<span>" + player.name + "</span>&nbsp &nbsp<span>Waiting...</span>"
            }
            playerListDiv.append("<br/>").append(readyStatusSpan);
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
        console.log("broadcast message:" + messageOutput.message);
        let space = "&nbsp";
        let sender = "<span>" + messageOutput.sender + "</span>"
        let time = "<span>" + messageOutput.time + "</span>"
        let message = "<span>" + messageOutput.message + "</span>"
        let new_message = "<li>" + time + space + sender + space + message + "</li>"
        var div = document.getElementById('system-message');
        $('#system-message').append(new_message)
            .animate({scrollTop: div.scrollHeight - div.clientHeight}, 700);
    }

    function showPrivateMessageOutputForRole(messageOutput) {
        console.log(messageOutput);
        $('#role').text(messageOutput.role);
        $('#btn-role').prop('disabled', false);
    }


    function findUserId(str) {
        if (str.includes('user-name:')) {
            let userId = str.substr(51).replace(/\s+/g, '');
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
        }

        //reset stomp connection
        resetStomp() {
            let myStomp = new StompConnector();
            myStomp.enableDebug(true, myStomp);
            myStomp.disconnect();
        }
    }
    //reset stomp connection, start polling on player status
    let werewolf = new Werewolf();

});


const CONNECTED_STATUS = 'connected';
const DISCONNECTED_STATUS = 'disconnected';

//not a good woy of disable refresh page
window.onbeforeunload = function () {
    return "Dude, are you sure you want to leave?";
}
