$(document).ready(function () {
    var index = parent.layer.getFrameIndex(window.name); //获取窗口索引

    parent.$("#parent_receive").text("test");
    console.log("try to send params");

    $('#childBtn').click(function () {
        window.parent.parentFun("child");
        console.log("child clicked");
        parent.$("#parent_receive").text("child clicked");
        parent.layer.close(index);
    });

    //function for parent to call
    window.parent._childFun = {
        childFun: function (param) {
            console.log("child function is called:" + param);
            return 3;
        }
    };


    function addClickListenerForElement(elementId, role) {
        $(document).on('click', elementId, role, function () {
            $("a[select='selected']").attr('select', 'false');
            $(elementId).attr('select', 'selected');

            // todo add role specified onclick action
            //$("a[role='seer']").addClass('seerClass');
            // setCardHighlight(elementId)
        });


        function setCardHighlight(elementId) {
            $(elementId).css({'pointer-events': 'none'})
                .toggleClass('card_hovered');
        }
    }

    /**
     * return selected card to parent page.
     */
    window.parent._callbackdata = {
        callBackData: function () {
            return {
                selectedName: $("a[select='selected']").attr('id'),
                selectedNickName: $("a[select='selected']").find('span').html(),
                witchAction: $(".witchItem[select='selected']").html()
            };
        }
    }

    function renderPlayersOnOverLayer(playerList, imageUrl, role) {
        $.each(playerList, function (key, player) {
            let imageUrl = '../img/unknown_icon.png';

            let card_background_div = $('<div>').addClass('card__background').css('background-image', 'url(' + imageUrl + ')');
            let card_content_div = $('<div>').addClass('card__content');
            let card__category_p = $('<p>').addClass('card__category').text('Player');
            let card__heading_span = $('<span>').addClass('card__heading').text(player.nickName);
            card_content_div.append(card__category_p).append(card__heading_span)

            let card = $('<a>').attr('id', player.name)
                .attr('role', role)
                .addClass('card')
                .append(card_background_div)
                .append(card_content_div);

            $('.card-grid').append(card);
            let cardId = '#' + player.name;
            addClickListenerForElement(cardId, role);
        });

    }

    function renderVotePageData(voteList) {
        console.log("show vote page at overlayer");
        displayFeaturePageHeadline('请投票');
        let imageUrl = '../img/unknown_icon.png';
        renderPlayersOnOverLayer(voteList, imageUrl, 'vote');
    }


    function getWitchItems(currentPlayer) {
        console.log("getWitchItems started");
        let currentPlayerName = currentPlayer.name;

        let witchItemEndpoint = '/vote/witch/' + currentPlayerName + '/witchItems'
        $.getJSON(witchItemEndpoint, function (items) {
            if (items !== undefined) {
                updatePageForWitchItems(items);
            } else {
                console.log("error finding user details, userId: " + Player.prototype.userId);
            }
            console.log("getWitchItems done");
        }).fail(function (result) {
            console.log(result);
        });
    }

    function updatePageForWitchItems(items) {
        let witchItemDiv = $('#witch_items').attr('align', 'center');
        let witch_item_table = $('<table>');
        let witch_item_row = $('<tr>');
        witch_item_table.append(witch_item_row);
        witchItemDiv.append(witch_item_table);
        $.each(items, function (key, item) {
            let itemCheckbox = $('<label />').html(item).prepend($('<input>').attr({type: 'checkbox', 'id': item}));
            let witch_item = $('<td>').addClass('witchItem').append(itemCheckbox);
            witch_item_row.append(witch_item);
        });

    }

    function renderWitchPageData(currentPlayer, witchList) {
        console.log("show witch page at overlayer");
        displayFeaturePageHeadline('女巫请选择你的目标');
        let imageUrl = '../img/unknown_icon.png';
        renderPlayersOnOverLayer(witchList, imageUrl, 'witch');

        console.log("renderWitchPageData execute")
    }

    function renderSeerPageData(seerList) {
        console.log("show seer page at overlayer");
        displayFeaturePageHeadline('预言家请选择你要查验的玩家');
        let imageUrl = '../img/unknown_icon.png';
        renderPlayersOnOverLayer(seerList, imageUrl, 'seer');

        // TODO 7/2/21
        // display checked player identity
    }

    function displayFeaturePageHeadline(content) {
        let pageTitle = $('#head_line');
        pageTitle.text(content);
        let width = $(window).width();
        let outerWidth = pageTitle.outerWidth();
        let offset = (width - outerWidth) / 2;
        console.log("width:" + offset);
        pageTitle
            .css({left: offset})
            .attr('align', 'center');
    }

    function renderWolfPageData(killList) {
        console.log("show wolf page at overlayer");
        displayFeaturePageHeadline('狼人请选择你的猎物');
        let imageUrl = '../img/unknown_icon.png';
        renderPlayersOnOverLayer(killList, imageUrl, 'wolf');
    }


    window.parent._showOverlayer = {
        show: function (role, currentPlayer, playerList) {
            // show different page view by role(vote, witch, seer, wolf)
            switch (role) {
                case 'vote':
                    renderVotePageData(playerList);
                    break;
                case 'witch':
                    renderWitchPageData(currentPlayer, playerList);
                    getWitchItems(currentPlayer);
                    break;
                case 'seer':
                    renderSeerPageData(playerList);
                    break;
                case 'wolf':
                    renderWolfPageData(playerList);
                    break;
                default:
                    console.log("can't find role:" + role);
            }
        }
    }


});