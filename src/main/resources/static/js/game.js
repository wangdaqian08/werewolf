$(document).ready(function () {

    function testCode() {
        let randomStr = Math.random().toString(36).substring(4);
        $('#from').val(randomStr);
    }

    testCode();


    let player;
    let StompConnector = function StompConnector(url) {
        let userId;
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
                    userId = findUserId(str);
                    var div = "<div>" + str + "</div>";
                    $("#debug").prepend(div);
                };
            }
        }

        StompConnector.prototype.subscribePrivateRoleChannel = function (url, callback) {
            this.stompClient.subscribe(url, callback);
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
            this.subscribe('/broadcast/vote/result/messages', function (messageOutput) {
                console.log("Receive broadcast vote results");
                showBroadcastMessageOutputForVoteResult(JSON.parse(messageOutput.body));
                clearVoteStatusIcon();
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
            toggleReadyButton(false);
        } else {
            alert("please enter a nick name")
        }
    });

    $('#btnVote').click(function () {
        //todo need to add toggle for this button: enable/disable
        player.getPlayerListToVote()
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
        let onlinePlayersList = messageOutput;
        let playerListDiv = $('#player-list');
        playerListDiv.empty();
        $.each(onlinePlayersList, function (key, player) {
            let vote = "<span>&nbsp</span><img class='vote' src='../img/voted.png' width='15' height='15' alt='voted'><span>&nbsp</span>"
            let playerStatusSpan;
            if (player.isReady) {

                let playerValue = player.nickName ? player.nickName : player.name;
                let playerStatus = "Ready!";
                if (!player.inGame) {
                    playerValue = playerValue.strike();
                    playerStatus = "Dead!"
                }
                let playerNameSpan = "'playerName_" + player.name.toString() + "'";
                playerStatusSpan = "<img src='../img/circle_green_512.png' alt='readyIcon' width='15' height='15'/>&nbsp<span>" + playerStatus + "</span><span>&nbsp &nbsp</span>" + "<span id=" + playerNameSpan + ">" + playerValue + "</span>"
                if (player.hasVoted) {
                    playerStatusSpan += vote
                }

            } else {
                // players not ready
                playerStatusSpan = "<img src='../img/circle_red_600.png' alt='notReadyIcon' width='15' height='15'/>&nbsp<span>Waiting...</span><span>&nbsp &nbsp</span>" + player.name
            }
            playerListDiv.append("<br/>").append(playerStatusSpan);
            highlightMyself(player);
        });
    }

    function highlightMyself(player) {
        let nickname = $('#from').val();
        if (player.nickName === nickname) {
            let playerNameSpanId = "#playerName_" + player.name.toString();
            $(playerNameSpanId).css({'background-color': '#c3c2c2'});
        }
    }

    function showBroadcastMessageOutputForVoteResult(voteResult) {

        console.log(voteResult);
        // TODO 10/1/21
        // pop up window for vote result message
        if (voteResult.isDraw) {
            console.log(voteResult.message);
            console.log(voteResult.drawList)
        } else {
            console.log(voteResult.message);
        }
    }

    function clearVoteStatusIcon() {
        $(".vote").remove();
        console.log("clearVoteStatusIcon called");
    }

    function showMessageOutput(messageOutput) {
        var response = document.getElementById('response');
        var p = document.createElement('p');
        p.style.wordWrap = 'break-word';
        p.appendChild(document.createTextNode(messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")"));
        response.appendChild(p);
        $('#player-id').text(messageOutput.name);
    }

    function scrollToLatestMsg(messageOutput) {
        let space = "&nbsp";
        let sender = "<span>" + messageOutput.sender + "</span>"
        let time = "<span>" + messageOutput.time + "</span>"
        let message = "<span>" + messageOutput.message + "</span>"
        let new_message = "<li>" + time + space + sender + space + message + "</li>"
        var div = document.getElementById('system-message');
        $('#system-message').append(new_message)
            .animate({scrollTop: div.scrollHeight - div.clientHeight}, 700);
    }

    function showBroadcastMessageOutput(messageOutput) {
        console.log("broadcast message:" + messageOutput.message);
        scrollToLatestMsg(messageOutput);
    }

    function showPrivateMessageOutputForRole(messageOutput) {
        console.log(messageOutput);
        subscribeRoleDestination(messageOutput.role);
        $('#role').text(messageOutput.role);
        $('#btn-role').prop('disabled', false);
    }

    function subscribeRoleDestination(role) {
        let role_destination;
        if (role !== 'VILLAGER') {
            role_destination = '/user/private/' + role.toLowerCase();
        }

        if (player.currentPlayer === null && role === 'WITCH') {
            player.getSelfDetails();
            console.log("self: " + JSON.stringify(player.currentPlayer));
        }
        // subscribe role destination
        myStomp.subscribePrivateRoleChannel(role_destination, function (roleMessage) {
            handleRoleActionMessage(JSON.parse(roleMessage.body));
            openLayerForAction(role.toLowerCase());
        });
    }

    function handleRoleActionMessage(roleMessage) {
        console.log('private role message' + roleMessage.message)
        scrollToLatestMsg(roleMessage);
    }


    function findUserId(str) {
        if (str.includes('user-name:')) {
            //  remove non printable characters
            let userId = str.substr(51, 54).replace(/[^\x20-\x7E]/g, '');
            $('#player-id').text(userId);
            console.log(encodeURI(userId));
            player = new Player(userId);
            return userId;
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


    class Player {


        constructor(userId) {
            this.userId = userId;
            this.voteList = null;
            this.currentPlayer = null;
        }


        getPlayerListToVote() {
            let getVotePlayersUrl = '/player/voteList/' + this.userId;
            $.getJSON(getVotePlayersUrl, function (voteList) {
                if (voteList.length !== 0) {
                    console.log(voteList);
                } else {
                    console.log("error finding vote list for user:" + Player.prototype.userId);
                }

            }).fail(function (result) {
                console.log(result);
            });
            return this.voteList;
        }

        getSelfDetails() {
            let current = null;

            let getVotePlayersUrl = '/player/details/' + this.userId;
            $.ajax({
                url: getVotePlayersUrl,
                dataType: 'json',
                async: false,
                success: function (playerDetails) {
                    if (playerDetails !== undefined) {
                        current = playerDetails;
                    } else {
                        console.log("error finding user details, userId: " + Player.prototype.userId);
                    }
                },
                error: function (msg) {
                    console.log(msg);
                }
            });
            this.currentPlayer = current;
            return current;
        }

        executeRoleAction(role, data) {
            let selectedName = data.selectedName.trim();
            let currentPlayerName = String(this.userId.trim());

            // switch role find action
            let action = findActionByRole(role);

            if (action === 'witch') {
                action = data.witchAction;
                console.log("witch action: " + action);
            }
            let actionEndpoint = '/vote/voter/' + currentPlayerName + '/player/' + selectedName + '/action/' + action;


            $.getJSON(actionEndpoint, function (actionResult) {
                let actionedPlayer = actionResult.actionList;
                // TODO 6/2/21
                // when there are 2 wolves, actionList could be 2 otherwise should be 1.
                if (actionedPlayer.length !== 0) {

                    let actionResultJson = {
                        "sender": actionResult.sender,
                        "time": actionResult.time,
                        "message": actionResult.message
                    }
                    showBroadcastMessageOutput(actionResultJson)
                    console.log(actionedPlayer);
                    return actionedPlayer;
                } else {
                    console.log("error finding user details, userId: " + Player.prototype.userId);
                }

            }).fail(function (result) {
                console.log(result);
            });
        }

    }


    function getPlayerListForFeaturedRole() {
        let actionList;
        let getPlayersForFeatureUrl = '/player/forAction/' + player.userId;
        $.ajax({
            url: getPlayersForFeatureUrl,
            dataType: 'json',
            async: false,
            success: function (playerList) {
                if (playerList.length !== 0) {
                    console.log(playerList);
                    actionList = playerList;
                } else {
                    console.log("error finding user details, userId: " + Player.prototype.userId);
                }
            },
            error: function (msg) {
                console.log(msg);
            }
        });
        return actionList;
    }

    function findActionByRole(role) {
        let action;
        switch (role) {
            case 'wolf':
                action = 'kill';
                break;
            case 'seer':
                action = 'check';
                break;
            case 'witch':
                action = 'witch';
                break;
        }
        return action;
    }


    function openLayerForAction(role) {

        let playersForAction = getPlayerListForFeaturedRole();
        idx = layer.open({
            type: 2,
            title: false,
            maxmin: true,
            btnAlign: 'c',
            btn: ['确认'],
            shadeClose: false, //点击遮罩关闭层
            area: ['540px', '420px'],
            content: 'layer',
            success: function (layero, index) {
                console.log(playersForAction);
                window._showOverlayer.show(role, player.currentPlayer, playersForAction);
            },
            yes: function (index, layero) {
                //call overlayer function
                let overLayerData = window._callbackdata.callBackData();
                console.log(overLayerData.selectedName);
                console.log(overLayerData.selectedNickName);
                layer.close(index);
                player.executeRoleAction(role, overLayerData);
            }
        });


    }

    var idx;
    $('#layer').on('click', function () {
        console.log("layer button clicked");

        console.log(player.getSelfDetails());

        let playersForAction = getPlayerListForFeaturedRole();
        idx = layer.open({
            type: 2,
            title: false,
            maxmin: true,
            btnAlign: 'c',
            btn: ['确认'],
            shadeClose: false, //点击遮罩关闭层
            area: ['540px', '420px'],
            content: 'layer',
            success: function (layero, index) {
                console.log(playersForAction);
                var width = $(window).width();
                window._showOverlayer.show('wolf', playersForAction);

            },
            yes: function (index, layero) {
                //call overlayer function
                let selectedPlayer = window._callbackdata.callBackData();
                console.log(selectedPlayer.selectedName);
                console.log(selectedPlayer.selectedNickName);
                layer.close(index);
                player.executeRoleAction('wolf', selectedPlayer);
            }
        });

    });

    window.parentFun = function (ddd) {
        console.log("child called" + ddd);
    }

});


const CONNECTED_STATUS = 'connected';
const DISCONNECTED_STATUS = 'disconnected';

//not a good woy of disable refresh page
window.onbeforeunload = function () {
    return "Dude, are you sure you want to leave?";
}
